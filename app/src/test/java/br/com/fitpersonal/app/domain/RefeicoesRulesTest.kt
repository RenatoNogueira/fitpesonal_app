package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.Refeicao
import br.com.fitpersonal.app.domain.rules.RefeicoesRules
import br.com.fitpersonal.app.domain.rules.TipoRefeicao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RefeicoesRulesTest {
    private val dia = LocalDate.of(2026, 10, 16)
    private fun r(id: String, d: LocalDate, h: Int, tipo: TipoRefeicao, nota: String? = null, foto: String? = null) =
        Refeicao(id, d, LocalTime.of(h, 0), tipo, nota, foto)

    @Test fun sugereTipoPeloHorario() {
        assertEquals(TipoRefeicao.CAFE, RefeicoesRules.sugerirTipo(LocalTime.of(7, 30)))
        assertEquals(TipoRefeicao.ALMOCO, RefeicoesRules.sugerirTipo(LocalTime.of(12, 0)))
        assertEquals(TipoRefeicao.LANCHE, RefeicoesRules.sugerirTipo(LocalTime.of(16, 0)))
        assertEquals(TipoRefeicao.JANTAR, RefeicoesRules.sugerirTipo(LocalTime.of(20, 0)))
        assertEquals(TipoRefeicao.OUTRO, RefeicoesRules.sugerirTipo(LocalTime.of(2, 0)))
    }

    @Test fun tipoDesconhecidoViraOutro() {
        assertEquals(TipoRefeicao.OUTRO, TipoRefeicao.de("sobremesa_do_futuro"))
        assertEquals(TipoRefeicao.OUTRO, TipoRefeicao.de(null))
        assertEquals(TipoRefeicao.JANTAR, TipoRefeicao.de("jantar"))
    }

    @Test fun porDiaOrdenaDiasEHorarios() {
        val lista = listOf(
            r("1", dia.minusDays(1), 19, TipoRefeicao.JANTAR),
            r("2", dia, 13, TipoRefeicao.ALMOCO),
            r("3", dia, 8, TipoRefeicao.CAFE),
        )
        val g = RefeicoesRules.porDia(lista)
        assertEquals(listOf(dia, dia.minusDays(1)), g.map { it.first })
        assertEquals(listOf("3", "2"), g.first().second.map { it.id })
    }

    @Test fun textoDoDia() {
        val lista = listOf(
            r("1", dia, 8, TipoRefeicao.CAFE, "Pão integral e ovos", foto = "a.jpg"),
            r("2", dia, 13, TipoRefeicao.ALMOCO),
            r("3", dia.minusDays(1), 20, TipoRefeicao.JANTAR, "outro dia"),
        )
        val t = RefeicoesRules.textoDoDia(dia, lista, "Ana")
        assertTrue(t.startsWith("Diário de refeições de Ana — 16/10/2026"))
        assertTrue(t.contains("• 08:00 Café da manhã: Pão integral e ovos (foto)"))
        assertTrue(t.contains("• 13:00 Almoço"))
        assertTrue(!t.contains("outro dia"))
    }

    @Test fun diaSemRegistro() {
        assertTrue(RefeicoesRules.textoDoDia(dia, emptyList()).contains("Nenhuma refeição registrada."))
    }
}
