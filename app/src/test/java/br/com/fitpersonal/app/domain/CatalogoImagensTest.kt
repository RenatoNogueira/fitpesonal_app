package br.com.fitpersonal.app.domain

import br.com.fitpersonal.app.domain.rules.CatalogoImagens
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogoImagensTest {
    @Test fun montaUrl() {
        assertEquals(
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/3_4_Sit-Up/0.jpg",
            CatalogoImagens.url("3_4_Sit-Up/0.jpg"),
        )
        assertEquals(
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/A B/0.jpg".replace(" ", "%20"),
            CatalogoImagens.url("/A B/0.jpg"),
        )
    }

    @Test fun limitaQuantidade() {
        assertEquals(2, CatalogoImagens.urls(listOf("a/0.jpg", "a/1.jpg", "a/2.jpg")).size)
        assertEquals(0, CatalogoImagens.urls(listOf("", " ")).size)
    }
}
