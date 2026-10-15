package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.ExtensoRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensoRulesTest {

    @Test fun valoresSimples() {
        assertEquals("zero real", ExtensoRules.reais(0))
        assertEquals("um real", ExtensoRules.reais(100))
        assertEquals("dois reais", ExtensoRules.reais(200))
        assertEquals("um centavo", ExtensoRules.reais(1))
        assertEquals("cem reais", ExtensoRules.reais(10000))
    }

    @Test fun centenasEDezenas() {
        assertEquals("cento e cinquenta reais", ExtensoRules.reais(15000))
        assertEquals("cento e vinte e um reais", ExtensoRules.reais(12100))
        assertEquals("quinze reais", ExtensoRules.reais(1500))
        assertEquals("novecentos e noventa e nove reais", ExtensoRules.reais(99900))
    }

    @Test fun milhares() {
        assertEquals("mil reais", ExtensoRules.reais(100000))
        assertEquals("mil duzentos e cinquenta reais e noventa centavos", ExtensoRules.reais(125090))
        assertEquals("dois mil reais", ExtensoRules.reais(200000))
        assertEquals("dois mil e quinhentos reais", ExtensoRules.reais(250000))
        assertEquals("duzentos e trinta e quatro mil quinhentos e sessenta e sete reais", ExtensoRules.reais(23456700))
    }

    @Test fun centavosJuntoComReais() {
        assertEquals("cento e cinquenta reais e cinquenta centavos", ExtensoRules.reais(15050))
        assertEquals("um real e um centavo", ExtensoRules.reais(101))
    }

    @Test fun milhoes() {
        assertEquals("um milhão de reais", ExtensoRules.reais(100_000_000))
        assertEquals("dois milhões e quinhentos mil reais", ExtensoRules.reais(250_000_000))
    }
}
