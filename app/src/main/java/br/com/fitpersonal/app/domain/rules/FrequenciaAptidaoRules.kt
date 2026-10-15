package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class ResumoFrequencia(
    val concluidos: Int,
    val faltas: Int,
    val cancelados: Int,
    /** faltas / (concluídos + faltas); null sem sessões que contem. */
    val taxaFaltas: Double?,
    /** Faltas consecutivas mais recentes (ignora cancelados e pendentes). */
    val faltasSeguidas: Int,
)

object FrequenciaRules {
    /** Taxa de faltas a partir da qual vale chamar o aluno para conversar. */
    const val LIMIAR_ATENCAO = 0.25

    /** Considera só agendamentos até [hoje]; futuros pendentes não contam. */
    fun resumir(agendamentos: List<Agendamento>, hoje: LocalDate): ResumoFrequencia {
        val passados = agendamentos.filter { !it.data.isAfter(hoje) }
        val concluidos = passados.count { it.status == StatusAgendamento.CONCLUIDO }
        val faltas = passados.count { it.status == StatusAgendamento.FALTOU }
        val cancelados = passados.count { it.status == StatusAgendamento.CANCELADO }
        val base = concluidos + faltas

        val seguidas = passados
            .filter { it.status == StatusAgendamento.CONCLUIDO || it.status == StatusAgendamento.FALTOU }
            .sortedWith(compareByDescending<Agendamento> { it.data }.thenByDescending { it.horario })
            .takeWhile { it.status == StatusAgendamento.FALTOU }
            .size

        return ResumoFrequencia(
            concluidos = concluidos,
            faltas = faltas,
            cancelados = cancelados,
            taxaFaltas = if (base == 0) null else faltas.toDouble() / base,
            faltasSeguidas = seguidas,
        )
    }

    fun pedeAtencao(r: ResumoFrequencia): Boolean =
        r.faltasSeguidas >= 2 || ((r.taxaFaltas ?: 0.0) >= LIMIAR_ATENCAO && r.faltas >= 3)

    fun rotuloDia(d: DayOfWeek): String =
        d.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
}

enum class TipoTeste(
    val rotulo: String,
    val unidade: String,
    val casas: Int,
    /** Faixa plausível para evitar erro de digitação. */
    val minimo: Double,
    val maximo: Double,
    val descricao: String,
) {
    COOPER_12MIN("Cooper 12 min", "m", 0, 300.0, 6000.0, "Distância percorrida em 12 minutos."),
    SENTAR_ALCANCAR("Sentar e alcançar", "cm", 1, -30.0, 80.0, "Flexibilidade (banco de Wells)."),
    FLEXAO_BRACO("Flexão de braço", "reps", 0, 0.0, 500.0, "Repetições máximas com técnica correta."),
    ABDOMINAL_1MIN("Abdominal 1 min", "reps", 0, 0.0, 500.0, "Repetições em 1 minuto."),
    PRANCHA("Prancha", "s", 0, 0.0, 3600.0, "Tempo máximo mantendo a posição."),
}

data class TesteAptidao(
    val id: Long = 0,
    val idAluno: Long,
    val data: LocalDate = LocalDate.now(),
    val tipo: TipoTeste,
    val valor: Double,
    val observacao: String? = null,
)

object AptidaoRules {

    /** VO2 máx estimado (ml/kg/min) pela equação de Cooper: (distância − 504,9) / 44,73. */
    fun vo2Cooper(distanciaM: Double): Double? =
        if (distanciaM in 500.0..6000.0) (distanciaM - 504.9) / 44.73 else null

    fun valido(tipo: TipoTeste, valor: Double): Boolean = valor in tipo.minimo..tipo.maximo

    /** Todos os testes aqui são "quanto maior, melhor". */
    fun sentido(antes: Double, depois: Double, casas: Int): SentidoVariacao {
        val limiar = 0.5 * Math.pow(10.0, -casas.toDouble())
        val delta = depois - antes
        return when {
            kotlin.math.abs(delta) < limiar -> SentidoVariacao.IGUAL
            delta > 0 -> SentidoVariacao.MELHOROU
            else -> SentidoVariacao.PIOROU
        }
    }

    fun serie(testes: List<TesteAptidao>, tipo: TipoTeste): List<PontoSerie> =
        testes.filter { it.tipo == tipo }
            .sortedWith(compareBy({ it.data }, { it.id }))
            .map { PontoSerie(it.data, it.valor) }
}
