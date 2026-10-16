package br.com.fitpersonal.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.rules.LembreteRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class ResultadoBackupAuto(val sucesso: Boolean, val mensagem: String)

/**
 * Backup automático (diário, via WorkManager) ou manual ("Fazer backup agora").
 *
 * Destino: a pasta escolhida pelo usuário (SAF — pode ser uma pasta do Drive/Downloads) ou,
 * se não houver pasta ou ela perdeu a permissão, o armazenamento interno do app (filesDir/backups_auto).
 * Mantém os 7 backups mais recentes (prefixo `fitpersonal_auto_`).
 */
@Singleton
class BackupAutomatico @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val backup: BackupManager,
    private val config: ConfiguracoesRepository,
) {
    suspend fun executar(): ResultadoBackupAuto = withContext(Dispatchers.IO) {
        val agora = LocalDateTime.now()
        val nome = LembreteRules.nomeBackupAuto(agora)
        val cfg = config.config.first()

        val resultado = try {
            val pasta = cfg.pastaBackup?.let { DocumentFile.fromTreeUri(ctx, Uri.parse(it)) }
                ?.takeIf { it.exists() && it.canWrite() }

            val descricao = if (pasta != null) {
                // O provedor acrescenta a extensão conforme o MIME; por isso o nome vai sem a extensão (.fitbak).
                val doc = pasta.createFile("application/octet-stream", nome + ".fitbak")
                    ?: throw IllegalStateException("Não foi possível criar o arquivo na pasta escolhida")
                backup.exportar(doc.uri)
                rotacionarPasta(pasta)
                "pasta \"${cfg.pastaBackupNome.ifBlank { pasta.name.orEmpty() }}\""
            } else {
                val dir = File(ctx.filesDir, "backups_auto").apply { mkdirs() }
                backup.exportar(Uri.fromFile(File(dir, "$nome.fitbak")))
                rotacionarInterno(dir)
                val aviso = if (cfg.pastaBackup != null) " (a pasta escolhida está indisponível)" else ""
                "armazenamento interno do app$aviso"
            }
            ResultadoBackupAuto(true, "Backup salvo em $descricao")
        } catch (e: Exception) {
            ResultadoBackupAuto(false, "Falha: ${e.message ?: e.javaClass.simpleName}")
        }

        config.registrarBackup(System.currentTimeMillis(), resultado.mensagem)
        resultado
    }

    private fun rotacionarPasta(pasta: DocumentFile) {
        val arquivos = pasta.listFiles().mapNotNull { f -> f.name?.let { it to f } }
        val apagar = LembreteRules.backupsParaApagar(arquivos.map { it.first }).toSet()
        arquivos.filter { it.first in apagar }.forEach { it.second.delete() }
    }

    private fun rotacionarInterno(dir: File) {
        val nomes = dir.listFiles()?.map { it.name }.orEmpty()
        LembreteRules.backupsParaApagar(nomes).forEach { File(dir, it).delete() }
    }
}
