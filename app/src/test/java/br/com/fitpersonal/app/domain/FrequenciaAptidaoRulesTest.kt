package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.AptidaoRules
import br.com.fitpersonal.app.domain.rules.FrequenciaRules
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.domain.rules.TesteAptidao
import br.com.fitpersonal.app.domain.rules.TipoTeste
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class FrequenciaAptidaoRulesTest {

    private val hoje = LocalDate.of(2026, 5, 20)

    private fun ag(dia: Int, status: StatusAgendamento) = Agendamento(
        idAluno = 1, data = LocalDate.of(2026, 5, dia), horario = LocalTime.of(8, 0),
        tipo = TipoAgendamento.TREINO, status = status,
    )

    @Test fun contagemETaxa() {
        val r = FrequenciaRules.resumir(
            listOf(
                ag(1, StatusAgendamento.CONCLUIDO), ag(3, StatusAgendamento.CONCLUIDO), ag(5, StatusAgendamento.CONCLUIDO),
                ag(8, StatusAgendamento.FALTOU), ag(10, StatusAgendamento.CANCELADO),
                ag(25, StatusAgendamento.PENDENTE),   // futuro: ignorado
            ),
            hoje,
        )
        assertEquals(3, r.concluidos)
        assertEquals(1, r.faltas)
        assertEquals(1, r.cancelados)
        assertEquals(0.25, r.taxaFaltas!!, 1e-9)
        assertEquals(1, r.faltasSeguidas.coerceAtMost(1))
    }

    @Test fun faltasSeguidasIgnoramCanceladas() {
        val r = FrequenciaRules.resumir(
            listOf(
                ag(1, StatusAgendamento.CONCLUIDO),
                ag(5, StatusAgendamento.FALTOU),
                ag(8, StatusAgendamento.CANCELADO),
                ag(12, StatusAgendamento.FALTOU),
            ),
            hoje,
        )
        assertEquals(2, r.faltasSeguidas)
        assertTrue(FrequenciaRules.pedeAtencao(r))
    }

    @Test fun semSessoesNaoTemTaxa() {
        val r = FrequenciaRules.resumir(emptyList(), hoje)
        assertNull(r.taxaFaltas)
        assertFalse(FrequenciaRules.pedeAtencao(r))
    }

    @Test fun nomeDoDia() {
        assertEquals("Segunda-feira", FrequenciaRules.rotuloDia(DayOfWeek.MONDAY))
    }

    @Test fun cooperVo2() {
        assertEquals(44.73 * 0 + (2400 - 504.9) / 44.73, AptidaoRules.vo2Cooper(2400.0)!!, 1e-9)
        assertNull(AptidaoRules.vo2Cooper(100.0))
    }

    @Test fun validacaoEEvolucao() {
        assertTrue(AptidaoRules.valido(TipoTeste.FLEXAO_BRACO, 25.0))
        assertFalse(AptidaoRules.valido(TipoTeste.FLEXAO_BRACO, -1.0))
        assertFalse(AptidaoRules.valido(TipoTeste.COOPER_12MIN, 50.0))
        assertEquals(SentidoVariacao.MELHOROU, AptidaoRules.sentido(20.0, 25.0, 0))
        assertEquals(SentidoVariacao.PIOROU, AptidaoRules.sentido(25.0, 20.0, 0))
        assertEquals(SentidoVariacao.IGUAL, AptidaoRules.sentido(20.0, 20.0, 0))
    }

    @Test fun serieFiltraETipoEOrdena() {
        val lista = listOf(
            TesteAptidao(id = 2, idAluno = 1, data = LocalDate.of(2026, 5, 2), tipo = TipoTeste.PRANCHA, valor = 60.0),
            TesteAptidao(id = 1, idAluno = 1, data = LocalDate.of(2026, 4, 2), tipo = TipoTeste.PRANCHA, valor = 45.0),
            TesteAptidao(id = 3, idAluno = 1, data = LocalDate.of(2026, 4, 9), tipo = TipoTeste.COOPER_12MIN, valor = 2000.0),
        )
        val s = AptidaoRules.serie(lista, TipoTeste.PRANCHA)
        assertEquals(listOf(45.0, 60.0), s.map { it.valor })
    }
}
