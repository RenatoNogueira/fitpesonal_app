@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.fitpersonal.app.ui.treinos

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.TreinoRepository
import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioTreino
import br.com.fitpersonal.app.domain.model.Treino
import br.com.fitpersonal.app.domain.rules.ExecucaoRules
import br.com.fitpersonal.app.ui.theme.corSobre
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ───────────────────────── Estado ─────────────────────────

data class ItemExecucao(
    val exercicio: ExercicioTreino,
    val series: Int,
    val concluidas: Int = 0,
    val carga: String,
    val cargaOriginal: String,
    val descansoS: Int,
)

data class ExecucaoState(
    val carregando: Boolean = true,
    val nome: String = "",
    val objetivo: String? = null,
    val itens: List<ItemExecucao> = emptyList(),
    val divisoes: List<DivisaoTreino> = emptyList(),
    val divisao: DivisaoTreino? = null,
    val descansoRestante: Int = 0,
    val descansoTotal: Int = 0,
    /** Incrementa quando o descanso termina (a tela reage com vibração). */
    val alertas: Int = 0,
    val salvando: Boolean = false,
    val finalizado: Boolean = false,
    val mensagem: String? = null,
) {
    val seriesTotais: Int get() = itens.sumOf { it.series }
    val seriesFeitas: Int get() = itens.sumOf { it.concluidas }
    val itensDaDivisao: List<ItemExecucao>
        get() = if (divisao == null) itens else itens.filter { it.exercicio.divisao == divisao }
}

// ───────────────────────── ViewModel ─────────────────────────

/**
 * Modo treino: marca séries, cronometra o descanso e ajusta a carga na hora. Só a carga é gravada
 * (ao finalizar); o histórico de progressão é alimentado pelo próprio repositório.
 */
@HiltViewModel
class TreinoExecucaoViewModel @Inject constructor(
    private val repo: TreinoRepository,
    private val videoServico: br.com.fitpersonal.app.data.files.VideoExercicioService,
    savedState: SavedStateHandle,
) : ViewModel() {

    val videos: StateFlow<Map<String, br.com.fitpersonal.app.data.files.VideoExercicio>> =
        videoServico.todos.estadoSeguro(viewModelScope, emptyMap())

    fun uriVideo(v: br.com.fitpersonal.app.data.files.VideoExercicio): android.net.Uri = videoServico.uriDe(v)
    fun arquivoVideo(v: br.com.fitpersonal.app.data.files.VideoExercicio): java.io.File = videoServico.arquivoDe(v)

    private val idTreino: Long = savedState.get<Long>("id") ?: 0L
    private var treino: Treino? = null
    private var jobDescanso: Job? = null

    private val _state = MutableStateFlow(ExecucaoState())
    val state: StateFlow<ExecucaoState> = _state

    init {
        viewModelScope.launch(TratadorDeErros) {
            val t = repo.buscar(idTreino)
            treino = t
            val itens = repo.exerciciosDe(idTreino).map { e ->
                ItemExecucao(
                    exercicio = e,
                    series = ExecucaoRules.series(e.series),
                    carga = e.carga.orEmpty(),
                    cargaOriginal = e.carga.orEmpty(),
                    descansoS = ExecucaoRules.descansoSegundos(e.descanso),
                )
            }
            val divisoes = itens.map { it.exercicio.divisao }.distinct().sorted()
            _state.update {
                it.copy(
                    carregando = false,
                    nome = t?.nome.orEmpty(),
                    objetivo = t?.objetivo,
                    itens = itens,
                    divisoes = divisoes,
                    divisao = divisoes.firstOrNull(),
                )
            }
        }
    }

    fun selecionarDivisao(d: DivisaoTreino) = _state.update { it.copy(divisao = d) }

    /** Toque na série [indice] (0-based): marca até ela; tocar na última marcada desfaz uma. */
    fun marcarSerie(idExercicio: Long, indice: Int) {
        var iniciarDescanso = 0
        _state.update { s ->
            s.copy(
                itens = s.itens.map { i ->
                    if (i.exercicio.id != idExercicio) i else {
                        val novo = if (indice + 1 == i.concluidas) indice else indice + 1
                        if (novo > i.concluidas) iniciarDescanso = i.descansoS
                        i.copy(concluidas = novo)
                    }
                },
            )
        }
        if (iniciarDescanso > 0) iniciarDescanso(iniciarDescanso)
    }

    fun ajustarCarga(idExercicio: Long, deltaKg: Double) = alterarItem(idExercicio) {
        it.copy(carga = ExecucaoRules.ajustarCarga(it.carga, deltaKg))
    }

    private fun alterarItem(idExercicio: Long, f: (ItemExecucao) -> ItemExecucao) =
        _state.update { s -> s.copy(itens = s.itens.map { if (it.exercicio.id == idExercicio) f(it) else it }) }

    private fun iniciarDescanso(segundos: Int) {
        jobDescanso?.cancel()
        _state.update { it.copy(descansoRestante = segundos, descansoTotal = segundos) }
        jobDescanso = viewModelScope.launch(TratadorDeErros) {
            while (_state.value.descansoRestante > 0) {
                delay(1000)
                _state.update { it.copy(descansoRestante = (it.descansoRestante - 1).coerceAtLeast(0)) }
            }
            _state.update { it.copy(alertas = it.alertas + 1) }
        }
    }

    fun pularDescanso() {
        jobDescanso?.cancel()
        _state.update { it.copy(descansoRestante = 0) }
    }

    fun finalizar() {
        val t = treino ?: return
        if (_state.value.salvando) return
        jobDescanso?.cancel()
        _state.update { it.copy(salvando = true, descansoRestante = 0) }
        viewModelScope.launch(TratadorDeErros) {
            val alterados = _state.value.itens.filter { it.carga.trim() != it.cargaOriginal.trim() }
            runCatching { alterados.forEach { repo.atualizarCarga(t.idAluno, it.exercicio, it.carga) } }
                .onSuccess { _state.update { it.copy(salvando = false, finalizado = true) } }
                .onFailure { e ->
                    _state.update { it.copy(salvando = false, mensagem = "Não foi possível salvar as cargas: ${e.message}") }
                }
        }
    }

    fun mensagemLida() = _state.update { it.copy(mensagem = null) }

    override fun onCleared() {
        jobDescanso?.cancel()
        super.onCleared()
    }
}

// ───────────────────────── Tela ─────────────────────────

@Composable
fun TreinoExecucaoScreen(onVoltar: () -> Unit, vm: TreinoExecucaoViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val videos by vm.videos.collectAsStateWithLifecycle()
    val ctxLocal = androidx.compose.ui.platform.LocalContext.current
    var reproduzindo by remember { mutableStateOf<java.io.File?>(null) }
    var confirmarSaida by remember { mutableStateOf(false) }

    // Durante o treino a tela não apaga.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(s.alertas) { if (s.alertas > 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }
    LaunchedEffect(s.finalizado) { if (s.finalizado) onVoltar() }

    val emAndamento = s.seriesFeitas > 0 && !s.finalizado
    BackHandler(enabled = emAndamento) { confirmarSaida = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.nome.ifBlank { "Modo treino" }, maxLines = 1)
                        Text(
                            "${s.seriesFeitas} de ${s.seriesTotais} séries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (emAndamento) confirmarSaida = true else onVoltar() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
            )
        },
        bottomBar = {
            if (!s.carregando && s.itens.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = vm::finalizar,
                        enabled = !s.salvando,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text(if (s.salvando) "Salvando…" else "Finalizar treino") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                s.itens.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Este treino ainda não tem exercícios.", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    LinearProgressIndicator(
                        progress = { if (s.seriesTotais == 0) 0f else s.seriesFeitas / s.seriesTotais.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (s.descansoRestante > 0) {
                        CartaoDescanso(s.descansoRestante, s.descansoTotal, onPular = vm::pularDescanso)
                    }
                    if (s.divisoes.size > 1) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            s.divisoes.forEach { d ->
                                FilterChip(
                                    selected = s.divisao == d,
                                    onClick = { vm.selecionarDivisao(d) },
                                    label = { Text("Treino ${d.name}") },
                                )
                            }
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(s.itensDaDivisao, key = { it.exercicio.id }) { item ->
                            val local = videos[br.com.fitpersonal.app.domain.rules.CargaRules.chaveExercicio(item.exercicio.nome)]
                            val link = item.exercicio.video
                            CartaoExercicio(
                                item = item,
                                onSerie = { indice -> vm.marcarSerie(item.exercicio.id, indice) },
                                onCarga = { delta -> vm.ajustarCarga(item.exercicio.id, delta) },
                                onVideo = if (local == null && link == null) null else {
                                    {
                                        val ok = if (local != null) {
                                            reproduzindo = vm.arquivoVideo(local)
                                            true
                                        } else {
                                            br.com.fitpersonal.app.ui.common.VideoIntents.abrirLink(ctxLocal, link!!)
                                        }
                                        if (!ok) android.widget.Toast.makeText(
                                            ctxLocal, "Nenhum aplicativo para abrir o vídeo.", android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    reproduzindo?.let { br.com.fitpersonal.app.ui.common.PlayerVideoDialog(it, onFechar = { reproduzindo = null }) }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair do treino?") },
            text = { Text("As séries marcadas e as cargas alteradas não serão salvas.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; onVoltar() }) { Text("Sair") } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar") } },
        )
    }
}

@Composable
private fun CartaoDescanso(restante: Int, total: Int, onPular: () -> Unit) {
    val fundo = MaterialTheme.colorScheme.primaryContainer
    val sobre = corSobre(fundo)
    Surface(color = fundo, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Descanso", style = MaterialTheme.typography.labelLarge, color = sobre)
                    Text(
                        ExecucaoRules.formatarTempo(restante),
                        style = MaterialTheme.typography.headlineMedium,
                        color = sobre,
                    )
                }
                TextButton(onClick = onPular) { Text("Pular", color = sobre) }
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else restante / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CartaoExercicio(
    item: ItemExecucao,
    onSerie: (Int) -> Unit,
    onCarga: (Double) -> Unit,
    onVideo: (() -> Unit)? = null,
) {
    val e = item.exercicio
    val completo = item.concluidas >= item.series
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (completo) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(e.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        "${item.series} × ${e.repeticoes?.takeIf { it.isNotBlank() } ?: "—"}",
                        "descanso ${ExecucaoRules.formatarTempo(item.descansoS)}",
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                e.observacoes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            br.com.fitpersonal.app.ui.common.AnimacaoMovimento(e.catalogoId, "Como executar: " + e.nome, Modifier.fillMaxWidth())

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (i in 0 until item.series) {
                    val feita = i < item.concluidas
                    val fundo = if (feita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                    Surface(
                        onClick = { onSerie(i) },
                        shape = CircleShape,
                        color = fundo,
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = "Série " + (i + 1)
                            stateDescription = if (feita) "feita" else "pendente"
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = corSobre(fundo),
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Carga", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        item.carga.ifBlank { "—" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(onClick = { onCarga(-1.0) }) { Text("−1 kg") }
                FilledTonalButton(onClick = { onCarga(1.0) }) { Text("+1 kg") }
            }
            if (onVideo != null) {
                OutlinedButton(onClick = onVideo, modifier = Modifier.fillMaxWidth()) { Text("▶ Ver vídeo do exercício") }
            }
        }
    }
}
