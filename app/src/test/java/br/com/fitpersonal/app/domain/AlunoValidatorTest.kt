package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AlunoValidatorTest {
    private val hoje = LocalDate.of(2026, 10, 14)

    @Test fun nomeObrigatorio() {
        val r = AlunoValidator.validar(null, AlunoInput(), hoje) as ResultadoAluno.Erros
        assertTrue(CampoAluno.NOME in r.erros)
    }

    @Test fun camposOpcionaisVaziosSaoValidos() {
        val r = AlunoValidator.validar(null, AlunoInput(nome = " Ana "), hoje)
        assertTrue(r is ResultadoAluno.Ok)
        assertEquals("Ana", (r as ResultadoAluno.Ok).aluno.nome)
    }

    @Test fun validaCamposInformados() {
        val r = AlunoValidator.validar(
            null,
            AlunoInput(nome = "Ana", dataNascimento = "31022000", telefone = "123", email = "x", cpf = "111", cep = "1"),
            hoje,
        ) as ResultadoAluno.Erros
        assertEquals(
            setOf(CampoAluno.DATA_NASCIMENTO, CampoAluno.TELEFONE, CampoAluno.EMAIL, CampoAluno.CPF, CampoAluno.CEP),
            r.erros.keys,
        )
    }

    @Test fun dataFuturaInvalida() {
        assertNull(AlunoValidator.parseData("01012030", hoje))
        assertEquals(LocalDate.of(1990, 5, 20), AlunoValidator.parseData("20051990", hoje))
    }

    @Test fun consentimentoRegistraDataUmaVezESePodeRevogar() {
        val marcado = AlunoInput(nome = "Ana", consentimento = true)
        val a1 = (AlunoValidator.validar(null, marcado, hoje) as ResultadoAluno.Ok).aluno
        assertEquals(hoje, a1.consentimentoLgpdEm)

        // Reabrir e salvar de novo não muda a data original
        val depois = hoje.plusDays(30)
        val a2 = (AlunoValidator.validar(a1, AlunoValidator.paraInput(a1), depois) as ResultadoAluno.Ok).aluno
        assertEquals(hoje, a2.consentimentoLgpdEm)

        // Desmarcar revoga
        val a3 = (AlunoValidator.validar(a2, AlunoValidator.paraInput(a2).copy(consentimento = false), depois) as ResultadoAluno.Ok).aluno
        assertNull(a3.consentimentoLgpdEm)

        // Sem marcar, não há consentimento
        val a4 = (AlunoValidator.validar(null, AlunoInput(nome = "Bia"), hoje) as ResultadoAluno.Ok).aluno
        assertNull(a4.consentimentoLgpdEm)
    }

    @Test fun ida_e_volta() {
        val input = AlunoInput(nome = "Ana", dataNascimento = "20051990", cpf = "52998224725", instagram = "@ana")
        val a = (AlunoValidator.validar(null, input, hoje) as ResultadoAluno.Ok).aluno
        assertEquals("ana", a.instagram)
        assertEquals("20051990", AlunoValidator.paraInput(a).dataNascimento)
    }
}
