package br.com.fitpersonal.app

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import br.com.fitpersonal.app.data.local.BancoCriptografado
import br.com.fitpersonal.app.testutil.Ambiente
import br.com.fitpersonal.app.testutil.criarBancoCifrado
import br.com.fitpersonal.app.testutil.criarBancoTextoPuro
import br.com.fitpersonal.app.testutil.lerNomesCifrado
import br.com.fitpersonal.app.testutil.lerNomesTextoPuro
import br.com.fitpersonal.app.testutil.senhaAleatoria
import br.com.fitpersonal.app.testutil.versaoCifrado
import br.com.fitpersonal.app.testutil.versaoTextoPuro
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Migração do banco antigo (texto puro) para SQLCipher e cópia legível usada nos backups.
 * Roda em aparelho/emulador: usa a biblioteca nativa do SQLCipher de verdade.
 */
class CriptografiaBancoTest {

    private lateinit var amb: Ambiente
    private lateinit var senha: ByteArray

    @Before fun preparar() {
        amb = Ambiente.criar()
        senha = senhaAleatoria()
    }

    @After fun limpar() = amb.limpar()

    @Test fun bancoTextoPuroEhConvertidoPreservandoDadosEVersao() {
        val arq = amb.arquivoBanco
        criarBancoTextoPuro(arq, listOf("Maria Confidencial", "João Sigiloso"), versao = 2)
        assertTrue(BancoCriptografado.ehTextoPuro(arq))

        BancoCriptografado.criptografarSePreciso(arq, senha)

        assertTrue("o arquivo deve continuar no mesmo caminho", arq.isFile)
        assertFalse("o arquivo não pode mais ser SQLite comum", BancoCriptografado.ehTextoPuro(arq))
        assertFalse(
            "nomes não podem aparecer em texto claro no arquivo",
            String(arq.readBytes(), Charsets.ISO_8859_1).contains("Maria Confidencial"),
        )
        assertEquals(listOf("Maria Confidencial", "João Sigiloso"), lerNomesCifrado(arq, senha))
        assertEquals("user_version é usado pelas migrações do Room", 2, versaoCifrado(arq, senha))
        assertFalse("sobras do arquivo temporário", File(arq.path + ".enc").exists())
    }

    @Test fun bancoJaCifradoNaoEhAlteradoPorNovaMigracao() {
        val arq = amb.arquivoBanco
        criarBancoCifrado(arq, senha, listOf("Ana"))
        val antes = arq.readBytes()

        BancoCriptografado.criptografarSePreciso(arq, senha)

        assertArrayEquals(antes, arq.readBytes())
    }

    @Test fun chaveErradaNaoAbreOBancoCifrado() {
        val arq = amb.arquivoBanco
        criarBancoCifrado(arq, senha, listOf("Ana"))

        assertThrows(Exception::class.java) { lerNomesCifrado(arq, senhaAleatoria()) }
        assertThrows(Exception::class.java) { lerNomesTextoPuro(arq) }
    }

    @Test fun copiaLegivelParaBackupTemOsDadosEPodeVoltarACriptografar() {
        val cifrado = File(amb.raiz, "cifrado.db")
        criarBancoCifrado(cifrado, senha, listOf("Maria", "José"), versao = 3)

        // Abre pelo mesmo caminho que o Room usa em produção e exporta a cópia legível
        val config = SupportSQLiteOpenHelper.Configuration.builder(amb.ctx)
            .name(cifrado.path)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = SupportOpenHelperFactory(senha).create(config)
        val legivel = File(amb.raiz, "legivel.db")
        try {
            BancoCriptografado.exportarLegivel(helper.writableDatabase, legivel)
        } finally {
            helper.close()
        }

        assertTrue(BancoCriptografado.ehTextoPuro(legivel))
        assertEquals(listOf("Maria", "José"), lerNomesTextoPuro(legivel))
        assertEquals(3, versaoTextoPuro(legivel))

        // Restauração: cópia legível → banco cifrado com a chave deste aparelho
        val novaSenha = senhaAleatoria()
        val restaurado = File(amb.raiz, "restaurado.db")
        BancoCriptografado.converterParaCriptografado(legivel, restaurado, novaSenha)

        assertEquals(listOf("Maria", "José"), lerNomesCifrado(restaurado, novaSenha))
        assertEquals(3, versaoCifrado(restaurado, novaSenha))
        assertFalse(BancoCriptografado.ehTextoPuro(restaurado))
    }
}
