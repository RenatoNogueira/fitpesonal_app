package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.MetaComposicaoRules
import br.com.fitpersonal.app.domain.rules.Metrica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaComposicaoRulesTest {
    @Test fun descerAteAMeta() {
        // 28% → meta 18%, agora 23%: metade do caminho
        val p = MetaComposicaoRules.progresso(Metrica.PERCENTUAL_GORDURA, 28.0, 23.0, 18.0)
        assertEquals(0.5f, p.fracao, 0.001f)
        assertFalse(p.atingida)
        assertEquals(5.0, p.faltam, 0.001)
    }

    @Test fun subirAteAMeta() {
        val p = MetaComposicaoRules.progresso(Metrica.MASSA_MAGRA, 50.0, 55.0, 60.0)
        assertEquals(0.5f, p.fracao, 0.001f)
    }

    @Test fun metaAtingidaPassandoDoAlvo() {
        val p = MetaComposicaoRules.progresso(Metrica.PESO, 90.0, 79.0, 80.0)
        assertTrue(p.atingida)
        assertEquals(1f, p.fracao, 0f)
        assertEquals(0.0, p.faltam, 0.0)
    }

    @Test fun andandoParaOLadoErradoFicaEmZero() {
        val p = MetaComposicaoRules.progresso(Metrica.PESO, 90.0, 93.0, 80.0)
        assertEquals(0f, p.fracao, 0f)
        assertFalse(p.atingida)
    }

    @Test fun alvoIgualAoInicioNaoGeraNaN() {
        val p = MetaComposicaoRules.progresso(Metrica.PESO, 80.0, 82.0, 80.0)
        assertFalse(p.fracao.isNaN())
        assertEquals(0f, p.fracao, 0f)
        assertTrue(MetaComposicaoRules.progresso(Metrica.PESO, 80.0, 80.0, 80.0).atingida)
    }
}
