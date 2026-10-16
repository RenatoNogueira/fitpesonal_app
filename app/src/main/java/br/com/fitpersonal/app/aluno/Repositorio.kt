package br.com.fitpersonal.app.aluno

import android.content.Context
import br.com.fitpersonal.contrato.CodigoTexto
import br.com.fitpersonal.contrato.ContratoJson
import br.com.fitpersonal.contrato.ItemSessao
import br.com.fitpersonal.contrato.Leitura
import br.com.fitpersonal.contrato.MotivoErro
import br.com.fitpersonal.contrato.PacoteTreino
import br.com.fitpersonal.contrato.Resumo
import br.com.fitpersonal.contrato.SessaoRef
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Resultado de receber um treino (arquivo ou código). */
sealed interface Recebido {
    data class Novo(val pacote: PacoteTreino) : Recebido
    data class Atualizado(val pacote: PacoteTreino) : Recebido
    data class JaTemIgualOuMaisNovo(val pacote: PacoteTreino) : Recebido
    data class Erro(val mensagem: String) : Recebido
}

/**
 * Armazenamento local simples (arquivos JSON no diretório privado do app):
 *   pacotes/{treinoId}.json  treino recebido (forma canônica do contrato)
 *   sessoes.json             sessões executadas pelo aluno
 */
class Repositorio(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val pasta = File(ctx.filesDir, "pacotes_aluno").apply { mkdirs() }
    private val pastaVideos = File(ctx.filesDir, "videos_aluno").apply { mkdirs() }
    private val arquivoSessoes = File(ctx.filesDir, "sessoes_aluno.json")

    // ── treinos ──

    fun treinos(): List<PacoteTreino> = pasta.listFiles().orEmpty()
        .mapNotNull { f -> (ContratoJson.lerPacote(f.readText(Charsets.UTF_8)) as? Leitura.Ok)?.valor }
        .sortedByDescending { it.emitidoEm }

    fun treino(id: String): PacoteTreino? = treinos().firstOrNull { it.treinoId == id }

    fun excluirTreino(id: String) {
        File(pasta, "$id.json").delete()
        File(pastaVideos, pastaSegura(id)).deleteRecursively()
    }

    // ── vídeos que vieram no pacote do profissional ──

    private fun pastaSegura(treinoId: String) = treinoId.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "treino" }

    /** Vídeo do exercício [exercicio] (nome como está no treino), se veio no pacote; senão null. */
    fun videoDe(treinoId: String, exercicio: String): File? {
        val dir = File(pastaVideos, pastaSegura(treinoId))
        val indice = File(dir, "indice.json")
        if (!indice.exists()) return null
        return runCatching {
            val nome = JSONObject(indice.readText(Charsets.UTF_8)).optString(chaveNome(exercicio), "")
            if (nome.isEmpty()) null else File(dir, nome).takeIf { it.exists() }
        }.getOrNull()
    }

    private fun chaveNome(s: String) = s.trim().lowercase(java.util.Locale.ROOT)

    /** Recebe um `.fittreinopack` (treino + vídeos). Os vídeos só são guardados se o treino for novo ou mais recente. */
    fun receberPacoteComVideos(entrada: java.io.InputStream): Recebido {
        val tmp = File(appCtx.cacheDir, "pacote_" + System.nanoTime()).apply { mkdirs() }
        try {
            val extraido = when (val r = br.com.fitpersonal.contrato.PacoteVideos.ler(entrada, tmp)) {
                is Leitura.Ok -> r.valor
                is Leitura.Erro -> return Recebido.Erro(
                    if (r.motivo == MotivoErro.TAMANHO) "O pacote é grande demais." else "Pacote inválido: ${r.detalhe}.",
                )
            }
            val resultado = receber(extraido.treinoJson)
            val pacote = when (resultado) {
                is Recebido.Novo -> resultado.pacote
                is Recebido.Atualizado -> resultado.pacote
                else -> return resultado   // já tinha esta versão (ou mais nova): não troca os vídeos
            }
            val dir = File(pastaVideos, pastaSegura(pacote.treinoId))
            dir.deleteRecursively()
            dir.mkdirs()
            val indice = JSONObject()
            extraido.videos.forEachIndexed { i, v ->
                val destino = File(dir, "v${i + 1}.mp4")
                if (v.arquivo.renameTo(destino) || runCatching { v.arquivo.copyTo(destino, overwrite = true) }.isSuccess) {
                    v.exercicios.forEach { indice.put(chaveNome(it), destino.name) }
                }
            }
            File(dir, "indice.json").writeText(indice.toString(), Charsets.UTF_8)
            return resultado
        } catch (e: Exception) {
            return Recebido.Erro("Não foi possível abrir o pacote: ${e.message}")
        } finally {
            tmp.deleteRecursively()
        }
    }

    /** Aceita o JSON do `.fittreino` ou o código de texto (inclusive dentro de uma mensagem). */
    fun receber(texto: String): Recebido {
        val t = texto.trim()
        val json = if (t.startsWith("{")) t else when (val c = CodigoTexto.decodificar(t)) {
            is Leitura.Ok -> c.valor
            is Leitura.Erro -> return Recebido.Erro("Não encontrei um treino neste texto.")
        }
        if (ContratoJson.tipoDe(json) == "resumo") {
            return Recebido.Erro("Isto é um resumo de treino, não um treino novo.")
        }
        val pacote = when (val r = ContratoJson.lerPacote(json)) {
            is Leitura.Ok -> r.valor
            is Leitura.Erro -> return Recebido.Erro(
                when (r.motivo) {
                    MotivoErro.VERSAO_FUTURA -> "Este treino foi criado por uma versão mais nova. Atualize o app."
                    MotivoErro.INTEGRIDADE -> "O arquivo parece alterado ou corrompido."
                    else -> "Treino inválido: ${r.detalhe}."
                }
            )
        }
        val atual = treino(pacote.treinoId)
        return when {
            atual == null -> { salvar(pacote); Recebido.Novo(pacote) }
            pacote.revisao > atual.revisao -> { salvar(pacote); Recebido.Atualizado(pacote) }
            else -> Recebido.JaTemIgualOuMaisNovo(atual)
        }
    }

    private fun salvar(p: PacoteTreino) {
        File(pasta, "${p.treinoId}.json").writeText(ContratoJson.serializar(p), Charsets.UTF_8)
    }

    // ── sessões ──

    fun sessoes(): List<SessaoRef> {
        if (!arquivoSessoes.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(arquivoSessoes.readText(Charsets.UTF_8))
            (0 until arr.length()).map { lerSessao(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun sessoesDe(treinoId: String): List<SessaoRef> =
        sessoes().filter { it.treinoId == treinoId }.sortedByDescending { it.data }

    /** O que o aluno fez neste exercício nas sessões passadas do treino, da mais recente para a mais antiga. */
    fun historicoDoExercicio(treinoId: String, exercicio: String): List<br.com.fitpersonal.app.domain.rules.RegistroExercicio> {
        val chave = br.com.fitpersonal.app.domain.rules.CargaRules.chaveExercicio(exercicio)
        return sessoesDe(treinoId).mapNotNull { sessao ->
            sessao.itens
                .firstOrNull { br.com.fitpersonal.app.domain.rules.CargaRules.chaveExercicio(it.exercicio) == chave }
                ?.let { br.com.fitpersonal.app.domain.rules.RegistroExercicio(it.cargaKg, it.feito, sessao.esforco) }
        }
    }

    fun registrar(s: SessaoRef) {
        val arr = JSONArray()
        (sessoes() + s).forEach { arr.put(escreverSessao(it)) }
        arquivoSessoes.writeText(arr.toString(), Charsets.UTF_8)
    }

    /** Resumo de todas as sessões do treino, pronto para ir ao profissional. Null se não há sessões. */
    fun resumoDe(treinoId: String): Resumo? {
        val pacote = treino(treinoId) ?: return null
        val lista = sessoesDe(treinoId)
        if (lista.isEmpty()) return null
        val datas = lista.map { it.data }.sorted()
        return Resumo(
            resumoId = UUID.randomUUID().toString(),
            alunoRef = pacote.aluno.ref,
            de = datas.first(),
            ate = datas.last(),
            sessoes = lista,
            mensagem = null,
        )
    }

    private fun escreverSessao(s: SessaoRef): JSONObject = JSONObject().apply {
        put("execucaoId", s.execucaoId)
        put("data", s.data)
        put("treinoId", s.treinoId)
        put("divisao", s.divisao)
        put("esforco", s.esforco ?: JSONObject.NULL)
        put(
            "itens",
            JSONArray().also { itens ->
                s.itens.forEach { i ->
                    itens.put(
                        JSONObject().apply {
                            put("exercicio", i.exercicio)
                            put("cargaKg", i.cargaKg ?: JSONObject.NULL)
                            put("reps", i.reps ?: JSONObject.NULL)
                            put("feito", i.feito)
                        }
                    )
                }
            },
        )
    }

    private fun lerSessao(o: JSONObject): SessaoRef {
        val itens = o.getJSONArray("itens")
        return SessaoRef(
            execucaoId = o.getString("execucaoId"),
            data = o.getString("data"),
            treinoId = o.getString("treinoId"),
            divisao = o.getString("divisao"),
            esforco = if (o.isNull("esforco")) null else o.getInt("esforco"),
            itens = (0 until itens.length()).map { k ->
                val i = itens.getJSONObject(k)
                ItemSessao(
                    exercicio = i.getString("exercicio"),
                    cargaKg = if (i.isNull("cargaKg")) null else i.getDouble("cargaKg"),
                    reps = if (i.isNull("reps")) null else i.getString("reps"),
                    feito = i.optBoolean("feito", true),
                )
            },
        )
    }
}
