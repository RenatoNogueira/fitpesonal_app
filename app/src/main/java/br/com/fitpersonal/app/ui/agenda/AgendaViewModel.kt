package br.com.fitpersonal.app.ui.agenda

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.local.AgendamentoComAluno
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.rules.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ───────────────────────── Lista do dia ─────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val repo: AgendaRepository,
    private val relatorios: br.com.fitpersonal.app.report.RelatorioService,
    private val lembretes: br.com.fitpersonal.app.work.LembreteAgendador,
    perfil: br.com.fitpersonal.app.data.repo.PerfilRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    /** Nome do profissional, para assinar as mensagens de WhatsApp. */
    val profissional: StateFlow<String> = perfil.perfil
        .map { it.nome }
        .estadoSeguro(viewModelScope, "")

    private val _data = MutableStateFlow(
        savedState.get<Long>("dia")?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
    )
    val data: StateFlow<LocalDate> = _data

    private val _saved = savedState

    val agendamentos: StateFlow<List<AgendamentoComAluno>> = _data
        .flatMapLatest { repo.doDia(it) }
        .estadoSeguro(viewModelScope, emptyList())

    fun irPara(d: LocalDate) {
        _data.value = d
        _saved["dia"] = d.toEpochDay()
    }

    fun diaAnterior() = irPara(_data.value.minusDays(1))
    fun proximoDia() = irPara(_data.value.plusDays(1))
    fun hoje() = irPara(LocalDate.now())

    private val _pdf = MutableStateFlow<java.io.File?>(null)
    val pdf: StateFlow<java.io.File?> = _pdf
    private val _erroPdf = MutableStateFlow<String?>(null)
    val erroPdf: StateFlow<String?> = _erroPdf

    fun gerarPdf() {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.agenda(_data.value) }
                .onSuccess { _pdf.value = it }
                .onFailure { _erroPdf.value = "Não foi possível gerar o PDF: ${it.message}" }
        }
    }

    /** Relatório do mês do dia que está sendo exibido. */
    fun gerarRelatorioMes() {
        val mes = java.time.YearMonth.from(_data.value)
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.mensal(mes) }
                .onSuccess { _pdf.value = it }
                .onFailure { _erroPdf.value = "Não foi possível gerar o relatório: ${it.message}" }
        }
    }

    fun pdfConsumido() { _pdf.value = null }
    fun erroLido() { _erroPdf.value = null }

    /** Reaproveita o canal de mensagens da tela (snackbar) para avisos curtos. */
    fun avisarErro(texto: String) { _erroPdf.value = texto }

    fun mudarStatus(id: Long, status: StatusAgendamento) {
        viewModelScope.launch(TratadorDeErros) {
            repo.mudarStatus(id, status)
            repo.buscar(id)?.let { lembretes.agendar(it) }   // cancela se deixou de ser pendente
        }
    }
}

// ───────────────────────── Formulário ─────────────────────────

/** Dados do agendamento que colide, exibidos no aviso de conflito. */
data class ConflitoInfo(val nomeAluno: String, val horario: String)

data class AgendamentoFormState(
    val carregando: Boolean = true,
    val editando: Boolean = false,
    val alunos: List<Aluno> = emptyList(),
    val input: AgendamentoInput = AgendamentoInput(),
    val erros: Map<CampoAgendamento, String> = emptyMap(),
    val conflito: ConflitoInfo? = null,
    val concluido: Boolean = false,
    /** Total de ocorrências semanais ao criar (1 = sem repetição). */
    val ocorrencias: Int = 1,
    /** Resumo da série criada quando algum horário foi pulado por conflito; fecha a tela ao ser lido. */
    val avisoSerie: String? = null,
)

@HiltViewModel
class AgendamentoFormViewModel @Inject constructor(
    private val repo: AgendaRepository,
    private val alunosRepo: AlunoRepository,
    private val lembretes: br.com.fitpersonal.app.work.LembreteAgendador,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAgendamento: Long = savedState.get<Long>("id") ?: 0L
    private val diaArg: Long = savedState.get<Long>("data") ?: 0L
    private val alunoArg: Long = savedState.get<Long>("aluno") ?: 0L
    private var original: Agendamento? = null
    /** Agendamento validado aguardando a confirmação do usuário no aviso de conflito. */
    private var pendente: Agendamento? = null

    private val _state = MutableStateFlow(
        AgendamentoFormState(
            carregando = idAgendamento != 0L,
            editando = idAgendamento != 0L,
            input = AgendamentoInput(
                idAluno = alunoArg.takeIf { it != 0L },
                data = if (diaArg != 0L) LocalDate.ofEpochDay(diaArg) else LocalDate.now(),
            ),
        )
    )
    val state: StateFlow<AgendamentoFormState> = _state

    init {
        viewModelScope.launch(TratadorDeErros) {
            alunosRepo.observar("").collect { lista -> _state.update { it.copy(alunos = lista) } }
        }
        if (idAgendamento != 0L) viewModelScope.launch(TratadorDeErros) {
            original = repo.buscar(idAgendamento)
            _state.update {
                it.copy(carregando = false, input = original?.let(AgendamentoValidator::paraInput) ?: it.input)
            }
        }
    }

    fun alterar(campo: CampoAgendamento? = null, transform: (AgendamentoInput) -> AgendamentoInput) {
        _state.update { s -> s.copy(input = transform(s.input), erros = campo?.let { s.erros - it } ?: s.erros) }
    }

    fun salvar() {
        when (val r = AgendamentoValidator.validar(original, _state.value.input)) {
            is ResultadoAgendamento.Erros -> _state.update { it.copy(erros = r.erros) }
            is ResultadoAgendamento.Ok -> viewModelScope.launch(TratadorDeErros) {
                val novo = r.agendamento
                val colisao = repo.conflito(novo)
                if (colisao == null) {
                    gravar(novo)
                } else {
                    pendente = novo
                    val nome = alunosRepo.buscar(colisao.idAluno)?.nome ?: "outro aluno"
                    _state.update { it.copy(conflito = ConflitoInfo(nome, colisao.horario.toString())) }
                }
            }
        }
    }

    /** O desktop apenas avisa: o usuário pode salvar mesmo assim. */
    fun confirmarConflito() {
        val p = pendente ?: return
        pendente = null
        _state.update { it.copy(conflito = null) }
        viewModelScope.launch(TratadorDeErros) { gravar(p) }
    }

    fun cancelarConflito() {
        pendente = null
        _state.update { it.copy(conflito = null) }
    }

    fun excluir() {
        val a = original ?: return
        viewModelScope.launch(TratadorDeErros) {
            repo.excluir(a)
            lembretes.cancelar(a.id)
            _state.update { it.copy(concluido = true) }
        }
    }

    fun definirOcorrencias(n: Int) = _state.update { it.copy(ocorrencias = n) }

    fun avisoSerieLido() = _state.update { it.copy(avisoSerie = null, concluido = true) }

    private suspend fun gravar(a: Agendamento) {
        val id = repo.salvar(a)
        lembretes.agendar(a.copy(id = id))

        // Série semanal: só ao criar. Datas que já têm outro agendamento no horário são puladas (e avisadas).
        val total = if (original == null) _state.value.ocorrencias else 1
        if (total <= 1) {
            _state.update { it.copy(concluido = true) }
            return
        }
        var criadas = 1
        val puladas = mutableListOf<LocalDate>()
        RecorrenciaRules.datas(a.data, total).drop(1).forEach { dia ->
            val copia = a.copy(id = 0, data = dia, anotacao = null)
            if (repo.conflito(copia) != null) {
                puladas += dia
            } else {
                val novoId = repo.salvar(copia)
                lembretes.agendar(copia.copy(id = novoId))
                criadas++
            }
        }
        if (puladas.isEmpty()) {
            _state.update { it.copy(concluido = true) }
        } else {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM")
            val lista = puladas.joinToString(", ") { it.format(fmt) }
            _state.update {
                it.copy(
                    avisoSerie = "$criadas de $total agendamentos criados. " +
                        "Pulados por já haver outro agendamento no horário: $lista.",
                )
            }
        }
    }
}
