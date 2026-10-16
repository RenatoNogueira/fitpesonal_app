package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.rules.LembreteRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LembreteRulesTest {
    private val dia = LocalDate.of(2026, 10, 20)

    @Test fun momentoDoLembrete() {
        val m = LembreteRules.momentoLembrete(dia, LocalTime.of(9, 0), 60)
        assertEquals(LocalDateTime.of(2026, 10, 20, 8, 0), m)
        // 1 dia antes atravessa a meia-noite
        assertEquals(LocalDateTime.of(2026, 10, 19, 9, 0), LembreteRules.momentoLembrete(dia, LocalTime.of(9, 0), 1440))
    }

    @Test fun atrasoSoQuandoFuturo() {
        val agora = LocalDateTime.of(2026, 10, 20, 7, 0)
        assertEquals(3_600_000L, LembreteRules.atrasoMillis(agora, LocalDateTime.of(2026, 10, 20, 8, 0)))
        assertNull(LembreteRules.atrasoMillis(agora, LocalDateTime.of(2026, 10, 20, 7, 0)))
        assertNull(LembreteRules.atrasoMillis(agora, LocalDateTime.of(2026, 10, 19, 7, 0)))
    }

    @Test fun apenasPendentesNotificam() {
        assertTrue(LembreteRules.deveNotificar(StatusAgendamento.PENDENTE))
        assertFalse(LembreteRules.deveNotificar(StatusAgendamento.CONCLUIDO))
        assertFalse(LembreteRules.deveNotificar(StatusAgendamento.CANCELADO))
    }

    @Test fun textoRelativoAoDia() {
        val h = LocalTime.of(9, 5)
        assertEquals("Hoje às 09:05 — Ana", LembreteRules.texto("Ana", dia, h, dia))
        assertEquals("Amanhã às 09:05 — Ana", LembreteRules.texto("Ana", dia, h, dia.minusDays(1)))
        assertEquals("20/10 às 09:05 — Ana", LembreteRules.texto("Ana", dia, h, dia.minusDays(5)))
    }

    @Test fun backupNoturnoSempreNoFuturo() {
        val antes = LocalDateTime.of(2026, 10, 20, 1, 0)
        assertEquals(2 * 60, LembreteRules.atrasoAteHorario(antes, 3).toMinutes())
        val depois = LocalDateTime.of(2026, 10, 20, 3, 0)   // exatamente na hora → amanhã
        assertEquals(24 * 60, LembreteRules.atrasoAteHorario(depois, 3).toMinutes())
        val tarde = LocalDateTime.of(2026, 10, 20, 23, 0)
        assertEquals(4 * 60, LembreteRules.atrasoAteHorario(tarde, 3).toMinutes())
    }

    @Test fun rotacaoMantemOsMaisRecentes() {
        val nomes = (1..10).map { "fitpersonal_auto_2026-10-%02d_0300.rjebak".format(it) } + "foto.jpg" + "fitpersonal_backup_x.rjebak"
        val apagar = LembreteRules.backupsParaApagar(nomes, manter = 7)
        assertEquals(
            listOf("fitpersonal_auto_2026-10-03_0300.rjebak", "fitpersonal_auto_2026-10-02_0300.rjebak", "fitpersonal_auto_2026-10-01_0300.rjebak"),
            apagar,
        )
    }

    @Test fun nomesELegendas() {
        assertEquals("fitpersonal_auto_2026-10-20_0300", LembreteRules.nomeBackupAuto(LocalDateTime.of(2026, 10, 20, 3, 0)))
        assertEquals("1 h", LembreteRules.rotuloAntecedencia(60))
        assertEquals("1 dia", LembreteRules.rotuloAntecedencia(1440))
        assertEquals("15 min", LembreteRules.rotuloAntecedencia(15))
    }
}
