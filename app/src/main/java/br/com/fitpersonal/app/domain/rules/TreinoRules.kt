package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioCatalogo
import br.com.fitpersonal.app.domain.model.ExercicioTreino

object TreinoRules {

    /** Regra do desktop: nome do treino é obrigatório. */
    fun validarNome(nome: String): String? =
        if (nome.isBlank()) "Informe o nome do treino." else null

    fun validarExercicio(nome: String): String? =
        if (nome.isBlank()) "Informe o nome do exercício." else null

    /** Todas as divisões presentes, mesmo vazias, na ordem A–D. */
    fun agruparPorDivisao(exercicios: List<ExercicioTreino>): Map<DivisaoTreino, List<ExercicioTreino>> =
        DivisaoTreino.entries.associateWith { d -> exercicios.filter { it.divisao == d } }

    /** Converte itens do catálogo em exercícios do treino (fluxo "Adicionar selecionados"). */
    fun doCatalogo(itens: Collection<ExercicioCatalogo>, divisao: DivisaoTreino): List<ExercicioTreino> =
        itens.map { ExercicioTreino(idTreino = 0, nome = it.nome, divisao = divisao) }

    /** "3 x 12 • 20 kg • 60 s" — só as partes preenchidas. */
    fun resumo(e: ExercicioTreino): String {
        val serieRep = listOfNotNull(e.series?.takeIf { it.isNotBlank() }, e.repeticoes?.takeIf { it.isNotBlank() })
            .joinToString(" x ")
        return listOf(serieRep, e.carga.orEmpty(), e.descanso.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" • ")
    }
}
