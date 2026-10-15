package br.com.fitpersonal.contrato

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Pacote "treino + vídeos" (`.rjetreinopack`): um zip com
 *   treino.rjetreino   o mesmo JSON do contrato (com checksum)
 *   videos.json        {"formato":1,"videos":[{"arquivo":"v1.mp4","exercicios":["Supino reto"]}]}
 *   videos/v1.mp4 ...  vídeos próprios dos exercícios
 *
 * O treino em si não muda: o app que não entende o pacote ainda lê `treino.rjetreino`. Os vídeos são
 * ligados aos exercícios pelo **nome** do exercício, como aparece no treino.
 */
object PacoteVideos {
    const val EXTENSAO = ".rjetreinopack"
    const val ENTRADA_TREINO = "treino.rjetreino"
    const val ENTRADA_INDICE = "videos.json"
    const val MAX_TOTAL_BYTES = 600L * 1024 * 1024
    const val MAX_VIDEOS = 40
    private const val MAX_INDICE = 64 * 1024

    /** Só estes nomes são extraídos: nada de caminhos arbitrários vindos do zip. */
    private val NOME_VIDEO = Regex("""videos/v\d{1,3}\.mp4""")

    data class VideoParaPacote(val exercicios: List<String>, val arquivo: File)
    data class VideoExtraido(val exercicios: List<String>, val arquivo: File)
    data class Extraido(val treinoJson: String, val videos: List<VideoExtraido>)

    fun escrever(destino: File, treinoJson: String, videos: List<VideoParaPacote>) {
        require(videos.size <= MAX_VIDEOS) { "vídeos demais no pacote" }
        val indice = JSONArray()
        videos.forEachIndexed { i, v ->
            indice.put(
                JSONObject().put("arquivo", "v${i + 1}.mp4").put("exercicios", JSONArray(v.exercicios)),
            )
        }
        val indiceJson = JSONObject().put("formato", 1).put("videos", indice).toString()

        ZipOutputStream(destino.outputStream().buffered()).use { z ->
            z.putNextEntry(ZipEntry(ENTRADA_TREINO))
            z.write(treinoJson.toByteArray(Charsets.UTF_8))
            z.closeEntry()

            z.putNextEntry(ZipEntry(ENTRADA_INDICE))
            z.write(indiceJson.toByteArray(Charsets.UTF_8))
            z.closeEntry()

            videos.forEachIndexed { i, v ->
                z.putNextEntry(ZipEntry("videos/v${i + 1}.mp4"))
                v.arquivo.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }

    /** Extrai os vídeos para [pasta] (nomes v1.mp4, v2.mp4…) e devolve o treino e o mapa vídeo → exercícios. */
    fun ler(entrada: InputStream, pasta: File): Leitura<Extraido> {
        pasta.mkdirs()
        var treino: String? = null
        var indice: String? = null
        var total = 0L
        val extraidos = mutableMapOf<String, File>()

        try {
            ZipInputStream(entrada).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val nome = e.name
                    when {
                        e.isDirectory -> Unit
                        nome == ENTRADA_TREINO -> treino = lerTexto(z, Limites.TAMANHO_JSON)
                        nome == ENTRADA_INDICE -> indice = lerTexto(z, MAX_INDICE)
                        NOME_VIDEO.matches(nome) && extraidos.size < MAX_VIDEOS -> {
                            val destino = File(pasta, nome.substringAfter('/'))
                            destino.outputStream().use { sai ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val n = z.read(buf)
                                    if (n < 0) break
                                    total += n
                                    if (total > MAX_TOTAL_BYTES) {
                                        return Leitura.Erro(MotivoErro.TAMANHO, "pacote grande demais")
                                    }
                                    sai.write(buf, 0, n)
                                }
                            }
                            extraidos[destino.name] = destino
                        }
                    }
                    z.closeEntry()
                }
            }
        } catch (ex: Exception) {
            return Leitura.Erro(MotivoErro.JSON_INVALIDO, ex.message.orEmpty())
        }

        val json = treino ?: return Leitura.Erro(MotivoErro.JSON_INVALIDO, "o pacote não contém o treino")
        val videos = mutableListOf<VideoExtraido>()
        indice?.let { txt ->
            runCatching {
                val arr = JSONObject(txt).getJSONArray("videos")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val arquivo = extraidos[o.getString("arquivo")] ?: continue
                    val ex = o.getJSONArray("exercicios")
                    val nomes = (0 until ex.length()).map { ex.getString(it) }.filter { it.isNotBlank() }
                    if (nomes.isNotEmpty()) videos += VideoExtraido(nomes, arquivo)
                }
            }
        }
        return Leitura.Ok(Extraido(json, videos))
    }

    private fun lerTexto(z: InputStream, max: Int): String {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = z.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            if (out.size() > max) throw IllegalStateException("entrada grande demais")
        }
        return out.toByteArray().toString(Charsets.UTF_8)
    }
}
