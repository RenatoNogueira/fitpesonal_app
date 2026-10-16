package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import java.time.LocalDate
import java.time.LocalTime

enum class CampoAgendamento { ALUNO, HORARIO }

data class AgendamentoInput(
    val idAluno: Long? = null,
    val data: LocalDate = LocalDate.now(),
    val horario: String = "",              // HHmm (somente dígitos)
    val tipo: TipoAgendamento = TipoAgendamento.TREINO,
    val status: StatusAgendamento = StatusAgendamento.PENDENTE,
    val anotacao: String = "",
)

sealed interface ResultadoAgendamento {
    data class Ok(val agendamento: Agendamento) : ResultadoAgendamento
    data class Erros(val erros: Map<CampoAgendamento, String>) : ResultadoAgendamento
}

object AgendamentoValidator {

    fun parseHorario(hhmm: String): LocalTime? {
        if (hhmm.length != 4 || !hhmm.all(Char::isDigit)) return null
        val h = hhmm.substring(0, 2).toInt()
        val m = hhmm.substring(2).toInt()
        return if (h in 0..23 && m in 0..59) LocalTime.of(h, m) else null
    }

    fun horarioParaInput(t: LocalTime) = "%02d%02d".format(t.hour, t.minute)

    fun validar(base: Agendamento?, i: AgendamentoInput): ResultadoAgendamento {
        val erros = linkedMapOf<CampoAgendamento, String>()
        if (i.idAluno == null) erros[CampoAgendamento.ALUNO] = "Selecione um aluno."
        val horario = parseHorario(i.horario)
        if (horario == null) erros[CampoAgendamento.HORARIO] = "Horário inválido. Use HH:MM (24h)."
        if (erros.isNotEmpty()) return ResultadoAgendamento.Erros(erros)

        return ResultadoAgendamento.Ok(
            (base ?: Agendamento(idAluno = i.idAluno!!, data = i.data, horario = horario!!, tipo = i.tipo)).copy(
                idAluno = i.idAluno!!, data = i.data, horario = horario!!, tipo = i.tipo, status = i.status,
                anotacao = i.anotacao.trim().ifEmpty { null },
            )
        )
    }

    fun paraInput(a: Agendamento) = AgendamentoInput(
        idAluno = a.idAluno, data = a.data, horario = horarioParaInput(a.horario), tipo = a.tipo, status = a.status,
        anotacao = a.anotacao.orEmpty(),
    )
}
