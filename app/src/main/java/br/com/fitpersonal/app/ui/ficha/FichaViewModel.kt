package br.com.fitpersonal.app.ui.ficha

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.data.repo.PagamentoRepository
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.data.repo.TreinoRepository
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.Pagamento
import br.com.fitpersonal.app.domain.model.Treino
import br.com.fitpersonal.app.domain.rules.Metrica
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class FichaState(
    val carregado: Boolean = false,
    val aluno: Aluno? = null,
    val foto: File? = null,
    /** Mais recente primeiro. */
    val avaliacoes: List<AvaliacaoFisica> = emptyList(),
    val treinos: List<Treino> = emptyList(),
    val agendamentos: List<Agendamento> = emptyList(),
    val pagamentos: List<Pagamento> = emptyList(),
)

@HiltViewModel
class FichaViewModel @Inject constructor(
    private val alunos: AlunoRepository,
    avaliacoes: AvaliacaoRepository,
    treinos: TreinoRepository,
    agenda: AgendaRepository,
    private val pagamentos: PagamentoRepository,
    perfil: PerfilRepository,
    private val fotos: FotoStorage,
    private val relatorios: br.com.fitpersonal.app.report.RelatorioService,
    private val resumos: br.com.fitpersonal.app.data.contrato.ResumoImportService,
    retornoDao: br.com.fitpersonal.app.data.local.RetornoDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val _pdf = kotlinx.coroutines.flow.MutableStateFlow<File?>(null)
    /** PDF gerado (recibo) aguardando a pré-visualização. */
    val pdf: StateFlow<File?> = _pdf
    private val _erro = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    fun gerarRecibo(idPagamento: Long) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.recibo(idPagamento) }
                .onSuccess { _pdf.value = it }
                .onFailure { _erro.value = "Não foi possível gerar o recibo: ${it.message}" }
        }
    }

    fun pdfConsumido() { _pdf.value = null }

    // ── retornos do aluno (resumos importados) ──
    private val _previa = kotlinx.coroutines.flow.MutableStateFlow<br.com.fitpersonal.app.data.contrato.PreviaResumo?>(null)
    val previa: StateFlow<br.com.fitpersonal.app.data.contrato.PreviaResumo?> = _previa

    val retornos: StateFlow<List<br.com.fitpersonal.app.data.local.RetornoAlunoEntity>> =
        retornoDao.doAluno(savedState.get<Long>("id") ?: 0L)
            .estadoSeguro(viewModelScope, emptyList())

    fun analisarResumo(texto: String) {
        viewModelScope.launch(TratadorDeErros) { _previa.value = resumos.analisar(texto, idAlunoFicha) }
    }

    fun limparPrevia() { _previa.value = null }

    fun importarResumo() {
        val p = _previa.value as? br.com.fitpersonal.app.data.contrato.PreviaResumo.Ok ?: return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { resumos.importar(p.resumo, idAlunoFicha) }
                .onSuccess { r ->
                    _erro.value = "Resumo importado: ${r.sessoes} sessão(ões) nova(s), ${r.cargasRegistradas} carga(s) no histórico."
                }
                .onFailure { _erro.value = "Não foi possível importar: ${it.message}" }
            _previa.value = null
        }
    }
    fun erroLido() { _erro.value = null }

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L
    private val idAlunoFicha: Long get() = idAluno

    val state: StateFlow<FichaState> = combine(
        alunos.observar("").map { l -> l.firstOrNull { it.id == idAluno } },
        avaliacoes.doAluno(idAluno),
        treinos.doAluno(idAluno),
        agenda.doAluno(idAluno),
        pagamentos.doAluno(idAluno),
    ) { aluno, av, tr, ag, pg ->
        FichaState(
            carregado = true,
            aluno = aluno,
            foto = aluno?.fotoPerfil?.let { fotos.arquivo(it) }?.takeIf { it.exists() },
            avaliacoes = av,
            treinos = tr,
            agendamentos = ag,
            pagamentos = pg,
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).estadoSeguro(viewModelScope, FichaState())

    /** Nome do profissional, usado para assinar as mensagens de WhatsApp. */
    val profissional: StateFlow<String> = perfil.perfil
        .map { it.nome }
        .estadoSeguro(viewModelScope, "")

    /** Perfil completo (nome, cidade e chave Pix) para montar o Pix das cobranças. */
    val perfilCompleto: StateFlow<br.com.fitpersonal.app.domain.model.PerfilProfissional> = perfil.perfil
        .estadoSeguro(viewModelScope, br.com.fitpersonal.app.domain.model.PerfilProfissional())

    fun salvarPlano(metrica: Metrica?, valor: Double?, reavaliacaoDias: Int?) {
        viewModelScope.launch(TratadorDeErros) {
            val meta = if (metrica != null && valor != null) metrica.name to valor else null
            alunos.atualizarPlano(idAluno, meta?.first, meta?.second, reavaliacaoDias)
        }
    }

    /** Inicia um pacote novo hoje (zera o consumo); null remove o pacote. */
    fun salvarPacote(sessoes: Int?) {
        viewModelScope.launch(TratadorDeErros) { alunos.atualizarPacote(idAluno, sessoes, LocalDate.now()) }
    }

    fun salvarMensalidade(centavos: Long?, dia: Int?) {
        viewModelScope.launch(TratadorDeErros) { alunos.atualizarMensalidade(idAluno, centavos, dia) }
    }

    fun registrarPagamento(competencia: YearMonth, valorCentavos: Long) {
        viewModelScope.launch(TratadorDeErros) {
            pagamentos.registrar(
                Pagamento(idAluno = idAluno, competencia = competencia, valorCentavos = valorCentavos, pagoEm = LocalDate.now())
            )
        }
    }

    fun excluirPagamento(id: Long) {
        viewModelScope.launch(TratadorDeErros) { pagamentos.excluir(id) }
    }
}
