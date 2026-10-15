package br.com.fitpersonal.app.data.remote

import br.com.fitpersonal.app.domain.model.Clima
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

sealed interface ResultadoClima {
    data class Ok(val clima: Clima) : ResultadoClima
    /** O app foi compilado sem OPENWEATHER_API_KEY: o recurso fica desligado. */
    data object SemChave : ResultadoClima
    data object SemCidade : ResultadoClima
    data object CidadeNaoEncontrada : ResultadoClima
    data object ChaveInvalida : ResultadoClima
    data class Falha(val motivo: String) : ResultadoClima
}

/** Converte a resposta JSON de /data/2.5/weather. Separado da rede para ser testado na JVM. */
object ClimaParser {
    fun parse(json: String): Clima? = try {
        val raiz = JSONObject(json)
        val principal = raiz.getJSONObject("main")
        val tempo = raiz.getJSONArray("weather").getJSONObject(0)
        val descricao = tempo.optString("description").replaceFirstChar { it.uppercase() }
        Clima(
            cidade = raiz.optString("name"),
            temperaturaC = principal.getDouble("temp").roundToInt(),
            sensacaoC = if (principal.has("feels_like")) principal.getDouble("feels_like").roundToInt() else null,
            umidade = if (principal.has("humidity")) principal.getInt("humidity") else null,
            descricao = descricao,
            icone = tempo.optString("icon"),
        )
    } catch (e: JSONException) {
        null
    }
}

/**
 * Cliente mínimo do OpenWeather (sem dependências além do que o Android já traz).
 * Envia apenas o nome da cidade e a chave; nada de dados de alunos.
 */
@Singleton
class ClimaApi @Inject constructor() {

    suspend fun buscar(cidade: String, chave: String): ResultadoClima = withContext(Dispatchers.IO) {
        val consulta = "q=${URLEncoder.encode(cidade, "UTF-8")}&units=metric&lang=pt_br&appid=${URLEncoder.encode(chave, "UTF-8")}"
        val conexao = URL("https://api.openweathermap.org/data/2.5/weather?$consulta").openConnection() as HttpURLConnection
        try {
            conexao.connectTimeout = 5_000
            conexao.readTimeout = 5_000
            conexao.requestMethod = "GET"
            when (val codigo = conexao.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
                    ClimaParser.parse(corpo)?.let { ResultadoClima.Ok(it) }
                        ?: ResultadoClima.Falha("Resposta inesperada do serviço de clima")
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> ResultadoClima.ChaveInvalida
                HttpURLConnection.HTTP_NOT_FOUND -> ResultadoClima.CidadeNaoEncontrada
                else -> ResultadoClima.Falha("Serviço de clima respondeu $codigo")
            }
        } catch (e: IOException) {
            ResultadoClima.Falha("Sem conexão")
        } finally {
            conexao.disconnect()
        }
    }
}
