package br.com.fitpersonal.app.aluno

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.fitpersonal.app.data.files.decodificarOrientada
import br.com.fitpersonal.app.domain.rules.Refeicao
import br.com.fitpersonal.app.domain.rules.TipoRefeicao
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Diário de refeições do aluno, no aparelho: `refeicoes_aluno/refeicoes.json` e as fotos ao lado
 * (JPEG de no máximo 1280 px). Leitura tolerante a arquivo corrompido; nada sai do aparelho
 * sem o aluno compartilhar.
 */
class RefeicoesStore(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val pasta = File(appCtx.filesDir, "refeicoes_aluno").apply { mkdirs() }
    private val arquivo = File(pasta, "refeicoes.json")

    fun listar(): List<Refeicao> {
        if (!arquivo.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(arquivo.readText(Charsets.UTF_8))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                runCatching {
                    Refeicao(
                        id = o.getString("id"),
                        data = LocalDate.parse(o.getString("data")),
                        hora = LocalTime.parse(o.getString("hora")),
                        tipo = TipoRefeicao.de(o.optString("tipo")),
                        nota = o.optString("nota").takeIf { it.isNotBlank() },
                        // só o nome do arquivo: nunca caminho vindo do JSON
                        foto = o.optString("foto").takeIf { it.isNotBlank() }?.let { File(it).name },
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun gravar(lista: List<Refeicao>) {
        val arr = JSONArray()
        lista.forEach { r ->
            arr.put(
                JSONObject().put("id", r.id).put("data", r.data.toString())
                    .put("hora", r.hora.withNano(0).toString()).put("tipo", r.tipo.id)
                    .put("nota", r.nota ?: JSONObject.NULL).put("foto", r.foto ?: JSONObject.NULL)
            )
        }
        arquivo.writeText(arr.toString(), Charsets.UTF_8)
    }

    fun arquivoDaFoto(r: Refeicao): File? = r.foto?.let { File(pasta, File(it).name) }?.takeIf { it.exists() }

    fun uriDaFoto(r: Refeicao): Uri? = arquivoDaFoto(r)?.let {
        FileProvider.getUriForFile(appCtx, "${appCtx.packageName}.fileprovider", it)
    }

    /** Registra a refeição agora; [foto] (câmera ou galeria) é reduzida e copiada para a pasta do diário. Bloqueante. */
    fun adicionar(tipo: TipoRefeicao, nota: String?, foto: Uri?): Refeicao {
        val agora = java.time.LocalDateTime.now()
        val id = UUID.randomUUID().toString()
        val nomeFoto = foto?.let { copiarFoto(it, id) }
        val nova = Refeicao(
            id = id, data = agora.toLocalDate(), hora = agora.toLocalTime().withNano(0).withSecond(0),
            tipo = tipo, nota = nota?.trim()?.take(300)?.ifEmpty { null }, foto = nomeFoto,
        )
        gravar(listar() + nova)
        return nova
    }

    fun excluir(id: String) {
        val lista = listar()
        lista.firstOrNull { it.id == id }?.let { r -> arquivoDaFoto(r)?.delete() }
        gravar(lista.filter { it.id != id })
    }

    private fun copiarFoto(origem: Uri, id: String): String {
        var bmp = decodificarOrientada(appCtx.contentResolver, origem, MAX_LADO)
        val escala = MAX_LADO.toFloat() / maxOf(bmp.width, bmp.height)
        if (escala < 1f) {
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * escala).toInt().coerceAtLeast(1), (bmp.height * escala).toInt().coerceAtLeast(1), true)
        }
        val nome = "$id.jpg"
        File(pasta, nome).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        return nome
    }

    private companion object {
        const val MAX_LADO = 1280
    }
}
