package br.com.fitpersonal.app.domain.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Hábitos diários que o aluno marca no app (fixos, sem servidor). */
enum class Habito(val id: String, val rotulo: String) {
    AGUA("agua", "Beber 2 litros de água"),
    SONO("sono", "Dormir 7 horas ou mais"),
    ALONGAR("alongar", "Alongar ou fazer mobilidade"),
    ALIMENTACAO("alimentacao", "Comer frutas e verduras"),
}

/** Check-in semanal: como o aluno se sentiu (1 a 5; em [dor], 5 = muita dor). */
data class CheckIn(val data: LocalDate, val energia: Int, val sono: Int, val dor: Int, val obs: String? = null)

object HabitosRules {
    /** Hábitos marcados no dia para ele contar como cumprido (metade da lista). */
    const val MINIMO_DIA = 2

    private val IDS = Habito.entries.map { it.id }.toSet()

    fun inicioDaSemana(dia: LocalDate): LocalDate = dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Só ids conhecidos contam (arquivo antigo ou editado não quebra a conta). */
    fun marcadosValidos(marcados: Set<String>): Set<String> = marcados.filter { it in IDS }.toSet()

    fun diaCumprido(marcados: Set<String>): Boolean = marcadosValidos(marcados).size >= MINIMO_DIA

    /**
     * Dias seguidos cumpridos até hoje. Se hoje ainda não está cumprido, a contagem parte de ontem
     * (o dia de hoje ainda pode ser completado, então não zera a sequência).
     */
    fun sequencia(dias: Map<LocalDate, Set<String>>, hoje: LocalDate): Int {
        var dia = if (diaCumprido(dias[hoje].orEmpty())) hoje else hoje.minusDays(1)
        var total = 0
        while (diaCumprido(dias[dia].orEmpty())) {
            total++
            dia = dia.minusDays(1)
        }
        return total
    }

    /** Situação de cada um dos 7 dias da semana de [hoje] (segunda a domingo). */
    fun semana(dias: Map<LocalDate, Set<String>>, hoje: LocalDate): List<Pair<LocalDate, Boolean>> {
        val inicio = inicioDaSemana(hoje)
        return (0 until 7).map { i -> inicio.plusDays(i.toLong()).let { it to diaCumprido(dias[it].orEmpty()) } }
    }

    fun cumpridosNaSemana(dias: Map<LocalDate, Set<String>>, hoje: LocalDate): Int =
        semana(dias, hoje).count { (d, ok) -> ok && !d.isAfter(hoje) }

    /** O check-in é semanal: pede de novo quando não há nenhum feito na semana corrente. */
    fun precisaCheckIn(checkins: List<CheckIn>, hoje: LocalDate): Boolean {
        val inicio = inicioDaSemana(hoje)
        return checkins.none { !it.data.isBefore(inicio) && !it.data.isAfter(hoje) }
    }

    fun checkInDaSemana(checkins: List<CheckIn>, hoje: LocalDate): CheckIn? {
        val inicio = inicioDaSemana(hoje)
        return checkins.filter { !it.data.isBefore(inicio) && !it.data.isAfter(hoje) }.maxByOrNull { it.data }
    }

    /** Texto curto para ir junto do resumo ao professor. */
    fun textoSemana(dias: Map<LocalDate, Set<String>>, checkins: List<CheckIn>, hoje: LocalDate): String {
        val partes = mutableListOf<String>()
        val inicio = inicioDaSemana(hoje)
        val decorridos = (java.time.temporal.ChronoUnit.DAYS.between(inicio, hoje).toInt() + 1).coerceIn(1, 7)
        partes += "Hábitos: ${cumpridosNaSemana(dias, hoje)} de $decorridos dias cumpridos nesta semana" +
            sequencia(dias, hoje).let { if (it >= 2) " (sequência de $it dias)" else "" } + "."
        checkInDaSemana(checkins, hoje)?.let { c ->
            partes += "Check-in: energia ${c.energia}/5, sono ${c.sono}/5, dor ${c.dor}/5." +
                (c.obs?.takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
        }
        return partes.joinToString(" ")
    }
}
