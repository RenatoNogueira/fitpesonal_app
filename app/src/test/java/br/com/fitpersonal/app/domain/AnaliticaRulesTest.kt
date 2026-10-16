package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.AnaliticaRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnaliticaRulesTest {
    @Test fun telasPrincipais() {
        assertEquals("inicio", AnaliticaRules.nomeDaTela("inicio"))
        assertEquals("lista_espera", AnaliticaRules.nomeDaTela("espera"))
        assertEquals("avaliacoes", AnaliticaRules.nomeDaTela("avaliacoes"))
    }

    @Test fun argumentosDaRotaNaoVazam() {
        assertEquals("aluno", AnaliticaRules.nomeDaTela("aluno/{id}"))
        assertEquals("avaliacao", AnaliticaRules.nomeDaTela("avaliacao/{id}?aluno={aluno}"))
        assertEquals("treino_executar", AnaliticaRules.nomeDaTela("treino/{id}/executar"))
        assertFalse(AnaliticaRules.nomeDaTela("aluno/42")!!.contains("42"))
    }

    @Test fun rotaVazia() {
        assertNull(AnaliticaRules.nomeDaTela(null))
        assertNull(AnaliticaRules.nomeDaTela("  "))
    }

    @Test fun formularios() {
        assertTrue(AnaliticaRules.ehFormulario("aluno/{id}"))
        assertTrue(AnaliticaRules.ehFormulario("treino/{id}?aluno={aluno}"))
        assertTrue(AnaliticaRules.ehFormulario("agendamento/{id}"))
        assertFalse(AnaliticaRules.ehFormulario("treino/{id}/executar"))
        assertFalse(AnaliticaRules.ehFormulario("alunos"))
        assertFalse(AnaliticaRules.ehFormulario(null))
    }

    @Test fun chaveSegura() {
        assertEquals("aluno_criado", AnaliticaRules.chaveSegura("Aluno criado"))
        assertEquals("pagamento_registrado", AnaliticaRules.chaveSegura(" Pagamento registrado! "))
        assertTrue(AnaliticaRules.chaveSegura("a".repeat(80)).length <= 40)
    }
}
