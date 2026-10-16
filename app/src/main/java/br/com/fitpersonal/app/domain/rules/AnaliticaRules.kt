package br.com.fitpersonal.app.domain.rules

/**
 * Nomes de telas para as estatísticas de uso. Só devolve nomes fixos: ids de aluno, treino etc. que aparecem
 * nas rotas ("aluno/{id}") NUNCA saem do aparelho, e nada digitado pelo usuário entra nos eventos.
 */
object AnaliticaRules {
    private val NOMES = mapOf(
        "inicio" to "inicio",
        "alunos" to "alunos",
        "agenda" to "agenda",
        "treinos" to "treinos",
        "avaliacoes" to "avaliacoes",
        "busca" to "busca",
        "perfil" to "perfil",
        "espera" to "lista_espera",
    )

    /** Telas de formulário/edição: sair delas sem concluir nada conta como abandono. */
    private val FORMULARIOS = listOf("aluno/", "agendamento/", "avaliacao/", "treino/")

    /** Nome estável da tela a partir da rota de navegação, sem argumentos; null para rota vazia. */
    fun nomeDaTela(rota: String?): String? {
        if (rota.isNullOrBlank()) return null
        val base = rota.substringBefore('?').substringBefore('/')
        NOMES[base]?.let { return it }
        // Rotas com argumento: "treino/{id}/executar", "aluno/{id}", "avaliacao/{id}?aluno={aluno}", ...
        val partes = rota.substringBefore('?').split('/').filter { it.isNotEmpty() && !it.startsWith("{") && !it.all(Char::isDigit) }
        val nome = partes.joinToString("_").lowercase().filter { it.isLetterOrDigit() || it == '_' }
        return nome.ifEmpty { null }?.take(36)
    }

    fun ehFormulario(rota: String?): Boolean =
        rota != null && !rota.contains("/executar") && FORMULARIOS.any { rota.startsWith(it) }

    /** Parâmetro de evento seguro: só letras, números e "_" (limite do Firebase: 40 caracteres). */
    fun chaveSegura(texto: String): String =
        texto.lowercase().map { if (it.isLetterOrDigit()) it else '_' }.joinToString("").trim('_').take(40)
}
