package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Textos de contexto do início (saudação por hora e datas em português). */
object Saudacao {

    fun porHora(hora: Int): String = when (hora) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    fun primeiroNome(nomeCompleto: String): String =
        nomeCompleto.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

    /** "Hoje", "Amanhã" ou "dd/MM" — para rótulos curtos de agendamento. */
    fun rotuloData(data: LocalDate, hoje: LocalDate): String = when (data) {
        hoje -> "Hoje"
        hoje.plusDays(1) -> "Amanhã"
        hoje.minusDays(1) -> "Ontem"
        else -> "%02d/%02d".format(data.dayOfMonth, data.monthValue)
    }

    /** "Quarta-feira, 14 de outubro". */
    fun dataExtensa(data: LocalDate): String {
        val ptBr = Locale("pt", "BR")
        val dia = data.dayOfWeek.getDisplayName(TextStyle.FULL, ptBr)
        val mes = data.month.getDisplayName(TextStyle.FULL, ptBr)
        return "${dia.replaceFirstChar { it.uppercase() }}, ${data.dayOfMonth} de $mes"
    }

    /** Abreviação de 3 letras do dia da semana ("SEG", "TER"...). */
    fun diaSemanaCurto(data: LocalDate): String =
        data.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).uppercase().take(3)
}
