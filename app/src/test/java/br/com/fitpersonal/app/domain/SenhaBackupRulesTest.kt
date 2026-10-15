package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.SenhaBackupRules
import br.com.fitpersonal.app.domain.rules.TermoLgpd
import org.junit.Assert.*
import org.junit.Test

class SenhaBackupRulesTest {
    @Test fun regras() {
        assertNotNull(SenhaBackupRules.validar("curta1", "curta1"))
        assertNotNull(SenhaBackupRules.validar("aaaaaaaaaa", "aaaaaaaaaa"))
        assertNotNull(SenhaBackupRules.validar("somenteletras", "somenteletras"))
        assertNotNull(SenhaBackupRules.validar("12345678901", "12345678901"))
        assertNotNull(SenhaBackupRules.validar("senhaforte1", "senhaforte2"))
        assertNull(SenhaBackupRules.validar("senhaforte1", "senhaforte1"))
    }

    @Test fun termoTemConteudoEVersao() {
        assertTrue(TermoLgpd.VERSAO >= 1)
        assertTrue(TermoLgpd.SECOES.size >= 5)
        assertTrue(TermoLgpd.SECOES.all { it.titulo.isNotBlank() && it.texto.length > 40 })
    }
}
