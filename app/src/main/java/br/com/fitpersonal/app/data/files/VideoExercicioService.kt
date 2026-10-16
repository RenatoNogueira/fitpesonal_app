package br.com.fitpersonal.app.data.files

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import br.com.fitpersonal.app.data.local.VideoExercicioDao
import br.com.fitpersonal.app.data.local.VideoExercicioEntity
import br.com.fitpersonal.app.domain.rules.CargaRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Vídeo próprio de um exercício (um por nome de exercício, reaproveitado em todos os treinos). */
data class VideoExercicio(
    val chave: String,
    val nome: String,
    /** Caminho relativo a filesDir. */
    val arquivo: String,
    val duracaoMs: Long,
    val tamanhoBytes: Long,
) {
    val duracaoTexto: String
        get() {
            val s = (duracaoMs / 1000).toInt()
            return "%d:%02d".format(s / 60, s % 60)
        }
    val tamanhoTexto: String
        get() = if (tamanhoBytes >= 1_048_576) "%.1f MB".format(tamanhoBytes / 1_048_576.0) else "${tamanhoBytes / 1024} KB"
}

/**
 * Biblioteca de vídeos próprios em filesDir/videos. O arquivo é copiado para o armazenamento privado
 * (não depende da galeria) e identificado pelo nome normalizado do exercício. Fica fora do backup
 * `.fitbak` por causa do tamanho — o profissional pode compartilhar o arquivo separadamente.
 */
@Singleton
class VideoExercicioService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dao: VideoExercicioDao,
) {
    companion object {
        const val MAX_BYTES = 150L * 1024 * 1024
        const val MAX_DURACAO_MS = 5L * 60 * 1000
    }

    /** Só vídeos cujo arquivo existe: backup e sincronização levam o banco mas não os vídeos. */
    val todos: Flow<Map<String, VideoExercicio>> =
        dao.todos().map { l ->
            l.map { it.toDomain() }.filter { existe(it) }.associateBy { it.chave }
        }

    /** Uri temporária (cache) para o app de câmera gravar o vídeo. */
    fun novaUriGravacao(): Uri {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val f = File.createTempFile("vid_", ".mp4", dir)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    }

    /** Copia o vídeo escolhido/gravado para o armazenamento do app e o associa ao exercício [nome]. */
    suspend fun importar(nome: String, origem: Uri): VideoExercicio = withContext(Dispatchers.IO) {
        val chave = CargaRules.chaveExercicio(nome)
        if (chave.isEmpty()) throw IOException("Digite o nome do exercício antes de adicionar o vídeo.")
        val cr = ctx.contentResolver

        val declarado = cr.query(origem, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        if (declarado > MAX_BYTES) throw IOException("Vídeo muito grande (máximo 150 MB). Grave um trecho mais curto.")

        val dir = File(ctx.filesDir, "videos").apply { mkdirs() }
        val seguro = chave.replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40).ifEmpty { "exercicio" }
        val destino = File(dir, "${seguro}_${System.currentTimeMillis()}.mp4")
        try {
            var copiados = 0L
            val entrada = cr.openInputStream(origem) ?: throw IOException("Não foi possível abrir o vídeo.")
            entrada.use { ent ->
                destino.outputStream().use { sai ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = ent.read(buf)
                        if (n < 0) break
                        copiados += n
                        if (copiados > MAX_BYTES) throw IOException("Vídeo muito grande (máximo 150 MB).")
                        sai.write(buf, 0, n)
                    }
                }
            }
            if (copiados == 0L) throw IOException("O vídeo está vazio.")

            val duracao = lerDuracao(destino)
            if (duracao > MAX_DURACAO_MS) throw IOException("Vídeo longo demais (máximo 5 minutos).")

            val anterior = dao.buscar(chave)
            val e = VideoExercicioEntity(
                chave = chave, nome = nome.trim(), arquivo = "videos/${destino.name}",
                duracaoMs = duracao, tamanhoBytes = copiados, criadoEm = LocalDate.now().toString(),
            )
            dao.salvar(e)
            anterior?.let { runCatching { File(ctx.filesDir, it.arquivo).delete() } }
            e.toDomain()
        } catch (t: Throwable) {
            runCatching { destino.delete() }
            throw t
        }
    }

    suspend fun remover(chave: String) = withContext(Dispatchers.IO) {
        dao.buscar(chave)?.let { runCatching { File(ctx.filesDir, it.arquivo).delete() } }
        dao.excluir(chave)
    }

    /** Uri (FileProvider) para abrir ou compartilhar o vídeo com outros apps. */
    fun uriDe(v: VideoExercicio): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(ctx.filesDir, v.arquivo))

    fun existe(v: VideoExercicio): Boolean = File(ctx.filesDir, v.arquivo).exists()

    fun arquivoDe(v: VideoExercicio): File = File(ctx.filesDir, v.arquivo)

    suspend fun todosSnapshot(): Map<String, VideoExercicio> =
        dao.todosSnapshot().map { it.toDomain() }.filter { existe(it) }.associateBy { it.chave }

    private fun lerDuracao(arquivo: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(arquivo.path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { r.release() }
        }
    }

    private fun VideoExercicioEntity.toDomain() = VideoExercicio(chave, nome, arquivo, duracaoMs, tamanhoBytes)
}
