package br.com.fitpersonal.app.domain.rules

import kotlin.math.max
import java.util.Locale

/** Regras do modo treino: leitura dos campos de texto da ficha (séries, descanso, carga). */
object ExecucaoRules {
    const val SERIES_PADRAO = 3
    const val DESCANSO_PADRAO_S = 60
    private const val DESCANSO_MIN_S = 10
    private const val DESCANSO_MAX_S = 600

    private val NUMERO = Regex("""\d+""")
    private val MIN_SEG = Regex("""(\d+)\s*:\s*(\d{1,2})""")

    /** Quantidade de séries do campo "séries" ("3", "4x", "3 séries" → 3 e 4); vazio/inválido → [SERIES_PADRAO]. */
    fun series(texto: String?): Int {
        val n = NUMERO.find(texto.orEmpty())?.value?.toIntOrNull() ?: return SERIES_PADRAO
        return n.takeIf { it in 1..20 } ?: SERIES_PADRAO
    }

    /**
     * Segundos de descanso: "60", "60s", "90 seg", "1:30", "2 min", "1min30". Número puro de 1 a 5 é
     * lido como minutos (quem digita "2" quer dois minutos); acima disso, segundos.
     * Vazio/inválido → [DESCANSO_PADRAO_S]. Limitado a 10 s..10 min.
     */
    fun descansoSegundos(texto: String?): Int {
        val t = texto.orEmpty().trim().lowercase(Locale.ROOT)
        if (t.isEmpty()) return DESCANSO_PADRAO_S
        MIN_SEG.find(t)?.let { m ->
            val s = m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
            return s.coerceIn(DESCANSO_MIN_S, DESCANSO_MAX_S)
        }
        val numeros = NUMERO.findAll(t).map { it.value.toInt() }.toList()
        val primeiro = numeros.firstOrNull() ?: return DESCANSO_PADRAO_S
        val bruto = when {
            "min" in t -> primeiro * 60 + (numeros.getOrNull(1) ?: 0)
            "s" in t -> primeiro
            primeiro in 1..5 -> primeiro * 60
            else -> primeiro
        }
        return bruto.coerceIn(DESCANSO_MIN_S, DESCANSO_MAX_S)
    }

    /** "1:05" a partir de segundos. */
    fun formatarTempo(segundos: Int): String {
        val s = max(0, segundos)
        return "%d:%02d".format(s / 60, s % 60)
    }

    /**
     * Soma [deltaKg] à carga do texto e devolve "20 kg" / "12,5 kg". Texto sem número parte de zero
     * quando o ajuste é positivo; nunca fica abaixo de zero (zero vira vazio).
     */
    fun ajustarCarga(texto: String?, deltaKg: Double): String {
        val atual = CargaRules.parseKg(texto) ?: 0.0
        val novo = atual + deltaKg
        if (novo <= 0.0) return ""
        val inteiro = novo == Math.floor(novo)
        return (if (inteiro) novo.toLong().toString() else "%.1f".format(Locale("pt", "BR"), novo)) + " kg"
    }
}
