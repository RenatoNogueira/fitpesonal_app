package br.com.fitpersonal.app.domain.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Configuração dos lembretes do modo aluno. [dias] é uma máscara: bit 0 = segunda … bit 6 = domingo. */
data class LembreteAluno(
    val ativo: Boolean = false,
    val dias: Int = DIAS_PADRAO,
    /** Horário do aviso, em minutos desde 00:00. */
    val minutos: Int = 18 * 60,
) {
    companion object {
        /** Segunda, quarta e sexta. */
        const val DIAS_PADRAO = 0b0010101
    }
}

data class AvisoAluno(val titulo: String, val texto: String)

/** Regras puras dos avisos do modo aluno: dia de treino marcado e retenção (sumiço de alguns dias). */
object LembreteAlunoRules {
    /** A partir de quantos dias sem treinar o aluno recebe o "sentimos sua falta". */
    const val DIAS_PARA_RETENCAO = 3
    /** Depois disso para de insistir (evita notificação eterna para quem abandonou o app). */
    const val DIAS_LIMITE_RETENCAO = 14

    fun bit(d: DayOfWeek): Int = 1 shl (d.value - 1)   // segunda = 1 → bit 0

    fun diaMarcado(mascara: Int, d: DayOfWeek): Boolean = mascara and bit(d) != 0

    fun alternar(mascara: Int, d: DayOfWeek): Int = mascara xor bit(d)

    fun rotuloDia(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Seg"
        DayOfWeek.TUESDAY -> "Ter"
        DayOfWeek.WEDNESDAY -> "Qua"
        DayOfWeek.THURSDAY -> "Qui"
        DayOfWeek.FRIDAY -> "Sex"
        DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }

    fun rotuloHora(minutos: Int): String = "%02d:%02d".format(minutos / 60, minutos % 60)

    /**
     * Próxima divisão a treinar: a seguinte à última sessão, em ordem alfabética, voltando ao início.
     * Sem sessão anterior (ou divisão desconhecida) começa pela primeira.
     */
    fun proximaDivisao(divisoes: List<String>, ultima: String?): String? {
        val ordem = divisoes.map { it.uppercase() }.distinct().sorted()
        if (ordem.isEmpty()) return null
        val i = ordem.indexOf(ultima?.uppercase())
        return if (i < 0) ordem.first() else ordem[(i + 1) % ordem.size]
    }

    /**
     * O que avisar hoje (ou null para não avisar):
     *  - já treinou hoje → nada;
     *  - hoje é dia marcado → "Hoje é dia de treinar", com treino e divisão sugerida;
     *  - não é dia marcado mas faz 3 a 14 dias sem treinar → retenção.
     */
    fun avisoDeHoje(
        hoje: LocalDate,
        config: LembreteAluno,
        ultimaSessao: LocalDate?,
        nomeTreino: String?,
        proximaDivisao: String?,
    ): AvisoAluno? {
        if (!config.ativo) return null
        if (ultimaSessao == hoje) return null

        if (diaMarcado(config.dias, hoje.dayOfWeek)) {
            val qual = listOfNotNull(
                nomeTreino?.takeIf { it.isNotBlank() },
                proximaDivisao?.let { "Treino $it" },
            ).joinToString(" • ")
            return AvisoAluno(
                titulo = "Hoje é dia de treinar 💪",
                texto = if (qual.isEmpty()) "Abra o app e registre sua sessão." else "$qual. Bora?",
            )
        }

        if (ultimaSessao != null) {
            val dias = ChronoUnit.DAYS.between(ultimaSessao, hoje).toInt()
            if (dias in DIAS_PARA_RETENCAO..DIAS_LIMITE_RETENCAO) {
                return AvisoAluno(
                    titulo = "Sentimos sua falta",
                    texto = "Faz $dias dias que você não treina. Que tal retomar hoje?",
                )
            }
        }
        return null
    }
}
