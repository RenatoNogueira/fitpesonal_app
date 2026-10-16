package br.com.fitpersonal.app.domain.rules

import kotlin.math.abs

/** O que o aluno fez num exercício em uma sessão passada (mais recente primeiro nas listas). */
data class RegistroExercicio(val cargaKg: Double?, val feito: Boolean, val esforcoSessao: Int?)

data class SugestaoCarga(val cargaKg: Double, val motivo: String)

/**
 * Progressão automática de carga: sugere subir quando o aluno fechou as últimas sessões com a mesma
 * carga, sem esforço máximo. Não sobe se a última sessão ficou incompleta ou foi exaustiva.
 * É só uma sugestão — o aluno decide.
 */
object ProgressaoRules {
    /** Sessões seguidas, na mesma carga e completas, antes de sugerir aumento. */
    const val SESSOES_PARA_AUMENTAR = 2

    /** Esforço da sessão (1–10) a partir do qual não se sugere aumento. */
    const val ESFORCO_LIMITE = 9

    /** Carga da sessão mais recente em que o exercício teve carga registrada. */
    fun ultimaCarga(historico: List<RegistroExercicio>): Double? =
        historico.firstOrNull { it.cargaKg != null }?.cargaKg

    fun sugerir(historico: List<RegistroExercicio>): SugestaoCarga? {
        val recentes = historico.filter { it.cargaKg != null }.take(SESSOES_PARA_AUMENTAR)
        if (recentes.size < SESSOES_PARA_AUMENTAR) return null
        val carga = recentes.first().cargaKg ?: return null
        val estavel = recentes.all { abs((it.cargaKg ?: -1.0) - carga) < 0.01 }
        val completas = recentes.all { it.feito }
        val leves = recentes.all { (it.esforcoSessao ?: 0) < ESFORCO_LIMITE }
        if (!estavel || !completas || !leves) return null

        val nova = CargaRules.proximaCarga(carga)
        return SugestaoCarga(
            cargaKg = nova,
            motivo = "Você fechou as últimas $SESSOES_PARA_AUMENTAR sessões com ${formatarKg(carga)} kg.",
        )
    }

    fun formatarKg(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else String.format(java.util.Locale("pt", "BR"), "%.1f", v)
}
