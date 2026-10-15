package br.com.fitpersonal.app.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Imagens do app ficam em filesDir (privado). O banco/DataStore guarda caminhos relativos, então a
 * pasta pode ser restaurada de um backup sem quebrar referências.
 *   fotos/{idAluno}/   fotos posturais
 *   branding/          logo do profissional
 */
@Singleton
class FotoStorage @Inject constructor(@ApplicationContext private val ctx: Context) {

    /** Uri de um arquivo temporário (cache) para o app de câmera gravar. Não exige permissão CAMERA. */
    fun novaUriCamera(): Uri {
        val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
        val f = File.createTempFile("cam_", ".jpg", dir)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    }

    /**
     * Foto postural: lado maior ≤ 1600 px, JPEG. Retorna o caminho relativo.
     * [recorte] = fração (0..1) da imagem já orientada a manter; null = imagem inteira.
     */
    suspend fun importar(origem: Uri, idAluno: Long, chave: String, recorte: RectF? = null): String =
        importarImagem(origem, "fotos/$idAluno/${chave}_${System.currentTimeMillis()}.jpg", 1600, recorte)

    /** Foto de perfil do aluno: lado maior ≤ 800 px, JPEG. Independe do id (o aluno pode ainda não existir). */
    suspend fun importarFotoAluno(origem: Uri, recorte: RectF? = null): String =
        importarImagem(origem, "fotos/perfil/aluno_${System.currentTimeMillis()}.jpg", 800, recorte)

    /** Logo: lado maior ≤ 600 px, PNG (preserva transparência). */
    suspend fun importarLogo(origem: Uri, recorte: RectF? = null): String =
        importarImagem(origem, "branding/logo_${System.currentTimeMillis()}.png", 600, recorte)

    private suspend fun importarImagem(origem: Uri, rel: String, maxLado: Int, recorte: RectF?): String =
        withContext(Dispatchers.IO) {
            var bmp = decodificarOrientada(ctx.contentResolver, origem, maxLado)

            if (recorte != null) {
                val x = (recorte.left.coerceIn(0f, 1f) * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
                val y = (recorte.top.coerceIn(0f, 1f) * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
                val w = ((recorte.right - recorte.left) * bmp.width).roundToInt().coerceIn(1, bmp.width - x)
                val h = ((recorte.bottom - recorte.top) * bmp.height).roundToInt().coerceIn(1, bmp.height - y)
                bmp = Bitmap.createBitmap(bmp, x, y, w, h)
            }

            val escala = maxLado.toFloat() / maxOf(bmp.width, bmp.height)
            if (escala < 1f) {
                bmp = Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * escala).roundToInt().coerceAtLeast(1),
                    (bmp.height * escala).roundToInt().coerceAtLeast(1),
                    true,
                )
            }

            val destino = File(ctx.filesDir, rel).also { it.parentFile?.mkdirs() }
            val png = rel.endsWith(".png")
            destino.outputStream().use {
                bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 85, it)
            }
            rel
        }

    /** Assinatura desenhada na tela (PNG, fundo branco). Retorna o caminho relativo. */
    suspend fun salvarAssinatura(imagem: Bitmap, idAluno: Long): String = withContext(Dispatchers.IO) {
        val rel = "fotos/$idAluno/assinatura_${System.currentTimeMillis()}.png"
        val destino = File(ctx.filesDir, rel).also { it.parentFile?.mkdirs() }
        destino.outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rel
    }

    fun arquivo(relativo: String): File = File(ctx.filesDir, relativo)

    fun apagar(relativo: String?) {
        if (relativo.isNullOrBlank()) return
        runCatching { arquivo(relativo).delete() }
    }
}
