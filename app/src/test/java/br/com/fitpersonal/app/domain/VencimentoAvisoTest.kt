package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.ResumoDiarioRules
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VencimentoAvisoTest {
    @Test fun semNadaNaoAvisa() {
        assertNull(ResumoDiarioRules.vencendo(0, 1))
        assertNull(ResumoDiarioRules.vencendo(2, -1))
    }

    @Test fun textosPorPrazo() {
        assertTrue(ResumoDiarioRules.vencendo(1, 0)!!.contains("vence hoje"))
        assertTrue(ResumoDiarioRules.vencendo(1, 1)!!.contains("vence amanhã"))
        assertTrue(ResumoDiarioRules.vencendo(3, 2)!!.startsWith("3 mensalidades vencem em breve"))
        assertTrue(ResumoDiarioRules.vencendo(3, 2)!!.contains("em 2 dias"))
    }
}
