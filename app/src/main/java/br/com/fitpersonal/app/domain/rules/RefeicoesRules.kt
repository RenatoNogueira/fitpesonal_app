package br.com.fitpersonal.app.domain.rules

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TipoRefeicao(val id: String, val rotulo: String) {
    CAFE("cafe", "Café da manhã"),
    ALMOCO("almoco", "Almoço"),
    LANCHE("lanche", "Lanche"),
    JANTAR("jantar", "Jantar"),
    OUTRO("outro", "Outro"),
    ;

    companion object {
        /** Tipo salvo; id desconhecido ou vazio cai em [OUTRO] (arquivo editado ou de versão futura). */
        fun de(id: String?): TipoRefeicao = entries.firstOrNull { it.id == id } ?: OUTRO
    }
}

/** Uma refeição registrada pelo aluno; [foto] é o nome do arquivo na pasta do diário (opcional). */
data class Refeicao(
    val id: String,
    val data: LocalDate,
    val hora: LocalTime,
    val tipo: TipoRefeicao,
    val nota: String? = null,
    val foto: String? = null,
)

object RefeicoesRules {
    private val DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    /** Tipo sugerido pelo horário em que o aluno está registrando. */
    fun sugerirTipo(hora: LocalTime): TipoRefeicao = when {
        hora.hour in 5..9 -> TipoRefeicao.CAFE
        hora.hour in 10..14 -> TipoRefeicao.ALMOCO
        hora.hour in 15..17 -> TipoRefeicao.LANCHE
        hora.hour in 18..22 -> TipoRefeicao.JANTAR
        else -> TipoRefeicao.OUTRO
    }

    /** Dias com registro, do mais recente para o mais antigo; dentro do dia, em ordem de horário. */
    fun porDia(refeicoes: List<Refeicao>): List<Pair<LocalDate, List<Refeicao>>> =
        refeicoes.groupBy { it.data }
            .toList()
            .sortedByDescending { it.first }
            .map { (dia, lista) -> dia to lista.sortedBy { it.hora } }

    /** Texto do dia para mandar ao profissional junto das fotos. */
    fun textoDoDia(dia: LocalDate, refeicoes: List<Refeicao>, nomeAluno: String? = null): String {
        val doDia = refeicoes.filter { it.data == dia }.sortedBy { it.hora }
        return buildString {
            append("Diário de refeições")
            nomeAluno?.takeIf { it.isNotBlank() }?.let { append(" de ").append(it) }
            append(" — ").append(dia.format(DIA)).append('\n')
            if (doDia.isEmpty()) append("Nenhuma refeição registrada.")
            doDia.forEach { r ->
                append("• ").append(r.hora.format(HORA)).append(' ').append(r.tipo.rotulo)
                r.nota?.takeIf { it.isNotBlank() }?.let { append(": ").append(it.trim()) }
                if (r.foto != null) append(" (foto)")
                append('\n')
            }
        }.trimEnd()
    }
}
