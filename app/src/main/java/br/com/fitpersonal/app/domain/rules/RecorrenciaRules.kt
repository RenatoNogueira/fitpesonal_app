package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate

/** Agendamentos semanais repetidos (mesmo dia da semana e horário). */
object RecorrenciaRules {
    const val MAX_OCORRENCIAS = 12

    /** Opções do seletor: total de ocorrências, contando a primeira (1 = não repetir). */
    val OPCOES = listOf(1, 4, 8, 12)

    fun rotulo(ocorrencias: Int): String =
        if (ocorrencias <= 1) "Não repetir" else "Semanal • $ocorrencias semanas"

    /** Datas das ocorrências a partir de [inicio] (inclusive), uma por semana; limitado a [MAX_OCORRENCIAS]. */
    fun datas(inicio: LocalDate, ocorrencias: Int): List<LocalDate> {
        val total = ocorrencias.coerceIn(1, MAX_OCORRENCIAS)
        return List(total) { inicio.plusWeeks(it.toLong()) }
    }
}
