package br.com.fitpersonal.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.fitpersonal.app.data.crypto.SenhaIncorretaException
import br.com.fitpersonal.app.data.local.DB_NOME
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** O que está gravado na pasta: qual versão do conjunto de dados e quem enviou. */
data class MetaSync(
    val versao: Long,
    val dispositivo: String,
    val nomeDispositivo: String,
    val emMillis: Long,
)

sealed interface ResultadoSync {
    data object JaSincronizado : ResultadoSync
    data class Enviado(val versao: Long) : ResultadoSync
    /** Os dados da pasta foram aplicados: o app precisa ser fechado e reaberto. */
    data class Recebido(val versao: Long) : ResultadoSync
    /** A pasta tem uma versão nova E este aparelho também mudou: o usuário decide quem vence. */
    data class Conflito(val remoto: MetaSync) : ResultadoSync
    data class Erro(val mensagem: String) : ResultadoSync
}

/**
 * Sincronização entre aparelhos por **pasta compartilhada** (Google Drive, Dropbox, USB…), sem servidor.
 *
 * A pasta guarda um único conjunto de dados: `fitpersonal_sync.rjebak` (o mesmo backup cifrado) e
 * `fitpersonal_sync.json` (versão e autor). Cada aparelho lembra a última versão que aplicou ou enviou:
 *  - pasta com versão maior e nada mudou aqui → **recebe** (substitui os dados deste aparelho);
 *  - nada mudou na pasta e algo mudou aqui → **envia** (versão + 1);
 *  - mudou nos dois lados → **conflito**: o usuário escolhe, e o estado atual é guardado antes (snapshot).
 *
 * Premissas: os aparelhos usam a **mesma senha de backup**, e a pasta escolhida é a mesma do backup
 * automático (Perfil → Dados). É sincronização por "último a enviar vence", não mescla registro a registro.
 */
@Singleton
class SincronizacaoPasta @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val backup: BackupManager,
    private val config: ConfiguracoesRepository,
) {
    private companion object {
        const val PREFIXO = "fitpersonal_sync"
        const val ARQ_DADOS = "fitpersonal_sync.rjebak"
        const val ARQ_META = "fitpersonal_sync.json"
        /** Tolerância entre o horário gravado no fim da sincronização e a data dos arquivos do banco. */
        const val FOLGA_MS = 2_000L
    }

    suspend fun sincronizar(): ResultadoSync = executar(Modo.AUTO)

    /** Resolve um conflito (ou força) mantendo os dados DESTE aparelho: sobrescreve a pasta. */
    suspend fun enviarEsteAparelho(): ResultadoSync = executar(Modo.ENVIAR)

    /** Resolve um conflito (ou força) usando a pasta: substitui os dados deste aparelho. */
    suspend fun usarDaPasta(): ResultadoSync = executar(Modo.RECEBER)

    private enum class Modo { AUTO, ENVIAR, RECEBER }

    private suspend fun executar(modo: Modo): ResultadoSync = withContext(Dispatchers.IO) {
        try {
            val cfg = config.config.first()
            val pasta = cfg.pastaBackup?.let { DocumentFile.fromTreeUri(ctx, Uri.parse(it)) }
                ?.takeIf { it.exists() && it.canWrite() }
                ?: return@withContext ResultadoSync.Erro(
                    "Escolha uma pasta (Drive ou outra) em Perfil → Dados → Backup automático antes de sincronizar.",
                )
            if (!backup.temSenha()) {
                return@withContext ResultadoSync.Erro("Defina a senha do backup (a mesma nos dois aparelhos).")
            }

            val remoto = lerMeta(pasta)
            val arquivoDados = pasta.listFiles().firstOrNull { ehDados(it) }
            val alterado = alteradoLocalmente(cfg.syncUltimoEm)

            val acao = when (modo) {
                Modo.ENVIAR -> Acao.ENVIAR
                Modo.RECEBER -> if (remoto != null && arquivoDados != null) Acao.RECEBER else Acao.ENVIAR
                Modo.AUTO -> when {
                    remoto == null || arquivoDados == null -> Acao.ENVIAR
                    remoto.versao == cfg.syncVersao -> if (alterado) Acao.ENVIAR else Acao.NADA
                    remoto.versao > cfg.syncVersao -> if (!alterado) Acao.RECEBER else Acao.CONFLITO
                    else -> Acao.ENVIAR   // pasta com versão MENOR (foi limpa/recriada): este aparelho reenvia
                }
            }

            when (acao) {
                Acao.NADA -> ResultadoSync.JaSincronizado
                Acao.CONFLITO -> ResultadoSync.Conflito(remoto!!)
                Acao.ENVIAR -> {
                    val nova = maxOf(remoto?.versao ?: 0L, cfg.syncVersao) + 1
                    enviar(pasta, arquivoDados, nova, cfg.syncDispositivo, nomeDoAparelho())
                    config.registrarSincronizacao(nova, System.currentTimeMillis(), "Enviado (versão $nova)")
                    ResultadoSync.Enviado(nova)
                }
                Acao.RECEBER -> {
                    backup.restaurarComSenhaSalva(arquivoDados!!.uri)
                    val v = remoto!!.versao
                    config.registrarSincronizacao(v, System.currentTimeMillis(), "Recebido (versão $v)")
                    ResultadoSync.Recebido(v)
                }
            }
        } catch (e: SenhaIncorretaException) {
            ResultadoSync.Erro("A senha do backup deste aparelho é diferente da usada no outro. Defina a mesma senha nos dois.")
        } catch (e: Exception) {
            ResultadoSync.Erro(e.message ?: e.javaClass.simpleName)
        }
    }

    private enum class Acao { NADA, ENVIAR, RECEBER, CONFLITO }

    // ── pasta ──

    private fun ehDados(f: DocumentFile): Boolean {
        val n = f.name.orEmpty()
        return n.startsWith(PREFIXO) && n.contains(".rjebak")
    }

    private fun ehMeta(f: DocumentFile): Boolean {
        val n = f.name.orEmpty()
        return n.startsWith(PREFIXO) && n.endsWith(".json")
    }

    private fun lerMeta(pasta: DocumentFile): MetaSync? {
        val doc = pasta.listFiles().firstOrNull { ehMeta(it) } ?: return null
        return runCatching {
            ctx.contentResolver.openInputStream(doc.uri)?.use { s ->
                val j = JSONObject(s.readBytes().toString(Charsets.UTF_8))
                MetaSync(
                    versao = j.getLong("versao"),
                    dispositivo = j.optString("dispositivo"),
                    nomeDispositivo = j.optString("nomeDispositivo"),
                    emMillis = j.optLong("emMillis"),
                )
            }
        }.getOrNull()
    }

    private suspend fun enviar(
        pasta: DocumentFile,
        existente: DocumentFile?,
        versao: Long,
        dispositivoSalvo: String,
        nome: String,
    ) {
        val idDispositivo = dispositivoSalvo.ifBlank { UUID.randomUUID().toString().also { config.definirDispositivoSync(it) } }

        // Apaga e recria: alguns provedores não truncam o arquivo ao regravar.
        existente?.delete()
        pasta.listFiles().filter { ehMeta(it) }.forEach { it.delete() }

        val dados = pasta.createFile("application/octet-stream", ARQ_DADOS)
            ?: throw IllegalStateException("Não foi possível criar o arquivo na pasta escolhida")
        backup.exportar(dados.uri)

        val meta = JSONObject()
            .put("versao", versao)
            .put("dispositivo", idDispositivo)
            .put("nomeDispositivo", nome)
            .put("emMillis", System.currentTimeMillis())
        val doc = pasta.createFile("application/json", ARQ_META)
            ?: throw IllegalStateException("Não foi possível gravar a versão na pasta")
        ctx.contentResolver.openOutputStream(doc.uri, "w")?.use { it.write(meta.toString().toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("Não foi possível gravar a versão na pasta")
    }

    // ── mudanças locais ──

    /** Algo mudou neste aparelho depois da última sincronização? (banco, log do banco ou pasta de fotos) */
    private fun alteradoLocalmente(ultimoEm: Long): Boolean {
        if (ultimoEm <= 0L) return true
        val banco = ctx.getDatabasePath(DB_NOME)
        val marcas = listOf(
            banco.lastModified(),
            File(banco.path + "-wal").lastModified(),
            File(ctx.filesDir, "fotos").lastModified(),
            File(ctx.filesDir, "branding").lastModified(),
        )
        return marcas.maxOrNull()!! > ultimoEm + FOLGA_MS
    }

    private fun nomeDoAparelho(): String =
        listOf(android.os.Build.MANUFACTURER, android.os.Build.MODEL).filter { it.isNotBlank() }.joinToString(" ")
}
