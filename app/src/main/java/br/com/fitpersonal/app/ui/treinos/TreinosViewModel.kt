package br.com.fitpersonal.app.ui.treinos

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.CatalogoRepository
import br.com.fitpersonal.app.data.repo.ModeloTreinoRepository
import br.com.fitpersonal.app.data.repo.TreinoRepository
import br.com.fitpersonal.app.domain.model.*
import br.com.fitpersonal.app.domain.rules.TreinoRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ───────────────────────── Lista ─────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TreinosViewModel @Inject constructor(
    alunosRepo: AlunoRepository,
    treinoRepo: TreinoRepository,
    private val modeloRepo: ModeloTreinoRepository,
) : ViewModel() {

    val modelos: StateFlow<List<ModeloTreino>> = modeloRepo.todos
        .estadoSeguro(viewModelScope, emptyList())

    /** Cria um treino novo para o aluno selecionado a partir do modelo e devolve o id (null se não deu). */
    fun aplicarModelo(idModelo: Long, onCriado: (Long) -> Unit) {
        val aluno = _alunoSelecionado.value ?: return
        viewModelScope.launch(TratadorDeErros) { modeloRepo.aplicar(idModelo, aluno)?.let(onCriado) }
    }

    fun excluirModelo(idModelo: Long) {
        viewModelScope.launch(TratadorDeErros) { modeloRepo.excluir(idModelo) }
    }

    val alunos: StateFlow<List<Aluno>> = alunosRepo.observar("")
        .estadoSeguro(viewModelScope, emptyList())

    private val _alunoSelecionado = MutableStateFlow<Long?>(null)
    val alunoSelecionado: StateFlow<Long?> = _alunoSelecionado

    val treinos: StateFlow<List<Treino>> = _alunoSelecionado
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else treinoRepo.doAluno(id) }
        .estadoSeguro(viewModelScope, emptyList())

    /** Exercícios por treino, para o resumo dos cartões. */
    val totais: StateFlow<Map<Long, Int>> = treinoRepo.contagemExercicios()
        .estadoSeguro(viewModelScope, emptyMap())

    fun selecionar(idAluno: Long) { _alunoSelecionado.value = idAluno }
}

// ───────────────────────── Editor ─────────────────────────

/** Exercício em edição; [uid] é estável na UI mesmo antes de existir id no banco. */
data class ExercicioDraft(val uid: Long, val ex: ExercicioTreino)

data class CatalogoState(
    val termo: String = "",
    val resultados: List<ExercicioCatalogo> = emptyList(),
    val selecionados: Map<String, ExercicioCatalogo> = emptyMap(),
    val carregando: Boolean = false,
    val temMais: Boolean = false,
)

data class TreinoFormState(
    val carregando: Boolean = true,
    val editando: Boolean = false,
    val nome: String = "",
    val objetivo: String = "",
    val exercicios: List<ExercicioDraft> = emptyList(),
    val divisao: DivisaoTreino = DivisaoTreino.A,
    val erroNome: String? = null,
    val catalogo: CatalogoState = CatalogoState(),
    val alterado: Boolean = false,
    val mensagem: String? = null,
    val concluido: Boolean = false,
    val pdf: java.io.File? = null,
    val gerandoPdf: Boolean = false,
    /** Página HTML do treino pronta para enviar ao aluno (consumida pela tela). */
    val html: java.io.File? = null,
    /** Treino em texto formatado pronto para compartilhar (consumido pela tela). */
    val texto: String? = null,
    /** Arquivo `.fittreino` para o app do aluno (consumido pela tela). */
    val arquivoAluno: java.io.File? = null,
)

@HiltViewModel
class TreinoFormViewModel @Inject constructor(
    private val repo: TreinoRepository,
    avaliacoesRepo: br.com.fitpersonal.app.data.repo.AvaliacaoRepository,
    private val modeloRepo: ModeloTreinoRepository,
    private val catalogoRepo: CatalogoRepository,
    private val relatorios: br.com.fitpersonal.app.report.RelatorioService,
    private val contrato: br.com.fitpersonal.app.data.contrato.ContratoService,
    private val videoServico: br.com.fitpersonal.app.data.files.VideoExercicioService,
    savedState: SavedStateHandle,
) : ViewModel() {

    val idTreino: Long = savedState.get<Long>("id") ?: 0L

    // ── vídeos próprios dos exercícios ──
    val videos: StateFlow<Map<String, br.com.fitpersonal.app.data.files.VideoExercicio>> =
        videoServico.todos.estadoSeguro(viewModelScope, emptyMap())

    private val _videoOcupado = MutableStateFlow(false)
    val videoOcupado: StateFlow<Boolean> = _videoOcupado

    fun novaUriVideo(): android.net.Uri = videoServico.novaUriGravacao()
    fun uriVideo(v: br.com.fitpersonal.app.data.files.VideoExercicio): android.net.Uri = videoServico.uriDe(v)
    fun arquivoVideo(v: br.com.fitpersonal.app.data.files.VideoExercicio): java.io.File = videoServico.arquivoDe(v)
    fun avisar(msg: String) = _state.update { it.copy(mensagem = msg) }

    fun importarVideo(nome: String, uri: android.net.Uri) {
        _videoOcupado.value = true
        viewModelScope.launch(TratadorDeErros) {
            runCatching { videoServico.importar(nome, uri) }
                .onSuccess { v -> avisar("Vídeo de \"${v.nome}\" salvo (${v.duracaoTexto}, ${v.tamanhoTexto}).") }
                .onFailure { e -> avisar(e.message ?: "Não foi possível salvar o vídeo.") }
            _videoOcupado.value = false
        }
    }

    fun removerVideo(chave: String) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { videoServico.remover(chave) }
                .onSuccess { avisar("Vídeo removido.") }
                .onFailure { e -> avisar("Não foi possível remover: ${e.message}") }
        }
    }
    private val idAlunoArg: Long = savedState.get<Long>("aluno") ?: 0L
    private var original: Treino? = null

    /** Aluno dono do treino (vem do argumento ou, ao editar, do treino salvo). */
    private val alunoDoTreino = MutableStateFlow(idAlunoArg)

    /** Avaliações do aluno, para o painel de composição corporal. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val avaliacoes: StateFlow<List<AvaliacaoFisica>> = alunoDoTreino
        .flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else avaliacoesRepo.doAluno(id) }
        .estadoSeguro(viewModelScope, emptyList())
    private var proximoUid = 1L
    private var jobBusca: Job? = null

    private val _state = MutableStateFlow(TreinoFormState(carregando = idTreino != 0L, editando = idTreino != 0L))
    val state: StateFlow<TreinoFormState> = _state

    init {
        if (idTreino != 0L) viewModelScope.launch(TratadorDeErros) {
            original = repo.buscar(idTreino)
            original?.let { alunoDoTreino.value = it.idAluno }
            val exs = repo.exerciciosDe(idTreino).map { ExercicioDraft(proximoUid++, it) }
            _state.update {
                it.copy(carregando = false, nome = original?.nome.orEmpty(),
                    objetivo = original?.objetivo.orEmpty(), exercicios = exs)
            }
        }
    }

    // ── cabeçalho ──
    fun alterarNome(v: String) = _state.update { it.copy(nome = v, erroNome = null, alterado = true) }
    fun alterarObjetivo(v: String) = _state.update { it.copy(objetivo = v, alterado = true) }
    fun selecionarDivisao(d: DivisaoTreino) = _state.update { it.copy(divisao = d) }
    fun mensagemLida() = _state.update { it.copy(mensagem = null) }

    // ── exercícios ──
    /** Adiciona (uid == null) ou atualiza um exercício. Uma troca de divisão leva o usuário à nova aba. */
    fun salvarExercicio(uid: Long?, ex: ExercicioTreino) {
        _state.update { s ->
            val lista = if (uid == null) s.exercicios + ExercicioDraft(proximoUid++, ex)
            else s.exercicios.map { if (it.uid == uid) it.copy(ex = ex) else it }
            s.copy(exercicios = lista, divisao = ex.divisao, alterado = true)
        }
    }

    fun removerExercicio(uid: Long) =
        _state.update { s -> s.copy(exercicios = s.exercicios.filterNot { it.uid == uid }, alterado = true) }

    // ── catálogo ──
    fun buscarCatalogo(termo: String) {
        _state.update { it.copy(catalogo = it.catalogo.copy(termo = termo)) }
        jobBusca?.cancel()
        if (termo.isBlank()) {
            _state.update { it.copy(catalogo = it.catalogo.copy(resultados = emptyList(), temMais = false, carregando = false)) }
            return
        }
        jobBusca = viewModelScope.launch(TratadorDeErros) {
            delay(300)   // debounce
            carregarPagina(termo, offset = 0)
        }
    }

    fun maisResultados() {
        val c = _state.value.catalogo
        if (!c.temMais || c.carregando) return
        viewModelScope.launch(TratadorDeErros) { carregarPagina(c.termo, offset = c.resultados.size) }
    }

    private suspend fun carregarPagina(termo: String, offset: Int) {
        _state.update { it.copy(catalogo = it.catalogo.copy(carregando = true)) }
        val pagina = catalogoRepo.buscar(termo, offset)
        _state.update { s ->
            val base = if (offset == 0) emptyList() else s.catalogo.resultados
            s.copy(catalogo = s.catalogo.copy(resultados = base + pagina, temMais = pagina.size >= 100, carregando = false))
        }
    }

    fun alternarSelecao(item: ExercicioCatalogo) = _state.update { s ->
        val sel = s.catalogo.selecionados
        s.copy(catalogo = s.catalogo.copy(selecionados = if (item.id in sel) sel - item.id else sel + (item.id to item)))
    }

    /** Adiciona os selecionados à divisão ativa e limpa a seleção. Retorna a quantidade. */
    fun adicionarSelecionados(): Int {
        val s = _state.value
        val novos = TreinoRules.doCatalogo(s.catalogo.selecionados.values, s.divisao)
        if (novos.isEmpty()) return 0
        _state.update { cur ->
            cur.copy(
                exercicios = cur.exercicios + novos.map { ExercicioDraft(proximoUid++, it) },
                catalogo = cur.catalogo.copy(selecionados = emptyMap()),
                alterado = true,
                mensagem = "${novos.size} exercício(s) adicionados à divisão ${s.divisao}",
            )
        }
        return novos.size
    }

    fun limparSelecaoCatalogo() =
        _state.update { it.copy(catalogo = it.catalogo.copy(selecionados = emptyMap())) }

    // ── persistência ──
    fun salvar() {
        val s = _state.value
        TreinoRules.validarNome(s.nome)?.let { erro ->
            _state.update { it.copy(erroNome = erro) }
            return
        }
        val base = original ?: Treino(idAluno = idAlunoArg, nome = s.nome)
        val treino = base.copy(nome = s.nome.trim(), objetivo = s.objetivo.trim().ifEmpty { null })
        viewModelScope.launch(TratadorDeErros) {
            repo.salvarCompleto(treino, s.exercicios.map { it.ex })
            _state.update { it.copy(concluido = true, alterado = false) }
        }
    }

    fun gerarPdf() {
        if (idTreino == 0L || _state.value.gerandoPdf) return
        _state.update { it.copy(gerandoPdf = true) }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.treino(idTreino) }
                .onSuccess { f -> _state.update { it.copy(pdf = f, gerandoPdf = false) } }
                .onFailure { e -> _state.update { it.copy(gerandoPdf = false, mensagem = "Não foi possível gerar o PDF: ${e.message}") } }
        }
    }

    fun pdfConsumido() = _state.update { it.copy(pdf = null) }

    fun enviarComoPagina() {
        if (idTreino == 0L) return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.treinoHtml(idTreino) }
                .onSuccess { f -> _state.update { it.copy(html = f) } }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível gerar a página: ${e.message}") } }
        }
    }

    fun enviarComoTexto() {
        if (idTreino == 0L) return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.treinoTexto(idTreino) }
                .onSuccess { t -> _state.update { it.copy(texto = t) } }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível montar o texto: ${e.message}") } }
        }
    }

    /** Gera o `.fittreino` (e o código de texto, usado como `texto`) para o app do aluno. */
    fun enviarParaAppDoAluno(comoCodigo: Boolean) {
        if (idTreino == 0L) return
        viewModelScope.launch(TratadorDeErros) {
            runCatching { contrato.gerar(idTreino) }
                .onSuccess { g ->
                    _state.update {
                        if (comoCodigo) {
                            it.copy(
                                texto = "Seu treino (revisão ${g.revisao}). No app FitPersonal Treino toque em \"Colar código\" " +
                                    "e cole este texto:\n\n${g.codigo}",
                            )
                        } else it.copy(arquivoAluno = g.arquivo)
                    }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível gerar o arquivo: ${e.message}") } }
        }
    }

    /** Treino + vídeos próprios num único arquivo `.fittreinopack` para o app do aluno. */
    fun enviarPacoteComVideos() {
        if (idTreino == 0L) return
        _videoOcupado.value = true
        viewModelScope.launch(TratadorDeErros) {
            runCatching { contrato.gerarComVideos(idTreino) }
                .onSuccess { g ->
                    _state.update {
                        it.copy(
                            arquivoAluno = g.arquivo,
                            mensagem = "Pacote com ${g.videos} vídeo(s), ${g.tamanhoBytes / 1_048_576} MB (revisão ${g.revisao}).",
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = e.message ?: "Não foi possível montar o pacote.") } }
            _videoOcupado.value = false
        }
    }

    fun entregaConsumida() = _state.update { it.copy(html = null, texto = null, arquivoAluno = null) }

    fun duplicar() {
        val id = original?.id ?: return
        viewModelScope.launch(TratadorDeErros) {
            repo.duplicar(id)
            _state.update { it.copy(concluido = true, alterado = false) }
        }
    }

    /** Guarda o treino (já salvo) na biblioteca de modelos, sem vínculo com o aluno. */
    fun salvarComoModelo(nome: String) {
        val id = original?.id ?: return
        viewModelScope.launch(TratadorDeErros) {
            val ok = modeloRepo.salvarComoModelo(id, nome) != null
            _state.update {
                it.copy(mensagem = if (ok) "Modelo salvo na biblioteca" else "Não foi possível salvar o modelo")
            }
        }
    }

    fun excluir() {
        val t = original ?: return
        viewModelScope.launch(TratadorDeErros) {
            repo.excluir(t)
            _state.update { it.copy(concluido = true, alterado = false) }
        }
    }
}
