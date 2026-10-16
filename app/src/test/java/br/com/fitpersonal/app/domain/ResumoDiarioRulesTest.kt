package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.ResumoDiarioRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumoDiarioRulesTest {

    @Test fun agenda() {
        assertNull(ResumoDiarioRules.agendaDoDia(0, null))
        assertEquals("Hoje: 1 sessão, a primeira às 08:00.", ResumoDiarioRules.agendaDoDia(1, "08:00"))
        assertEquals("Hoje: 3 sessões.", ResumoDiarioRules.agendaDoDia(3, null))
    }

    @Test fun mensalidades() {
        assertNull(ResumoDiarioRules.mensalidades(0, 0))
        assertEquals("1 aluno com mensalidade em atraso.", ResumoDiarioRules.mensalidades(1, 0))
        assertEquals(
            "2 alunos com mensalidade em atraso (R$ 300,00 em aberto).",
            ResumoDiarioRules.mensalidades(2, 30_000),
        )
    }

    @Test fun aniversarios() {
        assertNull(ResumoDiarioRules.aniversarios(emptyList()))
        assertEquals("Hoje é aniversário de Ana.", ResumoDiarioRules.aniversarios(listOf("Ana Souza")))
        assertEquals("Hoje é aniversário de Ana e Bruno.", ResumoDiarioRules.aniversarios(listOf("Ana Souza", "Bruno Lima")))
        assertEquals(
            "Hoje é aniversário de Ana, Bruno e mais 1.",
            ResumoDiarioRules.aniversarios(listOf("Ana S", "Bruno L", "Caio M")),
        )
    }
}
