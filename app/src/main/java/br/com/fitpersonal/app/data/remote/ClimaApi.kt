package br.com.fitpersonal.app.data.remote

import br.com.fitpersonal.app.domain.model.Clima
import br.com.fitpersonal.app.domain.rules.CandidatoCidade
import br.com.fitpersonal.app.domain.rules.CidadeRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

/** Converte a resposta JSON da geocodificação (/geo/1.0/direct): lista de cidades possíveis. */
object GeocodificacaoParser {
    fun parse(json: String): List<CandidatoCidade> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (!o.has("lat") || !o.has("lon")) return@mapNotNull null
            CandidatoCidade(
                nome = o.optString("name"),
                estado = o.optString("state").takeIf { it.isNotBlank() },
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
            )
        }
    } catch (e: JSONException) {
        emptyList()
    }
}

/**
 * Cliente mínimo do OpenWeather (sem dependências além do que o Android já traz).
 * Envia apenas o nome da cidade (e o estado, se informado) e a chave; nada de dados de alunos.
 *
 * A cidade é localizada primeiro pela geocodificação (que devolve o estado de cada resultado), para escolher
 * a do estado certo — buscar só pelo nome devolvia a cidade errada quando há várias com o mesmo nome.
 * Se a geocodificação não achar nada, cai na busca antiga por nome.
 */
@Singleton
class ClimaApi @Inject constructor() {

    private class Resposta(val codigo: Int, val corpo: String?)

    private fun get(url: String): Resposta {
        val conexao = URL(url).openConnection() as HttpURLConnection
        return try {
            conexao.connectTimeout = 5_000
            conexao.readTimeout = 5_000
            conexao.requestMethod = "GET"
            val codigo = conexao.responseCode
            val corpo = if (codigo == HttpURLConnection.HTTP_OK) {
                conexao.inputStream.bufferedReader().use { it.readText() }
            } else null
            Resposta(codigo, corpo)
        } finally {
            conexao.disconnect()
        }
    }

    private fun codificar(t: String) = URLEncoder.encode(t, "UTF-8")

    suspend fun buscar(cidade: String, chave: String): ResultadoClima = withContext(Dispatchers.IO) {
        try {
            val consulta = CidadeRules.interpretar(cidade)
            if (consulta.cidade.isBlank()) return@withContext ResultadoClima.SemCidade
            val key = codificar(chave)

            // 1) geocodificação: cidade (no Brasil) → coordenadas + estado
            val geo = get("https://api.openweathermap.org/geo/1.0/direct?q=" + codificar(consulta.cidade + ",BR") + "&limit=5&appid=" + key)
            if (geo.codigo == HttpURLConnection.HTTP_UNAUTHORIZED) return@withContext ResultadoClima.ChaveInvalida
            val escolhida = geo.corpo
                ?.let { GeocodificacaoParser.parse(it) }
                ?.let { CidadeRules.escolher(it, consulta.uf) }

            val resultado = if (escolhida != null) {
                val r = get(
                    "https://api.openweathermap.org/data/2.5/weather?lat=" + escolhida.lat + "&lon=" + escolhida.lon +
                        "&units=metric&lang=pt_br&appid=" + key,
                )
                // O nome da resposta do clima é o da estação mais próxima; o rótulo vem da cidade escolhida.
                when (val c = respostaDoClima(r)) {
                    is ResultadoClima.Ok -> ResultadoClima.Ok(c.clima.copy(cidade = CidadeRules.rotulo(escolhida)))
                    else -> c
                }
            } else {
                // 2) plano B: busca direta pelo nome, como antes
                respostaDoClima(
                    get(
                        "https://api.openweathermap.org/data/2.5/weather?q=" + codificar(cidade.trim()) +
                            "&units=metric&lang=pt_br&appid=" + key,
                    ),
                )
            }
            resultado ?: ResultadoClima.Falha("Resposta inesperada do serviço de clima")
        } catch (e: IOException) {
            ResultadoClima.Falha("Sem conexão")
        }
    }

    private fun respostaDoClima(r: Resposta): ResultadoClima? = when (r.codigo) {
        HttpURLConnection.HTTP_OK -> r.corpo?.let { ClimaParser.parse(it) }?.let { ResultadoClima.Ok(it) }
        HttpURLConnection.HTTP_UNAUTHORIZED -> ResultadoClima.ChaveInvalida
        HttpURLConnection.HTTP_NOT_FOUND -> ResultadoClima.CidadeNaoEncontrada
        else -> ResultadoClima.Falha("Serviço de clima respondeu " + r.codigo)
    }
}
