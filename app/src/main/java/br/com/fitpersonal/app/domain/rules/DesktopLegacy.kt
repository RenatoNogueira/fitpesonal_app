package br.com.fitpersonal.app.domain.rules

import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Conversões de valores do banco do app desktop (Python/SQLite) para o modelo do Android.
 * Puro Kotlin: testável sem Android.
 */
object DesktopLegacy {

    private fun semAcento(s: String) =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun chave(s: String?) = semAcento(s.orEmpty()).trim().lowercase()

    /** Desktop guardava ISO (yyyy-MM-dd); tolera dd/MM/yyyy e timestamps "yyyy-MM-dd HH:mm:ss". */
    fun data(texto: String?): LocalDate? {
        val t = texto?.trim().orEmpty()
        if (t.isEmpty()) return null
        return runCatching { LocalDate.parse(t.take(10)) }.getOrNull()
            ?: runCatching { LocalDate.parse(t, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()
    }

    /** "9:00", "09:00", "09:00:00" → 09:00. */
    fun horario(texto: String?): LocalTime? {
        val m = Regex("^(\\d{1,2}):(\\d{2})").find(texto?.trim().orEmpty()) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        return if (h in 0..23 && min in 0..59) LocalTime.of(h, min) else null
    }

    fun tipo(texto: String?): TipoAgendamento? = when (chave(texto)) {
        "avaliacao" -> TipoAgendamento.AVALIACAO
        "treino" -> TipoAgendamento.TREINO
        "consultoria" -> TipoAgendamento.CONSULTORIA
        else -> null
    }

    fun status(texto: String?): StatusAgendamento? = when (chave(texto)) {
        "pendente" -> StatusAgendamento.PENDENTE
        "concluido" -> StatusAgendamento.CONCLUIDO
        "cancelado" -> StatusAgendamento.CANCELADO
        else -> null
    }

    /** "Mulher (Cisgênero/Transgênero)" → Feminino · "Homem (...)" → Masculino · demais → Outro. */
    fun sexo(texto: String?): String? {
        val c = chave(texto)
        return when {
            c.isEmpty() -> null
            c.startsWith("mulher") || c.startsWith("feminino") -> "Feminino"
            c.startsWith("homem") || c.startsWith("masculino") -> "Masculino"
            else -> "Outro"
        }
    }

    fun digitos(s: String?): String = s.orEmpty().filter { it.isDigit() }

    /** Telefone/CPF/CEP viram só dígitos; vazio → null. */
    fun digitosOuNulo(s: String?): String? = digitos(s).ifEmpty { null }

    /**
     * Desktop serializava o checklist como prefixo de texto: "Condições: Diabetes, Hipertensão; texto livre".
     * Retorna (condições reconhecidas, texto livre restante).
     */
    fun separarCondicoes(historico: String?): Pair<Set<CondicaoSaude>, String?> {
        val h = historico?.trim().orEmpty()
        if (h.isEmpty()) return emptySet<CondicaoSaude>() to null
        val prefixo = "Condições:"
        if (!h.startsWith(prefixo)) return emptySet<CondicaoSaude>() to h
        val resto = h.removePrefix(prefixo)
        val (lista, livre) = if (';' in resto) resto.split(";", limit = 2).let { it[0] to it[1] } else resto to ""
        val condicoes = lista.split(",").mapNotNull { nome ->
            val k = chave(nome)
            CondicaoSaude.entries.firstOrNull { chave(it.rotulo) == k }
        }.toSet()
        return condicoes to livre.trim().ifEmpty { null }
    }

    /**
     * Caminho gravado no desktop → caminho de entrada dentro do zip.
     * Relativos já são "media/..." ou "data/..."; absolutos antigos (ex.: C:\...\_internal\media\x.jpg)
     * são remapeados a partir do último segmento "media"/"data".
     */
    fun caminhoNoZip(gravado: String?): String? {
        val p = gravado?.trim()?.replace('\\', '/').orEmpty()
        if (p.isEmpty()) return null
        val partes = p.split('/').filter { it.isNotEmpty() && it != "." }
        val idx = partes.indexOfLast { it.equals("media", true) || it.equals("data", true) }
        val seguro = if (idx >= 0) partes.drop(idx) else partes
        if (seguro.any { it == ".." } || seguro.isEmpty()) return null
        return seguro.joinToString("/")
    }

    /** Chave de comparação de aluno para evitar duplicatas: CPF completo, senão nome + nascimento. */
    fun chaveAluno(nome: String?, nascimento: LocalDate?, cpf: String?): String {
        val d = digitos(cpf)
        return if (d.length == 11) "cpf:$d" else "nome:${chave(nome)}|${nascimento ?: ""}"
    }
}
