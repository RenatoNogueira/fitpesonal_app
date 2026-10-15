package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.PixRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixRulesTest {

    @Test fun codigoTemEstruturaEValor() {
        val c = PixRules.copiaECola("contato@fitpersonal.teste", 28_000, "Renato Nogueira", "São Paulo")!!
        assertTrue(c.startsWith("000201010211"))
        assertTrue(c.contains("br.gov.bcb.pix"))
        assertTrue(c.contains("0125contato@fitpersonal.teste"))
        assertTrue(c.contains("540628.00"))
        assertTrue(c.contains("5802BR"))
        assertTrue(c.contains("5915RENATO NOGUEIRA"))
        assertTrue(c.contains("6009SAO PAULO"))
        assertTrue(c.contains("62070503***"))
    }

    @Test fun crcFechaComOsQuatroUltimosCaracteres() {
        val c = PixRules.copiaECola("12345678909", 12_345, "Maria", "Rio")!!
        val corpo = c.dropLast(4)
        assertTrue(corpo.endsWith("6304"))
        assertEquals("%04X".format(PixRules.crc16(corpo)), c.takeLast(4))
    }

    @Test fun semValorOmiteOCampo54() {
        val c = PixRules.copiaECola("a@b.com", 0, "Fulano", "Cidade")!!
        // moeda (53) vem direto antes do país (58): não há campo 54 de valor
        assertTrue(c.contains("53039865802BR"))
    }

    @Test fun chaves() {
        assertEquals("fulano@exemplo.com", PixRules.normalizarChave("  Fulano@Exemplo.com "))
        assertEquals("+5511999990000", PixRules.normalizarChave("+55 (11) 99999-0000"))
        assertFalse(PixRules.chaveValida(""))
        assertFalse(PixRules.chaveValida("com espaco"))
        assertNull(PixRules.copiaECola("", 1000, "A", "B"))
        assertNotNull(PixRules.cobranca("a@b.com", 1000, "A", "B"))
    }

    @Test fun nomeELongoSaiTruncado() {
        val c = PixRules.copiaECola("a@b.com", 100, "Nome Muito Comprido Para o Campo Pix", "Cidade Muito Longa Demais")!!
        assertTrue(c.contains("5924NOME MUITO COMPRIDO PARA"))
        assertTrue(c.contains("6015CIDADE MUITO LO"))
    }
}
