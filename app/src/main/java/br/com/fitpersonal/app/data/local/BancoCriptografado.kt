package br.com.fitpersonal.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fitpersonal.app.data.crypto.SegredoLocal
import br.com.fitpersonal.app.data.crypto.paraHex
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.IOException

/**
 * Criptografia em repouso do banco com SQLCipher (AES-256).
 *
 * - A senha do banco é aleatória (32 bytes), guardada no cofre do Keystore ([SegredoLocal]); o usuário
 *   não a digita. Protege contra cópia do arquivo do banco (adb, root, imagem do armazenamento).
 * - Backups NÃO levam o banco cifrado com essa chave (ela é do aparelho): levam uma cópia legível
 *   protegida pela senha do backup (ver BackupManager/CifraBackup).
 */
object BancoCriptografado {
    private const val SEGREDO_BANCO = "db_key"
    private const val CABECALHO_SQLITE = "SQLite format 3\u0000"

    @Volatile private var biblioteca = false

    @Synchronized
    fun carregarBiblioteca() {
        if (!biblioteca) {
            System.loadLibrary("sqlcipher")
            biblioteca = true
        }
    }

    /** Senha (hex ASCII de 32 bytes aleatórios) usada tanto no Room quanto no ATTACH ... KEY. */
    fun senhaBanco(segredos: SegredoLocal): ByteArray =
        segredos.obterOuCriarAleatorio(SEGREDO_BANCO, 32).paraHex().toByteArray(Charsets.US_ASCII)

    fun ehTextoPuro(arquivo: File): Boolean {
        if (!arquivo.isFile || arquivo.length() < 16) return false
        val cab = arquivo.inputStream().use { i -> ByteArray(16).also { i.read(it) } }
        return String(cab, Charsets.ISO_8859_1) == CABECALHO_SQLITE
    }

    private fun apagarComplementos(arquivo: File) {
        File(arquivo.path + "-wal").delete()
        File(arquivo.path + "-shm").delete()
        File(arquivo.path + "-journal").delete()
    }

    /**
     * Converte [origem] (SQLite comum) em [destino] (SQLCipher) com `sqlcipher_export`,
     * preservando `user_version` (usado pelas migrações do Room).
     */
    fun converterParaCriptografado(origem: File, destino: File, senha: ByteArray) {
        carregarBiblioteca()
        destino.delete()
        apagarComplementos(destino)
        val chave = String(senha, Charsets.US_ASCII)

        val banco = SQLiteDatabase.openDatabase(origem.path, "", null, SQLiteDatabase.OPEN_READWRITE, null)
        try {
            val versao = banco.version
            banco.rawExecSQL("ATTACH DATABASE '${destino.path.replace("'", "''")}' AS encrypted KEY '$chave';")
            banco.rawQuery("SELECT sqlcipher_export('encrypted');", arrayOf<String>()).use { it.moveToFirst() }
            banco.rawExecSQL("PRAGMA encrypted.user_version = $versao;")
            banco.rawExecSQL("DETACH DATABASE encrypted;")
        } finally {
            banco.close()
        }
    }

    /**
     * Migração única: se o arquivo do banco ainda é texto puro (versão anterior do app), cifra-o no lugar.
     * O original só é apagado depois que a cópia cifrada abre com a senha e responde a uma consulta.
     */
    fun criptografarSePreciso(arquivo: File, senha: ByteArray) {
        if (!ehTextoPuro(arquivo)) return
        carregarBiblioteca()
        val novo = File(arquivo.path + ".enc")
        converterParaCriptografado(arquivo, novo, senha)

        val chave = String(senha, Charsets.US_ASCII)
        val verificacao = SQLiteDatabase.openDatabase(novo.path, chave, null, SQLiteDatabase.OPEN_READONLY, null)
        try {
            verificacao.rawQuery("SELECT count(*) FROM sqlite_master;", arrayOf<String>()).use { it.moveToFirst() }
        } catch (e: Exception) {
            novo.delete()
            throw IOException("Falha ao criptografar o banco de dados.", e)
        } finally {
            verificacao.close()
        }

        apagarComplementos(arquivo)
        if (!arquivo.delete()) throw IOException("Não foi possível substituir o banco antigo.")
        if (!novo.renameTo(arquivo)) throw IOException("Não foi possível instalar o banco criptografado.")
    }

    /** Abre em modo leitura com [senha] e executa uma consulta; false se não for um SQLCipher válido para essa chave. */
    private fun abreComChave(arquivo: File, senha: ByteArray): Boolean {
        carregarBiblioteca()
        return try {
            val banco = SQLiteDatabase.openDatabase(
                arquivo.path, String(senha, Charsets.US_ASCII), null, SQLiteDatabase.OPEN_READWRITE, null,
            )
            try {
                banco.rawQuery("SELECT count(*) FROM sqlite_master;", arrayOf<String>()).use { it.moveToFirst() }
            } finally {
                banco.close()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Guarda o banco ilegível numa pasta própria (nada é apagado) e libera o caminho para um banco novo.
     * Mantém só as 2 cópias mais recentes.
     */
    private fun moverParaLado(ctx: Context, arquivo: File) {
        val pasta = File(ctx.filesDir, "banco_ilegivel").apply { mkdirs() }
        val destino = File(pasta, "fitpersonal_${System.currentTimeMillis()}.db")
        if (!arquivo.renameTo(destino)) {
            arquivo.copyTo(destino, overwrite = true)
            arquivo.delete()
        }
        apagarComplementos(arquivo)
        pasta.listFiles()?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach { it.delete() }
    }

    /**
     * Ponto único de preparação do banco ANTES de o Room abri-lo. Nunca lança por causa de chave perdida
     * ou banco ilegível: aplica a [PoliticaRecuperacao] e devolve a senha a usar.
     *
     * Quando um banco é posto de lado, grava um [InfoRecuperacao] — a UI então mostra a tela de
     * recuperação (restaurar backup ou começar do zero) em vez de o app travar.
     */
    fun preparar(ctx: Context, segredos: SegredoLocal): ByteArray {
        carregarBiblioteca()
        val arquivo = ctx.getDatabasePath(DB_NOME)
        val existe = arquivo.isFile && arquivo.length() > 0

        var senha: ByteArray? = runCatching { senhaBanco(segredos) }.getOrNull()
        val textoPuro = existe && ehTextoPuro(arquivo)
        val abre = existe && !textoPuro && senha != null && abreComChave(arquivo, senha)

        val acao = PoliticaRecuperacao.decidir(EstadoBanco(existe, senha != null, textoPuro, abre))

        fun novaChave(): ByteArray {
            segredos.redefinirTudo()
            return senhaBanco(segredos)
        }

        when (acao) {
            AcaoBanco.NADA -> Unit
            AcaoBanco.CRIPTOGRAFAR -> criptografarSePreciso(arquivo, senha!!)
            AcaoBanco.REDEFINIR_CHAVE -> senha = novaChave()
            AcaoBanco.REDEFINIR_E_CRIPTOGRAFAR -> {
                senha = novaChave()
                criptografarSePreciso(arquivo, senha)
            }
            AcaoBanco.MOVER_E_RECOMECAR -> {
                moverParaLado(ctx, arquivo)
                RegistroRecuperacao.gravar(
                    ctx.noBackupFilesDir,
                    InfoRecuperacao(System.currentTimeMillis(), "O banco de dados não pôde ser aberto com a chave deste aparelho."),
                )
            }
            AcaoBanco.REDEFINIR_E_MOVER_E_RECOMECAR -> {
                senha = novaChave()
                moverParaLado(ctx, arquivo)
                RegistroRecuperacao.gravar(
                    ctx.noBackupFilesDir,
                    InfoRecuperacao(System.currentTimeMillis(), "A chave de segurança deste aparelho foi perdida."),
                )
            }
        }
        return senha!!
    }

    /**
     * Grava em [destino] uma cópia LEGÍVEL (sem cifra) do banco aberto, para entrar no backup
     * (que é protegido por senha em outra camada).
     */
    fun exportarLegivel(aberto: SupportSQLiteDatabase, destino: File) {
        destino.delete()
        apagarComplementos(destino)
        val versao = aberto.version
        aberto.execSQL("ATTACH DATABASE '${destino.path.replace("'", "''")}' AS plaintext KEY '';")
        try {
            aberto.query("SELECT sqlcipher_export('plaintext');").use { it.moveToFirst() }
            aberto.execSQL("PRAGMA plaintext.user_version = $versao;")
        } finally {
            aberto.execSQL("DETACH DATABASE plaintext;")
        }
    }
}
