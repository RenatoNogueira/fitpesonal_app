package br.com.fitpersonal.app.ui.avaliacoes

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.SexoBiologico
import kotlin.math.round
import br.com.fitpersonal.app.domain.rules.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

// ───────────────────────── Lista ─────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AvaliacoesViewModel @Inject constructor(
    alunosRepo: AlunoRepository,
    private val avaliacaoRepo: AvaliacaoRepository,
) : ViewModel() {

    val alunos: StateFlow<List<Aluno>> = alunosRepo.observar("")
        .estadoSeguro(viewModelScope, emptyList())

    private val _alunoSelecionado = MutableStateFlow<Long?>(null)
    val alunoSelecionado: StateFlow<Long?> = _alunoSelecionado

    val avaliacoes: StateFlow<List<AvaliacaoFisica>> = _alunoSelecionado
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else avaliacaoRepo.doAluno(id) }
        .estadoSeguro(viewModelScope, emptyList())

    fun selecionar(idAluno: Long) { _alunoSelecionado.value = idAluno }
}

// ───────────────────────── Formulário ─────────────────────────

const val PASSOS = 4
val TITULOS_PASSO = listOf("Medidas", "Saúde", "Avaliação física", "Fotos")

data class AvaliacaoFormState(
    val carregando: Boolean = true,
    val editando: Boolean = false,
    val passo: Int = 0,
    val input: AvaliacaoInput = AvaliacaoInput(),
    val fotos: Map<TipoFoto, String> = emptyMap(),
    /** Caminho relativo da assinatura do aluno (PNG), se coletada. */
    val assinatura: String? = null,
    val erros: Map<CampoAvaliacao, String> = emptyMap(),
    val resumo: ResumoAvaliacao = ResumoAvaliacao(),
    val mensagem: String? = null,
    val concluido: Boolean = false,
    val pdf: java.io.File? = null,
    val gerandoPdf: Boolean = false,
    val aluno: Aluno? = null,
    /** Sexo usado na equação de Pollock (vem do cadastro do aluno; editável quando ausente). */
    val sexoCalculo: SexoBiologico? = null,
    val pollock: AnalisePollock = AnalisePollock.Pendente("Preencha as dobras."),
    /** Outras avaliações salvas do aluno (sem a que está sendo editada), para o painel de composição. */
    val historico: List<AvaliacaoFisica> = emptyList(),
    /** Metas de composição do aluno (uma por métrica). */
    val metas: List<MetaComposicao> = emptyList(),
)

@HiltViewModel
class AvaliacaoFormViewModel @Inject constructor(
    private val repo: AvaliacaoRepository,
    private val alunosRepo: AlunoRepository,
    private val metasRepo: br.com.fitpersonal.app.data.repo.MetaComposicaoRepository,
    private val fotoStorage: FotoStorage,
    private val relatorios: br.com.fitpersonal.app.report.RelatorioService,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAvaliacao: Long = savedState.get<Long>("id") ?: 0L
    private val idAlunoArg: Long = savedState.get<Long>("aluno") ?: 0L
    private var original: AvaliacaoFisica? = null

    /** Arquivos criados nesta sessão (apagados se o usuário sair sem salvar). */
    private val criadas = mutableSetOf<String>()
    /** Fotos salvas anteriormente que foram trocadas/removidas (apagadas só após salvar). */
    private val substituidas = mutableSetOf<String>()
    private var salvou = false

    private val _state = MutableStateFlow(
        AvaliacaoFormState(carregando = idAvaliacao != 0L, editando = idAvaliacao != 0L)
    )
    val state: StateFlow<AvaliacaoFormState> = _state

    init {
        viewModelScope.launch(TratadorDeErros) {
            if (idAvaliacao != 0L) original = repo.buscar(idAvaliacao)
            val aluno = alunosRepo.buscar(original?.idAluno ?: idAlunoArg)
            val input = original?.let(AvaliacaoValidator::paraInput) ?: run {
                val hoje = LocalDate.now()
                AvaliacaoInput(data = "%02d%02d%04d".format(hoje.dayOfMonth, hoje.monthValue, hoje.year))
            }
            val idDoAluno = original?.idAluno ?: idAlunoArg
            if (idDoAluno != 0L) {
                viewModelScope.launch(TratadorDeErros) {
                    metasRepo.doAluno(idDoAluno).collect { metas -> _state.update { it.copy(metas = metas) } }
                }
            }
            val historico = runCatching {
                repo.doAluno(original?.idAluno ?: idAlunoArg).first().filter { a -> a.id != idAvaliacao }
            }.getOrDefault(emptyList())
            _state.update {
                analisar(
                    it.copy(
                        historico = historico,
                        carregando = false,
                        input = input,
                        fotos = original?.let(AvaliacaoValidator::fotosDe) ?: emptyMap(),
                        assinatura = original?.assinatura,
                        resumo = AvaliacaoValidator.resumir(input),
                        aluno = aluno,
                        sexoCalculo = SexoBiologico.deTexto(aluno?.sexo),
                    )
                )
            }
        }
    }

    /** Recalcula Pollock com os dados atuais (dobras, sexo e idade na data da avaliação). */
    private fun analisar(s: AvaliacaoFormState): AvaliacaoFormState {
        val i = s.input
        val dobras = i.dobras.mapNotNull { (d, t) -> AvaliacaoValidator.parseDecimal(t)?.let { d to it } }.toMap()
        val data = AlunoValidator.parseData(i.data) ?: LocalDate.now()
        return s.copy(pollock = Pollock.analisar(i.protocolo, dobras, s.sexoCalculo, s.aluno?.dataNascimento, data))
    }

    /** Define (ou remove, com null) a meta do aluno para a métrica. */
    fun definirMeta(metrica: Metrica, alvo: Double?) {
        val id = _state.value.aluno?.id ?: return
        viewModelScope.launch(TratadorDeErros) {
            if (alvo == null) metasRepo.remover(id, metrica) else metasRepo.definir(id, metrica, alvo)
        }
    }

    fun definirSexoCalculo(sexo: SexoBiologico) = _state.update { analisar(it.copy(sexoCalculo = sexo)) }

    /** Copia o % de gordura calculado para a etapa Medidas — decisão explícita do profissional. */
    fun usarPercentualPollock() {
        val r = (_state.value.pollock as? AnalisePollock.Calculada)?.resultado ?: return
        val valor = round(r.percentualGordura * 10) / 10
        val texto = AvaliacaoValidator.formatarDecimal(valor)
        alterar(CampoAvaliacao.PERCENTUAL) { it.copy(percentualGordura = texto) }
        _state.update { it.copy(mensagem = "% de gordura preenchido com $texto") }
    }

    private val idAluno get() = original?.idAluno ?: idAlunoArg

    fun alterar(campo: CampoAvaliacao? = null, transform: (AvaliacaoInput) -> AvaliacaoInput) {
        _state.update { s ->
            val novo = transform(s.input)
            analisar(
                s.copy(
                    input = novo,
                    resumo = AvaliacaoValidator.resumir(novo),
                    erros = campo?.let { s.erros - it } ?: s.erros,
                )
            )
        }
    }

    fun gerarPdf() {
        if (idAvaliacao == 0L || _state.value.gerandoPdf) return
        _state.update { it.copy(gerandoPdf = true) }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.avaliacao(idAvaliacao) }
                .onSuccess { f -> _state.update { it.copy(pdf = f, gerandoPdf = false) } }
                .onFailure { e -> _state.update { it.copy(gerandoPdf = false, mensagem = "Não foi possível gerar o PDF: ${e.message}") } }
        }
    }

    fun pdfConsumido() = _state.update { it.copy(pdf = null) }

    fun irPara(passo: Int) = _state.update { it.copy(passo = passo.coerceIn(0, PASSOS - 1)) }
    fun mensagemLida() = _state.update { it.copy(mensagem = null) }

    fun novaUriCamera(): Uri = fotoStorage.novaUriCamera()
    fun arquivoFoto(rel: String): File = fotoStorage.arquivo(rel)

    fun definirFoto(tipo: TipoFoto, origem: Uri, recorte: android.graphics.RectF? = null) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { fotoStorage.importar(origem, idAluno, tipo.chave, recorte) }
                .onSuccess { rel ->
                    criadas += rel
                    _state.update { s ->
                        s.fotos[tipo]?.let(::descartar)
                        s.copy(fotos = s.fotos + (tipo to rel))
                    }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível usar a foto: ${e.message}") } }
        }
    }

    fun removerFoto(tipo: TipoFoto) {
        _state.update { s ->
            s.fotos[tipo]?.let(::descartar)
            s.copy(fotos = s.fotos - tipo)
        }
    }

    fun definirAssinatura(imagem: android.graphics.Bitmap) {
        viewModelScope.launch(TratadorDeErros) {
            runCatching { fotoStorage.salvarAssinatura(imagem, idAluno) }
                .onSuccess { rel ->
                    criadas += rel
                    _state.update { s ->
                        s.assinatura?.let(::descartar)
                        s.copy(assinatura = rel)
                    }
                }
                .onFailure { e -> _state.update { it.copy(mensagem = "Não foi possível salvar a assinatura: ${e.message}") } }
        }
    }

    fun removerAssinatura() {
        _state.update { s ->
            s.assinatura?.let(::descartar)
            s.copy(assinatura = null)
        }
    }

    /** Marca/desmarca uma resposta do PAR-Q; tocar de novo na mesma resposta limpa a pergunta. */
    fun responderParq(indice: Int, sim: Boolean?) {
        alterar {
            val atuais = ParqRules.decodificar(it.parq).toMutableList()
            atuais[indice] = if (atuais[indice] == sim) null else sim
            it.copy(parq = ParqRules.codificar(atuais))
        }
    }

    /** Arquivo da sessão é apagado já; arquivo antigo só depois que salvar com sucesso. */
    private fun descartar(rel: String) {
        if (rel in criadas) { criadas -= rel; fotoStorage.apagar(rel) } else substituidas += rel
    }

    fun salvar() {
        val s = _state.value
        val base = original ?: AvaliacaoFisica(idAluno = idAlunoArg)
        when (val r = AvaliacaoValidator.validar(base, s.input)) {
            is ResultadoAvaliacao.Erros -> {
                val primeiro = r.erros.keys.minOf { passoDe(it) }
                _state.update { it.copy(erros = r.erros, passo = primeiro, mensagem = "Corrija os campos destacados.") }
            }
            is ResultadoAvaliacao.Ok -> viewModelScope.launch(TratadorDeErros) {
                repo.salvar(AvaliacaoValidator.comFotos(r.avaliacao, s.fotos).copy(assinatura = s.assinatura))
                salvou = true
                substituidas.forEach(fotoStorage::apagar)
                _state.update { it.copy(concluido = true) }
            }
        }
    }

    fun excluir() {
        val a = original ?: return
        viewModelScope.launch(TratadorDeErros) {
            repo.excluir(a)
            AvaliacaoValidator.fotosDe(a).values.forEach(fotoStorage::apagar)
            fotoStorage.apagar(a.assinatura)
            salvou = true
            _state.update { it.copy(concluido = true) }
        }
    }

    override fun onCleared() {
        if (!salvou) criadas.forEach(fotoStorage::apagar)
    }

    private fun passoDe(c: CampoAvaliacao) = when (c) {
        CampoAvaliacao.DATA, CampoAvaliacao.PESO, CampoAvaliacao.ALTURA,
        CampoAvaliacao.PERCENTUAL, CampoAvaliacao.MASSA_MAGRA, CampoAvaliacao.MASSA_GORDA -> 0
        CampoAvaliacao.PAS, CampoAvaliacao.PAD, CampoAvaliacao.LDL, CampoAvaliacao.HDL -> 1
        else -> 2   // dobras cutâneas
    }
}
