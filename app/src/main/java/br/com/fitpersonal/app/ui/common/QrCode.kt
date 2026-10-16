package br.com.fitpersonal.app.ui.common

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Gera a imagem de um QR Code (preto sobre branco, com margem) a partir de um texto. */
object QrCode {
    fun bitmap(texto: String, lado: Int = 720): Bitmap {
        val dicas = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        )
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, lado, lado, dicas)
        val w = matriz.width
        val h = matriz.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val linha = y * w
            for (x in 0 until w) pixels[linha + x] = if (matriz[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
