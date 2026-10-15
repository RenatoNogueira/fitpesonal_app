package br.com.fitpersonal.app.data

import br.com.fitpersonal.app.data.crypto.BackupCorrompidoException
import br.com.fitpersonal.app.data.crypto.CifraBackup
import br.com.fitpersonal.app.data.crypto.FormatoDesconhecidoException
import br.com.fitpersonal.app.data.crypto.SenhaIncorretaException
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class CifraBackupTest {
    // Poucas iterações só para o teste ficar rápido; o cabeçalho carrega o valor usado.
    private val IT = 1_000
    private val senha = "senha-forte-123".toCharArray()

    private fun bytes(n: Int) = ByteArray(n).also { Random(42).nextBytes(it) }

    @Test fun idaEVolta_variosTamanhos() {
        for (n in listOf(0, 1, 100, 64 * 1024 - 1, 64 * 1024, 64 * 1024 + 1, 300_000)) {
            val claro = bytes(n)
            val cifrado = CifraBackup.cifrar(claro, senha, IT)
            assertArrayEquals("tamanho $n", claro, CifraBackup.decifrar(cifrado, senha))
        }
    }

    @Test fun cifradoNaoContemTextoClaro() {
        val claro = "CPF 529.982.247-25 pressão 120/80".repeat(50).toByteArray()
        val cifrado = CifraBackup.cifrar(claro, senha, IT)
        assertFalse(String(cifrado, Charsets.ISO_8859_1).contains("529.982.247-25"))
        assertTrue(CifraBackup.ehCifrado(cifrado))
    }

    @Test fun senhaErrada() {
        val cifrado = CifraBackup.cifrar(bytes(1000), senha, IT)
        assertThrows(SenhaIncorretaException::class.java) {
            CifraBackup.decifrar(cifrado, "outra-senha".toCharArray())
        }
    }

    @Test fun adulteracaoDeUmByteEDetectada() {
        val cifrado = CifraBackup.cifrar(bytes(200_000), senha, IT)
        val adulterado = cifrado.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 0x01).toByte() }
        assertThrows(Exception::class.java) { CifraBackup.decifrar(adulterado, senha) }
    }

    @Test fun truncamentoNoLimiteDeBlocoEDetectado() {
        val cifrado = CifraBackup.cifrar(bytes(200_000), senha, IT)
        // Remove o último bloco inteiro: os restantes terminam "sem marca de final".
        val ultimoTam = (64 * 1024 + 16).coerceAtMost(200_000 % (64 * 1024) + 16)
        val cortado = cifrado.copyOf(cifrado.size - (4 + ultimoTam))
        assertThrows(BackupCorrompidoException::class.java) { CifraBackup.decifrar(cortado, senha) }
    }

    @Test fun arquivoQueNaoEBackup() {
        assertFalse(CifraBackup.ehCifrado("PK\u0003\u0004zip".toByteArray()))
        assertThrows(FormatoDesconhecidoException::class.java) {
            CifraBackup.decifrar("isto não é um backup, de jeito nenhum, mesmo".toByteArray(), senha)
        }
    }

    @Test fun saltsDiferentesGeramSaidasDiferentes() {
        val claro = bytes(500)
        assertFalse(CifraBackup.cifrar(claro, senha, IT).contentEquals(CifraBackup.cifrar(claro, senha, IT)))
    }
}
