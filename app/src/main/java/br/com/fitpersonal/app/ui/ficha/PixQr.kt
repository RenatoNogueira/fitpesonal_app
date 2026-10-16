package br.com.fitpersonal.app.ui.ficha

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.PixCobranca
import br.com.fitpersonal.app.ui.common.PdfSharer
import br.com.fitpersonal.app.ui.common.QrCode
import java.io.File
import java.text.Normalizer

/**
 * QR Code do Pix da cobrança: o aluno aponta a câmera do app do banco (ou recebe a imagem por
 * WhatsApp). Também permite copiar o código "copia e cola".
 */
@Composable
fun PixQrDialog(
    pix: PixCobranca,
    valorCentavos: Long,
    nomeAluno: String,
    onFechar: () -> Unit,
    onMensagem: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val codigo = pix.copiaECola
    val imagem: Bitmap? = remember(codigo) { codigo?.let { runCatching { QrCode.bitmap(it) }.getOrNull() } }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Pix • " + FinanceiroRules.formatarReais(valorCentavos)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (imagem != null) {
                    Image(
                        bitmap = imagem.asImageBitmap(),
                        contentDescription = "QR Code do Pix",
                        modifier = Modifier.size(248.dp).background(Color.White).padding(6.dp),
                    )
                } else {
                    Text("Não foi possível gerar o QR Code. Use a chave abaixo.", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Chave: " + pix.chave, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Cobrança de " + nomeOuAluno(nomeAluno),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (imagem != null) {
                TextButton(onClick = { enviarImagem(ctx, imagem, nomeAluno, onMensagem) }) { Text("Enviar imagem") }
            }
        },
        dismissButton = {
            Row {
                if (codigo != null) {
                    TextButton(
                        onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Pix copia e cola", codigo))
                            onMensagem("Código Pix copiado.")
                        },
                    ) { Text("Copiar código") }
                }
                TextButton(onClick = onFechar) { Text("Fechar") }
            }
        },
    )
}

private fun nomeOuAluno(nome: String): String = nome.trim().ifEmpty { "aluno" }

private fun enviarImagem(ctx: Context, imagem: Bitmap, nomeAluno: String, onMensagem: (String) -> Unit) {
    runCatching {
        val pasta = File(ctx.cacheDir, "pdf").apply { mkdirs() }
        val slug = Normalizer.normalize(nomeAluno, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').lowercase().take(24).ifEmpty { "aluno" }
        val arquivo = File(pasta, "pix_$slug.png")
        arquivo.outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        PdfSharer.compartilhar(ctx, arquivo, "Pix para pagamento", "image/png", pacote = "com.whatsapp")
    }.onFailure { onMensagem("Não foi possível enviar a imagem: " + it.message) }
}
