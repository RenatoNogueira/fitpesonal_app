package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.NegocioRules
import br.com.fitpersonal.app.domain.rules.PacoteRules
import br.com.fitpersonal.app.domain.rules.SituacaoPacote
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class PacoteNegocioRulesTest {

    private val inicio = LocalDate.of(2026, 5, 1)

    private fun ag(
        dia: Int,
        status: StatusAgendamento = StatusAgendamento.CONCLUIDO,
        tipo: TipoAgendamento = TipoAgendamento.TREINO,
        mes: Int = 5,
    ) = Agendamento(
        idAluno = 1, data = LocalDate.of(2026, mes, dia), horario = LocalTime.of(8, 0), tipo = tipo, status = status,
    )

    @Test fun semPacote() {
        assertEquals(SituacaoPacote.SEM_PACOTE, PacoteRules.resumir(null, inicio, emptyList()).situacao)
        assertEquals(SituacaoPacote.SEM_PACOTE, PacoteRules.resumir(8, null, emptyList()).situacao)
    }

    @Test fun saldoBaixaSoComSessoesConcluidasQueConsomem() {
        val lista = listOf(
            ag(2), ag(4), ag(6),
            ag(8, status = StatusAgendamento.CANCELADO),
            ag(9, status = StatusAgendamento.PENDENTE),
            ag(10, tipo = TipoAgendamento.AVALIACAO),      // não consome
            ag(28, mes = 4),                               // antes do início do pacote
        )
        val r = PacoteRules.resumir(8, inicio, lista)
        assertEquals(3, r.usadas)
        assertEquals(5, r.saldo)
        assertEquals(SituacaoPacote.EM_USO, r.situacao)
    }

    @Test fun ultimaSessaoEEsgotado() {
        val seis = (1..6).map { ag(it) }
        assertEquals(SituacaoPacote.ULTIMA_SESSAO, PacoteRules.resumir(7, inicio, seis).situacao)
        assertTrue(PacoteRules.resumir(7, inicio, seis).precisaAtencao)

        val oito = (1..8).map { ag(it) }
        val r = PacoteRules.resumir(7, inicio, oito)
        assertEquals(SituacaoPacote.ESGOTADO, r.situacao)
        assertEquals(0, r.saldo)
        assertEquals(1, r.excedentes)
    }

    @Test fun textoDoAvisoDePacote() {
        assertNull(PacoteRules.textoAviso(0, 0))
        assertEquals("Pacotes: 1 esgotado.", PacoteRules.textoAviso(1, 0))
        assertEquals("Pacotes: 2 na última sessão.", PacoteRules.textoAviso(0, 2))
        assertEquals("Pacotes: 2 esgotados e 1 na última sessão.", PacoteRules.textoAviso(2, 1))
    }

    @Test fun negocioComparaComMesAnterior() {
        val hoje = LocalDate.of(2026, 5, 20)
        val r = NegocioRules.resumir(
            sessoesConcluidas = listOf(
                LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 6),
            ),
            recebimentos = listOf(
                YearMonth.of(2026, 5) to 30000L, YearMonth.of(2026, 5) to 20000L, YearMonth.of(2026, 4) to 40000L,
            ),
            hoje = hoje,
            metaSessoes = 10,
            metaReceitaCentavos = 100000,
        )
        assertEquals(3, r.sessoes)
        assertEquals(2, r.sessoesMesAnterior)
        assertEquals(1, r.deltaSessoes)
        assertEquals(50000L, r.receitaCentavos)
        assertEquals(10000L, r.deltaReceitaCentavos)
        assertEquals(0.3, r.fracaoSessoes!!, 1e-9)
        assertEquals(0.5, r.fracaoReceita!!, 1e-9)
    }

    @Test fun metaZeroOuAusenteNaoTemFracao() {
        val r = NegocioRules.resumir(emptyList(), emptyList(), LocalDate.of(2026, 5, 1), 0, null)
        assertNull(r.fracaoSessoes)
        assertNull(r.fracaoReceita)
        assertNull(r.metaSessoes)
    }

    @Test fun fracaoLimitadaAUm() {
        val r = NegocioRules.resumir(
            List(12) { LocalDate.of(2026, 5, 1) }, emptyList(), LocalDate.of(2026, 5, 20), 10, null,
        )
        assertEquals(1.0, r.fracaoSessoes!!, 1e-9)
    }
}
