package br.com.fitpersonal.app.ui.progressao

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.local.HistoricoCargaDao
import br.com.fitpersonal.app.data.local.HistoricoCargaEntity
import br.com.fitpersonal.app.data.local.TreinoDao
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.domain.rules.CargaRules
import br.com.fitpersonal.app.domain.rules.PontoSerie
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.evolucao.GraficoLinha
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ItemProgressao(
    val exercicio: String,
    val pontos: List<PontoSerie>,
    /** Carga sugerida quando está parada há [CargaRules.DIAS_ESTAGNADO] dias ou mais. */
    val sugestaoKg: Double?,
)

data class ProgressaoState(
    val carregado: Boolean = false,
    val nomeAluno: String = "",
    val itens: List<ItemProgressao> = emptyList(),
)

@HiltViewModel
class ProgressaoViewModel @Inject constructor(
    alunos: AlunoRepository,
    private val historico: HistoricoCargaDao,
    private val treinos: TreinoDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem
    fun mensagemLida() { _mensagem.value = null }

    /**
     * Grava a carga sugerida em todos os exercícios de mesmo nome dos treinos do aluno e registra no histórico.
     * O profissional continua no controle: só acontece ao tocar em "Aplicar".
     */
    fun aplicarSugestao(item: ItemProgressao) {
        val kg = item.sugestaoKg ?: return
        viewModelScope.launch(TratadorDeErros) {
            val texto = if (kg % 1.0 == 0.0) "${kg.toLong()} kg" else String.format(PT_BR, "%.1f kg", kg)
            val chave = CargaRules.chaveExercicio(item.exercicio)
            var atualizados = 0
            treinos.doAlunoSnapshot(idAluno).forEach { t ->
                treinos.exerciciosSnapshot(t.id)
                    .filter { CargaRules.chaveExercicio(it.nome) == chave }
                    .forEach { treinos.atualizarExercicio(it.copy(carga = texto)); atualizados++ }
            }
            historico.inserir(
                HistoricoCargaEntity(
                    idAluno = idAluno, exercicio = item.exercicio, chave = chave,
                    data = LocalDate.now().toString(), cargaKg = kg,
                )
            )
            _mensagem.value = if (atualizados > 0) {
                "${item.exercicio}: carga atualizada para $texto em $atualizados exercício(s)."
            } else {
                "${item.exercicio}: carga registrada no histórico ($texto); nenhum treino atual tem esse exercício."
            }
        }
    }

    val state: StateFlow<ProgressaoState> = combine(
        alunos.observar("").map { l -> l.firstOrNull { it.id == idAluno }?.nome.orEmpty() },
        historico.doAluno(idAluno),
    ) { nome, registros ->
        val hoje = LocalDate.now()
        val itens = registros.groupBy { it.chave }.map { (_, lista) ->
            val ordenada = lista.sortedWith(compareBy({ it.data }, { it.id }))
            val pontos = ordenada.map { PontoSerie(LocalDate.parse(it.data), it.cargaKg) }
            ItemProgressao(
                exercicio = ordenada.last().exercicio,
                pontos = pontos,
                sugestaoKg = CargaRules.sugerirAumento(pontos.last(), hoje),
            )
        }.sortedBy { it.exercicio.lowercase() }
        ProgressaoState(carregado = true, nomeAluno = nome, itens = itens)
    }.estadoSeguro(viewModelScope, ProgressaoState())
}

private val PT_BR = Locale("pt", "BR")
private val DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yy")

private fun kg(v: Double) = String.format(PT_BR, "%.1f kg", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressaoScreen(onVoltar: () -> Unit, vm: ProgressaoViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(mensagem) { mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Progressão de carga", style = MaterialTheme.typography.titleLarge)
                        if (s.nomeAluno.isNotEmpty()) {
                            Text(
                                s.nomeAluno,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            )
        },
    ) { padding ->
        if (s.carregado && s.itens.isEmpty()) {
            EstadoVazio(
                icone = Icons.Filled.FitnessCenter,
                titulo = "Sem cargas registradas",
                texto = "Preencha a carga (ex.: 20 kg) nos exercícios do treino. Cada mudança entra no histórico.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(s.itens, key = { it.exercicio }) { item ->
                CartaoSecao(titulo = item.exercicio) {
                    val atual = item.pontos.last()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(kg(atual.valor), style = MaterialTheme.typography.headlineSmall)
                        if (item.pontos.size >= 2) {
                            val delta = atual.valor - item.pontos.first().valor
                            val sinal = if (delta > 0) "+" else ""
                            ChipStatus(
                                "$sinal${kg(delta)} desde ${item.pontos.first().data.format(DATA_CURTA)}",
                                when {
                                    delta > 0 -> TomChip.Sucesso
                                    delta < 0 -> TomChip.Atencao
                                    else -> TomChip.Neutro
                                },
                            )
                        }
                    }
                    Text(
                        "Desde ${atual.data.format(DATA_CURTA)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.pontos.size >= 2) {
                        GraficoLinha(
                            pontos = item.pontos,
                            cor = MaterialTheme.colorScheme.primary,
                            grade = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                        )
                    }
                    item.sugestaoKg?.let {
                        Text(
                            "Parado há ${CargaRules.DIAS_ESTAGNADO}+ dias: considere subir para ${kg(it)} se a execução está boa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedButton(onClick = { vm.aplicarSugestao(item) }) {
                            Text("Aplicar ${kg(it)} nos treinos")
                        }
                    }
                }
            }
        }
    }
}
