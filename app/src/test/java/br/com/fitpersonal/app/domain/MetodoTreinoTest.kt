package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.MetodoRules
import br.com.fitpersonal.app.domain.rules.MetodoTreino
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetodoTreinoTest {
    @Test fun idsConhecidosEDesconhecidos() {
        assertEquals(MetodoTreino.EMOM, MetodoTreino.de("EMOM"))
        assertNull(MetodoTreino.de(null))
        assertNull(MetodoTreino.de("TABATA_DO_FUTURO"))
    }

    @Test fun biSetAgrupaDoisExerciciosSeguidos() {
        val m = listOf(null, "BISET", "BISET", null)
        assertNull(MetodoRules.posicaoNoBloco(m, 0))
        assertEquals(1 to 2, MetodoRules.posicaoNoBloco(m, 1))
        assertEquals(2 to 2, MetodoRules.posicaoNoBloco(m, 2))
        assertNull(MetodoRules.posicaoNoBloco(m, 3))
    }

    @Test fun exercicioSozinhoNaoFormaBloco() {
        assertNull(MetodoRules.posicaoNoBloco(listOf("BISET", null, "BISET"), 0))
    }

    @Test fun circuitoComVarios() {
        val m = listOf("CIRCUITO", "CIRCUITO", "CIRCUITO", "BISET")
        assertEquals(3 to 3, MetodoRules.posicaoNoBloco(m, 2))
    }

    @Test fun metodosSemAgrupamentoNaoViramBloco() {
        assertNull(MetodoRules.posicaoNoBloco(listOf("EMOM", "EMOM"), 0))
    }

    @Test fun indiceForaDaListaNaoQuebra() {
        assertNull(MetodoRules.posicaoNoBloco(emptyList(), 0))
    }
}
