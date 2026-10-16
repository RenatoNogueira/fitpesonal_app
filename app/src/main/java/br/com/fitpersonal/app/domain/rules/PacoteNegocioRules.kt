package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import java.time.LocalDate
import java.time.YearMonth

enum class SituacaoPacote(val rotulo: String) {
    SEM_PACOTE("Sem pacote"),
    EM_USO("Em uso"),
    ULTIMA_SESSAO("Última sessão"),
    ESGOTADO("Esgotado"),
}

data class ResumoPacote(
    val situacao: SituacaoPacote,
    val total: Int = 0,
    val usadas: Int = 0,
    /** Sessões que ainda restam (nunca negativo). */
    val saldo: Int = 0,
    /** Sessões feitas além do pacote. */
    val excedentes: Int = 0,
) {
    val precisaAtencao: Boolean
        get() = situacao == SituacaoPacote.ULTIMA_SESSAO || situacao == SituacaoPacote.ESGOTADO
}

/** Pacote de sessões: o saldo baixa a cada agendamento concluído desde o início do pacote. */
object PacoteRules {
    val OPCOES = listOf(4, 8, 12, 16, 24)

    /** Avaliações físicas não consomem o pacote; treinos e consultorias sim. */
    fun consome(tipo: TipoAgendamento): Boolean = tipo != TipoAgendamento.AVALIACAO

    /** Texto do aviso diário; null quando nenhum pacote precisa de atenção. */
    fun textoAviso(esgotados: Int, naUltima: Int): String? {
        if (esgotados <= 0 && naUltima <= 0) return null
        val partes = buildList {
            if (esgotados > 0) add("$esgotados " + if (esgotados == 1) "esgotado" else "esgotados")
            if (naUltima > 0) add("$naUltima na última sessão")
        }
        return "Pacotes: " + partes.joinToString(" e ") + "."
    }

    fun resumir(total: Int?, inicio: LocalDate?, agendamentos: List<Agendamento>): ResumoPacote {
        if (total == null || total <= 0 || inicio == null) return ResumoPacote(SituacaoPacote.SEM_PACOTE)
        val usadas = agendamentos.count {
            it.status == StatusAgendamento.CONCLUIDO && consome(it.tipo) && !it.data.isBefore(inicio)
        }
        val saldo = (total - usadas).coerceAtLeast(0)
        val situacao = when {
            saldo == 0 -> SituacaoPacote.ESGOTADO
            saldo == 1 -> SituacaoPacote.ULTIMA_SESSAO
            else -> SituacaoPacote.EM_USO
        }
        return ResumoPacote(situacao, total, usadas, saldo, (usadas - total).coerceAtLeast(0))
    }
}

data class ResumoNegocio(
    val sessoes: Int,
    val sessoesMesAnterior: Int,
    val receitaCentavos: Long,
    val receitaMesAnteriorCentavos: Long,
    val metaSessoes: Int?,
    val metaReceitaCentavos: Long?,
) {
    /** 0..1 (limitado); null sem meta. */
    val fracaoSessoes: Double? get() = metaSessoes?.takeIf { it > 0 }?.let { (sessoes.toDouble() / it).coerceIn(0.0, 1.0) }
    val fracaoReceita: Double? get() =
        metaReceitaCentavos?.takeIf { it > 0 }?.let { (receitaCentavos.toDouble() / it).coerceIn(0.0, 1.0) }

    val deltaSessoes: Int get() = sessoes - sessoesMesAnterior
    val deltaReceitaCentavos: Long get() = receitaCentavos - receitaMesAnteriorCentavos
}

object NegocioRules {

    /**
     * [sessoesConcluidas] = datas dos agendamentos concluídos; [recebimentos] = (competência, valor).
     * Compara o mês de [hoje] com o anterior. Metas zero ou negativas viram "sem meta".
     */
    fun resumir(
        sessoesConcluidas: List<LocalDate>,
        recebimentos: List<Pair<YearMonth, Long>>,
        hoje: LocalDate,
        metaSessoes: Int?,
        metaReceitaCentavos: Long?,
    ): ResumoNegocio {
        val atual = YearMonth.from(hoje)
        val anterior = atual.minusMonths(1)
        return ResumoNegocio(
            sessoes = sessoesConcluidas.count { YearMonth.from(it) == atual },
            sessoesMesAnterior = sessoesConcluidas.count { YearMonth.from(it) == anterior },
            receitaCentavos = recebimentos.filter { it.first == atual }.sumOf { it.second },
            receitaMesAnteriorCentavos = recebimentos.filter { it.first == anterior }.sumOf { it.second },
            metaSessoes = metaSessoes?.takeIf { it > 0 },
            metaReceitaCentavos = metaReceitaCentavos?.takeIf { it > 0 },
        )
    }
}
