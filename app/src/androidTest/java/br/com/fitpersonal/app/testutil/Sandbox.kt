package br.com.fitpersonal.app.testutil

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.fitpersonal.app.data.crypto.SegredoLocal
import br.com.fitpersonal.app.data.local.BancoCriptografado
import br.com.fitpersonal.app.data.local.DB_NOME
import br.com.fitpersonal.app.data.local.MIGRATION_1_2
import br.com.fitpersonal.app.data.local.MIGRATION_2_3
import br.com.fitpersonal.app.data.local.MIGRATION_3_4
import br.com.fitpersonal.app.data.local.MIGRATION_4_5
import br.com.fitpersonal.app.data.local.MIGRATION_5_6
import br.com.fitpersonal.app.data.local.MIGRATION_6_7
import br.com.fitpersonal.app.data.local.MIGRATION_7_8
import br.com.fitpersonal.app.data.local.MIGRATION_8_9
import br.com.fitpersonal.app.data.local.MIGRATION_10_11
import br.com.fitpersonal.app.data.local.MIGRATION_9_10
import br.com.fitpersonal.app.data.local.FitDatabase
import br.com.fitpersonal.app.data.repo.PerfilFonte
import br.com.fitpersonal.app.domain.model.PerfilProfissional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.util.UUID

/**
 * Contexto "caixa de areia": redireciona banco, arquivos, cache e noBackup para uma pasta temporária,
 * de modo que os testes nunca toquem nos dados reais do app instalado no aparelho.
 */
class SandboxContext(base: Context, private val raiz: File) : ContextWrapper(base) {
    // DataStore e outros componentes derivam caminhos de applicationContext
    override fun getApplicationContext(): Context = this
    override fun getFilesDir(): File = File(raiz, "files").also { it.mkdirs() }
    override fun getCacheDir(): File = File(raiz, "cache").also { it.mkdirs() }
    override fun getNoBackupFilesDir(): File = File(raiz, "nobackup").also { it.mkdirs() }
    override fun getDatabasePath(name: String): File {
        if (name.startsWith("/")) return File(name)
        return File(File(raiz, "databases").also { it.mkdirs() }, name)
    }
}

/** Ambiente isolado de um teste: pasta, contexto e segredos com alias próprio no Keystore. */
class Ambiente private constructor(
    val raiz: File,
    val ctx: SandboxContext,
    val alias: String,
    val segredos: SegredoLocal,
) {
    val arquivoBanco: File get() = ctx.getDatabasePath(DB_NOME)

    /** Remove a pasta e a chave de teste do Keystore. */
    fun limpar() {
        segredos.redefinirTudo()
        raiz.deleteRecursively()
    }

    companion object {
        fun criar(): Ambiente {
            BancoCriptografado.carregarBiblioteca()
            val alvo = InstrumentationRegistry.getInstrumentation().targetContext
            val id = UUID.randomUUID().toString().take(8)
            val raiz = File(alvo.cacheDir, "teste_$id").also { it.mkdirs() }
            val ctx = SandboxContext(alvo, raiz)
            val alias = "fit_teste_$id"
            return Ambiente(raiz, ctx, alias, SegredoLocal.comAlias(ctx, alias))
        }
    }
}

/** Perfil em memória: o DataStore real é um singleton do processo e não pode ser redirecionado. */
class PerfilEmMemoria(inicial: PerfilProfissional = PerfilProfissional()) : PerfilFonte {
    private val estado = MutableStateFlow(inicial)
    override val perfil: Flow<PerfilProfissional> = estado
    override suspend fun salvar(p: PerfilProfissional) { estado.value = p }
    var valor: PerfilProfissional
        get() = estado.value
        set(v) { estado.value = v }
}

/** Abre o banco do app (Room + SQLCipher) no ambiente de teste, como o AppModule faz em produção. */
fun abrirRoom(ctx: Context, senha: ByteArray): FitDatabase =
    Room.databaseBuilder(ctx, FitDatabase::class.java, DB_NOME)
        .openHelperFactory(SupportOpenHelperFactory(senha))
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
        .build()
