package br.com.fitpersonal.app.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import br.com.fitpersonal.app.BuildConfig
import br.com.fitpersonal.app.data.crypto.CifraBackup
import br.com.fitpersonal.app.data.crypto.SegredoLocal
import br.com.fitpersonal.app.data.crypto.paraHex
import br.com.fitpersonal.app.data.local.BancoCriptografado
import br.com.fitpersonal.app.data.local.DB_NOME
import br.com.fitpersonal.app.data.local.DB_VERSAO
import br.com.fitpersonal.app.data.local.FitDatabase
import br.com.fitpersonal.app.data.repo.PerfilFonte
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

class BackupInvalidoException(message: String) : IOException(message)

/** Não há senha de backup configurada: backups só são criados com senha (dados de saúde). */
class SenhaAusenteException :
    IOException("Defina uma senha para os backups em Perfil e backup antes de criar um backup.")

/** O arquivo é um backup cifrado e a senha não foi informada. */
class SenhaNecessariaException : IOException("Este backup é protegido por senha.")

data class ResumoBackup(val arquivos: Int, val bytes: Long)

/**
 * Backup completo, sempre CIFRADO (AES-256-GCM, chave derivada da senha do backup):
 *   manifest.json · rje_avaliacoes.db (cópia legível) · perfil.json · pasta fotos · pasta branding
 * dentro de um .zip, dentro do envelope de [CifraBackup]. Extensão: `.rjebak`.
 *
 * O banco do aparelho é SQLCipher com chave local; por isso o backup leva uma cópia legível (sem a
 * chave do aparelho), protegida pela senha — assim ele restaura em qualquer aparelho.
 *
 * Restauração: decifra, valida, converte o banco recebido para SQLCipher com a chave deste aparelho,
 * tira um snapshot de segurança do estado atual e só então troca banco e imagens.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: FitDatabase,
    private val perfilRepo: PerfilFonte,
    private val segredos: SegredoLocal,
) {
    companion object {
        const val EXTENSAO = ".rjebak"
        private const val MANIFEST = "manifest.json"
        private const val DB_ENTRY = "rje_avaliacoes.db"
        private const val PERFIL = "perfil.json"
        private const val DESKTOP_DB = "data/rje_avaliacoes.db"
        private const val SQLITE_HEADER = "SQLite format 3\u0000"
        private const val MANTER_SNAPSHOTS = 3
        private const val SEGREDO_SENHA = "senha_backup"
        private const val SEGREDO_SNAPSHOT = "senha_snapshot"
    }

    /** Serializa backups (manual, automático e snapshots): todos usam o mesmo arquivo temporário do banco. */
    private val trava = Mutex()

    fun nomeSugerido(): String =
        "fitpersonal_backup_${SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())}$EXTENSAO"

    // ───────────── senha do backup ─────────────

    fun temSenha(): Boolean = segredos.ler(SEGREDO_SENHA) != null

    /** A senha fica no cofre do Keystore para permitir o backup automático; não pode ser lida de volta pela UI. */
    fun definirSenha(senha: CharArray) {
        segredos.guardar(SEGREDO_SENHA, String(senha).toByteArray(Charsets.UTF_8))
    }

    private fun senhaSalva(): CharArray? =
        segredos.ler(SEGREDO_SENHA)?.let { String(it, Charsets.UTF_8).toCharArray() }

    /** Snapshots internos de segurança usam um segredo do aparelho (o usuário nunca precisa digitá-lo). */
    private fun senhaSnapshot(): CharArray {
        val bytes = try {
            segredos.obterOuCriarAleatorio(SEGREDO_SNAPSHOT, 32)
        } catch (e: IllegalStateException) {
            // Segredo ilegível: snapshots antigos já não abrem de qualquer forma; gera outro para não bloquear backups.
            segredos.apagar(SEGREDO_SNAPSHOT)
            segredos.obterOuCriarAleatorio(SEGREDO_SNAPSHOT, 32)
        }
        return bytes.paraHex().toCharArray()
    }

    // ───────────── exportar ─────────────

    /** @throws SenhaAusenteException se [senha] é null e não há senha salva. */
    suspend fun exportar(
        destino: Uri,
        senha: CharArray? = null,
        /** Inclui os vídeos próprios dos exercícios (podem somar centenas de MB: use no backup manual). */
        incluirVideos: Boolean = false,
    ): ResumoBackup = withContext(Dispatchers.IO) {
        val s = senha ?: senhaSalva() ?: throw SenhaAusenteException()
        val saida = ctx.contentResolver.openOutputStream(destino, "w")
            ?: throw IOException("Não foi possível gravar no local escolhido")
        saida.use { trava.withLock { escreverCifrado(it, s, incluirVideos) } }
    }

    private suspend fun escreverCifrado(out: OutputStream, senha: CharArray, incluirVideos: Boolean = false): ResumoBackup =
        escreverZip(CifraBackup.cifrando(out, senha), incluirVideos)

    private suspend fun escreverZip(out: OutputStream, incluirVideos: Boolean = false): ResumoBackup {
        val copiaDb = File(ctx.cacheDir, "backup_tmp.db")
        val perfil = perfilRepo.perfil.first()
        var arquivos = 0
        var bytes = 0L
        try {
            // Cópia legível do banco (o backup é protegido pela senha, não pela chave do aparelho).
            BancoCriptografado.exportarLegivel(db.openHelper.writableDatabase, copiaDb)

            ZipOutputStream(out.buffered()).use { zip ->
                fun entrada(nome: String, escrever: (ZipOutputStream) -> Unit) {
                    zip.putNextEntry(ZipEntry(nome)); escrever(zip); zip.closeEntry(); arquivos++
                }
                entrada(MANIFEST) {
                    it.write(
                        JSONObject().put("app", "fitpersonal-android").put("versaoApp", BuildConfig.VERSION_NAME)
                            .put("versaoBanco", DB_VERSAO).put("criadoEm", System.currentTimeMillis())
                            .toString(2).toByteArray()
                    )
                }
                entrada(DB_ENTRY) { copiaDb.inputStream().use { i -> bytes += i.copyTo(it) } }
                entrada(PERFIL) { it.write(perfilParaJson(perfil).toString(2).toByteArray()) }
                val raizes = if (incluirVideos) listOf("fotos", "branding", "videos") else listOf("fotos", "branding")
                for (raiz in raizes) {
                    val pasta = File(ctx.filesDir, raiz)
                    if (!pasta.exists()) continue
                    pasta.walkTopDown().filter { it.isFile }.forEach { f ->
                        val nome = "$raiz/" + f.relativeTo(pasta).invariantSeparatorsPath
                        entrada(nome) { f.inputStream().use { i -> bytes += i.copyTo(it) } }
                    }
                }
            }
        } finally {
            copiaDb.delete()   // não deixar cópia legível do banco no cache
        }
        return ResumoBackup(arquivos, bytes)
    }

    /** Guarda o estado atual em filesDir/backups (cifrado; mantém os 3 mais recentes). */
    suspend fun snapshotDeSeguranca(prefixo: String): File = withContext(Dispatchers.IO) {
        val pasta = File(ctx.filesDir, "backups").apply { mkdirs() }
        val snap = File(pasta, "${prefixo}_${System.currentTimeMillis()}$EXTENSAO")
        snap.outputStream().use { trava.withLock { escreverCifrado(it, senhaSnapshot()) } }
        pasta.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MANTER_SNAPSHOTS)?.forEach { it.delete() }
        snap
    }

    // ───────────── restaurar ─────────────

    /** true se o arquivo é um backup cifrado (então a senha é necessária para restaurar). */
    suspend fun ehCifrado(origem: Uri): Boolean = withContext(Dispatchers.IO) {
        ctx.contentResolver.openInputStream(origem)?.use { i ->
            val cab = ByteArray(4)
            i.read(cab) == 4 && CifraBackup.ehCifrado(cab)
        } ?: false
    }

    /**
     * @param senha obrigatória quando o arquivo é cifrado.
     * @throws SenhaNecessariaException, [br.com.fitpersonal.app.data.crypto.SenhaIncorretaException],
     *         [BackupInvalidoException]
     */
    suspend fun restaurar(origem: Uri, senha: CharArray? = null) = withContext(Dispatchers.IO) {
        val tmp = File(ctx.cacheDir, "restore_tmp").apply { deleteRecursively(); mkdirs() }
        try {
            val achouDesktop = extrair(origem, tmp, senha)

            val dbExtraido = File(tmp, DB_ENTRY)
            if (!dbExtraido.exists()) {
                throw BackupInvalidoException(
                    if (achouDesktop) "Este é um backup do aplicativo desktop. Use \"Importar backup do desktop\"."
                    else "Arquivo inválido: não contém o banco de dados do FitPersonal."
                )
            }
            validarBanco(dbExtraido)

            // Converte para SQLCipher ANTES de tocar no banco atual: se falhar, nada foi alterado.
            val atual = ctx.getDatabasePath(DB_NOME)
            val novoBanco = File(atual.path + ".restore")
            BancoCriptografado.converterParaCriptografado(
                dbExtraido, novoBanco, BancoCriptografado.senhaBanco(segredos),
            )

            // Rede de segurança: estado atual guardado antes de qualquer alteração.
            snapshotDeSeguranca("pre_restauracao")

            // Troca
            db.close()
            listOf(atual, File(atual.path + "-wal"), File(atual.path + "-shm")).forEach { it.delete() }
            if (!novoBanco.renameTo(atual)) {
                novoBanco.copyTo(atual, overwrite = true)
                novoBanco.delete()
            }

            for (raiz in listOf("fotos", "branding")) {
                File(ctx.filesDir, raiz).deleteRecursively()
                val novo = File(tmp, raiz)
                if (novo.exists()) novo.copyRecursively(File(ctx.filesDir, raiz), overwrite = true)
            }

            // Vídeos só são trocados quando o backup os traz (senão, os vídeos deste aparelho ficam como estão).
            val videosDoBackup = File(tmp, "videos")
            if (videosDoBackup.exists()) {
                File(ctx.filesDir, "videos").deleteRecursively()
                videosDoBackup.copyRecursively(File(ctx.filesDir, "videos"), overwrite = true)
            }

            val perfilJson = File(tmp, PERFIL)
            if (perfilJson.exists()) {
                perfilRepo.salvar(jsonParaPerfil(JSONObject(perfilJson.readText())))
            }
        } finally {
            tmp.deleteRecursively()   // inclui a cópia legível do banco
        }
    }

    /** Restaura usando a senha de backup guardada neste aparelho (sincronização entre aparelhos). */
    suspend fun restaurarComSenhaSalva(origem: Uri) {
        val s = senhaSalva() ?: throw SenhaAusenteException()
        restaurar(origem, s)
    }

    /** Extrai só entradas conhecidas, bloqueando caminhos fora da pasta (zip-slip). Retorna true se for backup do desktop. */
    private fun extrair(origem: Uri, destino: File, senha: CharArray?): Boolean {
        var desktop = false
        val base = destino.canonicalPath + File.separator
        val bruto = BufferedInputStream(
            ctx.contentResolver.openInputStream(origem) ?: throw IOException("Não foi possível abrir o arquivo")
        )
        try {
            bruto.mark(16)
            val cab = ByteArray(4)
            val lidos = bruto.read(cab)
            bruto.reset()

            val fonte = if (lidos == 4 && CifraBackup.ehCifrado(cab)) {
                CifraBackup.decifrando(bruto, senha ?: throw SenhaNecessariaException())
            } else bruto

            ZipInputStream(fonte).use { zis ->
                while (true) {
                    val e = zis.nextEntry ?: break
                    val nome = e.name.replace('\\', '/')
                    if (nome == DESKTOP_DB) desktop = true
                    val aceita = nome == MANIFEST || nome == DB_ENTRY || nome == PERFIL ||
                        nome.startsWith("fotos/") || nome.startsWith("branding/") || nome.startsWith("videos/")
                    if (e.isDirectory || !aceita) continue
                    val alvo = File(destino, nome)
                    if (!alvo.canonicalPath.startsWith(base)) throw BackupInvalidoException("Arquivo de backup inválido.")
                    alvo.parentFile?.mkdirs()
                    alvo.outputStream().use { zis.copyTo(it) }
                }
            }
        } catch (e: ZipException) {
            throw BackupInvalidoException("O arquivo escolhido não é um backup válido.")
        } finally {
            bruto.close()
        }
        return desktop
    }

    private fun validarBanco(f: File) {
        val cabecalho = f.inputStream().use { i -> ByteArray(16).also { i.read(it) } }.toString(Charsets.ISO_8859_1)
        if (cabecalho != SQLITE_HEADER) throw BackupInvalidoException("O banco de dados do backup está corrompido.")
        val versao = try {
            SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        } catch (e: Exception) {
            throw BackupInvalidoException("Não foi possível ler o banco do backup.")
        }
        if (versao > DB_VERSAO) {
            throw BackupInvalidoException("Backup criado por uma versão mais nova do app. Atualize o aplicativo.")
        }
    }

    // ───────────── perfil <-> json ─────────────

    private fun perfilParaJson(p: PerfilProfissional) = JSONObject()
        .put("nome", p.nome).put("marca", p.marca).put("cref", p.cref).put("email", p.email)
        .put("telefone", p.telefone).put("cidade", p.cidade).put("chavePix", p.chavePix).put("logo", p.logo ?: JSONObject.NULL)

    private fun jsonParaPerfil(j: JSONObject) = PerfilProfissional(
        nome = j.optString("nome"), marca = j.optString("marca"), cref = j.optString("cref"),
        email = j.optString("email"), telefone = j.optString("telefone"), cidade = j.optString("cidade"), chavePix = j.optString("chavePix"),
        logo = if (j.isNull("logo")) null else j.optString("logo").ifBlank { null },
    )
}
