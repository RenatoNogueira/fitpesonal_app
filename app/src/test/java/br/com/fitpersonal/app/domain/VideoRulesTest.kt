package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.VideoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VideoRulesTest {

    @Test fun adicionaEsquemaQuandoFalta() {
        assertEquals("https://youtube.com/watch?v=abc", VideoRules.normalizar("youtube.com/watch?v=abc"))
        assertEquals("https://youtu.be/abc", VideoRules.normalizar("  https://youtu.be/abc  "))
        assertEquals("http://exemplo.com/v", VideoRules.normalizar("http://exemplo.com/v"))
    }

    @Test fun rejeitaInvalidos() {
        assertNull(VideoRules.normalizar(null))
        assertNull(VideoRules.normalizar(""))
        assertNull(VideoRules.normalizar("   "))
        assertNull(VideoRules.normalizar("javascript:alert(1)"))
        assertNull(VideoRules.normalizar("ftp://exemplo.com/a"))
        assertNull(VideoRules.normalizar("https://sem espaco.com/a b"))
        assertNull(VideoRules.normalizar("https://semponto/abc"))
        assertNull(VideoRules.normalizar("https://exemplo.com/\"onerror=1"))
        assertNull(VideoRules.normalizar("https://exemplo.com/" + "a".repeat(600)))
    }

    @Test fun erroSoParaTextoPreenchidoEInvalido() {
        assertNull(VideoRules.erro(""))
        assertNull(VideoRules.erro("youtube.com/abc"))
        assertNotNull(VideoRules.erro("isso nao e link"))
    }

    @Test fun origemAmigavel() {
        assertEquals("YouTube", VideoRules.origem("https://www.youtube.com/watch?v=1"))
        assertEquals("YouTube", VideoRules.origem("https://youtu.be/1"))
        assertEquals("exemplo.com", VideoRules.origem("https://exemplo.com/v"))
    }
}
