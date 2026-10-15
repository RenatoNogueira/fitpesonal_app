package br.com.fitpersonal.app.domain.rules

/**
 * Endereço das fotos do catálogo de exercícios (free-exercise-db, domínio público). O catálogo guarda só
 * o caminho relativo ("3_4_Sit-Up/0.jpg"); as imagens são baixadas sob demanda e ficam no cache do Coil,
 * então só consomem internet na primeira vez que são vistas.
 */
object CatalogoImagens {
    private const val BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

    fun url(relativo: String): String = BASE + relativo.trim().trimStart('/').replace(" ", "%20")

    /** Até [max] imagens por exercício (as demais costumam ser a mesma posição de outro ângulo). */
    fun urls(relativos: List<String>, max: Int = 2): List<String> =
        relativos.filter { it.isNotBlank() }.take(max).map(::url)
}
