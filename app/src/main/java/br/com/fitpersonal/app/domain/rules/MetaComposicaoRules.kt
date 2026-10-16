package br.com.fitpersonal.app.domain.rules

import kotlin.math.abs

/** Meta de composição corporal: quanto o aluno quer chegar em uma métrica. */
data class MetaComposicao(val idAluno: Long, val metrica: Metrica, val alvo: Double)

/**
 * Andamento rumo à meta. [fracao] vai de 0 (ainda no ponto de partida ou pior) a 1 (atingida);
 * [faltam] é a distância que resta até o alvo (0 quando atingida).
 */
data class ProgressoMeta(
    val metrica: Metrica,
    val inicio: Double,
    val atual: Double,
    val alvo: Double,
    val fracao: Float,
    val atingida: Boolean,
    val faltam: Double,
)

object MetaComposicaoRules {
    /** Métricas em que faz sentido ter meta (as demais dependem de interpretação clínica). */
    val METRICAS: List<Metrica> = listOf(
        Metrica.PESO, Metrica.PERCENTUAL_GORDURA, Metrica.MASSA_MAGRA, Metrica.CINTURA,
    )

    /** [inicio] é o valor da primeira avaliação; [atual], o da mais recente. Nunca devolve NaN. */
    fun progresso(metrica: Metrica, inicio: Double, atual: Double, alvo: Double): ProgressoMeta {
        val subir = alvo > inicio
        val atingida = if (alvo == inicio) abs(atual - alvo) < 0.05 else if (subir) atual >= alvo else atual <= alvo
        val total = alvo - inicio
        val fracao = when {
            atingida -> 1f
            total == 0.0 -> 0f
            else -> ((atual - inicio) / total).toFloat().coerceIn(0f, 1f)
        }
        return ProgressoMeta(
            metrica = metrica,
            inicio = inicio,
            atual = atual,
            alvo = alvo,
            fracao = fracao,
            atingida = atingida,
            faltam = if (atingida) 0.0 else abs(alvo - atual),
        )
    }

    /** Calcula o progresso a partir das avaliações do aluno; null se a métrica não tem medida. */
    fun progresso(meta: MetaComposicao, avaliacoes: List<br.com.fitpersonal.app.domain.model.AvaliacaoFisica>): ProgressoMeta? {
        val serie = EvolucaoRules.serie(avaliacoes, meta.metrica)
        val primeiro = serie.firstOrNull() ?: return null
        val ultimo = serie.last()
        return progresso(meta.metrica, primeiro.valor, ultimo.valor, meta.alvo)
    }
}
