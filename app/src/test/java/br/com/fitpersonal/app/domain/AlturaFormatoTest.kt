package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.AvaliacaoInput
import br.com.fitpersonal.app.domain.rules.AvaliacaoValidator
import br.com.fitpersonal.app.domain.rules.Gravidade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlturaFormatoTest {

    @Test fun digitando_insere_a_virgula() {
        assertEquals("1,", AvaliacaoValidator.formatarAltura("1", ""))
        assertEquals("1,7", AvaliacaoValidator.formatarAltura("1,7", "1,"))
        assertEquals("1,75", AvaliacaoValidator.formatarAltura("1,75", "1,7"))
        assertEquals("1,75", AvaliacaoValidator.formatarAltura("175", ""))
        assertEquals("1,75", AvaliacaoValidator.formatarAltura("1,758", "1,75"))
    }

    @Test fun apagando_nao_prende_a_virgula() {
        assertEquals("1,7", AvaliacaoValidator.formatarAltura("1,7", "1,75"))
        assertEquals("1", AvaliacaoValidator.formatarAltura("1", "1,"))
        assertEquals("", AvaliacaoValidator.formatarAltura("", "1"))
    }

    @Test fun avisos_trazem_gravidade() {
        val s = AvaliacaoValidator.resumir(
            AvaliacaoInput(peso = "120", altura = "1,70", pressaoSistolica = "165", pressaoDiastolica = "100"),
        )
        assertTrue(s.avisos.any { it.titulo.startsWith("Obesidade") && it.gravidade == Gravidade.ALTA })
        assertTrue(s.avisos.any { it.titulo.startsWith("Hipertensão") && it.gravidade == Gravidade.ALTA })
    }
}
