package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.Aluno
import java.text.Normalizer
import java.time.LocalDate

enum class FiltroAluno(val rotulo: String) {
    TODOS("Todos"),
    REAVALIACAO("Reavaliação"),
    ANIVERSARIO("Aniversário no mês"),
    SEM_AVALIACAO("Sem avaliação"),
    SEM_CONSENTIMENTO("Sem consentimento"),
}

object BuscaRules {

    /** Minúsculas e sem acentos, para "joao" achar "João". */
    fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .trim()

    /**
     * Todas as palavras do [termo] precisam aparecer em algum dos [campos] (ordem livre).
     * Termo vazio corresponde a tudo.
     */
    fun corresponde(termo: String, vararg campos: String?): Boolean {
        val palavras = normalizar(termo).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (palavras.isEmpty()) return true
        val texto = campos.filterNotNull().joinToString(" ") { normalizar(it) }
        return palavras.all { it in texto }
    }

    fun alunoCorresponde(termo: String, a: Aluno): Boolean =
        corresponde(termo, a.nome, a.objetivo, a.telefone, a.email, a.instagram)

    /** [ultimaAvaliacao] = data da avaliação mais recente do aluno (null = nenhuma). */
    fun passaFiltro(filtro: FiltroAluno, a: Aluno, ultimaAvaliacao: LocalDate?, hoje: LocalDate): Boolean =
        when (filtro) {
            FiltroAluno.TODOS -> true
            FiltroAluno.REAVALIACAO ->
                ReavaliacaoRules.pedeAtencao(ReavaliacaoRules.situacao(ultimaAvaliacao, a.reavaliacaoDias, hoje))
            FiltroAluno.ANIVERSARIO -> a.dataNascimento?.monthValue == hoje.monthValue
            FiltroAluno.SEM_AVALIACAO -> ultimaAvaliacao == null
            FiltroAluno.SEM_CONSENTIMENTO -> a.consentimentoLgpdEm == null
        }
}
