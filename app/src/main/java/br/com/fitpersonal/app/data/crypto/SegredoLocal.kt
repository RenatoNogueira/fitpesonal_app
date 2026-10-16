package br.com.fitpersonal.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cofre de segredos pequenos (chave do banco, senha do backup).
 *
 * Cada segredo é cifrado com AES-256-GCM por uma chave que NUNCA sai do Android Keystore
 * (hardware quando disponível) e gravado em `noBackupFilesDir`, fora de qualquer backup do sistema.
 * Se o Keystore perder a chave (restauração do aparelho, troca de bloqueio em alguns fabricantes),
 * `ler` devolve null — por isso o app oferece backup com senha própria do usuário.
 */
@Singleton
class SegredoLocal private constructor(
    private val ctx: Context,
    /** Alias da chave no Keystore. Só muda em testes, para não tocar nos segredos reais do app. */
    private val alias: String,
) {

    @Inject
    constructor(@ApplicationContext ctx: Context) : this(ctx, ALIAS_PADRAO)

    companion object {
        private const val ALIAS_PADRAO = "fitpersonal_segredos_v1"
        private const val PROVEDOR = "AndroidKeyStore"

        /** Instância com alias próprio (testes instrumentados). */
        fun comAlias(ctx: Context, alias: String) = SegredoLocal(ctx, alias)
    }

    private fun chave(): SecretKey {
        val ks = KeyStore.getInstance(PROVEDOR).apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR)
        gerador.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gerador.generateKey()
    }

    private fun arquivo(nome: String) = File(ctx.noBackupFilesDir, "segredo_$nome.bin")

    fun existe(nome: String): Boolean = arquivo(nome).isFile

    fun guardar(nome: String, valor: ByteArray) {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, chave()) }
        val iv = c.iv
        val cifrado = c.doFinal(valor)
        arquivo(nome).writeBytes(byteArrayOf(iv.size.toByte()) + iv + cifrado)
    }

    /** null se não existe ou se não pôde ser decifrado (chave do Keystore perdida). */
    fun ler(nome: String): ByteArray? {
        val f = arquivo(nome)
        if (!f.isFile) return null
        return runCatching {
            val dados = f.readBytes()
            val tamIv = dados[0].toInt()
            val iv = dados.copyOfRange(1, 1 + tamIv)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
                .apply { init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(128, iv)) }
            c.doFinal(dados, 1 + tamIv, dados.size - 1 - tamIv)
        }.getOrNull()
    }

    fun apagar(nome: String) { arquivo(nome).delete() }

    /**
     * Descarta todos os segredos e a chave do Keystore (usado quando ficaram ilegíveis).
     * A próxima chamada a obterOuCriarAleatorio/guardar cria uma chave nova.
     */
    @Synchronized
    fun redefinirTudo() {
        ctx.noBackupFilesDir.listFiles { f -> f.name.startsWith("segredo_") }?.forEach { it.delete() }
        runCatching { KeyStore.getInstance(PROVEDOR).apply { load(null) }.deleteEntry(alias) }
    }

    /** Devolve o segredo; se ainda não existe, gera [tamanho] bytes aleatórios e o guarda. */
    @Synchronized
    fun obterOuCriarAleatorio(nome: String, tamanho: Int = 32): ByteArray {
        ler(nome)?.let { return it }
        check(!existe(nome)) {
            // Arquivo existe mas não decifra: gerar outro silenciosamente tornaria o banco ilegível sem aviso.
            "Segredo '$nome' ilegível (chave do Keystore perdida)."
        }
        val novo = ByteArray(tamanho).also { SecureRandom().nextBytes(it) }
        guardar(nome, novo)
        return novo
    }
}

fun ByteArray.paraHex(): String = joinToString("") { "%02x".format(it) }
