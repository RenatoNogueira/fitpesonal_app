package br.com.fitpersonal.app.domain.rules

/**
 * Questionário PAR-Q (Prontidão para Atividade Física), 7 perguntas de sim/não.
 * Apoio à triagem: qualquer "sim" indica que convém liberação médica antes de iniciar/intensificar o treino.
 * Não é diagnóstico.
 */
object ParqRules {

    val PERGUNTAS: List<String> = listOf(
        "Algum médico já disse que você possui um problema de coração e que só deveria realizar atividade física supervisionada?",
        "Você sente dores no peito quando pratica atividade física?",
        "No último mês, você sentiu dores no peito quando praticou atividade física?",
        "Você apresenta desequilíbrio devido a tontura e/ou perda de consciência?",
        "Você possui algum problema ósseo ou articular que poderia ser piorado pela atividade física?",
        "Você toma atualmente algum medicamento para pressão arterial e/ou problema de coração?",
        "Sabe de alguma outra razão pela qual você não deveria praticar atividade física?",
    )

    private const val SIM = 'S'
    private const val NAO = 'N'
    private const val SEM_RESPOSTA = '-'

    /** Uma letra por pergunta: S (sim), N (não) ou - (sem resposta). */
    fun codificar(respostas: List<Boolean?>): String =
        PERGUNTAS.indices.joinToString("") { i ->
            when (respostas.getOrNull(i)) {
                true -> SIM.toString()
                false -> NAO.toString()
                null -> SEM_RESPOSTA.toString()
            }
        }

    /** Sempre devolve [PERGUNTAS].size itens; texto ausente, curto ou com letras desconhecidas vira "sem resposta". */
    fun decodificar(codigo: String?): List<Boolean?> =
        PERGUNTAS.indices.map { i ->
            when (codigo?.getOrNull(i)) {
                SIM -> true
                NAO -> false
                else -> null
            }
        }

    /** O questionário foi aplicado (ao menos uma pergunta respondida)? */
    fun aplicado(codigo: String?): Boolean = decodificar(codigo).any { it != null }

    fun completo(codigo: String?): Boolean = decodificar(codigo).all { it != null }

    /** Índices (0-based) das perguntas respondidas com "sim". */
    fun respostasSim(codigo: String?): List<Int> =
        decodificar(codigo).mapIndexedNotNull { i, r -> if (r == true) i else null }

    fun requerLiberacao(codigo: String?): Boolean = respostasSim(codigo).isNotEmpty()
}
