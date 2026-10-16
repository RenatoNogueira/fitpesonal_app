package br.com.fitpersonal.app.domain.rules

/** Cidade e estado a partir do texto livre do perfil ("Campinas", "Campinas - SP", "Campinas/SP", "Campinas, São Paulo"). */
data class ConsultaCidade(val cidade: String, val uf: String?)

/** Um resultado do serviço de geocodificação; [estado] vem por extenso ("São Paulo"). */
data class CandidatoCidade(val nome: String, val estado: String?, val lat: Double, val lon: Double)

object CidadeRules {
    private val ESTADOS = mapOf(
        "AC" to "Acre", "AL" to "Alagoas", "AP" to "Amapá", "AM" to "Amazonas", "BA" to "Bahia",
        "CE" to "Ceará", "DF" to "Distrito Federal", "ES" to "Espírito Santo", "GO" to "Goiás",
        "MA" to "Maranhão", "MT" to "Mato Grosso", "MS" to "Mato Grosso do Sul", "MG" to "Minas Gerais",
        "PA" to "Pará", "PB" to "Paraíba", "PR" to "Paraná", "PE" to "Pernambuco", "PI" to "Piauí",
        "RJ" to "Rio de Janeiro", "RN" to "Rio Grande do Norte", "RS" to "Rio Grande do Sul",
        "RO" to "Rondônia", "RR" to "Roraima", "SC" to "Santa Catarina", "SP" to "São Paulo",
        "SE" to "Sergipe", "TO" to "Tocantins",
    )

    private val SEPARADOR = Regex("""\s+[-–—]\s+|\s*[,/|]\s*""")

    private fun norm(s: String) = BuscaRules.normalizar(s)

    /** Sigla do estado a partir da sigla ou do nome por extenso (com ou sem acento); null se não for um estado. */
    fun ufDe(texto: String): String? {
        val t = texto.trim()
        if (t.length == 2 && t.uppercase() in ESTADOS) return t.uppercase()
        return ESTADOS.entries.firstOrNull { norm(it.value) == norm(t) }?.key
    }

    /**
     * Separa cidade e estado. Hífen só separa quando está entre espaços (para não quebrar "Embu-Guaçu"),
     * e uma sigla em maiúsculas no fim ("Santos SP") também vale como estado.
     */
    fun interpretar(texto: String): ConsultaCidade {
        val partes = texto.trim().split(SEPARADOR).map { it.trim() }.filter { it.isNotEmpty() }
        if (partes.isEmpty()) return ConsultaCidade("", null)
        if (partes.size >= 2) {
            val uf = ufDe(partes.last())
            if (uf != null) return ConsultaCidade(partes.dropLast(1).joinToString(" "), uf)
            return ConsultaCidade(partes.first(), null)
        }
        val unica = partes.first()
        val ultima = unica.substringAfterLast(' ', "")
        val resto = unica.substringBeforeLast(' ', "")
        if (ultima.length == 2 && ultima == ultima.uppercase() && ultima in ESTADOS && resto.length > 2) {
            return ConsultaCidade(resto.trim(), ultima)
        }
        return ConsultaCidade(unica, null)
    }

    /** Escolhe o candidato do estado pedido; sem estado (ou sem correspondência), o primeiro da lista. */
    fun escolher(candidatos: List<CandidatoCidade>, uf: String?): CandidatoCidade? {
        if (candidatos.isEmpty()) return null
        val estado = uf?.let { ESTADOS[it] }?.let(::norm)
        return candidatos.firstOrNull { estado != null && it.estado != null && norm(it.estado) == estado }
            ?: candidatos.first()
    }

    /** "Campinas, SP" quando o estado do resultado é conhecido; senão só o nome. */
    fun rotulo(c: CandidatoCidade): String {
        val uf = c.estado?.let(::ufDe)
        return if (uf != null) "${c.nome}, $uf" else c.nome
    }
}
