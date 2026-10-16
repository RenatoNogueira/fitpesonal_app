package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.AniversarioRules
import br.com.fitpersonal.app.domain.rules.SituacaoAniversario
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AniversarioRulesTest {
    private val hoje = LocalDate.of(2026, 10, 14)

    @Test fun situacao() {
        assertEquals(SituacaoAniversario.HOJE, AniversarioRules.situacao(LocalDate.of(1990, 10, 14), hoje))
        assertEquals(SituacaoAniversario.AMANHA, AniversarioRules.situacao(LocalDate.of(1990, 10, 15), hoje))
        assertEquals(SituacaoAniversario.PROXIMO, AniversarioRules.situacao(LocalDate.of(1990, 10, 25), hoje))
        assertEquals(SituacaoAniversario.JA_PASSOU, AniversarioRules.situacao(LocalDate.of(1990, 10, 2), hoje))
    }

    @Test fun rotulosEIdade() {
        assertEquals("Hoje", AniversarioRules.rotuloDia(LocalDate.of(1990, 10, 14), hoje))
        assertEquals("Dia 25", AniversarioRules.rotuloDia(LocalDate.of(1990, 10, 25), hoje))
        assertEquals("faz 36 anos", AniversarioRules.descricaoIdade(LocalDate.of(1990, 10, 25), hoje))
        assertEquals("fez 36 anos", AniversarioRules.descricaoIdade(LocalDate.of(1990, 10, 2), hoje))
        assertEquals("faz 1 ano", AniversarioRules.descricaoIdade(LocalDate.of(2025, 10, 20), hoje))
    }

    @Test fun vinteENoveDeFevereiroEmAnoComum() {
        assertEquals(LocalDate.of(2026, 2, 28), AniversarioRules.aniversarioEm(LocalDate.of(2000, 2, 29), 2026))
    }

    @Test fun iniciais() {
        assertEquals("MS", AniversarioRules.iniciais("  maria  da silva "))
        assertEquals("A", AniversarioRules.iniciais("Ana"))
        assertEquals("", AniversarioRules.iniciais("  "))
    }

    @Test fun proximidadeEOrdenacao() {
        val hoje = LocalDate.of(2026, 5, 20)
        fun nasc(dia: Int) = LocalDate.of(1990, 5, dia)
        assertEquals(0, AniversarioRules.diasAte(nasc(20), hoje))
        assertEquals("Hoje", AniversarioRules.rotuloProximidade(nasc(20), hoje))
        assertEquals("Amanhã", AniversarioRules.rotuloProximidade(nasc(21), hoje))
        assertEquals("em 5 dias", AniversarioRules.rotuloProximidade(nasc(25), hoje))
        assertEquals("ontem", AniversarioRules.rotuloProximidade(nasc(19), hoje))
        assertEquals("há 10 dias", AniversarioRules.rotuloProximidade(nasc(10), hoje))

        val ordem = AniversarioRules.ordenar(listOf(10, 25, 20, 19, 22), hoje) { nasc(it) }
        assertEquals(listOf(20, 22, 25, 19, 10), ordem)
    }
}
