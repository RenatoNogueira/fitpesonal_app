package br.com.fitpersonal.app.domain.rules

import java.text.Normalizer

/** O que acompanha a cobrança: a chave e, quando possível, o código "copia e cola" com o valor. */
data class PixCobranca(val chave: String, val copiaECola: String?)

/**
 * Pix estático (BR Code, padrão EMV do Banco Central): gera o texto "copia e cola" com chave, valor e
 * identificação do recebedor. O banco do aluno lê o código e já preenche chave e valor.
 */
object PixRules {
    private const val GUI = "br.gov.bcb.pix"
    private const val MAX_CHAVE = 77

    /** Limpa a chave: e-mail em minúsculas, espaços nas pontas removidos; telefone com "+" perde formatação. */
    fun normalizarChave(texto: String): String {
        val t = texto.trim()
        return when {
            t.isEmpty() -> ""
            '@' in t -> t.lowercase()
            t.startsWith("+") -> "+" + t.filter { it.isDigit() }
            else -> t
        }
    }

    fun chaveValida(texto: String): Boolean {
        val c = normalizarChave(texto)
        return c.isNotEmpty() && c.length <= MAX_CHAVE && c.none { it.isWhitespace() || it.isISOControl() }
    }

    /**
     * Código Pix "copia e cola" com [valorCentavos] (omitido se ≤ 0). Nome e cidade seguem as regras do
     * BR Code: sem acento, maiúsculas, até 25 e 15 caracteres. Devolve null se a chave for inválida.
     */
    fun copiaECola(
        chave: String,
        valorCentavos: Long,
        nomeRecebedor: String,
        cidade: String,
        identificador: String = "***",
    ): String? {
        val k = normalizarChave(chave)
        if (!chaveValida(k)) return null
        val nome = ascii(nomeRecebedor, 25).ifEmpty { "RECEBEDOR" }
        val cid = ascii(cidade, 15).ifEmpty { "BRASIL" }
        val txid = identificador.filter { it.isLetterOrDigit() }.take(25).ifEmpty { "***" }
            .let { if (identificador == "***") "***" else it }

        val conta = campo("00", GUI) + campo("01", k)
        val corpo = buildString {
            append(campo("00", "01"))
            append(campo("01", "11"))                 // QR estático (reutilizável)
            append(campo("26", conta))
            append(campo("52", "0000"))
            append(campo("53", "986"))                // BRL
            if (valorCentavos > 0) append(campo("54", "%d.%02d".format(valorCentavos / 100, valorCentavos % 100)))
            append(campo("58", "BR"))
            append(campo("59", nome))
            append(campo("60", cid))
            append(campo("62", campo("05", txid)))
            append("6304")
        }
        return corpo + "%04X".format(crc16(corpo))
    }

    /** Monta o que vai na cobrança a partir do perfil; null quando o profissional não cadastrou chave. */
    fun cobranca(chave: String, valorCentavos: Long, nomeRecebedor: String, cidade: String): PixCobranca? {
        val k = normalizarChave(chave)
        if (!chaveValida(k)) return null
        return PixCobranca(k, copiaECola(k, valorCentavos, nomeRecebedor, cidade))
    }

    private fun campo(id: String, valor: String) = id + "%02d".format(valor.length) + valor

    private fun ascii(texto: String, max: Int): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(max)
            .trim()

    /** CRC16/CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF) sobre o texto ASCII do código. */
    internal fun crc16(texto: String): Int {
        var crc = 0xFFFF
        for (b in texto.toByteArray(Charsets.US_ASCII)) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc
    }
}
