package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.ProgressaoRules
import br.com.fitpersonal.app.domain.rules.RegistroExercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressaoRulesTest {
    private fun r(kg: Double?, feito: Boolean = true, esforco: Int? = 7) = RegistroExercicio(kg, feito, esforco)

    @Test fun semHistoricoNaoSugere() = assertNull(ProgressaoRules.sugerir(emptyList()))

    @Test fun umaSessaoSoNaoBasta() = assertNull(ProgressaoRules.sugerir(listOf(r(40.0))))

    @Test fun duasSessoesCompletasNaMesmaCargaSugeremAumento() {
        val s = ProgressaoRules.sugerir(listOf(r(40.0), r(40.0)))
        assertNotNull(s)
        assertEquals(42.0, s!!.cargaKg, 0.001)
    }

    @Test fun cargaDiferenteNaoSugere() = assertNull(ProgressaoRules.sugerir(listOf(r(40.0), r(37.5))))

    @Test fun sessaoIncompletaNaoSugere() = assertNull(ProgressaoRules.sugerir(listOf(r(40.0, feito = false), r(40.0))))

    @Test fun esforcoMaximoNaoSugere() = assertNull(ProgressaoRules.sugerir(listOf(r(40.0, esforco = 9), r(40.0))))

    @Test fun esforcoNaoInformadoNaoImpede() {
        assertNotNull(ProgressaoRules.sugerir(listOf(r(40.0, esforco = null), r(40.0, esforco = null))))
    }

    @Test fun ignoraSessoesSemCarga() {
        val s = ProgressaoRules.sugerir(listOf(r(null), r(40.0), r(40.0)))
        assertEquals(42.0, s!!.cargaKg, 0.001)
    }

    @Test fun ultimaCargaPulaSemCarga() {
        assertEquals(35.0, ProgressaoRules.ultimaCarga(listOf(r(null), r(35.0), r(30.0)))!!, 0.001)
        assertNull(ProgressaoRules.ultimaCarga(listOf(r(null))))
    }

    @Test fun formatarKg() {
        assertEquals("40", ProgressaoRules.formatarKg(40.0))
        assertEquals("42,5", ProgressaoRules.formatarKg(42.5))
    }
}
