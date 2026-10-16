package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.Meta
import br.com.fitpersonal.app.domain.rules.MetaRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.ReavaliacaoRules
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MetaRulesTest {

    @Test fun metaDeReducao() {
        val p = MetaRules.progresso(inicial = 90.0, atual = 85.0, alvo = 80.0)
        assertEquals(0.5, p.fracao, 1e-9)
        assertFalse(p.atingida)
        assertEquals(-5.0, p.falta, 1e-9)
    }

    @Test fun metaDeGanho() {
        val p = MetaRules.progresso(inicial = 50.0, atual = 55.0, alvo = 60.0)
        assertEquals(0.5, p.fracao, 1e-9)
        assertEquals(5.0, p.falta, 1e-9)
    }

    @Test fun atingidaEFracaoLimitada() {
        val p = MetaRules.progresso(90.0, 78.0, 80.0)
        assertTrue(p.atingida)
        assertEquals(1.0, p.fracao, 1e-9)
        // andou para o lado errado: não fica negativo
        assertEquals(0.0, MetaRules.progresso(90.0, 95.0, 80.0).fracao, 1e-9)
    }

    @Test fun alvoIgualAoInicial() {
        assertTrue(MetaRules.progresso(80.0, 80.0, 80.0).atingida)
        assertFalse(MetaRules.progresso(80.0, 82.0, 80.0).atingida)
    }

    @Test fun metaSalvaInvalidaVira_null() {
        assertNull(MetaRules.meta(null, 10.0))
        assertNull(MetaRules.meta("INEXISTENTE", 10.0))
        assertNull(MetaRules.meta("PESO", null))
        assertNull(MetaRules.meta("PESO", 0.0))
        assertEquals(Meta(Metrica.PESO, 70.0), MetaRules.meta("PESO", 70.0))
    }

    private val hoje = LocalDate.of(2026, 5, 20)

    @Test fun reavaliacaoSemProgramacao() {
        assertNull(ReavaliacaoRules.situacao(hoje, null, hoje))
        assertNull(ReavaliacaoRules.situacao(hoje, 0, hoje))
    }

    @Test fun reavaliacaoNuncaAvaliado() {
        assertEquals(SituacaoReavaliacao.SemAvaliacao, ReavaliacaoRules.situacao(null, 30, hoje))
    }

    @Test fun reavaliacaoFaixas() {
        val ultima = hoje.minusDays(30)
        assertEquals(SituacaoReavaliacao.Proxima(0), ReavaliacaoRules.situacao(ultima, 30, hoje))
        assertEquals(SituacaoReavaliacao.Atrasada(1), ReavaliacaoRules.situacao(ultima, 29, hoje))
        assertEquals(SituacaoReavaliacao.Proxima(7), ReavaliacaoRules.situacao(ultima, 37, hoje))
        assertEquals(SituacaoReavaliacao.EmDia(8), ReavaliacaoRules.situacao(ultima, 38, hoje))
    }

    @Test fun textoDoAviso() {
        assertNull(ReavaliacaoRules.textoAviso(0, 0))
        assertEquals("Reavaliações: 1 atrasada.", ReavaliacaoRules.textoAviso(1, 0))
        assertEquals("Reavaliações: 3 vencem em breve.", ReavaliacaoRules.textoAviso(0, 3))
        assertEquals("Reavaliações: 2 atrasadas e 1 vence em breve.", ReavaliacaoRules.textoAviso(2, 1))
    }

    @Test fun atencaoSoQuandoPrecisa() {
        assertTrue(ReavaliacaoRules.pedeAtencao(SituacaoReavaliacao.Atrasada(3)))
        assertFalse(ReavaliacaoRules.pedeAtencao(SituacaoReavaliacao.EmDia(20)))
        assertFalse(ReavaliacaoRules.pedeAtencao(null))
    }
}
