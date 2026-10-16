package br.com.fitpersonal.app.data.repo

import br.com.fitpersonal.app.BuildConfig
import br.com.fitpersonal.app.data.remote.ClimaApi
import br.com.fitpersonal.app.data.remote.ResultadoClima
import br.com.fitpersonal.app.domain.model.Clima
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Clima da cidade do perfil, com cache em memória (10 min) para não consultar a API a cada abertura da tela. */
@Singleton
class ClimaRepository @Inject constructor(private val api: ClimaApi) {

    private companion object {
        const val VALIDADE_MS = 10 * 60 * 1000L
    }

    private val trava = Mutex()
    private val cache = mutableMapOf<String, Pair<Long, Clima>>()

    /** O recurso só existe se o app foi compilado com OPENWEATHER_API_KEY. */
    val disponivel: Boolean get() = BuildConfig.OPENWEATHER_API_KEY.isNotBlank()

    suspend fun atual(cidade: String, forcar: Boolean = false): ResultadoClima {
        if (!disponivel) return ResultadoClima.SemChave
        val nome = cidade.trim()
        if (nome.isEmpty()) return ResultadoClima.SemCidade

        val chaveCache = nome.lowercase()
        return trava.withLock {
            val agora = System.currentTimeMillis()
            val guardado = cache[chaveCache]
            if (!forcar && guardado != null && agora - guardado.first < VALIDADE_MS) {
                return@withLock ResultadoClima.Ok(guardado.second)
            }
            val r = api.buscar(nome, BuildConfig.OPENWEATHER_API_KEY)
            if (r is ResultadoClima.Ok) cache[chaveCache] = agora to r.clima
            // Sem rede: mostra o último valor conhecido em vez de uma tela de erro.
            if (r is ResultadoClima.Falha && guardado != null) ResultadoClima.Ok(guardado.second) else r
        }
    }
}
