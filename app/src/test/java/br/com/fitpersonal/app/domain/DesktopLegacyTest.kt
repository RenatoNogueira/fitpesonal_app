package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.DesktopLegacy
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DesktopLegacyTest {

    @Test fun datas() {
        assertEquals(LocalDate.of(2026, 3, 5), DesktopLegacy.data("2026-03-05"))
        assertEquals(LocalDate.of(2026, 3, 5), DesktopLegacy.data("2026-03-05 10:20:00"))
        assertEquals(LocalDate.of(2026, 3, 5), DesktopLegacy.data("05/03/2026"))
        assertNull(DesktopLegacy.data("lixo"))
        assertNull(DesktopLegacy.data(null))
    }

    @Test fun horarios() {
        assertEquals(LocalTime.of(9, 0), DesktopLegacy.horario("9:00"))
        assertEquals(LocalTime.of(14, 30), DesktopLegacy.horario("14:30:00"))
        assertNull(DesktopLegacy.horario("25:00"))
    }

    @Test fun enumsComAcento() {
        assertEquals(TipoAgendamento.AVALIACAO, DesktopLegacy.tipo("Avaliação"))
        assertEquals(StatusAgendamento.CONCLUIDO, DesktopLegacy.status("Concluído"))
        assertNull(DesktopLegacy.tipo("Outro"))
    }

    @Test fun sexo() {
        assertEquals("Feminino", DesktopLegacy.sexo("Mulher (Cisgênero/Transgênero)"))
        assertEquals("Masculino", DesktopLegacy.sexo("Homem (Cisgênero/Transgênero)"))
        assertEquals("Outro", DesktopLegacy.sexo("Prefiro não informar"))
        assertNull(DesktopLegacy.sexo(""))
    }

    @Test fun condicoesDoPrefixo() {
        val (c, livre) = DesktopLegacy.separarCondicoes("Condições: Diabetes, Hipertensão; usa medicação")
        assertEquals(setOf(CondicaoSaude.DIABETES, CondicaoSaude.HIPERTENSAO), c)
        assertEquals("usa medicação", livre)
        val (c2, l2) = DesktopLegacy.separarCondicoes("Sem prefixo")
        assertTrue(c2.isEmpty()); assertEquals("Sem prefixo", l2)
        assertEquals(setOf(CondicaoSaude.LESOES), DesktopLegacy.separarCondicoes("Condições: Lesões").first)
    }

    @Test fun caminhosDoZip() {
        assertEquals("media/alunos/a.jpg", DesktopLegacy.caminhoNoZip("media/alunos/a.jpg"))
        assertEquals("media/avaliacoes/3/1_frente.jpg",
            DesktopLegacy.caminhoNoZip("C:\\Program Files\\RJE\\_internal\\media\\avaliacoes\\3\\1_frente.jpg"))
        assertEquals("data/branding/logo.png", DesktopLegacy.caminhoNoZip("data/branding/logo.png"))
        assertNull(DesktopLegacy.caminhoNoZip("../../etc/passwd"))
        assertNull(DesktopLegacy.caminhoNoZip(""))
    }

    @Test fun chaveDeAluno() {
        val a = DesktopLegacy.chaveAluno("João  ", LocalDate.of(1990, 1, 1), null)
        val b = DesktopLegacy.chaveAluno("joao", LocalDate.of(1990, 1, 1), "")
        assertEquals(a.replace("  ", ""), b.replace("  ", ""))
        assertEquals("cpf:52998224725", DesktopLegacy.chaveAluno("X", null, "529.982.247-25"))
    }
}
