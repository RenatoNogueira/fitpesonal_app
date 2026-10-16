package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.Saudacao
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SaudacaoTest {
    private val hoje = LocalDate.of(2026, 10, 14)   // quarta-feira

    @Test fun saudacaoPorHora() {
        assertEquals("Boa noite", Saudacao.porHora(2))
        assertEquals("Bom dia", Saudacao.porHora(5))
        assertEquals("Bom dia", Saudacao.porHora(11))
        assertEquals("Boa tarde", Saudacao.porHora(12))
        assertEquals("Boa tarde", Saudacao.porHora(17))
        assertEquals("Boa noite", Saudacao.porHora(18))
    }

    @Test fun primeiroNome() {
        assertEquals("Maria", Saudacao.primeiroNome("  Maria da Silva "))
        assertEquals("", Saudacao.primeiroNome("  "))
    }

    @Test fun rotulosDeData() {
        assertEquals("Hoje", Saudacao.rotuloData(hoje, hoje))
        assertEquals("Amanhã", Saudacao.rotuloData(hoje.plusDays(1), hoje))
        assertEquals("Ontem", Saudacao.rotuloData(hoje.minusDays(1), hoje))
        assertEquals("20/10", Saudacao.rotuloData(LocalDate.of(2026, 10, 20), hoje))
    }

    @Test fun datasEmPortugues() {
        assertEquals("Quarta-feira, 14 de outubro", Saudacao.dataExtensa(hoje))
        assertEquals("QUA", Saudacao.diaSemanaCurto(hoje))
    }
}
