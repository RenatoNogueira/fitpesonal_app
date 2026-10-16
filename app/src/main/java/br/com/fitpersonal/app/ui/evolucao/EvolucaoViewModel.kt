package br.com.fitpersonal.app.ui.evolucao

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.report.RelatorioService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EvolucaoState(
    val aluno: Aluno? = null,
    /** Mais recente primeiro (ordem do repositório). */
    val avaliacoes: List<AvaliacaoFisica> = emptyList(),
    val metrica: Metrica = Metrica.PESO,
    val idAntes: Long? = null,
    val idDepois: Long? = null,
    val gerandoPdf: Boolean = false,
    val pdf: File? = null,
    val mensagem: String? = null,
)

/** Escolhas feitas pelo usuário; o restante é derivado do banco. */
private data class Escolhas(
    val metrica: Metrica = Metrica.PESO,
    val idAntes: Long? = null,
    val idDepois: Long? = null,
    val gerandoPdf: Boolean = false,
    val pdf: File? = null,
    val mensagem: String? = null,
)

@HiltViewModel
class EvolucaoViewModel @Inject constructor(
    alunos: AlunoRepository,
    avaliacoesRepo: AvaliacaoRepository,
    private val relatorios: RelatorioService,
    private val fotos: FotoStorage,
    savedState: SavedStateHandle,
) : ViewModel() {

    fun arquivoFoto(rel: String): File = fotos.arquivo(rel)

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L
    private val escolhas = MutableStateFlow(Escolhas())

    val state: StateFlow<EvolucaoState> = combine(
        alunos.observar("").map { lista -> lista.firstOrNull { it.id == idAluno } },
        avaliacoesRepo.doAluno(idAluno),
        escolhas,
    ) { aluno, avaliacoes, e ->
        val cronologicas = avaliacoes.sortedWith(compareBy({ it.data }, { it.id }))
        // Padrão do comparativo: primeira avaliação x mais recente; ignora ids que não existem mais.
        val antes = e.idAntes?.takeIf { id -> avaliacoes.any { it.id == id } } ?: cronologicas.firstOrNull()?.id
        val depois = e.idDepois?.takeIf { id -> avaliacoes.any { it.id == id } } ?: cronologicas.lastOrNull()?.id
        EvolucaoState(aluno, avaliacoes, e.metrica, antes, depois, e.gerandoPdf, e.pdf, e.mensagem)
    }.estadoSeguro(viewModelScope, EvolucaoState())

    fun selecionarMetrica(m: Metrica) = escolhas.update { it.copy(metrica = m) }
    fun escolherAntes(id: Long) = escolhas.update { it.copy(idAntes = id) }
    fun escolherDepois(id: Long) = escolhas.update { it.copy(idDepois = id) }
    fun pdfConsumido() = escolhas.update { it.copy(pdf = null) }
    fun mensagemLida() = escolhas.update { it.copy(mensagem = null) }

    fun gerarComparativo() {
        val s = state.value
        val a = s.idAntes ?: return
        val b = s.idDepois ?: return
        if (escolhas.value.gerandoPdf) return
        escolhas.update { it.copy(gerandoPdf = true) }
        viewModelScope.launch(TratadorDeErros) {
            runCatching { relatorios.comparativo(a, b) }
                .onSuccess { f -> escolhas.update { it.copy(pdf = f, gerandoPdf = false) } }
                .onFailure { e ->
                    escolhas.update {
                        it.copy(gerandoPdf = false, mensagem = "Não foi possível gerar o PDF: ${e.message}")
                    }
                }
        }
    }
}
