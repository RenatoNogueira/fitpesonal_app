package br.com.fitpersonal.app.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.fitpersonal.app.MainActivity

object Notificador {
    const val CANAL_LEMBRETES = "lembretes_agenda"
    const val CANAL_REAVALIACAO = "reavaliacoes"
    const val CANAL_RESUMO = "resumo_dia"
    const val CANAL_ALUNO = "treino_aluno"
    private const val ID_ALUNO = Int.MAX_VALUE - 6

    /** Id fixo: um aviso novo substitui o anterior (ids de agendamento são pequenos, não colidem). */
    private const val ID_REAVALIACAO = Int.MAX_VALUE - 1
    private const val ID_PACOTES = Int.MAX_VALUE - 2
    private const val ID_AGENDA_DIA = Int.MAX_VALUE - 3
    private const val ID_MENSALIDADES = Int.MAX_VALUE - 4
    private const val ID_ANIVERSARIOS = Int.MAX_VALUE - 5
    private const val ID_VENCIMENTOS = Int.MAX_VALUE - 7

    fun criarCanais(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_LEMBRETES, "Lembretes de agenda", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Aviso antes de avaliações, treinos e consultorias agendados" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_REAVALIACAO, "Reavaliações", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Resumo diário de alunos com reavaliação atrasada ou vencendo" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ALUNO, "Lembretes de treino", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Aviso nos dias de treino e quando você fica alguns dias sem treinar" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_RESUMO, "Resumo do dia", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Sessões do dia, mensalidades em atraso e aniversários" }
        )
    }

    @SuppressLint("MissingPermission")   // verificado em permitido()
    fun pacotes(ctx: Context, texto: String) {
        if (!permitido(ctx)) return
        criarCanais(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, ID_PACOTES,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CANAL_REAVALIACAO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pacotes de sessões")
            .setContentText(texto)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_PACOTES, n)
    }

    @SuppressLint("MissingPermission")   // verificado em permitido()
    fun reavaliacoes(ctx: Context, texto: String) {
        if (!permitido(ctx)) return
        criarCanais(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, ID_REAVALIACAO,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CANAL_REAVALIACAO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reavaliações pendentes")
            .setContentText(texto)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(ctx).notify(ID_REAVALIACAO, n)
    }

    fun agendaDoDia(ctx: Context, texto: String) = resumo(ctx, ID_AGENDA_DIA, "Agenda de hoje", texto)
    fun mensalidades(ctx: Context, texto: String) = resumo(ctx, ID_MENSALIDADES, "Mensalidades em atraso", texto)
    fun aniversarios(ctx: Context, texto: String) = resumo(ctx, ID_ANIVERSARIOS, "Aniversariantes", texto)
    fun vencimentos(ctx: Context, texto: String) = resumo(ctx, ID_VENCIMENTOS, "Mensalidades a vencer", texto)

    @SuppressLint("MissingPermission")   // verificado em permitido()
    fun alunoAviso(ctx: Context, titulo: String, texto: String) =
        resumo(ctx, ID_ALUNO, titulo, texto, CANAL_ALUNO)

    @SuppressLint("MissingPermission")   // verificado em permitido()
    private fun resumo(ctx: Context, id: Int, titulo: String, texto: String, canal: String = CANAL_RESUMO) {
        if (!permitido(ctx)) return
        criarCanais(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    /** Em Android 13+ a notificação exige permissão em tempo de execução. */
    fun permitido(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")   // verificado em permitido()
    fun lembrete(ctx: Context, idAgendamento: Long, titulo: String, texto: String) {
        if (!permitido(ctx)) return
        criarCanais(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, idAgendamento.toInt(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CANAL_LEMBRETES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        NotificationManagerCompat.from(ctx).notify(idAgendamento.toInt(), n)
    }
}
