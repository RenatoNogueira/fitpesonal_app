package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.rules.BuscaRules
import br.com.fitpersonal.app.domain.rules.FiltroAluno
import br.com.fitpersonal.app.domain.rules.ParqRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BuscaRulesTest {

    private val hoje = LocalDate.of(2026, 5, 20)
    private fun aluno(
        nome: String = "João da Silva",
        objetivo: String? = null,
        nasc: LocalDate? = null,
        reav: Int? = null,
        consent: LocalDate? = hoje,
    ) = Aluno(nome = nome, objetivo = objetivo, dataNascimento = nasc, reavaliacaoDias = reav, consentimentoLgpdEm = consent)

    @Test fun ignoraAcentoECaixa() {
        assertTrue(BuscaRules.corresponde("joao", "João da Silva"))
        assertTrue(BuscaRules.corresponde("SILVA joão", "João da Silva"))
        assertFalse(BuscaRules.corresponde("maria", "João da Silva"))
    }

    @Test fun termoVazioAceitaTudo() {
        assertTrue(BuscaRules.corresponde("   ", "qualquer"))
    }

    @Test fun procuraEmVariosCampos() {
        val a = aluno(objetivo = "Hipertrofia")
        assertTrue(BuscaRules.alunoCorresponde("hipertrofia", a))
        assertTrue(BuscaRules.alunoCorresponde("silva hiper", a))
        assertFalse(BuscaRules.alunoCorresponde("emagrecimento", a))
    }

    @Test fun filtros() {
        assertTrue(BuscaRules.passaFiltro(FiltroAluno.ANIVERSARIO, aluno(nasc = LocalDate.of(1990, 5, 3)), null, hoje))
        assertFalse(BuscaRules.passaFiltro(FiltroAluno.ANIVERSARIO, aluno(nasc = LocalDate.of(1990, 6, 3)), null, hoje))
        assertTrue(BuscaRules.passaFiltro(FiltroAluno.SEM_AVALIACAO, aluno(), null, hoje))
        assertFalse(BuscaRules.passaFiltro(FiltroAluno.SEM_AVALIACAO, aluno(), hoje, hoje))
        assertTrue(BuscaRules.passaFiltro(FiltroAluno.SEM_CONSENTIMENTO, aluno(consent = null), hoje, hoje))
        // reavaliação: vencida há 10 dias
        assertTrue(BuscaRules.passaFiltro(FiltroAluno.REAVALIACAO, aluno(reav = 30), hoje.minusDays(40), hoje))
        // sem intervalo programado não entra
        assertFalse(BuscaRules.passaFiltro(FiltroAluno.REAVALIACAO, aluno(reav = null), hoje.minusDays(400), hoje))
    }

    @Test fun parqCodificaEDecodifica() {
        val respostas = listOf(false, true, null, false, false, false, false)
        val cod = ParqRules.codificar(respostas)
        assertEquals("NS-NNNN", cod)
        assertEquals(respostas, ParqRules.decodificar(cod))
        assertTrue(ParqRules.requerLiberacao(cod))
        assertEquals(listOf(1), ParqRules.respostasSim(cod))
        assertFalse(ParqRules.completo(cod))
    }

    @Test fun parqVazioOuCurto() {
        assertFalse(ParqRules.aplicado(null))
        assertFalse(ParqRules.aplicado("-------"))
        assertFalse(ParqRules.requerLiberacao("NNNNNNN"))
        assertTrue(ParqRules.completo("NNNNNNN"))
        assertEquals(7, ParqRules.decodificar("S").size)
    }
}
