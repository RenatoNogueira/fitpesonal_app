package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento

data class LinhaAgendaMes(
    val nomeAluno: String,
    val tipo: TipoAgendamento,
    val status: StatusAgendamento,
)

data class ResumoAluno(
    val nome: String,
    val concluidos: Int,
    val cancelados: Int,
    val pendentes: Int,
)

data class ResumoMensal(
    val total: Int,
    val concluidos: Int,
    val cancelados: Int,
    val pendentes: Int,
    /** concluídos / (concluídos + cancelados); null quando nenhum foi registrado como concluído ou cancelado. */
    val taxaComparecimento: Double?,
    val porTipo: Map<TipoAgendamento, Int>,
    /** Mais sessões concluídas primeiro; empate em ordem alfabética. */
    val porAluno: List<ResumoAluno>,
    /** Faltas (aluno não compareceu); contam contra o comparecimento. */
    val faltas: Int = 0,
)

object RelatorioMensalRules {

    fun resumir(linhas: List<LinhaAgendaMes>): ResumoMensal {
        val concluidos = linhas.count { it.status == StatusAgendamento.CONCLUIDO }
        val cancelados = linhas.count { it.status == StatusAgendamento.CANCELADO }
        val pendentes = linhas.count { it.status == StatusAgendamento.PENDENTE }
        val faltas = linhas.count { it.status == StatusAgendamento.FALTOU }
        val registrados = concluidos + cancelados + faltas
        val porAluno = linhas.groupBy { it.nomeAluno }
            .map { (nome, l) ->
                ResumoAluno(
                    nome = nome,
                    concluidos = l.count { it.status == StatusAgendamento.CONCLUIDO },
                    cancelados = l.count { it.status == StatusAgendamento.CANCELADO },
                    pendentes = l.count { it.status == StatusAgendamento.PENDENTE },
                )
            }
            .sortedWith(compareByDescending<ResumoAluno> { it.concluidos }.thenBy { it.nome.lowercase() })
        return ResumoMensal(
            total = linhas.size,
            concluidos = concluidos,
            cancelados = cancelados,
            pendentes = pendentes,
            taxaComparecimento = if (registrados == 0) null else concluidos.toDouble() / registrados,
            porTipo = TipoAgendamento.entries.associateWith { t -> linhas.count { it.tipo == t } }.filterValues { it > 0 },
            porAluno = porAluno,
            faltas = faltas,
        )
    }
}
