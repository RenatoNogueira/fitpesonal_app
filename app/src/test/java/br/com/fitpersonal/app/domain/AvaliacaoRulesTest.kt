package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.AgendaRules
import br.com.fitpersonal.app.domain.rules.AvaliacaoRules
import br.com.fitpersonal.app.domain.rules.ClassePressao
import br.com.fitpersonal.app.domain.rules.ClasseImc
import br.com.fitpersonal.app.domain.rules.Validators
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AvaliacaoRulesTest {

    @Test fun imc() {
        assertEquals(22.86, AvaliacaoRules.imc(70.0, 1.75)!!, 0.01)
        assertNull(AvaliacaoRules.imc(70.0, 0.0))
        assertEquals(ClasseImc.NORMAL, AvaliacaoRules.classificarImc(22.86))
        assertEquals(ClasseImc.SOBREPESO, AvaliacaoRules.classificarImc(25.0))
    }

    @Test fun composicaoPreencheSoQuandoAusente() {
        val c = AvaliacaoRules.composicao(80.0, 25.0, null, null)
        assertEquals(20.0, c.massaGordaKg!!, 0.001)
        assertEquals(60.0, c.massaMagraKg!!, 0.001)
        val c2 = AvaliacaoRules.composicao(80.0, 25.0, 18.0, null)
        assertEquals(18.0, c2.massaGordaKg!!, 0.001)
    }

    @Test fun pressao() {
        assertEquals(ClassePressao.HIPOTENSAO, AvaliacaoRules.classificarPressao(85, 55))
        assertEquals(ClassePressao.NORMAL, AvaliacaoRules.classificarPressao(120, 80))
        assertEquals(ClassePressao.ELEVADA, AvaliacaoRules.classificarPressao(125, 80))
        assertEquals(ClassePressao.HIPERTENSAO_1, AvaliacaoRules.classificarPressao(140, 85))
        assertEquals(ClassePressao.HIPERTENSAO_2, AvaliacaoRules.classificarPressao(150, 100))
        assertEquals(ClassePressao.CRISE, AvaliacaoRules.classificarPressao(185, 90))
        assertNull(AvaliacaoRules.classificarPressao(null, null))
        assertEquals("12 por 8", AvaliacaoRules.pressaoPopular(120, 80))
    }

    @Test fun laboratoriais() {
        val a = AvaliacaoRules.alertasLaboratoriais(31.0, 165.0, 35.0)
        assertEquals(listOf("Gordura corporal elevada", "LDL alto", "HDL baixo"), a)
    }

    @Test fun cpfETelefone() {
        assertTrue(Validators.cpfValido("529.982.247-25"))
        assertFalse(Validators.cpfValido("111.111.111-11"))
        assertTrue(Validators.telefoneValido("(11) 98765-4321"))
        assertFalse(Validators.telefoneValido("0112345678"))
    }

    @Test fun conflitoAgenda() {
        val d = LocalDate.of(2026, 10, 20)
        val h = LocalTime.of(9, 0)
        val a = Agendamento(1, 1, d, h, TipoAgendamento.TREINO)
        val b = Agendamento(2, 2, d, h, TipoAgendamento.AVALIACAO)
        assertNotNull(AgendaRules.conflito(b, listOf(a)))
        assertNull(AgendaRules.conflito(b.copy(status = StatusAgendamento.CANCELADO), listOf(a)))
        assertNull(AgendaRules.conflito(a, listOf(a)))
    }
}
