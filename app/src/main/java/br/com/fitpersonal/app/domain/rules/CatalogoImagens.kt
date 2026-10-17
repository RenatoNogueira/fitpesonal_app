package br.com.fitpersonal.app.domain.rules

/**
 * Endereço das fotos do catálogo de exercícios (free-exercise-db, domínio público). O catálogo guarda só
 * o caminho relativo ("3_4_Sit-Up/0.jpg"); as imagens são baixadas sob demanda e ficam no cache do Coil,
 * então só consomem internet na primeira vez que são vistas.
 */
object CatalogoImagens {
    private const val BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    fun url(relativo: String): String = BASE + relativo.trim().trimStart('/').replace(" ", "%20")

    private val ID_SEGURO = Regex("^[A-Za-z0-9_.\\-]{1,80}$")

    /**
     * Fotos do movimento (posição inicial e final) de um exercício do catálogo: "<id>/0.jpg" e "<id>/1.jpg".
     * O id vem do arquivo do professor, então só aceita caracteres simples (nada de "/" ou ".."); senão, lista vazia.
     */
    fun movimento(catalogoId: String?): List<String> {
        val id = catalogoId?.trim().orEmpty()
        if (!ID_SEGURO.matches(id) || id.contains("..")) return emptyList()
        return listOf(url("$id/0.jpg"), url("$id/1.jpg"))
    }

    /** Até [max] imagens por exercício (as demais costumam ser a mesma posição de outro ângulo). */
    fun urls(relativos: List<String>, max: Int = 2): List<String> =
        relativos.filter { it.isNotBlank() }.take(max).map(::url)
}
