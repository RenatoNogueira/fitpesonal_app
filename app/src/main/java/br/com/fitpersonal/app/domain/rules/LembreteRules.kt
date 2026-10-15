package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Regras puras de lembretes de agenda e de rotação do backup automático. */
object LembreteRules {

    /** Antecedências oferecidas ao usuário, em minutos. */
    val ANTECEDENCIAS_MIN = listOf(15, 30, 60, 120, 1440)

    const val PREFIXO_BACKUP_AUTO = "fitpersonal_auto_"
    const val MANTER_BACKUPS_AUTO = 7

    fun rotuloAntecedencia(min: Int): String = when {
        min % 1440 == 0 -> if (min == 1440) "1 dia" else "${min / 1440} dias"
        min % 60 == 0 -> "${min / 60} h"
        else -> "$min min"
    }

    fun momentoLembrete(data: LocalDate, horario: LocalTime, antecedenciaMin: Int): LocalDateTime =
        LocalDateTime.of(data, horario).minusMinutes(antecedenciaMin.toLong())

    /** Atraso (ms) até o lembrete; null se o momento já passou (não notificar retroativamente). */
    fun atrasoMillis(agora: LocalDateTime, momento: LocalDateTime): Long? {
        val ms = Duration.between(agora, momento).toMillis()
        return if (ms > 0) ms else null
    }

    fun deveNotificar(status: StatusAgendamento) = status == StatusAgendamento.PENDENTE

    fun titulo(tipo: TipoAgendamento) = "${tipo.rotulo} em breve"

    fun texto(nomeAluno: String, data: LocalDate, horario: LocalTime, hoje: LocalDate): String {
        val hora = horario.format(DateTimeFormatter.ofPattern("HH:mm"))
        val quando = when (data) {
            hoje -> "Hoje às $hora"
            hoje.plusDays(1) -> "Amanhã às $hora"
            else -> "${data.format(DateTimeFormatter.ofPattern("dd/MM"))} às $hora"
        }
        return "$quando — $nomeAluno"
    }

    /** Tempo até a próxima ocorrência de [hora]:00 (sempre > 0), para o backup noturno. */
    fun atrasoAteHorario(agora: LocalDateTime, hora: Int): Duration {
        var alvo = agora.toLocalDate().atTime(hora, 0)
        if (!alvo.isAfter(agora)) alvo = alvo.plusDays(1)
        return Duration.between(agora, alvo)
    }

    /** Nome (sem extensão) do arquivo de backup automático; ordem alfabética = ordem cronológica. */
    fun nomeBackupAuto(agora: LocalDateTime): String =
        PREFIXO_BACKUP_AUTO + agora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))

    /** Dos nomes existentes, quais apagar para manter só os [manter] mais recentes (ignora arquivos alheios). */
    fun backupsParaApagar(nomes: List<String>, manter: Int = MANTER_BACKUPS_AUTO): List<String> =
        nomes.filter { it.startsWith(PREFIXO_BACKUP_AUTO) && it.endsWith(".rjebak") }
            .sortedDescending()
            .drop(manter)
}
