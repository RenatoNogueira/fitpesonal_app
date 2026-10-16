package br.com.fitpersonal.app.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abertura e compartilhamento de vídeos por meio de apps do aparelho (sem player embutido). */
object VideoIntents {

    /** Abre o link no navegador/YouTube. Devolve false se nenhum app puder abrir. */
    fun abrirLink(ctx: Context, url: String): Boolean = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /** Reproduz um vídeo local (Uri do FileProvider) no player do aparelho. */
    fun assistir(ctx: Context, uri: Uri): Boolean = try {
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(i)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /** Envia o vídeo por WhatsApp, e-mail etc. (o aluno não o recebe junto do treino). */
    fun compartilhar(ctx: Context, uri: Uri, titulo: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, titulo)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(envio, "Compartilhar vídeo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
