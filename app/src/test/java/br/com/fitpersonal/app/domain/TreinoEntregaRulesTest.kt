package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioTreino
import br.com.fitpersonal.app.domain.rules.TreinoEntregaRules
import org.junit.Assert.*
import org.junit.Test

class TreinoEntregaRulesTest {

    private fun ex(nome: String, d: DivisaoTreino = DivisaoTreino.A, series: String? = "3", reps: String? = "12", obs: String? = null) =
        ExercicioTreino(idTreino = 1, nome = nome, divisao = d, series = series, repeticoes = reps, carga = "20 kg", observacoes = obs)

    private val grupos = mapOf(
        DivisaoTreino.A to listOf(ex("Supino reto"), ex("Remada", obs = "controlar a descida")),
        DivisaoTreino.B to listOf(ex("Agachamento", DivisaoTreino.B)),
    )

    @Test fun escapaHtml() {
        assertEquals("&lt;b&gt;&amp;&quot;x&quot;&#39;", TreinoEntregaRules.escapar("<b>&\"x\"'"))
    }

    @Test fun textoTemDivisoesNumeradasEObservacao() {
        val t = TreinoEntregaRules.texto("Maria", "Hipertrofia", "Ganhar massa", grupos, "Prof. Renato")
        assertTrue(t.startsWith("*Hipertrofia*"))
        assertTrue(t.contains("*Treino A*"))
        assertTrue(t.contains("1. Supino reto — 3 séries, 12 reps, carga 20 kg"))
        assertTrue(t.contains("2. Remada"))
        assertTrue(t.contains("_controlar a descida_"))
        assertTrue(t.contains("*Treino B*"))
        assertFalse(t.contains("*Treino C*"))
        assertTrue(t.endsWith("Prof. Renato"))
    }

    @Test fun htmlSemInjecaoEComAbasSoDasDivisoesUsadas() {
        val hostil = mapOf(DivisaoTreino.A to listOf(ex("<script>alert(1)</script>")))
        val h = TreinoEntregaRules.html("Maria <b>", "Treino \"A\"", null, "20/05/2026", "", "", hostil)
        assertFalse(h.contains("<script>alert(1)</script>"))
        assertTrue(h.contains("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertTrue(h.contains("Maria &lt;b&gt;"))
        assertTrue(h.contains("data-d=\"A\""))
        assertFalse(h.contains("data-d=\"B\""))
    }

    @Test fun htmlSemExercicios() {
        val h = TreinoEntregaRules.html("Maria", "Treino", null, "hoje", "", "", emptyMap())
        assertTrue(h.contains("ainda não tem exercícios"))
        assertFalse(h.contains("<nav>"))
    }
}
