package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.CandidatoCidade
import br.com.fitpersonal.app.domain.rules.CidadeRules
import br.com.fitpersonal.app.domain.rules.ConsultaCidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CidadeRulesTest {
    @Test fun soCidade() = assertEquals(ConsultaCidade("Campinas", null), CidadeRules.interpretar("Campinas"))

    @Test fun cidadeEUfComHifen() = assertEquals(ConsultaCidade("Campinas", "SP"), CidadeRules.interpretar("Campinas - SP"))

    @Test fun cidadeEUfComBarraOuVirgula() {
        assertEquals(ConsultaCidade("Niterói", "RJ"), CidadeRules.interpretar("Niterói/RJ"))
        assertEquals(ConsultaCidade("Niterói", "RJ"), CidadeRules.interpretar("Niterói, RJ"))
    }

    @Test fun estadoPorExtenso() = assertEquals(ConsultaCidade("Curitiba", "PR"), CidadeRules.interpretar("Curitiba, Paraná"))

    @Test fun hifenDentroDoNomeNaoSepara() {
        assertEquals(ConsultaCidade("Embu-Guaçu", null), CidadeRules.interpretar("Embu-Guaçu"))
        assertEquals(ConsultaCidade("Embu-Guaçu", "SP"), CidadeRules.interpretar("Embu-Guaçu - SP"))
    }

    @Test fun siglaNoFimSemSeparador() = assertEquals(ConsultaCidade("Santos", "SP"), CidadeRules.interpretar("Santos SP"))

    @Test fun nomeComDePrepos() = assertEquals(ConsultaCidade("São José do Rio Preto", null), CidadeRules.interpretar("São José do Rio Preto"))

    @Test fun textoVazio() = assertEquals(ConsultaCidade("", null), CidadeRules.interpretar("   "))

    @Test fun parteFinalQueNaoEEstadoVira() = assertEquals(ConsultaCidade("Campinas", null), CidadeRules.interpretar("Campinas, Brasil"))

    @Test fun escolheOEstadoPedido() {
        val lista = listOf(
            CandidatoCidade("Bom Jesus", "Rio Grande do Sul", 1.0, 1.0),
            CandidatoCidade("Bom Jesus", "Paraíba", 2.0, 2.0),
        )
        assertEquals("Paraíba", CidadeRules.escolher(lista, "PB")!!.estado)
        assertEquals("Rio Grande do Sul", CidadeRules.escolher(lista, null)!!.estado)
        assertEquals("Rio Grande do Sul", CidadeRules.escolher(lista, "SP")!!.estado)
        assertNull(CidadeRules.escolher(emptyList(), "SP"))
    }

    @Test fun rotuloComSigla() {
        assertEquals("Campinas, SP", CidadeRules.rotulo(CandidatoCidade("Campinas", "São Paulo", 0.0, 0.0)))
        assertEquals("Campinas", CidadeRules.rotulo(CandidatoCidade("Campinas", null, 0.0, 0.0)))
    }
}
