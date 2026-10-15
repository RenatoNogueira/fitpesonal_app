package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.BloqueioRules
import br.com.fitpersonal.app.domain.rules.TemaApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtecaoRulesTest {

    @Test fun naoTravaSemTerSaido() {
        assertFalse(BloqueioRules.deveTravar(null, 1_000_000))
    }

    @Test fun travaSoDepoisDaTolerancia() {
        assertFalse(BloqueioRules.deveTravar(1_000, 1_000 + BloqueioRules.TOLERANCIA_MS))
        assertTrue(BloqueioRules.deveTravar(1_000, 1_001 + BloqueioRules.TOLERANCIA_MS))
    }

    @Test fun relogioParaTrasTrava() {
        assertTrue(BloqueioRules.deveTravar(5_000, 1_000))
    }

    @Test fun temaDesconhecidoVoltaAoAutomatico() {
        assertEquals(TemaApp.ESCURO, TemaApp.deNome("ESCURO"))
        assertEquals(TemaApp.SISTEMA, TemaApp.deNome("ROXO"))
        assertEquals(TemaApp.SISTEMA, TemaApp.deNome(null))
    }
}
