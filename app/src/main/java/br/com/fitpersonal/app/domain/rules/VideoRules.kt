package br.com.fitpersonal.app.domain.rules

/** Regras do link de vídeo de demonstração de um exercício. */
object VideoRules {
    const val MAX = 500

    private val PROIBIDOS = charArrayOf('"', '\'', '<', '>', '\\')

    /**
     * Link normalizado ("youtube.com/..." vira "https://youtube.com/...") ou null quando o texto está
     * vazio ou não é um endereço http(s) aceitável (sem espaços, aspas ou sinais de marcação).
     */
    fun normalizar(texto: String?): String? {
        val t = texto?.trim().orEmpty()
        if (t.isEmpty() || t.length > MAX) return null
        if (t.any { it.isWhitespace() } || t.any { it in PROIBIDOS }) return null
        val comEsquema = if ("://" in t) t else "https://$t"
        val m = comEsquema.lowercase()
        if (!m.startsWith("http://") && !m.startsWith("https://")) return null
        val host = comEsquema.substringAfter("://")
            .substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfter('@').substringBefore(':')
        if (host.isEmpty() || '.' !in host || host.startsWith('.') || host.endsWith('.')) return null
        return comEsquema
    }

    /** Mensagem para um texto preenchido e inválido; null quando está vazio ou correto. */
    fun erro(texto: String?): String? =
        if (texto.isNullOrBlank() || normalizar(texto) != null) null
        else "Link inválido. Use um endereço http(s), como o do YouTube."

    /** "YouTube" ou o domínio, para mostrar no botão. */
    fun origem(url: String): String {
        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').lowercase().removePrefix("www.")
        return when {
            "youtube" in host || host == "youtu.be" -> "YouTube"
            "instagram" in host -> "Instagram"
            "vimeo" in host -> "Vimeo"
            else -> host
        }
    }
}
