package br.com.fitpersonal.app.ui.fotos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.domain.rules.AvaliacaoValidator
import br.com.fitpersonal.app.domain.rules.TipoFoto
import br.com.fitpersonal.app.ui.common.EstadoVazio
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Fotos de uma avaliação (só posições existentes). */
data class FotosDaData(val idAvaliacao: Long, val data: LocalDate, val fotos: Map<TipoFoto, String>)

@HiltViewModel
class LinhaDoTempoFotosViewModel @Inject constructor(
    avaliacoes: AvaliacaoRepository,
    private val storage: FotoStorage,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L

    /** Mais recente primeiro. */
    val linhas: StateFlow<List<FotosDaData>?> = avaliacoes.doAluno(idAluno)
        .map { lista ->
            lista.map { FotosDaData(it.id, it.data, AvaliacaoValidator.fotosDe(it)) }
                .filter { it.fotos.isNotEmpty() }
                .sortedWith(compareByDescending<FotosDaData> { it.data }.thenByDescending { it.idAvaliacao })
        }
        .stateIn<List<FotosDaData>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun arquivo(rel: String): File = storage.arquivo(rel)
}

private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private data class Selecao(val data: LocalDate, val tipo: TipoFoto, val caminho: String, val antiga: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinhaDoTempoFotosScreen(onVoltar: () -> Unit, vm: LinhaDoTempoFotosViewModel = hiltViewModel()) {
    val linhas by vm.linhas.collectAsStateWithLifecycle()
    var selecao by remember { mutableStateOf<Selecao?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Linha do tempo de fotos", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            )
        },
    ) { padding ->
        val lista = linhas
        if (lista != null && lista.isEmpty()) {
            EstadoVazio(
                icone = Icons.Filled.PhotoLibrary,
                titulo = "Sem fotos ainda",
                texto = "Tire as fotos posturais na última etapa da avaliação para acompanhar a evolução.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(lista.orEmpty(), key = { it.idAvaliacao }) { linha ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(linha.data.format(DATA), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipoFoto.entries.forEach { tipo ->
                            val rel = linha.fotos[tipo]
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 4f)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable(enabled = rel != null) {
                                            // foto mais antiga da mesma posição (para sobrepor)
                                            val antiga = lista.orEmpty()
                                                .filter { it.data < linha.data || (it.data == linha.data && it.idAvaliacao < linha.idAvaliacao) }
                                                .sortedBy { it.data }
                                                .firstNotNullOfOrNull { it.fotos[tipo] }
                                            selecao = Selecao(linha.data, tipo, rel!!, antiga)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (rel != null) {
                                        AsyncImage(
                                            model = vm.arquivo(rel),
                                            contentDescription = tipo.rotulo,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                Text(
                                    tipo.rotulo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selecao?.let { sel ->
        Visualizador(
            titulo = "${sel.tipo.rotulo} • ${sel.data.format(DATA)}",
            atual = vm.arquivo(sel.caminho),
            antiga = sel.antiga?.let(vm::arquivo),
            onFechar = { selecao = null },
        )
    }
}

/** Tela cheia com zoom/arraste e, quando há foto mais antiga da mesma posição, sobreposição ajustável. */
@Composable
private fun Visualizador(titulo: String, atual: File, antiga: File?, onFechar: () -> Unit) {
    var escala by remember { mutableFloatStateOf(1f) }
    var deslocamento by remember { mutableStateOf(Offset.Zero) }
    var sobrepor by remember { mutableStateOf(false) }
    var alfa by remember { mutableFloatStateOf(0.5f) }

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                escala = (escala * zoom).coerceIn(1f, 5f)
                                deslocamento = if (escala == 1f) Offset.Zero else deslocamento + pan
                            }
                        }
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            translationX = deslocamento.x
                            translationY = deslocamento.y
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (sobrepor && antiga != null) {
                        AsyncImage(model = antiga, contentDescription = "Foto mais antiga", contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize())
                        AsyncImage(model = atual, contentDescription = titulo, contentScale = ContentScale.Fit,
                            alpha = alfa, modifier = Modifier.fillMaxSize())
                    } else {
                        AsyncImage(model = atual, contentDescription = titulo, contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize())
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(8.dp).align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(titulo, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    IconButton(onClick = onFechar) { Icon(Icons.Filled.Close, "Fechar", tint = Color.White) }
                }

                if (antiga != null) {
                    Column(
                        Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = sobrepor, onCheckedChange = { sobrepor = it })
                            Text(
                                "  Sobrepor à foto mais antiga desta posição",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (sobrepor) {
                            Slider(value = alfa, onValueChange = { alfa = it })
                        }
                    }
                }
            }
        }
    }
}
