package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Meta de um aluno: levar [metrica] até [valor] (para cima ou para baixo, conforme o ponto de partida). */
data class Meta(val metrica: Metrica, val valor: Double)

data class ProgressoMeta(
    val inicial: Double,
    val atual: Double,
    val alvo: Double,
    /** 0..1: quanto do caminho inicial → alvo já foi percorrido. */
    val fracao: Double,
    val atingida: Boolean,
    /** alvo − atual: positivo = falta subir, negativo = falta descer. */
    val falta: Double,
)

object MetaRules {

    /** Reconstrói a meta salva (nome do enum + valor); dado incompleto ou desconhecido = sem meta. */
    fun meta(metricaNome: String?, valor: Double?): Meta? {
        val m = Metrica.entries.firstOrNull { it.name == metricaNome } ?: return null
        return valor?.takeIf { it > 0 }?.let { Meta(m, it) }
    }

    /**
     * Progresso da primeira medida ([inicial]) até [alvo], com a medida [atual].
     * A direção vem do ponto de partida: alvo abaixo do inicial = meta de redução.
     */
    fun progresso(inicial: Double, atual: Double, alvo: Double): ProgressoMeta {
        val atingida = when {
            alvo > inicial -> atual >= alvo
            alvo < inicial -> atual <= alvo
            else -> abs(atual - alvo) < 1e-9
        }
        val fracao = if (alvo == inicial) {
            if (atingida) 1.0 else 0.0
        } else {
            ((atual - inicial) / (alvo - inicial)).coerceIn(0.0, 1.0)
        }
        return ProgressoMeta(inicial, atual, alvo, fracao, atingida, alvo - atual)
    }
}

sealed interface SituacaoReavaliacao {
    /** Há intervalo programado, mas o aluno nunca foi avaliado. */
    data object SemAvaliacao : SituacaoReavaliacao
    data class EmDia(val diasRestantes: Int) : SituacaoReavaliacao
    /** Vence em até [ReavaliacaoRules.AVISO_DIAS] dias (0 = hoje). */
    data class Proxima(val diasRestantes: Int) : SituacaoReavaliacao
    data class Atrasada(val diasAtraso: Int) : SituacaoReavaliacao
}

object ReavaliacaoRules {
    val OPCOES_DIAS = listOf(15, 30, 45, 60, 90)
    const val AVISO_DIAS = 7

    fun rotulo(dias: Int): String = "A cada $dias dias"

    /** null = aluno sem reavaliação programada. */
    fun situacao(ultimaAvaliacao: LocalDate?, intervaloDias: Int?, hoje: LocalDate): SituacaoReavaliacao? {
        if (intervaloDias == null || intervaloDias <= 0) return null
        if (ultimaAvaliacao == null) return SituacaoReavaliacao.SemAvaliacao
        val restantes = ChronoUnit.DAYS.between(hoje, ultimaAvaliacao.plusDays(intervaloDias.toLong())).toInt()
        return when {
            restantes < 0 -> SituacaoReavaliacao.Atrasada(-restantes)
            restantes <= AVISO_DIAS -> SituacaoReavaliacao.Proxima(restantes)
            else -> SituacaoReavaliacao.EmDia(restantes)
        }
    }

    /**
     * Texto do aviso diário; null quando não há nada a avisar.
     * [atrasadas] inclui os nunca avaliados; [vencendo] são os que vencem em até [AVISO_DIAS] dias.
     */
    fun textoAviso(atrasadas: Int, vencendo: Int): String? {
        if (atrasadas <= 0 && vencendo <= 0) return null
        val partes = buildList {
            if (atrasadas > 0) add("$atrasadas " + if (atrasadas == 1) "atrasada" else "atrasadas")
            if (vencendo > 0) add("$vencendo " + if (vencendo == 1) "vence em breve" else "vencem em breve")
        }
        return "Reavaliações: " + partes.joinToString(" e ") + "."
    }

    /** Precisa de atenção no painel (atrasada, vencendo ou nunca avaliada)? */
    fun pedeAtencao(s: SituacaoReavaliacao?): Boolean =
        s is SituacaoReavaliacao.Atrasada || s is SituacaoReavaliacao.Proxima || s is SituacaoReavaliacao.SemAvaliacao
}
