package br.com.fitpersonal.app.domain.rules

/**
 * Método de execução de um exercício. O [id] é o que vai para o banco e para o arquivo do aluno.
 * [agrupa] = o exercício forma uma sequência com os vizinhos do mesmo método (bi-set, circuito).
 * [cronometrado] = tem cronômetro no app do aluno (AMRAP e EMOM).
 */
enum class MetodoTreino(
    val id: String,
    val rotulo: String,
    val explicacao: String,
    val agrupa: Boolean = false,
    val cronometrado: Boolean = false,
) {
    BISET("BISET", "Bi-set", "Faça este exercício e o próximo em sequência, sem descanso entre eles.", agrupa = true),
    CIRCUITO("CIRCUITO", "Circuito", "Faça os exercícios do circuito um após o outro e só descanse ao completar a volta.", agrupa = true),
    AMRAP("AMRAP", "AMRAP", "Faça o máximo de voltas ou repetições possível dentro do tempo.", cronometrado = true),
    EMOM("EMOM", "EMOM", "Uma série a cada minuto: o que sobrar do minuto é descanso.", cronometrado = true),
    DROPSET("DROPSET", "Drop-set", "Ao falhar, reduza a carga e continue sem descansar."),
    ;

    companion object {
        /** Método de um id salvo; null (série normal) para vazio ou id desconhecido de versão futura. */
        fun de(id: String?): MetodoTreino? = entries.firstOrNull { it.id == id }
    }
}

object MetodoRules {
    /**
     * Posição do exercício [indice] dentro do bloco de exercícios consecutivos com o mesmo método
     * agrupável: Pair(posição começando em 1, tamanho do bloco). Null se não faz parte de um bloco.
     */
    fun posicaoNoBloco(metodos: List<String?>, indice: Int): Pair<Int, Int>? {
        val m = MetodoTreino.de(metodos.getOrNull(indice)) ?: return null
        if (!m.agrupa) return null
        var inicio = indice
        while (inicio > 0 && metodos[inicio - 1] == m.id) inicio--
        var fim = indice
        while (fim < metodos.lastIndex && metodos[fim + 1] == m.id) fim++
        val tamanho = fim - inicio + 1
        return if (tamanho < 2) null else (indice - inicio + 1) to tamanho
    }

    /** Duração padrão sugerida do cronômetro, em minutos. */
    fun minutosPadrao(m: MetodoTreino): Int = if (m == MetodoTreino.EMOM) 10 else 12
}
