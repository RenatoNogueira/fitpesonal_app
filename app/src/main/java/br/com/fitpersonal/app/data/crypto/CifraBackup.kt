package br.com.fitpersonal.app.data.crypto

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SenhaIncorretaException : IOException("Senha incorreta ou arquivo de backup corrompido.")
class BackupCorrompidoException(msg: String) : IOException(msg)
class FormatoDesconhecidoException : IOException("O arquivo não é um backup criptografado do FitPersonal.")

/**
 * Cifra de arquivos de backup, em fluxo (não carrega o arquivo inteiro na memória).
 *
 * Formato (big-endian):
 *   cabeçalho:  "RJEB" | versão(1) | iterações PBKDF2(4) | salt(16) | prefixo do nonce(8)   = 33 bytes
 *   blocos:     [tamanho do bloco cifrado (4)] [AES-256-GCM(texto de até 64 KiB)]
 *   nonce       = prefixo(8) || índice do bloco(4)
 *   AAD         = cabeçalho(33) || índice(4) || bloco final(1)
 *
 * O índice e o marcador de "último bloco" vão no AAD: reordenar, remover ou truncar blocos
 * invalida a autenticação. Chave: PBKDF2WithHmacSHA256 (210 mil iterações por padrão).
 * Java puro (sem Android), para ser testado na JVM.
 */
object CifraBackup {
    const val ITERACOES_PADRAO = 210_000

    private val MAGIA = byteArrayOf('R'.code.toByte(), 'J'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte())
    private const val VERSAO: Byte = 1
    private const val TAM_SALT = 16
    private const val TAM_PREFIXO = 8
    private const val TAM_CABECALHO = 4 + 1 + 4 + TAM_SALT + TAM_PREFIXO
    private const val TAM_BLOCO = 64 * 1024
    private const val TAM_TAG = 16
    private const val MAX_ITERACOES = 5_000_000   // evita DoS por cabeçalho adulterado

    /** Olha os primeiros bytes (≥ 4) para saber se o arquivo é um backup cifrado. */
    fun ehCifrado(inicio: ByteArray): Boolean =
        inicio.size >= MAGIA.size && MAGIA.indices.all { inicio[it] == MAGIA[it] }

    private fun derivarChave(senha: CharArray, salt: ByteArray, iteracoes: Int): SecretKeySpec {
        val spec = PBEKeySpec(senha, salt, iteracoes, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun nonce(prefixo: ByteArray, indice: Int): ByteArray =
        ByteBuffer.allocate(12).put(prefixo).putInt(indice).array()

    private fun aad(cabecalho: ByteArray, indice: Int, ultimo: Boolean): ByteArray =
        ByteBuffer.allocate(TAM_CABECALHO + 5).put(cabecalho).putInt(indice).put(if (ultimo) 1.toByte() else 0.toByte()).array()

    private fun cifra(modo: Int, chave: SecretKeySpec, nonce: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(modo, chave, GCMParameterSpec(128, nonce))
            updateAAD(aad)
        }

    // ───────────── escrita ─────────────

    fun cifrando(
        saida: OutputStream,
        senha: CharArray,
        iteracoes: Int = ITERACOES_PADRAO,
        random: SecureRandom = SecureRandom(),
    ): OutputStream {
        val salt = ByteArray(TAM_SALT).also { random.nextBytes(it) }
        val prefixo = ByteArray(TAM_PREFIXO).also { random.nextBytes(it) }
        val cabecalho = ByteBuffer.allocate(TAM_CABECALHO)
            .put(MAGIA).put(VERSAO).putInt(iteracoes).put(salt).put(prefixo).array()
        saida.write(cabecalho)
        return FluxoCifrado(saida, derivarChave(senha, salt, iteracoes), cabecalho, prefixo)
    }

    private class FluxoCifrado(
        private val destino: OutputStream,
        private val chave: SecretKeySpec,
        private val cabecalho: ByteArray,
        private val prefixo: ByteArray,
    ) : OutputStream() {
        private val buf = ByteArray(TAM_BLOCO)
        private var n = 0
        private var indice = 0
        private var fechado = false

        override fun write(b: Int) { write(byteArrayOf(b.toByte()), 0, 1) }

        override fun write(src: ByteArray, off: Int, len: Int) {
            var pos = off
            var restante = len
            while (restante > 0) {
                // Só emite um bloco cheio quando há mais dados: o último bloco sempre sai em close() como "final".
                if (n == TAM_BLOCO) emitir(ultimo = false)
                val c = minOf(restante, TAM_BLOCO - n)
                System.arraycopy(src, pos, buf, n, c)
                n += c; pos += c; restante -= c
            }
        }

        private fun emitir(ultimo: Boolean) {
            val c = cifra(Cipher.ENCRYPT_MODE, chave, nonce(prefixo, indice), aad(cabecalho, indice, ultimo))
            val cifrado = c.doFinal(buf, 0, n)
            destino.write(ByteBuffer.allocate(4).putInt(cifrado.size).array())
            destino.write(cifrado)
            indice++
            n = 0
        }

        override fun flush() = destino.flush()

        override fun close() {
            if (fechado) return
            fechado = true
            emitir(ultimo = true)
            destino.flush()
            destino.close()
        }
    }

    // ───────────── leitura ─────────────

    /**
     * @throws FormatoDesconhecidoException se não for um backup cifrado
     * @throws SenhaIncorretaException (lançada na primeira leitura) se a senha estiver errada
     */
    fun decifrando(entrada: InputStream, senha: CharArray): InputStream {
        val cabecalho = ByteArray(TAM_CABECALHO)
        try {
            lerTudo(entrada, cabecalho)
        } catch (e: EOFException) {
            throw FormatoDesconhecidoException()
        }
        if (!ehCifrado(cabecalho) || cabecalho[4] != VERSAO) throw FormatoDesconhecidoException()
        val bb = ByteBuffer.wrap(cabecalho, 5, TAM_CABECALHO - 5)
        val iteracoes = bb.int
        if (iteracoes !in 1..MAX_ITERACOES) throw BackupCorrompidoException("Cabeçalho do backup inválido.")
        val salt = ByteArray(TAM_SALT).also { bb.get(it) }
        val prefixo = ByteArray(TAM_PREFIXO).also { bb.get(it) }
        return FluxoDecifrado(entrada, derivarChave(senha, salt, iteracoes), cabecalho, prefixo)
    }

    private class FluxoDecifrado(
        private val origem: InputStream,
        private val chave: SecretKeySpec,
        private val cabecalho: ByteArray,
        private val prefixo: ByteArray,
    ) : InputStream() {
        private var bloco = ByteArray(0)
        private var pos = 0
        private var indice = 0
        private var fim = false
        private var proximoTam: ByteArray? = null

        override fun read(): Int {
            val um = ByteArray(1)
            return if (read(um, 0, 1) == -1) -1 else um[0].toInt() and 0xFF
        }

        override fun read(dst: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (pos >= bloco.size) {
                if (fim) return -1
                carregar()
            }
            val c = minOf(len, bloco.size - pos)
            System.arraycopy(bloco, pos, dst, off, c)
            pos += c
            return c
        }

        private fun carregar() {
            val tamBytes = proximoTam ?: ByteArray(4).also {
                if (!tentarLer(it)) throw BackupCorrompidoException("Backup truncado.")
            }
            proximoTam = null
            val tam = ByteBuffer.wrap(tamBytes).int
            if (tam < TAM_TAG || tam > TAM_BLOCO + TAM_TAG) throw BackupCorrompidoException("Bloco inválido no backup.")
            val cifrado = ByteArray(tam)
            try {
                lerTudo(origem, cifrado)
            } catch (e: EOFException) {
                throw BackupCorrompidoException("Backup truncado.")
            }

            // Para saber se este é o último bloco, espia o prefixo do próximo.
            val prox = ByteArray(4)
            val temProximo = tentarLer(prox)
            if (temProximo) proximoTam = prox

            try {
                val c = cifra(Cipher.DECRYPT_MODE, chave, nonce(prefixo, indice), aad(cabecalho, indice, ultimo = !temProximo))
                bloco = c.doFinal(cifrado)
            } catch (e: GeneralSecurityException) {
                // Falha no 1º bloco = chave errada (senha incorreta); depois dele = adulteração/corrupção.
                throw if (indice == 0) SenhaIncorretaException() else BackupCorrompidoException("Backup corrompido ou adulterado.")
            }
            pos = 0
            indice++
            if (!temProximo) fim = true
        }

        /** Lê exatamente buf.size bytes; false se o fluxo terminou antes de ler qualquer byte. */
        private fun tentarLer(buf: ByteArray): Boolean {
            var lidos = 0
            while (lidos < buf.size) {
                val r = origem.read(buf, lidos, buf.size - lidos)
                if (r == -1) {
                    if (lidos == 0) return false
                    throw BackupCorrompidoException("Backup truncado.")
                }
                lidos += r
            }
            return true
        }

        override fun close() = origem.close()
    }

    private fun lerTudo(i: InputStream, buf: ByteArray) {
        var lidos = 0
        while (lidos < buf.size) {
            val r = i.read(buf, lidos, buf.size - lidos)
            if (r == -1) throw EOFException()
            lidos += r
        }
    }

    /** Utilitário de teste/diagnóstico: cifra um array inteiro. */
    fun cifrar(dados: ByteArray, senha: CharArray, iteracoes: Int = ITERACOES_PADRAO): ByteArray {
        val bos = ByteArrayOutputStream()
        cifrando(bos, senha, iteracoes).use { it.write(dados) }
        return bos.toByteArray()
    }

    fun decifrar(cifrado: ByteArray, senha: CharArray): ByteArray =
        decifrando(cifrado.inputStream(), senha).use { it.readBytes() }
}
