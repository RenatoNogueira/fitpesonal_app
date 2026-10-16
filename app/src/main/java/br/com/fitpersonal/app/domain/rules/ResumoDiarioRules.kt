package br.com.fitpersonal.app.domain.rules

/** Textos dos avisos diários (~9h) que não pertencem a uma regra específica: agenda, cobrança e aniversários. */
object ResumoDiarioRules {

    /** "Hoje: 3 sessões, a primeira às 08:00." — null quando não há nada agendado. */
    fun agendaDoDia(quantidade: Int, primeiroHorario: String?): String? {
        if (quantidade <= 0) return null
        val sessoes = if (quantidade == 1) "1 sessão" else "$quantidade sessões"
        return if (primeiroHorario.isNullOrBlank()) "Hoje: $sessoes."
        else "Hoje: $sessoes, a primeira às $primeiroHorario."
    }

    /** "2 alunos com mensalidade em atraso (R$ 300,00 em aberto)." — null quando ninguém deve. */
    fun mensalidades(alunos: Int, emAbertoCentavos: Long): String? {
        if (alunos <= 0) return null
        val quem = if (alunos == 1) "1 aluno com mensalidade em atraso" else "$alunos alunos com mensalidade em atraso"
        return if (emAbertoCentavos > 0) "$quem (${FinanceiroRules.formatarReais(emAbertoCentavos)} em aberto)."
        else "$quem."
    }

    /**
     * Aviso de mensalidades que vencem em breve: "2 mensalidades vencem em até 3 dias — a primeira amanhã."
     * [diasAteMenor] = dias até o vencimento mais próximo (0 = hoje). Null quando não há nenhuma.
     */
    fun vencendo(quantidade: Int, diasAteMenor: Int): String? {
        if (quantidade <= 0 || diasAteMenor < 0) return null
        val quando = when (diasAteMenor) {
            0 -> "hoje"
            1 -> "amanhã"
            else -> "em $diasAteMenor dias"
        }
        return if (quantidade == 1) "1 mensalidade vence $quando. Já pode enviar o Pix de cobrança."
        else "$quantidade mensalidades vencem em breve — a primeira $quando. Já pode enviar o Pix de cobrança."
    }

    /** "Hoje é aniversário de Ana e Bruno." usando o primeiro nome; null sem aniversariantes. */
    fun aniversarios(nomesCompletos: List<String>): String? {
        val nomes = nomesCompletos.map { it.trim().substringBefore(' ') }.filter { it.isNotEmpty() }
        return when (nomes.size) {
            0 -> null
            1 -> "Hoje é aniversário de ${nomes[0]}."
            2 -> "Hoje é aniversário de ${nomes[0]} e ${nomes[1]}."
            else -> "Hoje é aniversário de ${nomes[0]}, ${nomes[1]} e mais ${nomes.size - 2}."
        }
    }
}
