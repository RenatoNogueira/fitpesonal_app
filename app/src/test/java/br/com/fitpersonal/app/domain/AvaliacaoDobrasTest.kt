package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.Dobra
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import br.com.fitpersonal.app.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AvaliacaoDobrasTest {
    private val hoje = LocalDate.of(2026, 10, 14)
    private val base = AvaliacaoFisica(idAluno = 1)

    @Test fun salvaDobrasEProtocolo() {
        val input = AvaliacaoInput(
            dobras = mapOf(Dobra.PEITORAL to "15,5", Dobra.COXA to "20"),
            protocolo = ProtocoloDobras.POLLOCK_7,
        )
        val a = (AvaliacaoValidator.validar(base, input, hoje) as ResultadoAvaliacao.Ok).avaliacao
        assertEquals(ProtocoloDobras.POLLOCK_7, a.protocoloDobras)
        assertEquals(15.5, a.dobrasMm[Dobra.PEITORAL]!!, 0.0)
        assertEquals(2, a.dobrasMm.size)
    }

    @Test fun semDobrasNaoGravaProtocolo() {
        val a = (AvaliacaoValidator.validar(base, AvaliacaoInput(), hoje) as ResultadoAvaliacao.Ok).avaliacao
        assertNull(a.protocoloDobras)
        assertTrue(a.dobrasMm.isEmpty())
    }

    @Test fun rejeitaDobraForaDaFaixa() {
        val r = AvaliacaoValidator.validar(
            base, AvaliacaoInput(dobras = mapOf(Dobra.TRICEPS to "150", Dobra.COXA to "0")), hoje,
        ) as ResultadoAvaliacao.Erros
        assertEquals(setOf(CampoAvaliacao.DOBRA_TRICEPS, CampoAvaliacao.DOBRA_COXA), r.erros.keys)
    }

    @Test fun idaEVolta() {
        val a = base.copy(protocoloDobras = ProtocoloDobras.POLLOCK_3, dobrasMm = mapOf(Dobra.ABDOMINAL to 22.5))
        val i = AvaliacaoValidator.paraInput(a)
        assertEquals("22,5", i.dobras[Dobra.ABDOMINAL])
        assertEquals(ProtocoloDobras.POLLOCK_3, i.protocolo)
    }
}
