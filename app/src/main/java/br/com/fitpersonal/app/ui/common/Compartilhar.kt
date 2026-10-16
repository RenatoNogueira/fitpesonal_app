package br.com.fitpersonal.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

object PdfSharer {
    const val PACOTE_WHATSAPP = "com.whatsapp"

    /** Compartilha texto puro (ex.: treino formatado). O usuário escolhe o app e o contato. */
    fun compartilharTexto(ctx: Context, texto: String, assunto: String = "FitPersonal") {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            putExtra(Intent.EXTRA_SUBJECT, assunto)
        }
        try {
            ctx.startActivity(Intent.createChooser(envio, "Enviar treino").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) { /* nenhum app disponível */ }
    }

    /**
     * Abre o seletor de compartilhamento (WhatsApp, e-mail, Drive…) para o arquivo.
     * Com [pacote] (ex.: WhatsApp) tenta abrir direto nesse app e, se ele não existir, cai no seletor.
     */
    fun compartilhar(
        ctx: Context,
        pdf: File,
        assunto: String = "FitPersonal",
        mime: String = "application/pdf",
        pacote: String? = null,
    ) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", pdf)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, assunto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (pacote != null) {
            try {
                ctx.startActivity(Intent(envio).setPackage(pacote).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (e: ActivityNotFoundException) {
                // app não instalado: segue para o seletor
            }
        }
        try {
            ctx.startActivity(Intent.createChooser(envio, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) { /* nenhum app disponível */ }
    }
}

/**
 * Mostra a pré-visualização do PDF quando [arquivo] fica disponível; ao fechar ou compartilhar,
 * avisa o chamador ([onConsumido]) para limpar o estado.
 */
@Composable
fun CompartilharPdfEffect(arquivo: File?, onConsumido: () -> Unit) {
    val ctx = LocalContext.current
    if (arquivo != null) {
        PreviaPdf(
            arquivo = arquivo,
            onFechar = onConsumido,
            onCompartilhar = { PdfSharer.compartilhar(ctx, arquivo); onConsumido() },
            onWhatsApp = { PdfSharer.compartilhar(ctx, arquivo, pacote = PdfSharer.PACOTE_WHATSAPP); onConsumido() },
        )
    }
}
