package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.min

enum class SituacaoPagamento(val rotulo: String) {
    SEM_MENSALIDADE("Sem mensalidade"),
    EM_DIA("Em dia"),
    A_VENCER("A vencer"),
    ATRASADO("Em atraso"),
}

data class ResumoFinanceiro(
    val situacao: SituacaoPagamento,
    /** Competências vencidas e sem pagamento (mais antiga primeiro), dentro da janela analisada. */
    val atrasadas: List<YearMonth> = emptyList(),
    /** Vencimento do mês corrente quando ainda não pago e não vencido. */
    val proximoVencimento: LocalDate? = null,
) {
    /** Competência que um novo pagamento quita: a mais antiga em atraso, ou o mês corrente se a vencer. */
    fun competenciaParaPagar(hoje: LocalDate): YearMonth? = when (situacao) {
        SituacaoPagamento.ATRASADO -> atrasadas.firstOrNull()
        SituacaoPagamento.A_VENCER -> YearMonth.from(hoje)
        else -> null
    }
}

object FinanceiroRules {
    private const val JANELA_MESES = 3

    /** Dia de vencimento ajustado a meses curtos (dia 31 em fevereiro vence no último dia). */
    fun vencimento(mes: YearMonth, dia: Int): LocalDate = mes.atDay(min(dia.coerceIn(1, 31), mes.lengthOfMonth()))

    /**
     * Situação da mensalidade olhando os últimos [JANELA_MESES] meses (sem voltar antes do cadastro).
     * [pagas] = competências com pagamento registrado.
     */
    fun resumir(
        mensalidadeCentavos: Long?,
        diaVencimento: Int?,
        cadastro: LocalDate,
        pagas: Set<YearMonth>,
        hoje: LocalDate,
    ): ResumoFinanceiro {
        if (mensalidadeCentavos == null || mensalidadeCentavos <= 0 || diaVencimento == null) {
            return ResumoFinanceiro(SituacaoPagamento.SEM_MENSALIDADE)
        }
        val atual = YearMonth.from(hoje)
        val inicio = maxOf(YearMonth.from(cadastro), atual.minusMonths((JANELA_MESES - 1).toLong()))
        // Só conta o mês cujo vencimento cai depois da entrada do aluno.
        val devidos = generateSequence(inicio) { it.plusMonths(1) }
            .takeWhile { it <= atual }
            .filter { !vencimento(it, diaVencimento).isBefore(cadastro) }
            .toList()

        val atrasadas = devidos.filter { it !in pagas && vencimento(it, diaVencimento).isBefore(hoje) }
        if (atrasadas.isNotEmpty()) return ResumoFinanceiro(SituacaoPagamento.ATRASADO, atrasadas)

        val venc = vencimento(atual, diaVencimento)
        return if (atual in pagas || atual !in devidos) {
            ResumoFinanceiro(SituacaoPagamento.EM_DIA)
        } else {
            ResumoFinanceiro(SituacaoPagamento.A_VENCER, proximoVencimento = venc)
        }
    }

    fun formatarReais(centavos: Long): String {
        val reais = centavos / 100
        val cent = (centavos % 100).toInt()
        return String.format(Locale("pt", "BR"), "R$ %,d,%02d", reais, cent)
    }

    /** "150", "150,5", "1.250,90" → centavos; inválido/negativo/zero → null. */
    fun parseReais(texto: String): Long? {
        val limpo = texto.trim().removePrefix("R$").trim().replace(".", "").replace(',', '.')
        if (limpo.isEmpty()) return null
        val v = limpo.toBigDecimalOrNull() ?: return null
        val centavos = v.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        return centavos.takeIf { it > 0 && it < 100_000_000 }
    }

    fun rotuloCompetencia(c: YearMonth): String {
        val mes = c.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))
        return "$mes/${c.year}"
    }
}
