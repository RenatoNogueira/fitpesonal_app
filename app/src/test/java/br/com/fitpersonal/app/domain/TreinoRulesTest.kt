package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioCatalogo
import br.com.fitpersonal.app.domain.model.ExercicioTreino
import br.com.fitpersonal.app.domain.rules.TreinoRules
import org.junit.Assert.*
import org.junit.Test

class TreinoRulesTest {

    private fun ex(nome: String, d: DivisaoTreino) = ExercicioTreino(idTreino = 1, nome = nome, divisao = d)

    @Test fun nomeObrigatorio() {
        assertNotNull(TreinoRules.validarNome("  "))
        assertNull(TreinoRules.validarNome("Hipertrofia"))
    }

    @Test fun agrupaMantendoDivisoesVazias() {
        val g = TreinoRules.agruparPorDivisao(listOf(ex("Supino", DivisaoTreino.A), ex("Agachamento", DivisaoTreino.C)))
        assertEquals(4, g.size)
        assertEquals(1, g[DivisaoTreino.A]!!.size)
        assertTrue(g[DivisaoTreino.B]!!.isEmpty())
    }

    @Test fun convertePraDivisaoEscolhida() {
        val cat = ExercicioCatalogo("1", "Remada", "forca", "iniciante", "barra", listOf("costas"), emptyList(), emptyList(), emptyList())
        val r = TreinoRules.doCatalogo(listOf(cat), DivisaoTreino.B)
        assertEquals(DivisaoTreino.B, r.single().divisao)
        assertEquals("Remada", r.single().nome)
    }

    @Test fun resumoIgnoraVazios() {
        val e = ex("Supino", DivisaoTreino.A).copy(series = "3", repeticoes = "12", descanso = "60 s")
        assertEquals("3 x 12 • 60 s", TreinoRules.resumo(e))
        assertEquals("", TreinoRules.resumo(ex("X", DivisaoTreino.A)))
    }
}
