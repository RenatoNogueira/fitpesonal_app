package br.com.fitpersonal.app

import br.com.fitpersonal.app.data.local.BancoCriptografado
import br.com.fitpersonal.app.data.local.DB_NOME
import br.com.fitpersonal.app.data.local.RegistroRecuperacao
import br.com.fitpersonal.app.testutil.Ambiente
import br.com.fitpersonal.app.testutil.abrirRoom
import br.com.fitpersonal.app.testutil.criarBancoCifrado
import br.com.fitpersonal.app.testutil.criarBancoTextoPuro
import br.com.fitpersonal.app.testutil.lerNomesCifrado
import br.com.fitpersonal.app.testutil.senhaAleatoria
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore

/**
 * O app nunca deve travar por chave do Keystore perdida: deve guardar o banco ilegível de lado,
 * abrir um novo e registrar a recuperação pendente. Usa Keystore e SQLCipher reais.
 */
class RecuperacaoChaveTest {

    private lateinit var amb: Ambiente

    private val pastaDeLado get() = File(amb.ctx.filesDir, "banco_ilegivel")
    private val pendente get() = RegistroRecuperacao.ler(amb.ctx.noBackupFilesDir)
    private val arquivoSegredoBanco get() = File(amb.ctx.noBackupFilesDir, "segredo_db_key.bin")

    @Before fun preparar() { amb = Ambiente.criar() }

    @After fun limpar() = amb.limpar()

    @Test fun primeiraExecucaoNaoGeraRecuperacao() {
        val senha = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        assertEquals(64, senha.size)   // 32 bytes em hex
        assertFalse(amb.arquivoBanco.exists())
        assertNull(pendente)
        assertFalse(pastaDeLado.exists())
    }

    @Test fun bancoCifradoComChaveValidaAbreNormalmente() {
        val k = BancoCriptografado.senhaBanco(amb.segredos)
        criarBancoCifrado(amb.arquivoBanco, k, listOf("Maria"))

        val usada = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        assertArrayEquals(k, usada)
        assertEquals(listOf("Maria"), lerNomesCifrado(amb.arquivoBanco, usada))
        assertNull(pendente)
    }

    @Test fun arquivoDeSegredoCorrompidoMoveOBancoDeLadoSemPerderOsDados() {
        val k1 = BancoCriptografado.senhaBanco(amb.segredos)
        criarBancoCifrado(amb.arquivoBanco, k1, listOf("Maria"))
        // Simula o Keystore/arquivo de segredo ilegível
        arquivoSegredoBanco.writeBytes(ByteArray(40) { 7 })

        val k2 = BancoCriptografado.preparar(amb.ctx, amb.segredos)   // não pode lançar

        assertFalse("chave nova", k1.contentEquals(k2))
        assertFalse("o caminho do banco fica livre para um banco novo", amb.arquivoBanco.exists())
        val guardados = pastaDeLado.listFiles().orEmpty()
        assertEquals(1, guardados.size)
        assertEquals("o banco antigo foi preservado, ainda legível com a chave antiga",
            listOf("Maria"), lerNomesCifrado(guardados[0], k1))
        assertNotNull("a UI precisa saber que há recuperação pendente", pendente)
    }

    @Test fun chaveRemovidaDoKeystoreTambemRecupera() {
        val k1 = BancoCriptografado.senhaBanco(amb.segredos)
        criarBancoCifrado(amb.arquivoBanco, k1, listOf("Maria"))
        // O arquivo de segredo permanece, mas a chave que o decifra some (restauração do aparelho)
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(amb.alias)

        val k2 = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        assertFalse(k1.contentEquals(k2))
        assertFalse(amb.arquivoBanco.exists())
        assertEquals(1, pastaDeLado.listFiles().orEmpty().size)
        assertNotNull(pendente)
    }

    @Test fun bancoAindaSemCriptografiaNaoPerdeDadosQuandoAChaveSumiu() {
        BancoCriptografado.senhaBanco(amb.segredos)           // cria o segredo...
        arquivoSegredoBanco.writeBytes(ByteArray(40) { 3 })   // ...e o corrompe
        criarBancoTextoPuro(amb.arquivoBanco, listOf("Maria"))

        val k = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        assertFalse(BancoCriptografado.ehTextoPuro(amb.arquivoBanco))
        assertEquals("dados preservados, agora cifrados com a chave nova", listOf("Maria"),
            lerNomesCifrado(amb.arquivoBanco, k))
        assertNull("nada foi perdido: não há recuperação a fazer", pendente)
        assertFalse(pastaDeLado.exists())
    }

    @Test fun bancoCifradoComOutraChaveVaiParaOLadoMantendoAChaveAtual() {
        val kAtual = BancoCriptografado.senhaBanco(amb.segredos)
        val kOutra = senhaAleatoria()
        criarBancoCifrado(amb.arquivoBanco, kOutra, listOf("Maria"))

        val usada = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        assertArrayEquals("a chave do aparelho continua a mesma", kAtual, usada)
        assertFalse(amb.arquivoBanco.exists())
        assertEquals(listOf("Maria"), lerNomesCifrado(pastaDeLado.listFiles()!!.single(), kOutra))
        assertNotNull(pendente)
    }

    @Test fun depoisDaRecuperacaoORoomAbreUmBancoNovoUtilizavel() = runBlocking {
        val k1 = BancoCriptografado.senhaBanco(amb.segredos)
        criarBancoCifrado(amb.arquivoBanco, k1, listOf("Maria"))
        arquivoSegredoBanco.writeBytes(ByteArray(40) { 9 })

        val k2 = BancoCriptografado.preparar(amb.ctx, amb.segredos)

        val db = abrirRoom(amb.ctx, k2)
        try {
            assertTrue(db.alunoDao().todosSnapshot().isEmpty())
            val id = db.alunoDao().inserir(
                br.com.fitpersonal.app.data.local.AlunoEntity(
                    nome = "Nova", dataNascimento = null, sexo = null, telefone = null, email = null,
                    cpf = null, cep = null, instagram = null, objetivo = null, observacoesMedicas = null,
                    fotoPerfil = null, dataCadastro = "2026-01-01",
                )
            )
            assertTrue(id > 0)
            assertEquals("Nova", db.alunoDao().buscar(id)?.nome)
        } finally {
            db.close()
        }
        // E é um arquivo SQLCipher, não SQLite comum
        assertFalse(BancoCriptografado.ehTextoPuro(amb.ctx.getDatabasePath(DB_NOME)))
    }

    @Test fun registroPendenteSomeAoSerLimpo() {
        val k1 = BancoCriptografado.senhaBanco(amb.segredos)
        criarBancoCifrado(amb.arquivoBanco, k1, listOf("Maria"))
        arquivoSegredoBanco.writeBytes(ByteArray(40) { 1 })
        BancoCriptografado.preparar(amb.ctx, amb.segredos)
        assertNotNull(pendente)

        RegistroRecuperacao.limpar(amb.ctx.noBackupFilesDir)

        assertNull(pendente)
    }
}
