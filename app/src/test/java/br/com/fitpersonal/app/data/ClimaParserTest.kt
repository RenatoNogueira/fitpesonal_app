package br.com.fitpersonal.app.data

import br.com.fitpersonal.app.data.remote.ClimaParser
import br.com.fitpersonal.app.domain.model.ClimaRules
import org.junit.Assert.*
import org.junit.Test

class ClimaParserTest {

    private val resposta = """
        {"coord":{"lon":-46.63,"lat":-23.55},
         "weather":[{"id":800,"main":"Clear","description":"céu limpo","icon":"01d"}],
         "main":{"temp":28.6,"feels_like":30.1,"temp_min":26.0,"temp_max":30.0,"pressure":1015,"humidity":61},
         "name":"São Paulo","cod":200}
    """.trimIndent()

    @Test fun converteRespostaDoOpenWeather() {
        val c = ClimaParser.parse(resposta)!!
        assertEquals("São Paulo", c.cidade)
        assertEquals(29, c.temperaturaC)          // 28,6 arredonda para 29
        assertEquals(30, c.sensacaoC)
        assertEquals(61, c.umidade)
        assertEquals("Céu limpo", c.descricao)   // primeira letra maiúscula
        assertEquals("01d", c.icone)
    }

    @Test fun camposOpcionaisAusentes() {
        val c = ClimaParser.parse(
            """{"weather":[{"description":"nublado","icon":"04d"}],"main":{"temp":-1.4},"name":"Gramado"}"""
        )!!
        assertEquals(-1, c.temperaturaC)
        assertNull(c.sensacaoC)
        assertNull(c.umidade)
    }

    @Test fun respostaInvalidaViraNull() {
        assertNull(ClimaParser.parse("não é json"))
        assertNull(ClimaParser.parse("""{"cod":"404","message":"city not found"}"""))
    }

    @Test fun emojiPorIcone() {
        assertEquals("☀️", ClimaRules.emoji("01d"))
        assertEquals("🌙", ClimaRules.emoji("01n"))
        assertEquals("⛈️", ClimaRules.emoji("11d"))
        assertEquals("🌤️", ClimaRules.emoji("999"))
        assertEquals("28°", ClimaRules.grau(28))
    }
}
