package br.com.fitpersonal.app

import android.net.Uri
import br.com.fitpersonal.app.data.backup.BackupInvalidoException
import br.com.fitpersonal.app.data.backup.BackupManager
import br.com.fitpersonal.app.data.backup.SenhaAusenteException
import br.com.fitpersonal.app.data.backup.SenhaNecessariaException
import br.com.fitpersonal.app.data.crypto.CifraBackup
import br.com.fitpersonal.app.data.crypto.SenhaIncorretaException
import br.com.fitpersonal.app.data.local.AlunoEntity
import br.com.fitpersonal.app.data.local.BancoCriptografado
import br.com.fitpersonal.app.data.local.FitDatabase
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import br.com.fitpersonal.app.testutil.Ambiente
import br.com.fitpersonal.app.testutil.PerfilEmMemoria
import br.com.fitpersonal.app.testutil.abrirRoom
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Backup cifrado com senha e restauração, de ponta a ponta: Room + SQLCipher + Keystore + zip.
 * Tudo acontece numa pasta temporária (ver [Ambiente]); os dados reais do app não são tocados.
 */
class BackupRestauracaoTest {

    private val senhaBackup = "senha-forte-123"

    private lateinit var amb: Ambiente
    private lateinit var senhaBanco: ByteArray
    private lateinit var db: FitDatabase
    private lateinit var perfil: PerfilEmMemoria
    private lateinit var backup: BackupManager

    private val abertos = mutableListOf<FitDatabase>()

    @Before fun preparar() {
        amb = Ambiente.criar()
        senhaBanco = BancoCriptografado.preparar(amb.ctx, amb.segredos)
        db = abrir()
        perfil = PerfilEmMemoria(PerfilProfissional(nome = "Prof. Teste", cref = "123456-G/SP", cidade = "Santos"))
        backup = BackupManager(amb.ctx, db, perfil, amb.segredos)
    }

    @After fun limpar() {
        abertos.forEach { runCatching { if (it.isOpen) it.close() } }
        amb.limpar()
    }

    private fun abrir(): FitDatabase = abrirRoom(amb.ctx, senhaBanco).also { abertos += it }

    private fun aluno(nome: String) = AlunoEntity(
        nome = nome, dataNascimento = "1990-05-20", sexo = "Feminino", telefone = "11987654321",
        email = null, cpf = "52998224725", cep = null, instagram = null, objetivo = "Hipertrofia",
        observacoesMedicas = "Hipertensa", fotoPerfil = "fotos/perfil/a.jpg", dataCadastro = "2026-01-01",
    )

    private fun foto() = File(amb.ctx.filesDir, "fotos/perfil/a.jpg")
    private fun uri(f: File): Uri = Uri.fromFile(f)

    private fun exportarPara(nome: String): File {
        val f = File(amb.ctx.cacheDir, nome)
        runBlocking { backup.exportar(uri(f)) }
        return f
    }

    private fun semear() = runBlocking {
        db.alunoDao().inserir(aluno("Maria Confidencial"))
        foto().apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
    }

    // ───────────── exportar ─────────────

    @Test fun exportarSemSenhaConfiguradaEhBloqueado() {
        assertFalse(backup.temSenha())
        assertThrows(SenhaAusenteException::class.java) {
            runBlocking { backup.exportar(uri(File(amb.ctx.cacheDir, "x.fitbak"))) }
        }
    }

    @Test fun backupEhCifradoENaoVazaDadosEmTextoClaro() {
        semear()
        backup.definirSenha(senhaBackup.toCharArray())

        val arq = exportarPara("b.fitbak")
        val bytes = arq.readBytes()

        assertTrue(CifraBackup.ehCifrado(bytes.copyOf(4)))
        val texto = String(bytes, Charsets.ISO_8859_1)
        assertFalse(texto.contains("Maria Confidencial"))
        assertFalse(texto.contains("52998224725"))
        assertFalse(texto.contains("SQLite format 3"))
        assertFalse("o arquivo temporário legível não pode sobrar no cache",
            File(amb.ctx.cacheDir, "backup_tmp.db").exists())
    }

    // ───────────── restaurar ─────────────

    @Test fun restauraBancoFotosEPerfilAposPerdaDosDados() {
        semear()
        backup.definirSenha(senhaBackup.toCharArray())
        val arq = exportarPara("b.fitbak")

        // Estrago: apaga aluno, foto e perfil
        runBlocking { db.alunoDao().todosSnapshot().forEach { db.alunoDao().excluir(it) } }
        foto().delete()
        perfil.valor = PerfilProfissional()

        runBlocking { backup.restaurar(uri(arq), senhaBackup.toCharArray()) }

        val reaberto = abrir()   // o BackupManager fechou a instância antiga
        val alunos = runBlocking { reaberto.alunoDao().todosSnapshot() }
        assertEquals(listOf("Maria Confidencial"), alunos.map { it.nome })
        assertEquals("52998224725", alunos.single().cpf)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), foto().readBytes())
        assertEquals("Prof. Teste", perfil.valor.nome)
        assertEquals("123456-G/SP", perfil.valor.cref)
        assertFalse("o banco restaurado deve estar cifrado com a chave deste aparelho",
            BancoCriptografado.ehTextoPuro(amb.arquivoBanco))
        assertFalse("sobras da restauração", File(amb.ctx.cacheDir, "restore_tmp").exists())
    }

    @Test fun restauracaoGuardaUmSnapshotCifradoDoEstadoAnterior() {
        semear()
        backup.definirSenha(senhaBackup.toCharArray())
        val arq = exportarPara("b.fitbak")

        runBlocking { backup.restaurar(uri(arq), senhaBackup.toCharArray()) }

        val snaps = File(amb.ctx.filesDir, "backups").listFiles().orEmpty()
        assertTrue(snaps.any { it.name.startsWith("pre_restauracao_") && it.name.endsWith(".fitbak") })
        snaps.forEach { assertTrue(CifraBackup.ehCifrado(it.readBytes().copyOf(4))) }
    }

    @Test fun senhaIncorretaNaoAlteraNada() {
        semear()
        backup.definirSenha(senhaBackup.toCharArray())
        val arq = exportarPara("b.fitbak")
        val bancoAntes = amb.arquivoBanco.length()

        assertThrows(SenhaIncorretaException::class.java) {
            runBlocking { backup.restaurar(uri(arq), "senha-errada-999".toCharArray()) }
        }

        // A instância original continua válida e com os dados
        val alunos = runBlocking { db.alunoDao().todosSnapshot() }
        assertEquals(1, alunos.size)
        assertTrue(db.isOpen)
        assertTrue(amb.arquivoBanco.length() >= bancoAntes)
        assertNotNull(foto().takeIf { it.exists() })
    }

    @Test fun backupCifradoSemSenhaPedeASenha() {
        semear()
        backup.definirSenha(senhaBackup.toCharArray())
        val arq = exportarPara("b.fitbak")

        assertThrows(SenhaNecessariaException::class.java) {
            runBlocking { backup.restaurar(uri(arq), null) }
        }
        assertTrue(runBlocking { backup.ehCifrado(uri(arq)) })
    }

    @Test fun backupDoDesktopEhRecusadoComOrientacao() {
        val zip = File(amb.ctx.cacheDir, "desktop.zip")
        ZipOutputStream(FileOutputStream(zip)).use { z ->
            z.putNextEntry(ZipEntry("data/rje_avaliacoes.db")); z.write(ByteArray(64)); z.closeEntry()
            z.putNextEntry(ZipEntry("media/alunos/a.jpg")); z.write(ByteArray(8)); z.closeEntry()
        }

        val e = assertThrows(BackupInvalidoException::class.java) {
            runBlocking { backup.restaurar(uri(zip), null) }
        }
        assertTrue(e.message.orEmpty().contains("desktop", ignoreCase = true))
    }

    @Test fun zipComCaminhoMaliciosoEhRejeitadoSemEscreverForaDaPasta() {
        backup.definirSenha(senhaBackup.toCharArray())
        val sujo = File(amb.ctx.cacheDir, "sujo.fitbak")
        val senha = senhaBackup.toCharArray()
        CifraBackup.cifrando(FileOutputStream(sujo), senha, iteracoes = 1_000).use { saida ->
            ZipOutputStream(saida).use { z ->
                z.putNextEntry(ZipEntry("fotos/../../fora.txt")); z.write("pwned".toByteArray()); z.closeEntry()
            }
        }

        assertThrows(BackupInvalidoException::class.java) {
            runBlocking { backup.restaurar(uri(sujo), senha) }
        }

        val fora = File(File(amb.ctx.cacheDir, "restore_tmp").parentFile, "fora.txt")
        assertFalse(fora.exists())
        assertFalse(File(amb.ctx.filesDir, "fora.txt").exists())
    }

    @Test fun backupLegadoSemCifraAindaRestaura() {
        semear()
        // Monta à mão um backup do formato antigo: .zip sem cifra com cópia legível do banco
        val legivel = File(amb.ctx.cacheDir, "legivel.db")
        BancoCriptografado.exportarLegivel(db.openHelper.writableDatabase, legivel)
        val zip = File(amb.ctx.cacheDir, "antigo.zip")
        ZipOutputStream(FileOutputStream(zip)).use { z ->
            z.putNextEntry(ZipEntry("rje_avaliacoes.db")); z.write(legivel.readBytes()); z.closeEntry()
            z.putNextEntry(ZipEntry("fotos/perfil/a.jpg")); z.write(byteArrayOf(9, 9)); z.closeEntry()
        }
        legivel.delete()
        runBlocking { db.alunoDao().todosSnapshot().forEach { db.alunoDao().excluir(it) } }

        runBlocking { backup.restaurar(uri(zip), null) }

        val alunos = runBlocking { abrir().alunoDao().todosSnapshot() }
        assertEquals(listOf("Maria Confidencial"), alunos.map { it.nome })
        assertArrayEquals(byteArrayOf(9, 9), foto().readBytes())
    }
}
