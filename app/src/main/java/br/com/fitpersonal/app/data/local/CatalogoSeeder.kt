package br.com.fitpersonal.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Importa assets/exercises-ptbr-full-translation.json (copiar do desktop para app/src/main/assets/)
 * na primeira execução. Idempotente: não faz nada se o catálogo já tem dados.
 */
class CatalogoSeeder(private val context: Context, private val dao: CatalogoDao) {

    suspend fun seedSeNecessario() {
        if (dao.total() > 0) return
        val texto = runCatching {
            context.assets.open(ARQUIVO).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return
        val arr = JSONArray(texto)
        val itens = (0 until arr.length()).map { arr.getJSONObject(it).toEntity() }
        itens.chunked(200).forEach { dao.inserirTodos(it) }
    }

    private fun JSONObject.lista(chave: String, sep: String): String {
        val a = optJSONArray(chave) ?: return ""
        return (0 until a.length()).joinToString(sep) { a.getString(it) }
    }

    private fun JSONObject.toEntity() = ExercicioCatalogoEntity(
        id = optString("id"),
        nome = optString("name"),
        categoria = optString("category"),
        nivel = optString("level"),
        equipamento = optString("equipment"),
        musculosPrimarios = lista("primaryMuscles", ";"),
        musculosSecundarios = lista("secondaryMuscles", ";"),
        instrucoes = lista("instructions", "\n"),
        imagens = lista("images", ";"),
    )

    companion object { const val ARQUIVO = "exercises-ptbr-full-translation.json" }
}
