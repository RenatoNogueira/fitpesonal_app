package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.SexoBiologico
import java.time.LocalDate
import kotlin.math.abs

/** Métricas acompanhadas ao longo das avaliações. */
enum class Metrica(
    val rotulo: String,
    val unidade: String,
    /** true = valor menor é melhor; false = maior é melhor; null = depende do objetivo (sem juízo). */
    val menorEhMelhor: Boolean?,
    val casas: Int,
) {
    PESO("Peso", "kg", null, 1),
    IMC("IMC", "", null, 1),
    PERCENTUAL_GORDURA("% de gordura", "%", true, 1),
    MASSA_MAGRA("Massa magra", "kg", false, 1),
    MASSA_GORDA("Massa gorda", "kg", true, 1),
    CINTURA("Cintura", "cm", true, 1),
    RCQ("RCQ", "", true, 2),
}

data class PontoSerie(val data: LocalDate, val valor: Double)

enum class SentidoVariacao { MELHOROU, PIOROU, NEUTRO, IGUAL }

data class Variacao(
    val absoluta: Double,
    val percentual: Double?,
    val sentido: SentidoVariacao,
)

data class LinhaComparativo(
    val metrica: Metrica,
    val antes: Double?,
    val depois: Double?,
    val variacao: Variacao?,
)

enum class ClasseRcq(val rotulo: String, val atencao: Boolean) {
    ADEQUADO("Dentro do esperado", false),
    ELEVADO("Risco aumentado", true),
}

object EvolucaoRules {

    /** Relação cintura-quadril; null se faltar dado ou o quadril for inválido. */
    fun rcq(cinturaCm: Double?, quadrilCm: Double?): Double? {
        if (cinturaCm == null || quadrilCm == null || cinturaCm <= 0 || quadrilCm <= 0) return null
        return cinturaCm / quadrilCm
    }

    /**
     * Referência da OMS para obesidade abdominal: RCQ ≥ 0,90 (homens) e ≥ 0,85 (mulheres).
     * Apoio ao profissional, não diagnóstico. Sem sexo informado não classifica.
     */
    fun classificarRcq(rcq: Double, sexo: SexoBiologico?): ClasseRcq? = when (sexo) {
        SexoBiologico.MASCULINO -> if (rcq >= 0.90) ClasseRcq.ELEVADO else ClasseRcq.ADEQUADO
        SexoBiologico.FEMININO -> if (rcq >= 0.85) ClasseRcq.ELEVADO else ClasseRcq.ADEQUADO
        null -> null
    }

    fun valor(a: AvaliacaoFisica, m: Metrica): Double? = when (m) {
        Metrica.PESO -> a.pesoKg
        Metrica.IMC -> AvaliacaoRules.imc(a.pesoKg, a.alturaM)
        Metrica.PERCENTUAL_GORDURA -> a.percentualGordura
        Metrica.MASSA_MAGRA -> a.massaMagraKg
        Metrica.MASSA_GORDA -> a.massaGordaKg
        Metrica.CINTURA -> a.cinturaCm
        Metrica.RCQ -> rcq(a.cinturaCm, a.quadrilCm)
    }

    /** Série em ordem cronológica, só com as avaliações que têm a métrica. Mesmo dia: a mais recente (maior id) vence. */
    fun serie(avaliacoes: List<AvaliacaoFisica>, m: Metrica): List<PontoSerie> =
        avaliacoes
            .sortedWith(compareBy({ it.data }, { it.id }))
            .mapNotNull { a -> valor(a, m)?.let { PontoSerie(a.data, it) } }

    fun variacao(m: Metrica, antes: Double, depois: Double): Variacao {
        val delta = depois - antes
        val limiar = 0.5 * Math.pow(10.0, -m.casas.toDouble())   // abaixo da precisão exibida = igual
        val pct = if (antes != 0.0) delta / antes * 100.0 else null
        val sentido = when {
            abs(delta) < limiar -> SentidoVariacao.IGUAL
            m.menorEhMelhor == null -> SentidoVariacao.NEUTRO
            (delta < 0) == m.menorEhMelhor -> SentidoVariacao.MELHOROU
            else -> SentidoVariacao.PIOROU
        }
        return Variacao(delta, pct, sentido)
    }

    /** Compara duas avaliações em todas as métricas em que ao menos uma delas tem valor. */
    fun comparar(antes: AvaliacaoFisica, depois: AvaliacaoFisica): List<LinhaComparativo> =
        Metrica.entries.mapNotNull { m ->
            val a = valor(antes, m)
            val d = valor(depois, m)
            if (a == null && d == null) null
            else LinhaComparativo(m, a, d, if (a != null && d != null) variacao(m, a, d) else null)
        }

    /** Texto curto: "+1,2 kg", "-0,5 %". */
    fun formatarDelta(v: Variacao, m: Metrica): String {
        val sinal = if (v.absoluta > 0) "+" else if (v.absoluta < 0) "-" else ""
        val numero = String.format(java.util.Locale("pt", "BR"), "%.${m.casas}f", abs(v.absoluta))
        val un = if (m.unidade.isNotEmpty()) " ${m.unidade}" else ""
        return "$sinal$numero$un"
    }
}
