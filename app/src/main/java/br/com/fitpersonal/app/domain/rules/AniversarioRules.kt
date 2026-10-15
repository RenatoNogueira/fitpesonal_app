package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate

enum class SituacaoAniversario { HOJE, AMANHA, PROXIMO, JA_PASSOU }

/** Regras de aniversariantes do dashboard (mesmo critério do desktop: pelo mês de nascimento). */
object AniversarioRules {

    /** Data do aniversário no ano dado; 29/02 cai em 28/02 em ano não bissexto. */
    fun aniversarioEm(nascimento: LocalDate, ano: Int): LocalDate = nascimento.withYear(ano)

    /** Idade que a pessoa completa (ou completou) no aniversário do ano dado. */
    fun idadeQueFaz(nascimento: LocalDate, ano: Int): Int = ano - nascimento.year

    fun situacao(nascimento: LocalDate, hoje: LocalDate): SituacaoAniversario {
        val dia = aniversarioEm(nascimento, hoje.year)
        return when {
            dia == hoje -> SituacaoAniversario.HOJE
            dia == hoje.plusDays(1) -> SituacaoAniversario.AMANHA
            dia.isAfter(hoje) -> SituacaoAniversario.PROXIMO
            else -> SituacaoAniversario.JA_PASSOU
        }
    }

    fun rotuloDia(nascimento: LocalDate, hoje: LocalDate): String {
        val dia = aniversarioEm(nascimento, hoje.year)
        return when (situacao(nascimento, hoje)) {
            SituacaoAniversario.HOJE -> "Hoje"
            SituacaoAniversario.AMANHA -> "Amanhã"
            else -> "Dia ${dia.dayOfMonth}"
        }
    }

    /** "faz 30 anos" (hoje ou ainda por vir) / "fez 30 anos" (já passou). */
    fun descricaoIdade(nascimento: LocalDate, hoje: LocalDate): String {
        val idade = idadeQueFaz(nascimento, hoje.year)
        val verbo = if (situacao(nascimento, hoje) == SituacaoAniversario.JA_PASSOU) "fez" else "faz"
        return "$verbo $idade ${if (idade == 1) "ano" else "anos"}"
    }

    /** Dias até o aniversário deste ano (0 = hoje; negativo = já passou). */
    fun diasAte(nascimento: LocalDate, hoje: LocalDate): Int =
        java.time.temporal.ChronoUnit.DAYS.between(hoje, aniversarioEm(nascimento, hoje.year)).toInt()

    /** "Hoje", "Amanhã", "em 5 dias" ou "há 3 dias" — complementa o dia do mês exibido no cartão. */
    fun rotuloProximidade(nascimento: LocalDate, hoje: LocalDate): String {
        val d = diasAte(nascimento, hoje)
        return when {
            d == 0 -> "Hoje"
            d == 1 -> "Amanhã"
            d > 1 -> "em $d dias"
            d == -1 -> "ontem"
            else -> "há ${-d} dias"
        }
    }

    /** Hoje primeiro, depois os que ainda vão fazer (mais próximos antes) e por fim os que já passaram. */
    fun <T> ordenar(itens: List<T>, hoje: LocalDate, nascimento: (T) -> LocalDate): List<T> =
        itens.sortedWith(
            compareBy<T> { diasAte(nascimento(it), hoje) < 0 }
                .thenBy { kotlin.math.abs(diasAte(nascimento(it), hoje)) },
        )

    /** Primeira letra do primeiro e do último nome ("Maria da Silva" -> "MS"). */
    fun iniciais(nome: String): String {
        val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when (partes.size) {
            0 -> ""
            1 -> partes[0].first().uppercase()
            else -> partes.first().first().uppercase() + partes.last().first().uppercase()
        }
    }
}
