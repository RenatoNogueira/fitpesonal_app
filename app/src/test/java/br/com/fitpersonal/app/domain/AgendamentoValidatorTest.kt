package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AgendamentoValidatorTest {

    @Test fun horarioValido() {
        assertEquals(LocalTime.of(9, 30), AgendamentoValidator.parseHorario("0930"))
        assertNull(AgendamentoValidator.parseHorario("2460"))
        assertNull(AgendamentoValidator.parseHorario("930"))
    }

    @Test fun alunoEHorarioObrigatorios() {
        val r = AgendamentoValidator.validar(null, AgendamentoInput()) as ResultadoAgendamento.Erros
        assertEquals(setOf(CampoAgendamento.ALUNO, CampoAgendamento.HORARIO), r.erros.keys)
    }

    @Test fun salvaEPreservaId() {
        val d = LocalDate.of(2026, 10, 20)
        val novo = (AgendamentoValidator.validar(
            null, AgendamentoInput(7, d, "0800", TipoAgendamento.AVALIACAO),
        ) as ResultadoAgendamento.Ok).agendamento
        assertEquals(LocalTime.of(8, 0), novo.horario)

        val editado = (AgendamentoValidator.validar(
            novo.copy(id = 5),
            AgendamentoInput(7, d, "0900", TipoAgendamento.AVALIACAO, StatusAgendamento.CONCLUIDO),
        ) as ResultadoAgendamento.Ok).agendamento
        assertEquals(5L, editado.id)
        assertEquals(LocalTime.of(9, 0), editado.horario)
        assertEquals(StatusAgendamento.CONCLUIDO, editado.status)
    }
}
