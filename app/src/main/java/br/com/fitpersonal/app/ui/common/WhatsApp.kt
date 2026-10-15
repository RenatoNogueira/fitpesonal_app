package br.com.fitpersonal.app.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.fitpersonal.app.domain.rules.WhatsAppRules

object WhatsApp {
    /**
     * Abre a conversa com [telefone] com [mensagem] já escrita (o usuário só confirma o envio).
     * Devolve false quando o número é inválido ou não há app que abra o link.
     */
    fun enviar(ctx: Context, telefone: String?, mensagem: String): Boolean {
        val url = WhatsAppRules.link(telefone, mensagem) ?: return false
        return try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
