package br.com.fitpersonal.app.data.files

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * Decodifica a imagem já com a rotação EXIF aplicada, reduzida por potências de 2 até o lado maior
 * ficar em no máximo `maxLado * 2` (a escala final fica a cargo de quem chama). Bloqueante: use fora
 * da thread principal.
 */
fun decodificarOrientada(cr: ContentResolver, origem: Uri, maxLado: Int): Bitmap {
    // Com inJustDecodeBounds o decodeStream devolve SEMPRE null (só preenche as dimensões):
    // o sucesso é medido pelas dimensões, não pelo retorno.
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val aberta = cr.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, limites); true } ?: false
    if (!aberta) throw IOException("Não foi possível abrir a imagem")
    if (limites.outWidth <= 0 || limites.outHeight <= 0) throw IOException("Formato de imagem não suportado")

    var amostra = 1
    while (maxOf(limites.outWidth, limites.outHeight) / amostra > maxLado * 2) amostra *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = amostra }
    val bmp = cr.openInputStream(origem)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: throw IOException("Imagem inválida")

    // EXIF é opcional: se a leitura falhar (PNG, WebP, stream sem EXIF), segue sem rotação.
    val rotacao = runCatching {
        cr.openInputStream(origem)?.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    }.getOrNull() ?: 0f
    if (rotacao == 0f) return bmp
    val m = Matrix().apply { postRotate(rotacao) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}
