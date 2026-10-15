package br.com.fitpersonal.app.di

import android.content.Context
import androidx.room.Room
import br.com.fitpersonal.app.data.crypto.SegredoLocal
import br.com.fitpersonal.app.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Banco SQLCipher (AES-256). Na primeira execução após atualizar de uma versão sem criptografia,
     * o arquivo existente é convertido no lugar antes de o Room abri-lo.
     */
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context, segredos: SegredoLocal): FitDatabase {
        // Também trata chave perdida / banco ilegível sem lançar: ver BancoCriptografado.preparar.
        val senha = BancoCriptografado.preparar(ctx, segredos)
        return Room.databaseBuilder(ctx, FitDatabase::class.java, DB_NOME)
            .openHelperFactory(SupportOpenHelperFactory(senha))
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .build()
    }

    @Provides fun alunoDao(db: FitDatabase) = db.alunoDao()
    @Provides fun agendamentoDao(db: FitDatabase) = db.agendamentoDao()
    @Provides fun avaliacaoDao(db: FitDatabase) = db.avaliacaoDao()
    @Provides fun treinoDao(db: FitDatabase) = db.treinoDao()
    @Provides fun catalogoDao(db: FitDatabase) = db.catalogoDao()
    @Provides fun modeloTreinoDao(db: FitDatabase) = db.modeloTreinoDao()
    @Provides fun pagamentoDao(db: FitDatabase) = db.pagamentoDao()
    @Provides fun historicoCargaDao(db: FitDatabase) = db.historicoCargaDao()
    @Provides fun listaEsperaDao(db: FitDatabase) = db.listaEsperaDao()
    @Provides fun videoExercicioDao(db: FitDatabase) = db.videoExercicioDao()
    @Provides fun testeAptidaoDao(db: FitDatabase) = db.testeAptidaoDao()
    @Provides fun retornoDao(db: FitDatabase) = db.retornoDao()

    @Provides @Singleton
    fun catalogoSeeder(@ApplicationContext ctx: Context, dao: CatalogoDao) = CatalogoSeeder(ctx, dao)
}
