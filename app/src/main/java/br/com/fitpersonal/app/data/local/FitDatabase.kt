package br.com.fitpersonal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

const val DB_NOME = "fitpersonal.db"
const val DB_VERSAO = 14

@Database(
    entities = [
        AlunoEntity::class, AgendamentoEntity::class, AvaliacaoEntity::class,
        TreinoEntity::class, ExercicioTreinoEntity::class, ExercicioCatalogoEntity::class,
        ModeloTreinoEntity::class, ModeloExercicioEntity::class,
        PagamentoEntity::class, HistoricoCargaEntity::class,
        ListaEsperaEntity::class, TesteAptidaoEntity::class,
        ExecucaoImportadaEntity::class, RetornoAlunoEntity::class,
        VideoExercicioEntity::class, MetaComposicaoEntity::class,
    ],
    version = DB_VERSAO,
    exportSchema = true,
)
abstract class FitDatabase : RoomDatabase() {
    abstract fun alunoDao(): AlunoDao
    abstract fun agendamentoDao(): AgendamentoDao
    abstract fun avaliacaoDao(): AvaliacaoDao
    abstract fun treinoDao(): TreinoDao
    abstract fun catalogoDao(): CatalogoDao
    abstract fun modeloTreinoDao(): ModeloTreinoDao
    abstract fun pagamentoDao(): PagamentoDao
    abstract fun historicoCargaDao(): HistoricoCargaDao
    abstract fun listaEsperaDao(): ListaEsperaDao
    abstract fun videoExercicioDao(): VideoExercicioDao
    abstract fun testeAptidaoDao(): TesteAptidaoDao
    abstract fun retornoDao(): RetornoDao
    abstract fun metaComposicaoDao(): MetaComposicaoDao
}
