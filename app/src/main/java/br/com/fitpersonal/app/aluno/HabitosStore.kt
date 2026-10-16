package br.com.fitpersonal.app.aluno

import android.content.Context
import br.com.fitpersonal.app.domain.rules.CheckIn
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Hábitos marcados por dia e check-ins semanais do aluno, em `habitos_aluno.json` (diretório privado).
 * Leitura tolerante: arquivo ausente, corrompido ou com campos inválidos devolve o que for possível.
 */
class HabitosStore(ctx: Context) {
    private val arquivo = File(ctx.filesDir, "habitos_aluno.json")

    data class Dados(val dias: Map<LocalDate, Set<String>>, val checkins: List<CheckIn>)

    fun ler(): Dados {
        if (!arquivo.exists()) return Dados(emptyMap(), emptyList())
        return runCatching {
            val raiz = JSONObject(arquivo.readText(Charsets.UTF_8))
            val dias = buildMap<LocalDate, Set<String>> {
                val o = raiz.optJSONObject("dias") ?: JSONObject()
                o.keys().forEach { k ->
                    val dia = runCatching { LocalDate.parse(k) }.getOrNull() ?: return@forEach
                    val arr = o.optJSONArray(k) ?: return@forEach
                    put(dia, (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }.toSet())
                }
            }
            val checkins = buildList {
                val arr = raiz.optJSONArray("checkins") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    val data = runCatching { LocalDate.parse(c.getString("data")) }.getOrNull() ?: continue
                    add(
                        CheckIn(
                            data = data,
                            energia = c.optInt("energia", 3).coerceIn(1, 5),
                            sono = c.optInt("sono", 3).coerceIn(1, 5),
                            dor = c.optInt("dor", 1).coerceIn(1, 5),
                            obs = c.optString("obs").takeIf { it.isNotBlank() },
                        )
                    )
                }
            }
            Dados(dias, checkins)
        }.getOrDefault(Dados(emptyMap(), emptyList()))
    }

    private fun gravar(d: Dados) {
        val dias = JSONObject()
        d.dias.filterValues { it.isNotEmpty() }.forEach { (dia, ids) -> dias.put(dia.toString(), JSONArray(ids.toList())) }
        val checkins = JSONArray()
        d.checkins.forEach { c ->
            checkins.put(
                JSONObject().put("data", c.data.toString()).put("energia", c.energia).put("sono", c.sono)
                    .put("dor", c.dor).put("obs", c.obs ?: JSONObject.NULL)
            )
        }
        arquivo.writeText(JSONObject().put("dias", dias).put("checkins", checkins).toString(), Charsets.UTF_8)
    }

    fun alternar(dia: LocalDate, habitoId: String) {
        val d = ler()
        val atual = d.dias[dia].orEmpty()
        val novo = if (habitoId in atual) atual - habitoId else atual + habitoId
        gravar(d.copy(dias = d.dias + (dia to novo)))
    }

    /** Guarda o check-in; um segundo no mesmo dia substitui o primeiro. */
    fun registrarCheckIn(c: CheckIn) {
        val d = ler()
        gravar(d.copy(checkins = d.checkins.filter { it.data != c.data } + c))
    }
}
