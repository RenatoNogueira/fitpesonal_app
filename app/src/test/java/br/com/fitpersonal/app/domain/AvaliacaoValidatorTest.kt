package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AvaliacaoValidatorTest {
    private val hoje = LocalDate.of(2026, 10, 14)
    private val base = AvaliacaoFisica(idAluno = 1)

    @Test fun dataVaziaUsaHoje() {
        val r = AvaliacaoValidator.validar(base, AvaliacaoInput(), hoje) as ResultadoAvaliacao.Ok
        assertEquals(hoje, r.avaliacao.data)
    }

    @Test fun aceitaVirgulaECalculaComposicao() {
        val r = AvaliacaoValidator.validar(
            base, AvaliacaoInput(peso = "80,0", altura = "1,80", percentualGordura = "25"), hoje,
        ) as ResultadoAvaliacao.Ok
        assertEquals(20.0, r.avaliacao.massaGordaKg!!, 0.001)
        assertEquals(60.0, r.avaliacao.massaMagraKg!!, 0.001)
    }

    @Test fun alturaEmCentimetrosEhRejeitada() {
        val r = AvaliacaoValidator.validar(base, AvaliacaoInput(altura = "175"), hoje) as ResultadoAvaliacao.Erros
        assertTrue(CampoAvaliacao.ALTURA in r.erros)
    }

    @Test fun pressaoForaDaFaixa() {
        val r = AvaliacaoValidator.validar(
            base, AvaliacaoInput(pressaoSistolica = "999", pressaoDiastolica = "5"), hoje,
        ) as ResultadoAvaliacao.Erros
        assertEquals(setOf(CampoAvaliacao.PAS, CampoAvaliacao.PAD), r.erros.keys)
    }

    @Test fun resumoGeraAlertas() {
        val s = AvaliacaoValidator.resumir(
            AvaliacaoInput(peso = "100", altura = "1,70", percentualGordura = "32", ldl = "170",
                pressaoSistolica = "150", pressaoDiastolica = "95"),
        )
        assertEquals(ClasseImc.OBESIDADE_1, s.classeImc)
        assertEquals(ClassePressao.HIPERTENSAO_1, s.pressao)
        assertTrue("LDL alto" in s.alertas)
        assertTrue("Hipertensão Estágio 1" in s.alertas)
    }

    @Test fun idaEVolta() {
        val a = base.copy(pesoKg = 72.5, alturaM = 1.75, condicoes = setOf(CondicaoSaude.DIABETES), pressaoSistolica = 120)
        val i = AvaliacaoValidator.paraInput(a)
        assertEquals("72,5", i.peso)
        val r = AvaliacaoValidator.validar(base, i, hoje) as ResultadoAvaliacao.Ok
        assertEquals(72.5, r.avaliacao.pesoKg!!, 0.0)
        assertEquals(setOf(CondicaoSaude.DIABETES), r.avaliacao.condicoes)
    }
}
