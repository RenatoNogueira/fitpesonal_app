package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.CargaRules
import br.com.fitpersonal.app.domain.rules.CsvRules
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.domain.rules.PontoSerie
import br.com.fitpersonal.app.domain.rules.SituacaoPagamento
import br.com.fitpersonal.app.domain.rules.WhatsAppRules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class FinanceiroMensagemTest {

    private val hoje = LocalDate.of(2026, 5, 20)
    private val cadastro = LocalDate.of(2025, 1, 10)

    // ── carga ──
    @Test fun cargaLeNumeroDoTexto() {
        assertEquals(20.0, CargaRules.parseKg("20 kg")!!, 1e-9)
        assertEquals(12.5, CargaRules.parseKg("12,5kg")!!, 1e-9)
        assertEquals(20.0, CargaRules.parseKg("20-25")!!, 1e-9)
        assertNull(CargaRules.parseKg("corporal"))
        assertNull(CargaRules.parseKg("0"))
        assertNull(CargaRules.parseKg(null))
    }

    @Test fun sugestaoDeAumento() {
        assertEquals(21.0, CargaRules.proximaCarga(20.0), 1e-9)      // 5% = 21,0
        assertEquals(10.5, CargaRules.proximaCarga(10.0), 1e-9)      // 5% = 10,5
        assertEquals(5.5, CargaRules.proximaCarga(5.0), 1e-9)        // 5% = 5,25 → mínimo +0,5
        val parado = PontoSerie(hoje.minusDays(21), 20.0)
        assertEquals(21.0, CargaRules.sugerirAumento(parado, hoje)!!, 1e-9)
        assertNull(CargaRules.sugerirAumento(PontoSerie(hoje.minusDays(20), 20.0), hoje))
    }

    @Test fun chaveDoExercicioIgnoraCaixaEEspacos() {
        assertEquals(CargaRules.chaveExercicio("Supino  Reto"), CargaRules.chaveExercicio(" supino reto "))
    }

    // ── financeiro ──
    @Test fun semMensalidade() {
        assertEquals(SituacaoPagamento.SEM_MENSALIDADE, FinanceiroRules.resumir(null, 10, cadastro, emptySet(), hoje).situacao)
        assertEquals(SituacaoPagamento.SEM_MENSALIDADE, FinanceiroRules.resumir(15000, null, cadastro, emptySet(), hoje).situacao)
    }

    @Test fun atrasadoQuandoVenceuSemPagamento() {
        val r = FinanceiroRules.resumir(15000, 10, cadastro, setOf(YearMonth.of(2026, 3)), hoje)
        assertEquals(SituacaoPagamento.ATRASADO, r.situacao)
        assertEquals(listOf(YearMonth.of(2026, 4), YearMonth.of(2026, 5)), r.atrasadas)
        assertEquals(YearMonth.of(2026, 4), r.competenciaParaPagar(hoje))
    }

    @Test fun emDiaEAVencer() {
        val pagas = setOf(YearMonth.of(2026, 3), YearMonth.of(2026, 4), YearMonth.of(2026, 5))
        assertEquals(SituacaoPagamento.EM_DIA, FinanceiroRules.resumir(15000, 10, cadastro, pagas, hoje).situacao)

        // vence dia 25: maio ainda não venceu
        val aVencer = FinanceiroRules.resumir(15000, 25, cadastro, setOf(YearMonth.of(2026, 3), YearMonth.of(2026, 4)), hoje)
        assertEquals(SituacaoPagamento.A_VENCER, aVencer.situacao)
        assertEquals(LocalDate.of(2026, 5, 25), aVencer.proximoVencimento)
        assertEquals(YearMonth.of(2026, 5), aVencer.competenciaParaPagar(hoje))
    }

    @Test fun naoCobraAntesDoCadastro() {
        val novo = LocalDate.of(2026, 5, 18)
        val r = FinanceiroRules.resumir(15000, 10, novo, emptySet(), hoje)
        // venceu dia 10, mas o aluno entrou dia 18: nada devido ainda
        assertTrue(r.atrasadas.isEmpty())
        assertEquals(SituacaoPagamento.EM_DIA, r.situacao)
    }

    @Test fun vencimentoEmMesCurto() {
        assertEquals(LocalDate.of(2026, 2, 28), FinanceiroRules.vencimento(YearMonth.of(2026, 2), 31))
    }

    @Test fun valoresEmReais() {
        assertEquals(15000L, FinanceiroRules.parseReais("150"))
        assertEquals(15050L, FinanceiroRules.parseReais("150,5"))
        assertEquals(125090L, FinanceiroRules.parseReais("R$ 1.250,90"))
        assertNull(FinanceiroRules.parseReais("0"))
        assertNull(FinanceiroRules.parseReais("abc"))
        assertEquals("R$ 1.250,90", FinanceiroRules.formatarReais(125090))
        assertEquals("R$ 150,00", FinanceiroRules.formatarReais(15000))
    }

    // ── mensagens ──
    @Test fun linkDoWhatsApp() {
        val l = WhatsAppRules.link("(11) 98888-7777", "Olá, tudo bem?")!!
        assertTrue(l.startsWith("https://wa.me/5511988887777?text="))
        assertTrue(l.contains("Ol%C3%A1,%20tudo%20bem%3F"))
        assertEquals("https://wa.me/5511988887777?text=a", WhatsAppRules.link("5511988887777", "a"))
        assertNull(WhatsAppRules.link("123", "x"))
        assertNull(WhatsAppRules.link(null, "x"))
    }

    @Test fun textosDasMensagens() {
        val m = MensagemRules.lembreteAgendamento(
            "Maria Souza", LocalDate.of(2026, 5, 21), LocalTime.of(7, 30), "Treino", "Prof. Renato",
        )
        assertTrue(m.contains("Olá, Maria!"))
        assertTrue(m.contains("21/05"))
        assertTrue(m.contains("07:30"))
        assertTrue(m.endsWith("Prof. Renato"))

        val c = MensagemRules.cobranca("Maria Souza", YearMonth.of(2026, 4), 15000, "")
        assertTrue(c.contains("R$ 150,00"))
        assertTrue(c.contains("abril/2026"))
        assertFalse(c.endsWith("\n"))
    }

    // ── csv ──
    @Test fun csvEscapaEProtege() {
        assertEquals("simples", CsvRules.texto("simples"))
        assertEquals("\"a;b\"", CsvRules.texto("a;b"))
        assertEquals("\"diz \"\"oi\"\"\"", CsvRules.texto("diz \"oi\""))
        assertEquals("'=SOMA(A1)", CsvRules.texto("=SOMA(A1)"))
        assertEquals("", CsvRules.texto(null))
        assertEquals("72,5", CsvRules.numero(72.5))
        assertEquals("", CsvRules.numero(null))
        assertEquals("a;b;c", CsvRules.linha("a", "b", "c"))
    }
}
