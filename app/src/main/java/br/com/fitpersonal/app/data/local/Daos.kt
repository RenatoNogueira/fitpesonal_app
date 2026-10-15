package br.com.fitpersonal.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlunoDao {
    @Query("SELECT * FROM alunos WHERE nome LIKE '%' || :busca || '%' ORDER BY nome")
    fun observar(busca: String): Flow<List<AlunoEntity>>

    @Query("SELECT * FROM alunos WHERE id = :id")
    suspend fun buscar(id: Long): AlunoEntity?

    @Query("SELECT * FROM alunos")
    suspend fun todosSnapshot(): List<AlunoEntity>

    @Query("UPDATE alunos SET refContrato = :ref WHERE id = :id")
    suspend fun definirRefContrato(id: Long, ref: String)

    @Query("SELECT COUNT(*) FROM alunos")
    fun total(): Flow<Int>

    @Query("SELECT * FROM alunos WHERE dataNascimento IS NOT NULL AND substr(dataNascimento, 6, 2) = :mes ORDER BY substr(dataNascimento, 9, 2)")
    fun aniversariantes(mes: String): Flow<List<AlunoEntity>>

    @Insert suspend fun inserir(e: AlunoEntity): Long
    @Update suspend fun atualizar(e: AlunoEntity)
    @Delete suspend fun excluir(e: AlunoEntity)
}

data class UltimaAvaliacao(val idAluno: Long, val ultima: String)

data class ContagemTreino(val idTreino: Long, val total: Int)

data class TreinoComAluno(
    val id: Long, val idAluno: Long, val nome: String, val objetivo: String?, val nomeAluno: String,
)

data class AgendamentoComAluno(
    val id: Long, val idAluno: Long, val data: String, val horario: String,
    val tipo: String, val status: String, val nomeAluno: String, val cpfAluno: String?,
    val telefoneAluno: String? = null,
)

@Dao
interface AgendamentoDao {
    @Query(
        """SELECT a.id, a.idAluno, a.data, a.horario, a.tipo, a.status, al.nome AS nomeAluno, al.cpf AS cpfAluno, al.telefone AS telefoneAluno
           FROM agendamentos a JOIN alunos al ON al.id = a.idAluno
           WHERE a.data = :data ORDER BY a.horario"""
    )
    fun doDia(data: String): Flow<List<AgendamentoComAluno>>

    @Query(
        """SELECT a.id, a.idAluno, a.data, a.horario, a.tipo, a.status, al.nome AS nomeAluno, al.cpf AS cpfAluno, al.telefone AS telefoneAluno
           FROM agendamentos a JOIN alunos al ON al.id = a.idAluno
           WHERE a.data >= :hoje ORDER BY a.data, a.horario LIMIT :limite"""
    )
    fun proximos(hoje: String, limite: Int): Flow<List<AgendamentoComAluno>>

    /** Todos os agendamentos de um mês ("yyyy-MM"), para o relatório mensal. */
    @Query(
        """SELECT a.id, a.idAluno, a.data, a.horario, a.tipo, a.status, al.nome AS nomeAluno, al.cpf AS cpfAluno, al.telefone AS telefoneAluno
           FROM agendamentos a JOIN alunos al ON al.id = a.idAluno
           WHERE substr(a.data, 1, 7) = :anoMes ORDER BY a.data, a.horario"""
    )
    suspend fun doMes(anoMes: String): List<AgendamentoComAluno>

    @Query("SELECT * FROM agendamentos WHERE data = :data AND horario = :horario AND status != 'CANCELADO'")
    suspend fun nesseHorario(data: String, horario: String): List<AgendamentoEntity>

    @Query("SELECT * FROM agendamentos ORDER BY data DESC, horario DESC")
    fun todos(): Flow<List<AgendamentoEntity>>

    @Query("SELECT * FROM agendamentos ORDER BY data, horario")
    suspend fun todosSnapshot(): List<AgendamentoEntity>

    /** Agendamentos concluídos (base do saldo de pacotes e das metas do mês). */
    @Query("SELECT * FROM agendamentos WHERE status = 'CONCLUIDO'")
    fun concluidos(): Flow<List<AgendamentoEntity>>

    @Query("SELECT * FROM agendamentos WHERE id = :id")
    suspend fun buscar(id: Long): AgendamentoEntity?

    @Query("SELECT * FROM agendamentos WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<AgendamentoEntity>

    @Query("SELECT * FROM agendamentos WHERE idAluno = :idAluno ORDER BY data DESC, horario DESC")
    fun doAluno(idAluno: Long): Flow<List<AgendamentoEntity>>

    @Query("SELECT * FROM agendamentos WHERE data >= :hoje AND status = 'PENDENTE'")
    suspend fun pendentesAPartir(hoje: String): List<AgendamentoEntity>

    @Insert suspend fun inserir(e: AgendamentoEntity): Long
    @Update suspend fun atualizar(e: AgendamentoEntity)
    @Delete suspend fun excluir(e: AgendamentoEntity)
}

@Dao
interface AvaliacaoDao {
    @Query("SELECT * FROM avaliacoes_fisicas WHERE idAluno = :idAluno ORDER BY data DESC, id DESC")
    fun doAluno(idAluno: Long): Flow<List<AvaliacaoEntity>>

    @Query("SELECT * FROM avaliacoes_fisicas ORDER BY data DESC, id DESC")
    fun todas(): Flow<List<AvaliacaoEntity>>

    @Query("SELECT * FROM avaliacoes_fisicas ORDER BY data, id")
    suspend fun todosSnapshot(): List<AvaliacaoEntity>

    @Query("SELECT * FROM avaliacoes_fisicas WHERE id = :id")
    suspend fun buscar(id: Long): AvaliacaoEntity?

    @Query("SELECT * FROM avaliacoes_fisicas WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<AvaliacaoEntity>

    @Query("SELECT COUNT(*) FROM avaliacoes_fisicas")
    fun total(): Flow<Int>

    /** Data da avaliação mais recente de cada aluno (alunos sem avaliação não aparecem). */
    @Query("SELECT idAluno AS idAluno, MAX(data) AS ultima FROM avaliacoes_fisicas GROUP BY idAluno")
    fun ultimasPorAluno(): Flow<List<UltimaAvaliacao>>

    @Query("SELECT COUNT(*) FROM avaliacoes_fisicas WHERE substr(data, 1, 7) = :anoMes")
    fun totalNoMes(anoMes: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM avaliacoes_fisicas WHERE substr(data, 1, 7) = :anoMes")
    suspend fun contarNoMes(anoMes: String): Int

    @Insert suspend fun inserir(e: AvaliacaoEntity): Long
    @Update suspend fun atualizar(e: AvaliacaoEntity)
    @Delete suspend fun excluir(e: AvaliacaoEntity)
}

@Dao
interface TreinoDao {
    @Query("SELECT * FROM treinos WHERE idAluno = :idAluno ORDER BY id DESC")
    fun doAluno(idAluno: Long): Flow<List<TreinoEntity>>

    @Query(
        """SELECT t.id, t.idAluno, t.nome, t.objetivo, al.nome AS nomeAluno
           FROM treinos t JOIN alunos al ON al.id = t.idAluno ORDER BY t.nome COLLATE NOCASE"""
    )
    fun todosComAluno(): Flow<List<TreinoComAluno>>

    @Query("SELECT idTreino AS idTreino, COUNT(*) AS total FROM exercicios_treino GROUP BY idTreino")
    fun contagemPorTreino(): Flow<List<ContagemTreino>>

    @Query("UPDATE treinos SET contratoId = :contratoId, contratoRevisao = :revisao WHERE id = :id")
    suspend fun definirContrato(id: Long, contratoId: String, revisao: Int)

    @Query("SELECT COUNT(*) FROM treinos")
    fun total(): Flow<Int>

    @Query("SELECT * FROM treinos WHERE id = :id")
    suspend fun buscar(id: Long): TreinoEntity?

    @Query("SELECT * FROM treinos WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<TreinoEntity>

    @Query("SELECT * FROM exercicios_treino WHERE idTreino = :idTreino ORDER BY divisao, id")
    fun exercicios(idTreino: Long): Flow<List<ExercicioTreinoEntity>>

    @Query("SELECT * FROM exercicios_treino WHERE idTreino = :idTreino ORDER BY id")
    suspend fun exerciciosSnapshot(idTreino: Long): List<ExercicioTreinoEntity>

    @Insert suspend fun inserir(e: TreinoEntity): Long
    @Update suspend fun atualizar(e: TreinoEntity)
    @Delete suspend fun excluir(e: TreinoEntity)

    @Insert suspend fun inserirExercicio(e: ExercicioTreinoEntity): Long
    @Insert suspend fun inserirExercicios(e: List<ExercicioTreinoEntity>)
    @Update suspend fun atualizarExercicio(e: ExercicioTreinoEntity)
    @Delete suspend fun excluirExercicio(e: ExercicioTreinoEntity)
}

@Dao
interface CatalogoDao {
    @Query("SELECT COUNT(*) FROM exercicios_catalogo")
    suspend fun total(): Int

    @Query(
        """SELECT * FROM exercicios_catalogo
           WHERE nome LIKE '%' || :termo || '%' OR categoria LIKE '%' || :termo || '%'
              OR musculosPrimarios LIKE '%' || :termo || '%' OR equipamento LIKE '%' || :termo || '%'
           ORDER BY nome LIMIT :limite OFFSET :offset"""
    )
    suspend fun buscar(termo: String, limite: Int, offset: Int): List<ExercicioCatalogoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTodos(itens: List<ExercicioCatalogoEntity>)
}

data class ModeloComTotal(
    val id: Long,
    val nome: String,
    val objetivo: String?,
    val dataCriacao: String,
    val total: Int,
)

@Dao
interface ModeloTreinoDao {
    @Query(
        """SELECT m.id, m.nome, m.objetivo, m.dataCriacao,
                  (SELECT COUNT(*) FROM exercicios_modelo e WHERE e.idModelo = m.id) AS total
           FROM modelos_treino m ORDER BY m.nome COLLATE NOCASE"""
    )
    fun todos(): Flow<List<ModeloComTotal>>

    @Query("SELECT * FROM modelos_treino WHERE id = :id")
    suspend fun buscar(id: Long): ModeloTreinoEntity?

    @Query("SELECT * FROM exercicios_modelo WHERE idModelo = :idModelo ORDER BY id")
    suspend fun exercicios(idModelo: Long): List<ModeloExercicioEntity>

    @Insert suspend fun inserir(e: ModeloTreinoEntity): Long
    @Insert suspend fun inserirExercicios(e: List<ModeloExercicioEntity>)

    @Query("DELETE FROM modelos_treino WHERE id = :id")
    suspend fun excluir(id: Long)
}

data class PagamentoComAluno(
    val id: Long,
    val idAluno: Long,
    val nomeAluno: String,
    val competencia: String,
    val valorCentavos: Long,
    val pagoEm: String,
)

@Dao
interface PagamentoDao {
    @Query("SELECT * FROM pagamentos WHERE idAluno = :idAluno ORDER BY competencia DESC, id DESC")
    fun doAluno(idAluno: Long): Flow<List<PagamentoEntity>>

    @Query("SELECT * FROM pagamentos WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<PagamentoEntity>

    @Query("SELECT * FROM pagamentos WHERE id = :id")
    suspend fun buscar(id: Long): PagamentoEntity?

    @Query("SELECT * FROM pagamentos")
    fun todos(): Flow<List<PagamentoEntity>>

    @Query("SELECT * FROM pagamentos ORDER BY competencia, id")
    suspend fun todosSnapshot(): List<PagamentoEntity>

    @Query(
        """SELECT p.id, p.idAluno, al.nome AS nomeAluno, p.competencia, p.valorCentavos, p.pagoEm
           FROM pagamentos p JOIN alunos al ON al.id = p.idAluno
           WHERE p.competencia = :competencia ORDER BY al.nome COLLATE NOCASE"""
    )
    suspend fun daCompetencia(competencia: String): List<PagamentoComAluno>

    @Insert suspend fun inserir(e: PagamentoEntity): Long

    @Query("DELETE FROM pagamentos WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface HistoricoCargaDao {
    @Query("SELECT * FROM historico_carga WHERE idAluno = :idAluno ORDER BY data, id")
    fun doAluno(idAluno: Long): Flow<List<HistoricoCargaEntity>>

    @Query("SELECT * FROM historico_carga WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<HistoricoCargaEntity>

    @Query(
        """SELECT * FROM historico_carga WHERE idAluno = :idAluno AND chave = :chave
           ORDER BY data DESC, id DESC LIMIT 1"""
    )
    suspend fun ultimo(idAluno: Long, chave: String): HistoricoCargaEntity?

    @Insert suspend fun inserir(e: HistoricoCargaEntity): Long
}

@Dao
interface ListaEsperaDao {
    @Query("SELECT * FROM lista_espera ORDER BY diaSemana, horario, id")
    fun todos(): Flow<List<ListaEsperaEntity>>

    @Query("SELECT * FROM lista_espera WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<ListaEsperaEntity>

    @Insert suspend fun inserir(e: ListaEsperaEntity): Long

    @Query("DELETE FROM lista_espera WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface TesteAptidaoDao {
    @Query("SELECT * FROM testes_aptidao WHERE idAluno = :idAluno ORDER BY data DESC, id DESC")
    fun doAluno(idAluno: Long): Flow<List<TesteAptidaoEntity>>

    @Query("SELECT * FROM testes_aptidao WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<TesteAptidaoEntity>

    @Query("SELECT * FROM testes_aptidao ORDER BY data, id")
    suspend fun todosSnapshot(): List<TesteAptidaoEntity>

    @Insert suspend fun inserir(e: TesteAptidaoEntity): Long

    @Query("DELETE FROM testes_aptidao WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface RetornoDao {
    @Query("SELECT * FROM retornos_aluno WHERE idAluno = :idAluno ORDER BY importadoEm DESC, id DESC")
    fun doAluno(idAluno: Long): Flow<List<RetornoAlunoEntity>>

    @Query("SELECT * FROM retornos_aluno WHERE idAluno = :idAluno")
    suspend fun doAlunoSnapshot(idAluno: Long): List<RetornoAlunoEntity>

    @Insert suspend fun inserir(e: RetornoAlunoEntity): Long

    @Query("SELECT COUNT(*) FROM retornos_aluno WHERE resumoId = :resumoId")
    suspend fun contarResumo(resumoId: String): Int

    @Query("SELECT execucaoId FROM execucoes_importadas WHERE execucaoId IN (:ids)")
    suspend fun jaImportadas(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrarExecucoes(e: List<ExecucaoImportadaEntity>)
}

@Dao
interface VideoExercicioDao {
    @Query("SELECT * FROM videos_exercicio ORDER BY nome COLLATE NOCASE")
    fun todos(): Flow<List<VideoExercicioEntity>>

    @Query("SELECT * FROM videos_exercicio WHERE chave = :chave")
    suspend fun buscar(chave: String): VideoExercicioEntity?

    @Query("SELECT * FROM videos_exercicio")
    suspend fun todosSnapshot(): List<VideoExercicioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(e: VideoExercicioEntity)

    @Query("DELETE FROM videos_exercicio WHERE chave = :chave")
    suspend fun excluir(chave: String)
}
