package br.com.fitpersonal.app.ui.alunos

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.privacidade.PrivacidadeService
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.rules.BuscaRules
import br.com.fitpersonal.app.domain.rules.FiltroAluno
import br.com.fitpersonal.app.domain.rules.ReavaliacaoRules
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import java.time.LocalDate
import br.com.fitpersonal.app.domain.rules.AlunoInput
import br.com.fitpersonal.app.domain.rules.AlunoValidator
import br.com.fitpersonal.app.domain.rules.CampoAluno
import br.com.fitpersonal.app.domain.rules.ResultadoAluno
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Linha da lista de alunos com os indicadores já calculados. */
data class AlunoItemUi(
    val aluno: Aluno,
    val ultimaAvaliacao: LocalDate?,
    val reavaliacao: SituacaoReavaliacao?,
    val aniversarioHoje: Boolean,
)

/** Contagens do cabeçalho (sobre todos os alunos, sem busca nem filtro). */
data class ResumoAlunos(
    val total: Int = 0,
    val reavaliacao: Int = 0,
    val aniversarioNoMes: Int = 0,
    val semAvaliacao: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class AlunosViewModel @Inject constructor(
    repo: AlunoRepository,
    avaliacoes: AvaliacaoRepository,
    private val fotos: FotoStorage,
) : ViewModel() {
    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca

    private val _filtro = MutableStateFlow(FiltroAluno.TODOS)
    val filtro: StateFlow<FiltroAluno> = _filtro

    /** Busca por nome, objetivo, telefone, e-mail ou Instagram (sem acento) + filtro rápido. */
    val alunos: StateFlow<List<AlunoItemUi>> = combine(
        _busca.debounce(250),
        _filtro,
        repo.observar(""),
        avaliacoes.ultimasPorAluno(),
    ) { termo, filtro, todos, ultimas ->
        val hoje = LocalDate.now()
        todos
            .filter { BuscaRules.alunoCorresponde(termo, it) && BuscaRules.passaFiltro(filtro, it, ultimas[it.id], hoje) }
            .map { a ->
                AlunoItemUi(
                    aluno = a,
                    ultimaAvaliacao = ultimas[a.id],
                    reavaliacao = ReavaliacaoRules.situacao(ultimas[a.id], a.reavaliacaoDias, hoje),
                    aniversarioHoje = a.dataNascimento?.let { it.month == hoje.month && it.dayOfMonth == hoje.dayOfMonth } == true,
                )
            }
    }.estadoSeguro(viewModelScope, emptyList())

    val resumo: StateFlow<ResumoAlunos> = combine(repo.observar(""), avaliacoes.ultimasPorAluno()) { todos, ultimas ->
        val hoje = LocalDate.now()
        ResumoAlunos(
            total = todos.size,
            reavaliacao = todos.count { BuscaRules.passaFiltro(FiltroAluno.REAVALIACAO, it, ultimas[it.id], hoje) },
            aniversarioNoMes = todos.count { BuscaRules.passaFiltro(FiltroAluno.ANIVERSARIO, it, ultimas[it.id], hoje) },
            semAvaliacao = todos.count { ultimas[it.id] == null },
        )
    }.estadoSeguro(viewModelScope, ResumoAlunos())

    fun buscar(texto: String) { _busca.value = texto }
    fun filtrar(f: FiltroAluno) { _filtro.value = f }

    fun arquivoFoto(rel: String?): File? = rel?.let(fotos::arquivo)
}

data class AlunoFormState(
    val carregando: Boolean = true,
    val input: AlunoInput = AlunoInput(),
    val erros: Map<CampoAluno, String> = emptyMap(),
    val editando: Boolean = false,
    val concluido: Boolean = false,
    /** Caminho relativo da foto de perfil (já copiada para o armazenamento do app). */
    val foto: String? = null,
    val mensagem: String? = null,
    /** Exportação LGPD pronta para compartilhar (consumida pela tela). */
    val exportacao: File? = null,
)

@HiltViewModel
class AlunoFormViewModel @Inject constructor(
    private val repo: AlunoRepository,
    private val fotos: FotoStorage,
    private val privacidade: PrivacidadeService,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L
    private var original: Aluno? = null

    /** Fotos importadas nesta sessão (apagadas se o usuário sair sem salvar). */
    private val criadas = mutableSetOf<String>()
    /** Foto salva anteriormente que foi trocada/removida (apagada só depois de salvar). */
    private val substituidas = mutableSetOf<String>()
    private var salvou = false

    private val _state = MutableStateFlow(AlunoFormState(carregando = idAluno != 0L, editando = idAluno != 0L))
    val state: StateFlow<AlunoFormState> = _state

    init {
        if (idAluno != 0L) viewModelScope.launch(TratadorDeErros) {
            original = repo.buscar(idAluno)
            _state.update {
                it.copy(
                    carregando = false,
                    input = original?.let(AlunoValidator::paraInput) ?: it.input,
                    foto = original?.fotoPerfil,
                )
            }
        }
    }

    fun alterar(transform: (AlunoInput) -> AlunoInput, campo: CampoAluno? = null) {
        _state.update { s -> s.copy(input = transform(s.input), erros = campo?.let { s.erros - it } ?: s.erros) }
    }

    fun mensagemLida() = _state.update { it.copy(mensagem = null) }

    fun arquivoFoto(rel: String?): File? = rel?.let(fotos::arquivo)
    fun novaUriCamera(): Uri = fotos.novaUriCamera()

    fun definirFoto(origem: Uri, recorte: android.graphics.RectF? = null) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { fotos.importarFotoAluno(origem, recorte) }
                .onSuccess { rel ->
                    criadas += rel
                    _state.update { s ->
                        s.foto?.let(::descartar)
                        s.copy(foto = rel)
                    }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível usar a foto: ${e.message}") } }
        }
    }

    fun removerFoto() {
        _state.update { s ->
            s.foto?.let(::descartar)
            s.copy(foto = null)
        }
    }

    private fun descartar(rel: String) {
        if (rel in criadas) { criadas -= rel; fotos.apagar(rel) } else substituidas += rel
    }

    fun salvar() {
        val s = _state.value
        when (val r = AlunoValidator.validar(original, s.input)) {
            is ResultadoAluno.Erros -> _state.update { it.copy(erros = r.erros) }
            is ResultadoAluno.Ok -> viewModelScope.launch(TratadorDeErros) {
                repo.salvar(r.aluno.copy(fotoPerfil = s.foto))
                salvou = true
                substituidas.forEach(fotos::apagar)
                _state.update { it.copy(concluido = true) }
            }
        }
    }

    /** Exclusão definitiva (LGPD): registros, todas as fotos, lembretes e cópias em cache. */
    fun excluir() {
        val a = original ?: return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { privacidade.excluirDefinitivamente(a.id) }
                .onSuccess {
                    salvou = true
                    _state.update { it.copy(concluido = true) }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível excluir: ${e.message}") } }
        }
    }

    fun exportarDados() {
        val a = original ?: return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { privacidade.exportar(a.id) }
                .onSuccess { f -> _state.update { it.copy(exportacao = f) } }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível exportar: ${e.message}") } }
        }
    }

    fun exportacaoConsumida() = _state.update { it.copy(exportacao = null) }

    override fun onCleared() {
        if (!salvou) criadas.forEach(fotos::apagar)
    }
}
