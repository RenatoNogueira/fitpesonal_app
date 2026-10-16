package br.com.fitpersonal.contrato

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Contrato de troca entre o app do profissional, o app do aluno e a página HTML:
 * `.rjetreino` (treino para o aluno) e `.rjeresumo` (retorno do aluno).
 * Biblioteca sem UI; a especificação textual está em docs/contrato/ESPECIFICACAO.md.
 */
object Contrato {
    /** Versão do formato que esta biblioteca entende. */
    const val FORMATO = 1
    const val TIPO_TREINO = "treino"
    const val TIPO_RESUMO = "resumo"
    const val EXTENSAO_TREINO = "fittreino"
    const val EXTENSAO_RESUMO = "rjeresumo"
}

// ───────────────────────── modelos ─────────────────────────

data class ProfissionalRef(
    val nome: String,
    val marca: String = "",
    val cref: String = "",
    val contato: String = "",
)

/** [ref] é um token aleatório por aluno (devolvido no resumo para o profissional reconhecer o aluno). */
data class AlunoRef(val nome: String, val ref: String)

data class ExercicioRef(
    val nome: String,
    val series: String? = null,
    val repeticoes: String? = null,
    val carga: String? = null,
    val descanso: String? = null,
    val obs: String? = null,
    val catalogoId: String? = null,
    /** Link (http/https) de vídeo de demonstração; só é gravado no arquivo quando existe. */
    val video: String? = null,
)

/** [divisoes] usa as chaves "A".."D"; divisões vazias podem ser omitidas. */
data class TreinoRef(
    val nome: String,
    val objetivo: String? = null,
    val divisoes: Map<String, List<ExercicioRef>>,
)

data class PacoteTreino(
    val treinoId: String,
    val revisao: Int,
    val emitidoEm: String,
    val validoAte: String? = null,
    val profissional: ProfissionalRef,
    val aluno: AlunoRef,
    val treino: TreinoRef,
)

data class ItemSessao(
    val exercicio: String,
    val cargaKg: Double? = null,
    val reps: String? = null,
    val feito: Boolean = true,
)

data class SessaoRef(
    val execucaoId: String,
    val data: String,
    val treinoId: String,
    val divisao: String,
    val esforco: Int? = null,
    val itens: List<ItemSessao>,
)

data class Resumo(
    val resumoId: String,
    val alunoRef: String,
    val de: String,
    val ate: String,
    val sessoes: List<SessaoRef>,
    val mensagem: String? = null,
)

// ───────────────────────── resultado de leitura ─────────────────────────

enum class MotivoErro {
    JSON_INVALIDO, TIPO_INESPERADO, VERSAO_FUTURA, INTEGRIDADE, CAMPO_INVALIDO, CODIGO_INVALIDO, TAMANHO,
}

sealed interface Leitura<out T> {
    data class Ok<T>(val valor: T) : Leitura<T>
    data class Erro(val motivo: MotivoErro, val detalhe: String) : Leitura<Nothing>
}

// ───────────────────────── limites ─────────────────────────

internal object Limites {
    const val TEXTO = 200
    const val OBS = 1000
    const val MENSAGEM = 2000
    const val EXERCICIOS_POR_DIVISAO = 300
    const val SESSOES = 400
    const val ITENS_POR_SESSAO = 300
    const val TAMANHO_JSON = 512 * 1024
    val ID = Regex("^[A-Za-z0-9_-]{1,64}$")
    val DATA = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    val DIVISOES = listOf("A", "B", "C", "D")
}

// ───────────────────────── escrita canônica ─────────────────────────

/**
 * Escritor de JSON com **ordem de chaves fixa**: o texto produzido é a base do checksum e deve ser
 * idêntico em qualquer plataforma. Por isso não usamos o JSONObject para escrever.
 */
internal class EscritorJson {
    private val sb = StringBuilder(1024)
    private var primeiro = true
    private val pilha = ArrayDeque<Boolean>()

    private fun virgula() {
        if (!primeiro) sb.append(',')
        primeiro = false
    }

    fun abrirObjeto(chave: String? = null): EscritorJson {
        virgula(); chave?.let { sb.append(texto(it)).append(':') }
        sb.append('{'); pilha.addLast(primeiro); primeiro = true; return this
    }

    fun fecharObjeto(): EscritorJson { sb.append('}'); primeiro = pilha.removeLast().let { false }; return this }

    fun abrirLista(chave: String): EscritorJson {
        virgula(); sb.append(texto(chave)).append(':').append('[')
        pilha.addLast(primeiro); primeiro = true; return this
    }

    fun fecharLista(): EscritorJson { sb.append(']'); primeiro = pilha.removeLast().let { false }; return this }

    /** Item de lista que abre um objeto (sem chave). */
    fun abrirItem(): EscritorJson = abrirObjeto(null)

    fun str(chave: String, valor: String?): EscritorJson {
        virgula(); sb.append(texto(chave)).append(':')
        if (valor == null) sb.append("null") else sb.append(texto(valor))
        return this
    }

    fun inteiro(chave: String, valor: Int?): EscritorJson {
        virgula(); sb.append(texto(chave)).append(':').append(valor?.toString() ?: "null"); return this
    }

    fun decimal(chave: String, valor: Double?): EscritorJson {
        virgula(); sb.append(texto(chave)).append(':')
        sb.append(valor?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "null")
        return this
    }

    fun booleano(chave: String, valor: Boolean): EscritorJson {
        virgula(); sb.append(texto(chave)).append(':').append(if (valor) "true" else "false"); return this
    }

    fun resultado(): String = sb.toString()

    companion object {
        fun texto(s: String): String {
            val out = StringBuilder(s.length + 2).append('"')
            for (c in s) when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u%04x".format(c.code))
                else -> out.append(c)
            }
            return out.append('"').toString()
        }
    }
}

// ───────────────────────── serialização ─────────────────────────

object ContratoJson {

    private fun corpoTreino(p: PacoteTreino): EscritorJson {
        val w = EscritorJson().abrirObjeto()
        w.inteiro("formato", Contrato.FORMATO).str("tipo", Contrato.TIPO_TREINO)
        w.str("treinoId", p.treinoId).inteiro("revisao", p.revisao)
        w.str("emitidoEm", p.emitidoEm).str("validoAte", p.validoAte)
        w.abrirObjeto("profissional")
            .str("nome", p.profissional.nome).str("marca", p.profissional.marca)
            .str("cref", p.profissional.cref).str("contato", p.profissional.contato)
            .fecharObjeto()
        w.abrirObjeto("aluno").str("nome", p.aluno.nome).str("ref", p.aluno.ref).fecharObjeto()
        w.abrirObjeto("treino").str("nome", p.treino.nome).str("objetivo", p.treino.objetivo)
        w.abrirObjeto("divisoes")
        Limites.DIVISOES.forEach { d ->
            val lista = p.treino.divisoes[d].orEmpty()
            if (lista.isEmpty()) return@forEach
            w.abrirLista(d)
            lista.forEach { e ->
                val item = w.abrirItem()
                    .str("nome", e.nome).str("series", e.series).str("repeticoes", e.repeticoes)
                    .str("carga", e.carga).str("descanso", e.descanso).str("obs", e.obs)
                    .str("catalogoId", e.catalogoId)
                // Campo opcional: ausente quando nulo, para não alterar o checksum de arquivos sem vídeo.
                if (e.video != null) item.str("video", e.video)
                item.fecharObjeto()
            }
            w.fecharLista()
        }
        w.fecharObjeto()   // divisoes
        w.fecharObjeto()   // treino
        return w
    }

    /** Texto base do checksum: o documento canônico **sem** o campo `integridade`. */
    fun canonicoTreino(p: PacoteTreino): String = corpoTreino(p).fecharObjeto().resultado()

    /** Documento final: canônico com `integridade` como último campo. */
    fun serializar(p: PacoteTreino): String {
        val base = canonicoTreino(p)
        return base.dropLast(1) + ",\"integridade\":" + EscritorJson.texto("sha256:" + sha256(base)) + "}"
    }

    private fun corpoResumo(r: Resumo): EscritorJson {
        val w = EscritorJson().abrirObjeto()
        w.inteiro("formato", Contrato.FORMATO).str("tipo", Contrato.TIPO_RESUMO)
        w.str("resumoId", r.resumoId).str("alunoRef", r.alunoRef)
        w.abrirObjeto("periodo").str("de", r.de).str("ate", r.ate).fecharObjeto()
        w.abrirLista("sessoes")
        r.sessoes.forEach { s ->
            w.abrirItem()
                .str("execucaoId", s.execucaoId).str("data", s.data).str("treinoId", s.treinoId)
                .str("divisao", s.divisao).inteiro("esforco", s.esforco)
            w.abrirLista("itens")
            s.itens.forEach { i ->
                w.abrirItem()
                    .str("exercicio", i.exercicio).decimal("cargaKg", i.cargaKg)
                    .str("reps", i.reps).booleano("feito", i.feito)
                    .fecharObjeto()
            }
            w.fecharLista()
            w.fecharObjeto()
        }
        w.fecharLista()
        w.str("mensagem", r.mensagem)
        return w
    }

    fun canonicoResumo(r: Resumo): String = corpoResumo(r).fecharObjeto().resultado()

    fun serializar(r: Resumo): String {
        val base = canonicoResumo(r)
        return base.dropLast(1) + ",\"integridade\":" + EscritorJson.texto("sha256:" + sha256(base)) + "}"
    }

    // ── leitura ──

    /** "treino", "resumo" ou null se não for um documento do contrato. */
    fun tipoDe(json: String): String? = try {
        val o = JSONObject(json)
        if (o.has("formato")) o.optString("tipo").takeIf { it.isNotEmpty() } else null
    } catch (e: JSONException) { null }

    fun lerPacote(json: String): Leitura<PacoteTreino> {
        if (json.length > Limites.TAMANHO_JSON) return erro(MotivoErro.TAMANHO, "arquivo grande demais")
        val o = try { JSONObject(json) } catch (e: JSONException) { return erro(MotivoErro.JSON_INVALIDO, e.message.orEmpty()) }
        cabecalho(o, Contrato.TIPO_TREINO)?.let { return it }
        return try {
            val p = PacoteTreino(
                treinoId = id(o, "treinoId"),
                revisao = o.getInt("revisao").also { require(it in 1..1_000_000) { "revisao" } },
                emitidoEm = data(o, "emitidoEm"),
                validoAte = if (o.isNull("validoAte")) null else data(o, "validoAte"),
                profissional = o.getJSONObject("profissional").let {
                    ProfissionalRef(
                        texto(it, "nome", Limites.TEXTO), textoOuVazio(it, "marca"),
                        textoOuVazio(it, "cref"), textoOuVazio(it, "contato"),
                    )
                },
                aluno = o.getJSONObject("aluno").let { AlunoRef(texto(it, "nome", Limites.TEXTO), id(it, "ref")) },
                treino = o.getJSONObject("treino").let { t ->
                    val divs = t.getJSONObject("divisoes")
                    require(divs.keys().asSequence().all { it in Limites.DIVISOES }) { "divisao desconhecida" }
                    TreinoRef(
                        nome = texto(t, "nome", Limites.TEXTO),
                        objetivo = textoOpc(t, "objetivo", Limites.TEXTO),
                        divisoes = Limites.DIVISOES.mapNotNull { d ->
                            if (!divs.has(d)) return@mapNotNull null
                            val arr = divs.getJSONArray(d)
                            require(arr.length() <= Limites.EXERCICIOS_POR_DIVISAO) { "exercicios demais" }
                            d to (0 until arr.length()).map { i -> exercicio(arr.getJSONObject(i)) }
                        }.toMap(),
                    )
                },
            )
            if (o.has("integridade")) {
                val esperado = "sha256:" + sha256(canonicoTreino(p))
                if (o.getString("integridade") != esperado) {
                    return erro(MotivoErro.INTEGRIDADE, "o conteúdo foi alterado ou está corrompido")
                }
            }
            Leitura.Ok(p)
        } catch (e: JSONException) {
            erro(MotivoErro.CAMPO_INVALIDO, e.message.orEmpty())
        } catch (e: IllegalArgumentException) {
            erro(MotivoErro.CAMPO_INVALIDO, e.message.orEmpty())
        }
    }

    fun lerResumo(json: String): Leitura<Resumo> {
        if (json.length > Limites.TAMANHO_JSON) return erro(MotivoErro.TAMANHO, "arquivo grande demais")
        val o = try { JSONObject(json) } catch (e: JSONException) { return erro(MotivoErro.JSON_INVALIDO, e.message.orEmpty()) }
        cabecalho(o, Contrato.TIPO_RESUMO)?.let { return it }
        return try {
            val periodo = o.getJSONObject("periodo")
            val sessoes = o.getJSONArray("sessoes")
            require(sessoes.length() <= Limites.SESSOES) { "sessoes demais" }
            val r = Resumo(
                resumoId = id(o, "resumoId"),
                alunoRef = id(o, "alunoRef"),
                de = data(periodo, "de"),
                ate = data(periodo, "ate"),
                sessoes = (0 until sessoes.length()).map { i -> sessao(sessoes.getJSONObject(i)) },
                mensagem = textoOpc(o, "mensagem", Limites.MENSAGEM),
            )
            if (o.has("integridade")) {
                val esperado = "sha256:" + sha256(canonicoResumo(r))
                if (o.getString("integridade") != esperado) {
                    return erro(MotivoErro.INTEGRIDADE, "o conteúdo foi alterado ou está corrompido")
                }
            }
            Leitura.Ok(r)
        } catch (e: JSONException) {
            erro(MotivoErro.CAMPO_INVALIDO, e.message.orEmpty())
        } catch (e: IllegalArgumentException) {
            erro(MotivoErro.CAMPO_INVALIDO, e.message.orEmpty())
        }
    }

    // ── auxiliares ──

    private fun erro(m: MotivoErro, d: String): Leitura.Erro = Leitura.Erro(m, d)

    private fun cabecalho(o: JSONObject, tipoEsperado: String): Leitura.Erro? {
        val formato = o.optInt("formato", -1)
        if (formato < 1) return erro(MotivoErro.JSON_INVALIDO, "campo formato ausente")
        if (formato > Contrato.FORMATO) {
            return erro(MotivoErro.VERSAO_FUTURA, "formato $formato; atualize o aplicativo")
        }
        if (o.optString("tipo") != tipoEsperado) {
            return erro(MotivoErro.TIPO_INESPERADO, "esperado \"$tipoEsperado\"")
        }
        return null
    }

    private fun texto(o: JSONObject, chave: String, max: Int): String {
        val v = o.getString(chave).trim()
        require(v.isNotEmpty()) { "$chave vazio" }
        require(v.length <= max) { "$chave longo demais" }
        return v
    }

    private fun textoOuVazio(o: JSONObject, chave: String): String = textoOpc(o, chave, Limites.TEXTO).orEmpty()

    private fun textoOpc(o: JSONObject, chave: String, max: Int): String? {
        if (!o.has(chave) || o.isNull(chave)) return null
        val v = o.getString(chave).trim()
        require(v.length <= max) { "$chave longo demais" }
        return v.ifEmpty { null }
    }

    private fun id(o: JSONObject, chave: String): String {
        val v = o.getString(chave)
        require(Limites.ID.matches(v)) { "$chave inválido" }
        return v
    }

    private fun data(o: JSONObject, chave: String): String {
        val v = o.getString(chave)
        require(Limites.DATA.matches(v)) { "$chave inválida" }
        return v
    }

    private fun exercicio(o: JSONObject) = ExercicioRef(
        nome = texto(o, "nome", Limites.TEXTO),
        series = textoOpc(o, "series", 40),
        repeticoes = textoOpc(o, "repeticoes", 40),
        carga = textoOpc(o, "carga", 60),
        descanso = textoOpc(o, "descanso", 40),
        obs = textoOpc(o, "obs", Limites.OBS),
        catalogoId = textoOpc(o, "catalogoId", 80),
        video = textoOpc(o, "video", 500)?.takeIf { v ->
            val m = v.lowercase()
            (m.startsWith("http://") || m.startsWith("https://")) && v.none { it.isWhitespace() }
        },
    )

    private fun sessao(o: JSONObject): SessaoRef {
        val itens = o.getJSONArray("itens")
        require(itens.length() <= Limites.ITENS_POR_SESSAO) { "itens demais" }
        val esforco = if (o.isNull("esforco")) null else o.getInt("esforco").also { require(it in 1..10) { "esforco" } }
        val divisao = o.getString("divisao")
        require(divisao in Limites.DIVISOES) { "divisao inválida" }
        return SessaoRef(
            execucaoId = id(o, "execucaoId"),
            data = data(o, "data"),
            treinoId = id(o, "treinoId"),
            divisao = divisao,
            esforco = esforco,
            itens = (0 until itens.length()).map { i -> item(itens.getJSONObject(i)) },
        )
    }

    private fun item(o: JSONObject): ItemSessao {
        val carga = if (!o.has("cargaKg") || o.isNull("cargaKg")) null else o.getDouble("cargaKg")
        require(carga == null || carga in 0.0..2000.0) { "cargaKg fora da faixa" }
        return ItemSessao(
            exercicio = texto(o, "exercicio", Limites.TEXTO),
            cargaKg = carga,
            reps = textoOpc(o, "reps", 40),
            feito = o.optBoolean("feito", true),
        )
    }

    internal fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

// ───────────────────────── código de texto ─────────────────────────

/**
 * Código colável: `RJE1:` + base64url(DEFLATE bruto do JSON UTF-8). `RJE0:` = sem compressão
 * (navegadores sem CompressionStream). Aceita o código dentro de uma mensagem maior.
 */
object CodigoTexto {
    private const val PREFIXO_COMPRIMIDO = "RJE1:"
    private const val PREFIXO_SIMPLES = "RJE0:"
    private val PADRAO = Regex("RJE[01]:[A-Za-z0-9_-]+")

    fun codificar(json: String): String {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)   // raw (nowrap): igual a "deflate-raw"
        deflater.setInput(bytes)
        deflater.finish()
        val saida = java.io.ByteArrayOutputStream(bytes.size / 2 + 16)
        val buf = ByteArray(4096)
        while (!deflater.finished()) saida.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return PREFIXO_COMPRIMIDO + Base64.getUrlEncoder().withoutPadding().encodeToString(saida.toByteArray())
    }

    fun decodificar(texto: String): Leitura<String> {
        val achado = PADRAO.find(texto.trim())?.value
            ?: return Leitura.Erro(MotivoErro.CODIGO_INVALIDO, "nenhum código RJE encontrado")
        val corpo = achado.substring(5)
        val bruto = try {
            Base64.getUrlDecoder().decode(corpo)
        } catch (e: IllegalArgumentException) {
            return Leitura.Erro(MotivoErro.CODIGO_INVALIDO, "código corrompido")
        }
        if (achado.startsWith(PREFIXO_SIMPLES)) {
            if (bruto.size > Limites.TAMANHO_JSON) return Leitura.Erro(MotivoErro.TAMANHO, "código grande demais")
            return Leitura.Ok(String(bruto, Charsets.UTF_8))
        }
        val inflater = Inflater(true)
        return try {
            inflater.setInput(bruto)
            val saida = java.io.ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var total = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                total += n
                // Proteção contra "bomba" de descompressão
                if (total > Limites.TAMANHO_JSON) return Leitura.Erro(MotivoErro.TAMANHO, "conteúdo grande demais")
                saida.write(buf, 0, n)
            }
            if (!inflater.finished()) Leitura.Erro(MotivoErro.CODIGO_INVALIDO, "código incompleto")
            else Leitura.Ok(String(saida.toByteArray(), Charsets.UTF_8))
        } catch (e: java.util.zip.DataFormatException) {
            Leitura.Erro(MotivoErro.CODIGO_INVALIDO, "código corrompido")
        } finally {
            inflater.end()
        }
    }
}

/** Leitura do campo livre "descanso" (ex.: "60 s", "1 min", "1:30", "90") para cronômetros. */
object DescansoRules {
    private val MIN_SEG = Regex("""^(\d{1,2}):(\d{2})$""")
    private val NUMERO_UNIDADE =
        Regex("""^(\d{1,4})(?:[.,]\d+)?\s*(s|seg|segundos?|m|min|minutos?)?$""", RegexOption.IGNORE_CASE)

    /** Segundos de descanso, ou null se o texto não for claro (1..3600). */
    fun segundos(texto: String?): Int? {
        val t = texto?.trim()?.lowercase().orEmpty()
        if (t.isEmpty()) return null
        MIN_SEG.matchEntire(t)?.let {
            val total = it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
            return total.takeIf { s -> s in 1..3600 }
        }
        val m = NUMERO_UNIDADE.matchEntire(t) ?: return null
        val n = m.groupValues[1].toInt()
        val unidade = m.groupValues[2]
        val segundos = if (unidade.startsWith("m")) n * 60 else n
        return segundos.takeIf { it in 1..3600 }
    }
}
