package br.com.fitpersonal.app.domain.model

/** Condições atuais de uma cidade (OpenWeather, unidades métricas). */
data class Clima(
    val cidade: String,
    val temperaturaC: Int,
    val sensacaoC: Int?,
    val umidade: Int?,
    val descricao: String,
    /** Código de ícone do OpenWeather, ex.: "01d", "10n". */
    val icone: String,
)

object ClimaRules {

    /** Emoji equivalente ao ícone do OpenWeather (mesmo mapa do app desktop). */
    fun emoji(icone: String): String = when (icone) {
        "01d" -> "☀️"
        "01n" -> "🌙"
        "02d" -> "🌤️"
        "02n", "03d", "03n", "04d", "04n" -> "☁️"
        "09d" -> "🌦️"
        "09n", "10d", "10n" -> "🌧️"
        "11d", "11n" -> "⛈️"
        "13d", "13n" -> "❄️"
        "50d", "50n" -> "🌫️"
        else -> "🌤️"
    }

    /** "28°" — o sinal de menos tipográfico não é usado para manter o texto simples de ler em voz alta. */
    fun grau(valor: Int): String = "$valor°"
}
