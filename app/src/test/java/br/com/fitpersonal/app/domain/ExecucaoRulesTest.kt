package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.ExecucaoRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecucaoRulesTest {

    @Test fun series_le_o_primeiro_numero_e_usa_padrao() {
        assertEquals(4, ExecucaoRules.series("4"))
        assertEquals(3, ExecucaoRules.series("3 séries"))
        assertEquals(ExecucaoRules.SERIES_PADRAO, ExecucaoRules.series(null))
        assertEquals(ExecucaoRules.SERIES_PADRAO, ExecucaoRules.series("abc"))
        assertEquals(ExecucaoRules.SERIES_PADRAO, ExecucaoRules.series("99"))
    }

    @Test fun descanso_formatos_comuns() {
        assertEquals(60, ExecucaoRules.descansoSegundos("60"))
        assertEquals(45, ExecucaoRules.descansoSegundos("45s"))
        assertEquals(90, ExecucaoRules.descansoSegundos("1:30"))
        assertEquals(120, ExecucaoRules.descansoSegundos("2 min"))
        assertEquals(90, ExecucaoRules.descansoSegundos("1min30"))
        assertEquals(120, ExecucaoRules.descansoSegundos("2"))
        assertEquals(ExecucaoRules.DESCANSO_PADRAO_S, ExecucaoRules.descansoSegundos(""))
        assertEquals(600, ExecucaoRules.descansoSegundos("30 min"))
    }

    @Test fun formatarTempo() {
        assertEquals("1:05", ExecucaoRules.formatarTempo(65))
        assertEquals("0:00", ExecucaoRules.formatarTempo(-3))
    }

    @Test fun ajustarCarga() {
        assertEquals("21 kg", ExecucaoRules.ajustarCarga("20 kg", 1.0))
        assertEquals("", ExecucaoRules.ajustarCarga("1 kg", -1.0))
        assertEquals("1 kg", ExecucaoRules.ajustarCarga("corporal", 1.0))
    }
}
