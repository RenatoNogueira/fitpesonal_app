package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.LembreteAluno
import br.com.fitpersonal.app.domain.rules.LembreteAlunoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class LembreteAlunoRulesTest {

    // 2026-05-18 é segunda-feira
    private val segunda = LocalDate.of(2026, 5, 18)
    private val config = LembreteAluno(ativo = true, dias = LembreteAluno.DIAS_PADRAO, minutos = 18 * 60)

    @Test fun mascaraDeDias() {
        assertTrue(LembreteAlunoRules.diaMarcado(LembreteAluno.DIAS_PADRAO, DayOfWeek.MONDAY))
        assertTrue(LembreteAlunoRules.diaMarcado(LembreteAluno.DIAS_PADRAO, DayOfWeek.WEDNESDAY))
        assertFalse(LembreteAlunoRules.diaMarcado(LembreteAluno.DIAS_PADRAO, DayOfWeek.TUESDAY))
        val sem = LembreteAlunoRules.alternar(LembreteAluno.DIAS_PADRAO, DayOfWeek.MONDAY)
        assertFalse(LembreteAlunoRules.diaMarcado(sem, DayOfWeek.MONDAY))
        assertTrue(LembreteAlunoRules.diaMarcado(LembreteAlunoRules.alternar(sem, DayOfWeek.SUNDAY), DayOfWeek.SUNDAY))
    }

    @Test fun diaMarcadoAvisaComTreinoEDivisao() {
        val a = LembreteAlunoRules.avisoDeHoje(segunda, config, segunda.minusDays(2), "Hipertrofia ABC", "B")!!
        assertTrue(a.titulo.contains("dia de treinar"))
        assertTrue(a.texto.contains("Hipertrofia ABC"))
        assertTrue(a.texto.contains("Treino B"))
    }

    @Test fun jaTreinouHojeNaoAvisa() {
        assertNull(LembreteAlunoRules.avisoDeHoje(segunda, config, segunda, "X", "A"))
    }

    @Test fun desligadoNaoAvisa() {
        assertNull(LembreteAlunoRules.avisoDeHoje(segunda, config.copy(ativo = false), null, "X", "A"))
    }

    @Test fun retencaoEntre3e14Dias() {
        val terca = segunda.plusDays(1)   // dia não marcado
        assertNull(LembreteAlunoRules.avisoDeHoje(terca, config, terca.minusDays(2), null, null))
        val a = LembreteAlunoRules.avisoDeHoje(terca, config, terca.minusDays(4), null, null)
        assertNotNull(a)
        assertTrue(a!!.texto.contains("4 dias"))
        assertNull(LembreteAlunoRules.avisoDeHoje(terca, config, terca.minusDays(30), null, null))
    }

    @Test fun proximaDivisaoRodaEmOrdem() {
        val ds = listOf("C", "A", "B")
        assertEquals("A", LembreteAlunoRules.proximaDivisao(ds, null))
        assertEquals("B", LembreteAlunoRules.proximaDivisao(ds, "A"))
        assertEquals("A", LembreteAlunoRules.proximaDivisao(ds, "C"))
        assertNull(LembreteAlunoRules.proximaDivisao(emptyList(), "A"))
    }
}
