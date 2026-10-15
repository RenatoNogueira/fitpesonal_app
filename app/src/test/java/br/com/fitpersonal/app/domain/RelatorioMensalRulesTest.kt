package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.StatusAgendamento.CANCELADO
import br.com.fitpersonal.app.domain.model.StatusAgendamento.CONCLUIDO
import br.com.fitpersonal.app.domain.model.StatusAgendamento.PENDENTE
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.LinhaAgendaMes
import br.com.fitpersonal.app.domain.rules.RecorrenciaRules
import br.com.fitpersonal.app.domain.rules.RelatorioMensalRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RelatorioMensalRulesTest {

    private fun l(nome: String, tipo: TipoAgendamento, status: br.com.fitpersonal.app.domain.model.StatusAgendamento) =
        LinhaAgendaMes(nome, tipo, status)

    @Test fun resumoContaStatusTipoEAluno() {
        val r = RelatorioMensalRules.resumir(
            listOf(
                l("Bia", TipoAgendamento.TREINO, CONCLUIDO),
                l("Bia", TipoAgendamento.TREINO, CONCLUIDO),
                l("Ana", TipoAgendamento.AVALIACAO, CONCLUIDO),
                l("Ana", TipoAgendamento.TREINO, CANCELADO),
                l("Caio", TipoAgendamento.CONSULTORIA, PENDENTE),
            ),
        )
        assertEquals(5, r.total)
        assertEquals(3, r.concluidos)
        assertEquals(1, r.cancelados)
        assertEquals(1, r.pendentes)
        assertEquals(0.75, r.taxaComparecimento!!, 1e-9)
        assertEquals(3, r.porTipo[TipoAgendamento.TREINO])
        assertEquals(listOf("Bia", "Ana", "Caio"), r.porAluno.map { it.nome })
    }

    @Test fun mesVazioOuSoPendente() {
        val vazio = RelatorioMensalRules.resumir(emptyList())
        assertEquals(0, vazio.total)
        assertNull(vazio.taxaComparecimento)
        assertTrue(vazio.porTipo.isEmpty())

        val pendente = RelatorioMensalRules.resumir(listOf(l("Ana", TipoAgendamento.TREINO, PENDENTE)))
        assertNull(pendente.taxaComparecimento)
    }

    @Test fun recorrenciaSemanal() {
        val inicio = LocalDate.of(2026, 1, 28)
        val d = RecorrenciaRules.datas(inicio, 3)
        assertEquals(listOf(inicio, LocalDate.of(2026, 2, 4), LocalDate.of(2026, 2, 11)), d)
        assertEquals(1, RecorrenciaRules.datas(inicio, 0).size)
        assertEquals(RecorrenciaRules.MAX_OCORRENCIAS, RecorrenciaRules.datas(inicio, 99).size)
    }
}
