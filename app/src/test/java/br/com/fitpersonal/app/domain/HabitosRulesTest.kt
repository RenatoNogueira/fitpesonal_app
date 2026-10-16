package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.CheckIn
import br.com.fitpersonal.app.domain.rules.HabitosRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitosRulesTest {
    // 16/10/2026 é uma sexta-feira
    private val hoje = LocalDate.of(2026, 10, 16)
    private fun ok() = setOf("agua", "sono")

    @Test fun diaCumpridoExigeMetadeDosHabitos() {
        assertTrue(HabitosRules.diaCumprido(setOf("agua", "sono")))
        assertFalse(HabitosRules.diaCumprido(setOf("agua")))
        assertFalse(HabitosRules.diaCumprido(setOf("agua", "inexistente")))
    }

    @Test fun sequenciaContaDiasSeguidos() {
        val dias = mapOf(hoje to ok(), hoje.minusDays(1) to ok(), hoje.minusDays(2) to ok(), hoje.minusDays(4) to ok())
        assertEquals(3, HabitosRules.sequencia(dias, hoje))
    }

    @Test fun hojeIncompletoNaoZeraASequencia() {
        val dias = mapOf(hoje to setOf("agua"), hoje.minusDays(1) to ok(), hoje.minusDays(2) to ok())
        assertEquals(2, HabitosRules.sequencia(dias, hoje))
    }

    @Test fun semDiasNaoHaSequencia() = assertEquals(0, HabitosRules.sequencia(emptyMap(), hoje))

    @Test fun semanaComecaNaSegunda() {
        assertEquals(LocalDate.of(2026, 10, 12), HabitosRules.inicioDaSemana(hoje))
        assertEquals(LocalDate.of(2026, 10, 12), HabitosRules.inicioDaSemana(LocalDate.of(2026, 10, 12)))
        assertEquals(LocalDate.of(2026, 10, 12), HabitosRules.inicioDaSemana(LocalDate.of(2026, 10, 18)))
        assertEquals(7, HabitosRules.semana(emptyMap(), hoje).size)
    }

    @Test fun cumpridosNaSemanaIgnoraDiasFuturos() {
        val dias = mapOf(LocalDate.of(2026, 10, 12) to ok(), LocalDate.of(2026, 10, 17) to ok())
        assertEquals(1, HabitosRules.cumpridosNaSemana(dias, hoje))
    }

    @Test fun checkInEhSemanal() {
        val antigo = CheckIn(LocalDate.of(2026, 10, 9), 3, 3, 1)
        assertTrue(HabitosRules.precisaCheckIn(listOf(antigo), hoje))
        val novo = CheckIn(LocalDate.of(2026, 10, 14), 4, 4, 2)
        assertFalse(HabitosRules.precisaCheckIn(listOf(antigo, novo), hoje))
        assertEquals(novo, HabitosRules.checkInDaSemana(listOf(antigo, novo), hoje))
    }

    @Test fun textoParaOProfessor() {
        val dias = mapOf(LocalDate.of(2026, 10, 15) to ok(), hoje to ok())
        val t = HabitosRules.textoSemana(dias, listOf(CheckIn(hoje, 4, 3, 1, "Joelho ok")), hoje)
        assertTrue(t.contains("2 de 5 dias"))
        assertTrue(t.contains("sequência de 2 dias"))
        assertTrue(t.contains("energia 4/5"))
        assertTrue(t.contains("Joelho ok"))
    }
}
