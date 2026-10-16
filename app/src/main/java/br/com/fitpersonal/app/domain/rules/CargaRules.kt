package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

/** Progressão de carga: leitura do campo de texto "carga" e sugestão de aumento. */
object CargaRules {
    /** Dias parado na mesma carga antes de sugerir aumento. */
    const val DIAS_ESTAGNADO = 21

    private val NUMERO = Regex("""(\d+(?:[.,]\d+)?)""")

    /**
     * Primeiro número do texto, em kg ("20 kg", "12,5", "20-25" → 20,0). Texto sem número,
     * zero ou absurdo (> 1000) devolve null: "corporal", "elástico leve" etc. não entram no histórico.
     */
    fun parseKg(texto: String?): Double? {
        val m = NUMERO.find(texto.orEmpty()) ?: return null
        val v = m.value.replace(',', '.').toDoubleOrNull() ?: return null
        return v.takeIf { it > 0 && it <= 1000 }
    }

    /** Próxima carga sugerida: +5%, arredondada para cima ao meio quilo, no mínimo +0,5 kg. */
    fun proximaCarga(atual: Double): Double = max(atual + 0.5, ceil(atual * 1.05 * 2) / 2)

    /** Sugere aumento quando a carga atual está parada há [DIAS_ESTAGNADO] dias ou mais; senão null. */
    fun sugerirAumento(ultimaMudanca: PontoSerie, hoje: LocalDate): Double? {
        val parado = ChronoUnit.DAYS.between(ultimaMudanca.data, hoje)
        return if (parado >= DIAS_ESTAGNADO) proximaCarga(ultimaMudanca.valor) else null
    }

    /** Chave estável para agrupar o mesmo exercício digitado com caixa/espaços diferentes. */
    fun chaveExercicio(nome: String): String = BuscaRules.normalizar(nome).replace(Regex("\\s+"), " ")
}
