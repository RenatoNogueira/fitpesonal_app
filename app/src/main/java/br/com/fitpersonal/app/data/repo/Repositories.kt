package br.com.fitpersonal.app.data.repo

import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import br.com.fitpersonal.app.data.local.*
import br.com.fitpersonal.app.domain.model.*
import br.com.fitpersonal.app.domain.rules.AgendaRules
import br.com.fitpersonal.app.domain.rules.CargaRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlunoRepository @Inject constructor(private val dao: AlunoDao) {
    fun observar(busca: String = ""): Flow<List<Aluno>> = dao.observar(busca).map { l -> l.map { it.toDomain() } }
    suspend fun buscar(id: Long) = dao.buscar(id)?.toDomain()
    suspend fun salvar(a: Aluno): Long =
        if (a.id == 0L) dao.inserir(a.toEntity()) else { dao.atualizar(a.toEntity()); a.id }
    suspend fun excluir(a: Aluno) = dao.excluir(a.toEntity())

    /** Atualiza só a meta e o intervalo de reavaliação, sem tocar no cadastro. */
    suspend fun atualizarPlano(id: Long, metaMetrica: String?, metaValor: Double?, reavaliacaoDias: Int?) {
        val atual = dao.buscar(id) ?: return
        dao.atualizar(atual.copy(metaMetrica = metaMetrica, metaValor = metaValor, reavaliacaoDias = reavaliacaoDias))
    }

    /** Novo pacote de [sessoes] contado a partir de [inicio]; sessoes nulo/zero remove o pacote. */
    suspend fun atualizarPacote(id: Long, sessoes: Int?, inicio: LocalDate?) {
        val atual = dao.buscar(id) ?: return
        val valido = sessoes != null && sessoes > 0 && inicio != null
        dao.atualizar(
            atual.copy(
                pacoteSessoes = if (valido) sessoes else null,
                pacoteInicio = if (valido) inicio.toString() else null,
            )
        )
    }

    /** Define (ou limpa, com nulls) a mensalidade e o dia de vencimento. */
    suspend fun atualizarMensalidade(id: Long, centavos: Long?, diaVencimento: Int?) {
        val atual = dao.buscar(id) ?: return
        val valido = centavos != null && diaVencimento != null
        dao.atualizar(
            atual.copy(
                mensalidadeCentavos = if (valido) centavos else null,
                diaVencimento = if (valido) diaVencimento else null,
            )
        )
    }
}

@Singleton
class AgendaRepository @Inject constructor(
    private val dao: AgendamentoDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: android.content.Context,
) {
    private val escopoWidget = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default,
    )

    /** Mantém o widget "Agenda de hoje" em dia a cada alteração (falha silenciosa: é só conveniência). */
    private fun atualizarWidget() {
        escopoWidget.launch {
            runCatching { br.com.fitpersonal.app.widget.AgendaWidget().updateAll(ctx) }
        }
    }

    fun doDia(data: LocalDate) = dao.doDia(data.toString())
    fun proximos(hoje: LocalDate = LocalDate.now(), limite: Int = 20) = dao.proximos(hoje.toString(), limite)
    fun doAluno(idAluno: Long): Flow<List<Agendamento>> =
        dao.doAluno(idAluno).map { l -> l.map { it.toDomain() } }

    suspend fun doMes(mes: java.time.YearMonth) = dao.doMes(mes.toString())

    /** Todos os agendamentos, do mais recente ao mais antigo (busca global). */
    fun todos(): Flow<List<Agendamento>> = dao.todos().map { l -> l.map { it.toDomain() } }

    /** Todos os agendamentos concluídos (saldo de pacotes e metas do mês). */
    fun concluidos(): Flow<List<Agendamento>> = dao.concluidos().map { l -> l.map { it.toDomain() } }

    /** Retorna o agendamento em conflito (para a UI confirmar) ou null. */
    suspend fun conflito(novo: Agendamento): Agendamento? {
        val mesmos = dao.nesseHorario(novo.data.toString(), novo.horario.toString()).map { it.toDomain() }
        return AgendaRules.conflito(novo, mesmos)
    }

    suspend fun buscar(id: Long) = dao.buscar(id)?.toDomain()

    suspend fun pendentesAPartir(hoje: LocalDate) = dao.pendentesAPartir(hoje.toString()).map { it.toDomain() }

    suspend fun salvar(a: Agendamento): Long {
        val id = if (a.id == 0L) dao.inserir(a.toEntity()) else { dao.atualizar(a.toEntity()); a.id }
        atualizarWidget()
        return id
    }

    suspend fun excluir(a: Agendamento) {
        dao.excluir(a.toEntity())
        atualizarWidget()
    }

    suspend fun mudarStatus(id: Long, status: StatusAgendamento) {
        dao.buscar(id)?.let { dao.atualizar(it.copy(status = status.name)) }
        atualizarWidget()
    }
}

@Singleton
class AvaliacaoRepository @Inject constructor(private val dao: AvaliacaoDao) {
    fun doAluno(idAluno: Long) = dao.doAluno(idAluno).map { l -> l.map { it.toDomain() } }
    suspend fun contarNoMes(mes: java.time.YearMonth): Int = dao.contarNoMes(mes.toString())

    /** Todas as avaliações, da mais recente à mais antiga (busca global). */
    fun todas(): Flow<List<AvaliacaoFisica>> = dao.todas().map { l -> l.map { it.toDomain() } }

    /** Data da avaliação mais recente por aluno (alunos sem avaliação não aparecem no mapa). */
    fun ultimasPorAluno(): Flow<Map<Long, LocalDate>> =
        dao.ultimasPorAluno().map { l -> l.associate { it.idAluno to LocalDate.parse(it.ultima) } }
    suspend fun buscar(id: Long) = dao.buscar(id)?.toDomain()
    suspend fun salvar(a: AvaliacaoFisica): Long =
        if (a.id == 0L) dao.inserir(a.toEntity()) else { dao.atualizar(a.toEntity()); a.id }
    suspend fun excluir(a: AvaliacaoFisica) = dao.excluir(a.toEntity())
}

@Singleton
class TreinoRepository @Inject constructor(
    private val dao: TreinoDao,
    private val db: FitDatabase,
    private val historicoCarga: HistoricoCargaDao,
) {
    suspend fun buscar(id: Long) = dao.buscar(id)?.toDomain()

    /** Guarda só as mudanças de carga (kg) por exercício, para o gráfico e a sugestão de progressão. */
    private suspend fun registrarCargas(idAluno: Long, exercicios: List<ExercicioTreino>) {
        val hoje = LocalDate.now().toString()
        exercicios.forEach { e ->
            val kg = CargaRules.parseKg(e.carga) ?: return@forEach
            val chave = CargaRules.chaveExercicio(e.nome)
            if (chave.isEmpty()) return@forEach
            val ultimo = historicoCarga.ultimo(idAluno, chave)
            if (ultimo == null || ultimo.cargaKg != kg) {
                historicoCarga.inserir(
                    HistoricoCargaEntity(
                        idAluno = idAluno, exercicio = e.nome.trim(), chave = chave, data = hoje, cargaKg = kg,
                    )
                )
            }
        }
    }
    suspend fun exerciciosDe(idTreino: Long) = dao.exerciciosSnapshot(idTreino).map { it.toDomain() }

    /** Modo treino: grava a nova carga do exercício e registra no histórico se o valor em kg mudou. */
    suspend fun atualizarCarga(idAluno: Long, exercicio: ExercicioTreino, novaCarga: String) {
        val atualizado = exercicio.copy(carga = novaCarga.trim().ifEmpty { null })
        dao.atualizarExercicio(atualizado.toEntity())
        registrarCargas(idAluno, listOf(atualizado))
    }
    fun todosComAluno() = dao.todosComAluno()

    /** Quantidade de exercícios por treino (treinos sem exercício não aparecem). */
    fun contagemExercicios(): Flow<Map<Long, Int>> =
        dao.contagemPorTreino().map { l -> l.associate { it.idTreino to it.total } }

    /**
     * Grava treino + exercícios numa transação (tudo ou nada). Exercícios com id 0 são inseridos,
     * os demais atualizados, e os que sumiram da lista são removidos.
     */
    suspend fun salvarCompleto(t: Treino, exercicios: List<ExercicioTreino>): Long = db.withTransaction {
        val id = if (t.id == 0L) dao.inserir(t.toEntity()) else { dao.atualizar(t.toEntity()); t.id }
        val manter = exercicios.map { it.id }.filter { it != 0L }.toSet()
        dao.exerciciosSnapshot(id).filter { it.id !in manter }.forEach { dao.excluirExercicio(it) }
        exercicios.forEach { e ->
            val ent = e.copy(idTreino = id).toEntity()
            if (e.id == 0L) dao.inserirExercicio(ent) else dao.atualizarExercicio(ent)
        }
        registrarCargas(t.idAluno, exercicios)
        id
    }

    fun doAluno(idAluno: Long) = dao.doAluno(idAluno).map { l -> l.map { it.toDomain() } }
    fun exercicios(idTreino: Long) = dao.exercicios(idTreino).map { l -> l.map { it.toDomain() } }
    suspend fun salvar(t: Treino): Long =
        if (t.id == 0L) dao.inserir(t.toEntity()) else { dao.atualizar(t.toEntity()); t.id }
    suspend fun excluir(t: Treino) = dao.excluir(t.toEntity())
    suspend fun salvarExercicio(e: ExercicioTreino) {
        if (e.id == 0L) dao.inserirExercicio(e.toEntity()) else dao.atualizarExercicio(e.toEntity())
    }
    suspend fun excluirExercicio(e: ExercicioTreino) = dao.excluirExercicio(e.toEntity())

    /** Adiciona vários exercícios do catálogo a uma divisão (fluxo "Adicionar selecionados" do desktop). */
    suspend fun adicionarDoCatalogo(idTreino: Long, itens: List<ExercicioCatalogo>, divisao: DivisaoTreino) {
        dao.inserirExercicios(itens.map { ExercicioTreino(idTreino = idTreino, nome = it.nome, divisao = divisao).toEntity() })
    }

    suspend fun duplicar(idTreino: Long, idAlunoDestino: Long? = null): Long? {
        val origem = dao.buscar(idTreino) ?: return null
        val novoId = dao.inserir(
            origem.copy(id = 0, idAluno = idAlunoDestino ?: origem.idAluno, nome = "${origem.nome} (cópia)",
                dataCriacao = LocalDate.now().toString(), contratoId = null, contratoRevisao = 0)
        )
        dao.inserirExercicios(dao.exerciciosSnapshot(idTreino).map { it.copy(id = 0, idTreino = novoId) })
        return novoId
    }
}

@Singleton
class CatalogoRepository @Inject constructor(private val dao: CatalogoDao) {
    suspend fun buscar(termo: String, offset: Int = 0) =
        if (termo.isBlank()) emptyList() else dao.buscar(termo.trim(), limite = 100, offset = offset).map { it.toDomain() }
}

/** Biblioteca de treinos modelo: treinos sem aluno que viram um treino novo ao serem aplicados. */
@Singleton
class ModeloTreinoRepository @Inject constructor(
    private val modelos: ModeloTreinoDao,
    private val treinos: TreinoDao,
    private val db: FitDatabase,
) {
    val todos: Flow<List<ModeloTreino>> = modelos.todos().map { l ->
        l.map { ModeloTreino(it.id, it.nome, it.objetivo, LocalDate.parse(it.dataCriacao), it.total) }
    }

    /** Copia o treino e seus exercícios para a biblioteca. Devolve o id do modelo ou null se o treino não existe. */
    suspend fun salvarComoModelo(idTreino: Long, nome: String): Long? = db.withTransaction {
        val origem = treinos.buscar(idTreino) ?: return@withTransaction null
        val id = modelos.inserir(
            ModeloTreinoEntity(nome = nome.trim().ifEmpty { origem.nome }, objetivo = origem.objetivo,
                dataCriacao = LocalDate.now().toString())
        )
        modelos.inserirExercicios(
            treinos.exerciciosSnapshot(idTreino).map {
                ModeloExercicioEntity(
                    idModelo = id, nome = it.nome, divisao = it.divisao, series = it.series,
                    repeticoes = it.repeticoes, carga = it.carga, descanso = it.descanso, observacoes = it.observacoes,
                    video = it.video,
                )
            }
        )
        id
    }

    /** Cria um treino novo para o aluno a partir do modelo. Devolve o id do treino ou null se o modelo não existe. */
    suspend fun aplicar(idModelo: Long, idAluno: Long): Long? = db.withTransaction {
        val m = modelos.buscar(idModelo) ?: return@withTransaction null
        val novo = treinos.inserir(
            TreinoEntity(idAluno = idAluno, nome = m.nome, objetivo = m.objetivo, dataCriacao = LocalDate.now().toString())
        )
        treinos.inserirExercicios(
            modelos.exercicios(idModelo).map {
                ExercicioTreinoEntity(
                    idTreino = novo, nome = it.nome, divisao = it.divisao, series = it.series,
                    repeticoes = it.repeticoes, carga = it.carga, descanso = it.descanso, observacoes = it.observacoes,
                    video = it.video,
                )
            }
        )
        novo
    }

    suspend fun excluir(idModelo: Long) = modelos.excluir(idModelo)
}

@Singleton
class PagamentoRepository @Inject constructor(private val dao: PagamentoDao) {
    fun doAluno(idAluno: Long): Flow<List<Pagamento>> =
        dao.doAluno(idAluno).map { l -> l.map { it.toDomain() } }

    fun todos(): Flow<List<Pagamento>> = dao.todos().map { l -> l.map { it.toDomain() } }

    suspend fun registrar(p: Pagamento): Long = dao.inserir(p.toEntity())
    suspend fun buscar(id: Long): Pagamento? = dao.buscar(id)?.toDomain()
    suspend fun excluir(id: Long) = dao.excluir(id)
    suspend fun daCompetencia(c: java.time.YearMonth) = dao.daCompetencia(c.toString())
}

@Singleton
class ListaEsperaRepository @Inject constructor(private val dao: ListaEsperaDao) {
    val todos: Flow<List<ListaEspera>> = dao.todos().map { l -> l.map { it.toDomain() } }
    suspend fun adicionar(e: ListaEspera): Long = dao.inserir(e.toEntity())
    suspend fun remover(id: Long) = dao.excluir(id)
}

@Singleton
class TesteAptidaoRepository @Inject constructor(private val dao: TesteAptidaoDao) {
    fun doAluno(idAluno: Long): Flow<List<br.com.fitpersonal.app.domain.rules.TesteAptidao>> =
        dao.doAluno(idAluno).map { l -> l.map { it.toDomain() } }

    suspend fun registrar(t: br.com.fitpersonal.app.domain.rules.TesteAptidao): Long = dao.inserir(t.toEntity())
    suspend fun excluir(id: Long) = dao.excluir(id)
}

@Singleton
class MetaComposicaoRepository @Inject constructor(private val dao: MetaComposicaoDao) {
    fun doAluno(idAluno: Long): Flow<List<br.com.fitpersonal.app.domain.rules.MetaComposicao>> =
        dao.doAluno(idAluno).map { l ->
            l.mapNotNull { e ->
                val m = br.com.fitpersonal.app.domain.rules.Metrica.entries.firstOrNull { it.name == e.metrica }
                    ?: return@mapNotNull null   // métrica removida em versão futura: ignora
                br.com.fitpersonal.app.domain.rules.MetaComposicao(e.idAluno, m, e.alvo)
            }
        }

    suspend fun definir(idAluno: Long, metrica: br.com.fitpersonal.app.domain.rules.Metrica, alvo: Double) =
        dao.salvar(MetaComposicaoEntity(idAluno, metrica.name, alvo))

    suspend fun remover(idAluno: Long, metrica: br.com.fitpersonal.app.domain.rules.Metrica) =
        dao.excluir(idAluno, metrica.name)
}
