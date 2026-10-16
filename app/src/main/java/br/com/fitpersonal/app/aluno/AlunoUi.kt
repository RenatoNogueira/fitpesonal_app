package br.com.fitpersonal.app.aluno

import br.com.fitpersonal.app.R
import androidx.compose.ui.text.font.FontWeight
import br.com.fitpersonal.app.ui.common.acompanharArrasto
import br.com.fitpersonal.app.ui.common.efeitoElastico
import br.com.fitpersonal.app.ui.common.rememberArrastoHorizontal
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.contrato.CodigoTexto
import br.com.fitpersonal.contrato.ContratoJson
import br.com.fitpersonal.contrato.DescansoRules
import br.com.fitpersonal.contrato.ExercicioRef
import br.com.fitpersonal.contrato.ItemSessao
import br.com.fitpersonal.contrato.SessaoRef
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// ───────────────────────── tema ─────────────────────────

// Mesma identidade do app do profissional (FitPersonal): verde-limão sobre preto.
private val Verde = Color(0xFF3A8208)
private val GradienteMarca = Brush.linearGradient(listOf(Color(0xFF3A7D0A), Color(0xFF16300A)))

private val Claro: ColorScheme = lightColorScheme(
    primary = Verde, onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F99D), onPrimaryContainer = Color(0xFF10250A),
    background = Color(0xFFF1F6EC), surface = Color.White,
    surfaceContainerHigh = Color(0xFFE6EDDD), outlineVariant = Color(0xFFCDD7C3),
    onSurfaceVariant = Color(0xFF4A5642),
)
private val Escuro: ColorScheme = darkColorScheme(
    primary = Color(0xFFA3F53A), onPrimary = Color(0xFF10250A),
    primaryContainer = Color(0xFF2A4D0B), onPrimaryContainer = Color(0xFFE4FBC4),
    background = Color(0xFF0A0D08), surface = Color(0xFF11160E),
    surfaceContainerHigh = Color(0xFF222B1C), outlineVariant = Color(0xFF3E4A35),
    onSurfaceVariant = Color(0xFFBAC4B0),
)

@Composable
fun AlunoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro, content = conteudo)
}

// Cores de apoio para diferenciar divisões (A, B, C, D) e cartões da lista.
private val CoresDivisao = listOf(Color(0xFF3A8208), Color(0xFF0E7490), Color(0xFFC2410C), Color(0xFF6D28D9))

private fun corDe(indice: Int): Color = CoresDivisao[indice.mod(CoresDivisao.size)]

// ───────────────────────── componentes ─────────────────────────

/** Faixa de topo em degradê da marca, com título, subtítulo e conteúdo opcional (pílulas, progresso). */
@Composable
internal fun Cabecalho(
    titulo: String,
    subtitulo: String?,
    modifier: Modifier = Modifier,
    inicio: (@Composable () -> Unit)? = null,
    acoes: @Composable RowScope.() -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(GradienteMarca)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            inicio?.invoke()
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            acoes()
        }
        Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = conteudo)
    }
}

@Composable
private fun Pilula(texto: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.22f)) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun PilulasHorizontais(itens: List<String>) {
    if (itens.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itens.forEach { Pilula(it) }
    }
}

/** Etiqueta discreta (tonal) para informações curtas dentro de cartões. */
@Composable
private fun Etiqueta(texto: String, cor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = cor,
            maxLines = 1,
        )
    }
}

private fun formatarData(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(iso)

private fun formatarKg(valor: Double): String =
    if (valor == Math.floor(valor)) valor.toLong().toString() else String.format(Locale("pt", "BR"), "%.1f", valor)

// ───────────────────────── navegação ─────────────────────────

private sealed interface Tela {
    data object Lista : Tela
    data class Executar(val treinoId: String) : Tela
    data class Historico(val treinoId: String) : Tela
    data object Diario : Tela
}

@Composable
fun AlunoApp(
    repo: Repositorio,
    aviso: String?,
    onAvisoLido: () -> Unit,
    onTrocarModo: () -> Unit,
    lembrete: br.com.fitpersonal.app.domain.rules.LembreteAluno,
    onLembrete: (br.com.fitpersonal.app.domain.rules.LembreteAluno) -> Unit,
) {
    var tela by remember { mutableStateOf<Tela>(Tela.Lista) }
    var versao by remember { mutableIntStateOf(0) }   // força reler a lista após mudanças
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(aviso) {
        if (aviso != null) {
            versao++
            snackbar.showSnackbar(aviso)
            onAvisoLido()
        }
    }
    BackHandler(enabled = tela != Tela.Lista) { tela = Tela.Lista }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        br.com.fitpersonal.app.ui.common.SemOverscrollPadrao {
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .efeitoElastico(),
        ) {
            AnimatedContent(
                targetState = tela,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
                label = "tela",
            ) { t ->
                when (t) {
                    Tela.Lista -> ListaTreinos(
                        repo = repo,
                        versao = versao,
                        onAbrir = { tela = Tela.Executar(it) },
                        onHistorico = { tela = Tela.Historico(it) },
                        onDiario = { tela = Tela.Diario },
                        onMudou = { versao++ },
                        onMensagem = { msg -> versaoMensagem = msg },
                        onTrocarModo = onTrocarModo,
                        lembrete = lembrete,
                        onLembrete = onLembrete,
                    )
                    is Tela.Executar -> ExecutarTreino(
                        repo = repo,
                        treinoId = t.treinoId,
                        onVoltar = { tela = Tela.Lista },
                        onHistorico = { tela = Tela.Historico(t.treinoId) },
                        onFinalizada = { versao++; tela = Tela.Lista; br.com.fitpersonal.app.core.Avisos.sucesso("Treino finalizado e salvo no histórico") },
                    )
                    is Tela.Historico -> Historico(
                        repo = repo,
                        treinoId = t.treinoId,
                        onVoltar = { tela = Tela.Lista },
                    )
                    Tela.Diario -> DiarioRefeicoes(
                        onVoltar = { tela = Tela.Lista; versao++ },
                        onMensagem = { msg -> versaoMensagem = msg },
                    )
                }
            }
            br.com.fitpersonal.app.ui.common.HostAvisos(Modifier.align(Alignment.TopCenter).fillMaxWidth())
        }
        }
    }

    // mensagens vindas das telas internas (ex.: "código inválido")
    val msg = versaoMensagem
    LaunchedEffect(msg) {
        if (msg != null) { snackbar.showSnackbar(msg); versaoMensagem = null }
    }
}

/** Canal simples de mensagens das telas internas para o Snackbar do app. */
private var versaoMensagem by mutableStateOf<String?>(null)

// ───────────────────────── lista ─────────────────────────

@Composable
private fun ListaTreinos(
    repo: Repositorio,
    versao: Int,
    onAbrir: (String) -> Unit,
    onHistorico: (String) -> Unit,
    onDiario: () -> Unit,
    onMudou: () -> Unit,
    onMensagem: (String) -> Unit,
    onTrocarModo: () -> Unit,
    lembrete: br.com.fitpersonal.app.domain.rules.LembreteAluno,
    onLembrete: (br.com.fitpersonal.app.domain.rules.LembreteAluno) -> Unit,
) {
    val treinos = remember(versao) { repo.treinos() }
    val totalSessoes = remember(versao) { treinos.sumOf { repo.sessoesDe(it.treinoId).size } }
    var colando by remember { mutableStateOf(false) }
    var trocando by remember { mutableStateOf(false) }
    var lembrando by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Cabecalho(
                titulo = "Meus treinos",
                subtitulo = if (treinos.isEmpty()) "Receba o treino do seu professor"
                else "${treinos.size} treino(s) • $totalSessoes sessão(ões) feitas",
                inicio = {
                    Image(
                        painter = painterResource(R.drawable.logo_app),
                        contentDescription = "FitPersonal",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
                    )
                },
                acoes = {
                    IconButton(onClick = { colando = true }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Colar código do treino", tint = Color.White)
                    }
                    IconButton(onClick = { lembrando = true }) {
                        Icon(
                            if (lembrete.ativo) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = "Lembretes de treino",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { trocando = true }) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Trocar de modo", tint = Color.White)
                    }
                },
            )

            if (treinos.isEmpty()) {
                EstadoVazioLista()
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "habitos") { CartaoHabitos() }
                    item(key = "diario") { CartaoDiario(onAbrir = onDiario, versao = versao) }
                    items(treinos.size, key = { treinos[it].treinoId }) { i ->
                        val t = treinos[i]
                        val feitas = remember(versao, t.treinoId) { repo.sessoesDe(t.treinoId).size }
                        CartaoTreino(
                            cor = corDe(i),
                            nome = t.treino.nome,
                            objetivo = t.treino.objetivo,
                            professor = t.profissional.nome,
                            revisao = t.revisao,
                            sessoes = feitas,
                            onTreinar = { onAbrir(t.treinoId) },
                            onHistorico = { onHistorico(t.treinoId) },
                        )
                    }
                }
            }
        }
    }

    if (lembrando) {
        LembreteDialog(
            inicial = lembrete,
            onSalvar = { onLembrete(it); lembrando = false },
            onFechar = { lembrando = false },
        )
    }

    if (trocando) {
        AlertDialog(
            onDismissRequest = { trocando = false },
            title = { Text("Trocar para o modo professor?") },
            text = { Text("Seus treinos recebidos continuam salvos neste aparelho. Você pode voltar ao modo aluno quando quiser.") },
            confirmButton = { TextButton(onClick = { trocando = false; onTrocarModo() }) { Text("Trocar") } },
            dismissButton = { TextButton(onClick = { trocando = false }) { Text("Cancelar") } },
        )
    }

    if (colando) {
        var texto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { colando = false },
            title = { Text("Colar código do treino") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Cole aqui a mensagem do professor") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank(),
                    onClick = {
                        colando = false
                        val msg = when (val r = repo.receber(texto)) {
                            is Recebido.Novo -> "Treino \"${r.pacote.treino.nome}\" recebido."
                            is Recebido.Atualizado -> "Treino atualizado (revisão ${r.pacote.revisao})."
                            is Recebido.JaTemIgualOuMaisNovo -> "Você já tem esta versão deste treino."
                            is Recebido.Erro -> r.mensagem
                        }
                        onMudou()
                        onMensagem(msg)
                    },
                ) { Text("Receber") }
            },
            dismissButton = { TextButton(onClick = { colando = false }) { Text("Cancelar") } },
        )
    }
}

/** Lembretes de treino: liga/desliga, dias da semana e horário. Pede a permissão de notificação ao ligar. */
@Composable
private fun LembreteDialog(
    inicial: br.com.fitpersonal.app.domain.rules.LembreteAluno,
    onSalvar: (br.com.fitpersonal.app.domain.rules.LembreteAluno) -> Unit,
    onFechar: () -> Unit,
) {
    var ativo by remember { mutableStateOf(inicial.ativo) }
    var dias by remember { mutableIntStateOf(inicial.dias) }
    var minutos by remember { mutableIntStateOf(inicial.minutos) }
    val regras = br.com.fitpersonal.app.domain.rules.LembreteAlunoRules

    fun montar() = br.com.fitpersonal.app.domain.rules.LembreteAluno(ativo, dias, minutos)

    val pedirPermissao = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { onSalvar(montar()) }
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Lembretes de treino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lembrar de treinar", modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = ativo, onCheckedChange = { ativo = it })
                }
                if (ativo) {
                    Text("Dias da semana", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        java.time.DayOfWeek.entries.forEach { d ->
                            androidx.compose.material3.FilterChip(
                                selected = regras.diaMarcado(dias, d),
                                onClick = { dias = regras.alternar(dias, d) },
                                label = { Text(regras.rotuloDia(d)) },
                            )
                        }
                    }
                    Text("Horário", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalIconButton(onClick = { minutos = (minutos - 30 + 1440) % 1440 }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Mais cedo")
                        }
                        Text(regras.rotuloHora(minutos), style = MaterialTheme.typography.headlineSmall)
                        FilledTonalIconButton(onClick = { minutos = (minutos + 30) % 1440 }) {
                            Icon(Icons.Filled.Add, contentDescription = "Mais tarde")
                        }
                    }
                    Text(
                        "Se você ficar 3 dias ou mais sem treinar, também avisamos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val precisaPermissao = ativo && android.os.Build.VERSION.SDK_INT >= 33 &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            ctx, android.Manifest.permission.POST_NOTIFICATIONS,
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (precisaPermissao) pedirPermissao.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    else onSalvar(montar())
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun EstadoVazioLista() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = null,
            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(28.dp)),
        )
        Text("Nenhum treino ainda", style = MaterialTheme.typography.titleLarge)
        PassoVazio(1, "Abra o arquivo do treino", "Toque no arquivo que seu professor enviou (WhatsApp, e-mail…).")
        PassoVazio(2, "Ou cole o código", "Toque no ícone de colar, no topo da tela, e cole a mensagem completa do professor.")
    }
}

@Composable
private fun PassoVazio(numero: Int, titulo: String, detalhe: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$numero", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                }
            }
            Column {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CartaoTreino(
    cor: Color,
    nome: String,
    objetivo: String?,
    professor: String,
    revisao: Int,
    sessoes: Int,
    onTreinar: () -> Unit,
    onHistorico: () -> Unit,
) {
    Card(
        onClick = onTreinar,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(6.dp).height(150.dp).background(cor))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = cor.copy(alpha = 0.16f), modifier = Modifier.size(46.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = cor)
                        }
                    }
                    Column {
                        Text(nome, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!objetivo.isNullOrBlank()) {
                            Text(
                                objetivo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etiqueta(professor)
                    Etiqueta("revisão $revisao")
                    Etiqueta(if (sessoes == 1) "1 sessão" else "$sessoes sessões")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTreinar) { Text("Treinar") }
                    FilledTonalButton(onClick = onHistorico) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Histórico")
                    }
                }
            }
        }
    }
}

// ───────────────────────── execução ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutarTreino(
    repo: Repositorio,
    treinoId: String,
    onVoltar: () -> Unit,
    onHistorico: () -> Unit,
    onFinalizada: () -> Unit,
) {
    val pacote = remember(treinoId) { repo.treino(treinoId) }
    if (pacote == null) {
        LaunchedEffect(Unit) { onVoltar() }
        return
    }
    val divisoes = pacote.treino.divisoes.filterValues { it.isNotEmpty() }.keys.toList()
    if (divisoes.isEmpty()) {
        LaunchedEffect(Unit) { onVoltar() }
        return
    }

    var aba by rememberSaveable { mutableIntStateOf(0) }
    val divisao = divisoes[aba.coerceIn(0, divisoes.lastIndex)]
    val lista = pacote.treino.divisoes[divisao].orEmpty()

    // estado por divisão: feito, carga digitada
    val feitos = remember(treinoId) { mutableStateMapOf<String, Boolean>() }
    val cargas = remember(treinoId) { mutableStateMapOf<String, String>() }
    var esforco by remember(treinoId, divisao) { mutableIntStateOf(0) }
    var descansoSeg by remember { mutableIntStateOf(0) }
    var descansoTotal by remember { mutableIntStateOf(0) }
    var confirmandoSaida by remember { mutableStateOf(false) }
    var assistindo by remember { mutableStateOf<java.io.File?>(null) }

    // cronômetro de descanso
    LaunchedEffect(descansoSeg) {
        if (descansoSeg > 0) {
            delay(1000)
            descansoSeg -= 1
        }
    }

    val feitosNaDivisao = lista.indices.count { feitos["$divisao$it"] == true }
    val corDivisao = corDe(aba)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column {
                    if (descansoSeg > 0) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "  Descanso  %d:%02d".format(descansoSeg / 60, descansoSeg % 60),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { descansoSeg = 0 }) { Text("Pular") }
                            }
                            LinearProgressIndicator(
                                progress = { if (descansoTotal == 0) 0f else descansoSeg / descansoTotal.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (esforco == 0) "Esforço da sessão (opcional)" else "Esforço da sessão",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (esforco > 0) Etiqueta("$esforco/10", cor = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = esforco.toFloat(), onValueChange = { esforco = it.toInt() },
                            valueRange = 0f..10f, steps = 9,
                        )
                        Button(onClick = { confirmandoSaida = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Finalizar sessão $divisao")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Cabecalho(
                titulo = pacote.treino.nome,
                subtitulo = pacote.treino.objetivo?.takeIf { it.isNotBlank() } ?: "Treino $divisao",
                inicio = {
                    IconButton(
                        onClick = onVoltar,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                acoes = {
                    IconButton(
                        onClick = onHistorico,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) { Icon(Icons.Filled.History, "Histórico") }
                },
            ) {
                LinearProgressIndicator(
                    progress = { if (lista.isEmpty()) 0f else feitosNaDivisao / lista.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f),
                )
                PilulasHorizontais(
                    listOf("$feitosNaDivisao de ${lista.size} exercícios", "Treino $divisao"),
                )
            }

            if (divisoes.size > 1) {
                TabRow(
                    selectedTabIndex = aba.coerceIn(0, divisoes.lastIndex),
                    containerColor = Color.Transparent,
                ) {
                    divisoes.forEachIndexed { i, d ->
                        Tab(selected = aba == i, onClick = { aba = i }, text = { Text("Treino $d") })
                    }
                }
            }
            // Arrastar para o lado troca de treino (A ↔ B); a lista acompanha o dedo e volta com quique.
            val arrasto = rememberArrastoHorizontal()
            val escopoGesto = rememberCoroutineScope()
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .acompanharArrasto(arrasto)
                    .pointerInput(aba, divisoes.size) {
                        if (divisoes.size < 2) return@pointerInput
                        var acumulado = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { acumulado = 0f },
                            onDragCancel = { acumulado = 0f; escopoGesto.launch { arrasto.soltar() } },
                            onDragEnd = {
                                val limiar = size.width * 0.25f
                                val atual = aba.coerceIn(0, divisoes.lastIndex)
                                val destino = when {
                                    acumulado <= -limiar && atual < divisoes.lastIndex -> atual + 1
                                    acumulado >= limiar && atual > 0 -> atual - 1
                                    else -> atual
                                }
                                acumulado = 0f
                                escopoGesto.launch { if (destino != atual) arrasto.zerar() else arrasto.soltar() }
                                if (destino != atual) aba = destino
                            },
                            onHorizontalDrag = { change, dx ->
                                change.consume()
                                acumulado += dx
                                escopoGesto.launch(start = CoroutineStart.UNDISPATCHED) { arrasto.definir(acumulado) }
                            },
                        )
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(lista.size, key = { "$divisao$it" }) { idx ->
                    val e = lista[idx]
                    val chave = "$divisao$idx"
                    // Histórico do aluno neste exercício: última carga usada e sugestão de progressão.
                    val historico = remember(treinoId, e.nome) { repo.historicoDoExercicio(treinoId, e.nome) }
                    val sugestao = remember(historico) { br.com.fitpersonal.app.domain.rules.ProgressaoRules.sugerir(historico) }
                    val padrao = remember(historico, e) {
                        br.com.fitpersonal.app.domain.rules.ProgressaoRules.ultimaCarga(historico)
                            ?.let { br.com.fitpersonal.app.domain.rules.ProgressaoRules.formatarKg(it) }
                            ?: cargaInicial(e)
                    }
                    CartaoExercicio(
                        numero = idx + 1,
                        cor = corDivisao,
                        e = e,
                        feito = feitos[chave] == true,
                        carga = cargas[chave] ?: padrao,
                        sugestao = sugestao,
                        metodo = br.com.fitpersonal.app.domain.rules.MetodoTreino.de(e.metodo),
                        bloco = br.com.fitpersonal.app.domain.rules.MetodoRules.posicaoNoBloco(lista.map { it.metodo }, idx),
                        onFeito = { feitos[chave] = it },
                        videoLocal = remember(treinoId, e.nome) { repo.videoDe(treinoId, e.nome) },
                        onAssistir = { assistindo = it },
                        onCarga = { cargas[chave] = it },
                        onDescanso = { seg ->
                            descansoTotal = seg
                            descansoSeg = seg
                        },
                    )
                }
            }
        }
    }

    assistindo?.let { PlayerVideo(it, onFechar = { assistindo = null }) }

    if (confirmandoSaida) {
        AlertDialog(
            onDismissRequest = { confirmandoSaida = false },
            title = { Text("Finalizar sessão?") },
            text = { Text("$feitosNaDivisao de ${lista.size} exercícios marcados como feitos.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoSaida = false
                        val itens = lista.mapIndexed { idx, e ->
                            val chave = "$divisao$idx"
                            val padraoSalvar = br.com.fitpersonal.app.domain.rules.ProgressaoRules
                                .ultimaCarga(repo.historicoDoExercicio(treinoId, e.nome))
                                ?.let { br.com.fitpersonal.app.domain.rules.ProgressaoRules.formatarKg(it) }
                                ?: cargaInicial(e)
                            val kg = (cargas[chave] ?: padraoSalvar).replace(',', '.').toDoubleOrNull()
                            ItemSessao(
                                exercicio = e.nome,
                                cargaKg = kg?.takeIf { it > 0 && it <= 2000 },
                                reps = null,
                                feito = feitos[chave] == true,
                            )
                        }
                        repo.registrar(
                            SessaoRef(
                                execucaoId = UUID.randomUUID().toString(),
                                data = LocalDate.now().toString(),
                                treinoId = treinoId,
                                divisao = divisao,
                                esforco = esforco.takeIf { it in 1..10 },
                                itens = itens,
                            )
                        )
                        lista.indices.forEach { feitos.remove("$divisao$it") }
                        onFinalizada()
                    },
                ) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirmandoSaida = false }) { Text("Voltar") } },
        )
    }
}

/** Player em tela cheia para o vídeo que veio no pacote do professor (VideoView do sistema, sem dependência extra). */
@Composable
private fun PlayerVideo(arquivo: java.io.File, onFechar: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onFechar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val moldura = android.widget.FrameLayout(ctx)
                        val video = android.widget.VideoView(ctx)
                        val controles = android.widget.MediaController(ctx)
                        controles.setAnchorView(moldura)
                        video.setMediaController(controles)
                        video.setVideoPath(arquivo.path)
                        video.setOnPreparedListener { it.isLooping = false; video.start() }
                        moldura.addView(
                            video,
                            android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.Gravity.CENTER,
                            ),
                        )
                        moldura
                    },
                )
                IconButton(
                    onClick = onFechar,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding(),
                ) { Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White) }
            }
        }
    }
}

/** Número inicial da carga a partir do texto do professor ("20 kg" → "20"). */
private fun cargaInicial(e: ExercicioRef): String =
    Regex("""\d+(?:[.,]\d+)?""").find(e.carga.orEmpty())?.value.orEmpty()

@Composable
private fun CartaoExercicio(
    numero: Int,
    cor: Color,
    e: ExercicioRef,
    feito: Boolean,
    carga: String,
    onFeito: (Boolean) -> Unit,
    onCarga: (String) -> Unit,
    onDescanso: (Int) -> Unit,
    sugestao: br.com.fitpersonal.app.domain.rules.SugestaoCarga? = null,
    metodo: br.com.fitpersonal.app.domain.rules.MetodoTreino? = null,
    /** Posição e tamanho do bloco de bi-set/circuito a que o exercício pertence. */
    bloco: Pair<Int, Int>? = null,
    videoLocal: java.io.File? = null,
    onAssistir: (java.io.File) -> Unit = {},
) {
    val seg = DescansoRules.segundos(e.descanso)
    val fundo by animateColorAsState(
        if (feito) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "fundoExercicio",
    )
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = fundo),
        border = BorderStroke(1.dp, if (feito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = cor, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$numero", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(e.nome, style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        e.series?.let { Etiqueta("$it séries") }
                        e.repeticoes?.let { Etiqueta("$it reps") }
                        e.descanso?.let { Etiqueta("descanso $it") }
                    }
                    if (metodo != null) {
                        Etiqueta(
                            if (bloco != null) metodo.rotulo + " " + bloco.first + " de " + bloco.second else metodo.rotulo,
                            cor = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            metodo.explicacao,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    e.obs?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledIconToggleButton(checked = feito, onCheckedChange = onFeito, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = if (feito) "Feito" else "Marcar como feito")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { onCarga(ajustarCarga(carga, -1.0)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Diminuir carga")
                }
                OutlinedTextField(
                    value = carga,
                    onValueChange = { onCarga(it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6)) },
                    label = { Text("Carga (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = { onCarga(ajustarCarga(carga, 1.0)) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumentar carga")
                }
            }

            if (metodo != null && metodo.cronometrado) CronometroMetodo(metodo)

            // Progressão automática: só aparece se a carga atual ainda não é a sugerida.
            if (sugestao != null && carga.replace(',', '.').toDoubleOrNull() != sugestao.cargaKg) {
                val texto = br.com.fitpersonal.app.domain.rules.ProgressaoRules.formatarKg(sugestao.cargaKg)
                Surface(
                    onClick = { onCarga(texto) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            "Sugestão: subir para $texto kg",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            sugestao.motivo + " Toque para usar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            if (videoLocal != null) {
                Button(onClick = { onAssistir(videoLocal) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Assistir vídeo do exercício")
                }
            }
            val contexto = LocalContext.current
            e.video?.takeIf { videoLocal == null }?.let { url ->
                OutlinedButton(
                    onClick = {
                        runCatching {
                            contexto.startActivity(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver vídeo do exercício")
                }
            }

            if (seg != null) {
                OutlinedButton(onClick = { onDescanso(seg) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar descanso de $seg s")
                }
            }
        }
    }
}

/** Soma [delta] kg ao valor digitado (vírgula ou ponto); nunca fica abaixo de zero (zero vira vazio). */
private fun ajustarCarga(texto: String, delta: Double): String {
    val atual = texto.replace(',', '.').toDoubleOrNull() ?: 0.0
    val novo = atual + delta
    return if (novo <= 0.0) "" else formatarKg(novo)
}

// ───────────────────────── histórico e envio do resumo ─────────────────────────

@Composable
private fun Historico(repo: Repositorio, treinoId: String, onVoltar: () -> Unit) {
    val ctx = LocalContext.current
    val pacote = remember(treinoId) { repo.treino(treinoId) }
    val sessoes = remember(treinoId) { repo.sessoesDe(treinoId) }
    var mensagem by remember { mutableStateOf("") }
    var incluirHabitos by remember { mutableStateOf(true) }
    val textoHabitos = remember {
        val d = HabitosStore(ctx.applicationContext).ler()
        if (d.dias.isEmpty() && d.checkins.isEmpty()) null
        else br.com.fitpersonal.app.domain.rules.HabitosRules.textoSemana(d.dias, d.checkins, LocalDate.now())
    }
    /** Recado digitado + hábitos da semana, dentro do limite do contrato (2000 caracteres). */
    fun mensagemFinal(): String? =
        listOfNotNull(
            mensagem.trim().ifEmpty { null },
            textoHabitos?.takeIf { incluirHabitos },
        ).joinToString("\n").take(2000).ifEmpty { null }

    val esforcos = sessoes.mapNotNull { it.esforco }
    val mediaEsforco = if (esforcos.isEmpty()) null else esforcos.average()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Cabecalho(
                titulo = "Histórico",
                subtitulo = pacote?.treino?.nome.orEmpty(),
                inicio = {
                    IconButton(
                        onClick = onVoltar,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
            ) {
                PilulasHorizontais(
                    listOfNotNull(
                        if (sessoes.size == 1) "1 sessão" else "${sessoes.size} sessões",
                        mediaEsforco?.let { "esforço médio " + String.format(Locale("pt", "BR"), "%.1f", it) },
                    ),
                )
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sessoes.isEmpty()) {
                    item {
                        Text(
                            "Nenhuma sessão registrada ainda. Finalize um treino para ver o histórico aqui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(sessoes, key = { it.execucaoId }) { s ->
                    val feitos = s.itens.count { it.feito }
                    val cor = corDe(CoresDivisaoIndice(s.divisao))
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.width(6.dp).height(110.dp).background(cor))
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatarData(s.data),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Etiqueta("Treino ${s.divisao}")
                                }
                                LinearProgressIndicator(
                                    progress = { if (s.itens.isEmpty()) 0f else feitos / s.itens.size.toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = cor,
                                )
                                Text(
                                    "$feitos de ${s.itens.size} exercícios" +
                                        (s.esforco?.let { " • esforço $it/10" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                val cargas = s.itens.filter { it.feito }.mapNotNull { i ->
                                    i.cargaKg?.let { "${i.exercicio}: ${formatarKg(it)} kg" }
                                }
                                if (cargas.isNotEmpty()) {
                                    Text(
                                        cargas.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                if (sessoes.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Enviar resumo ao professor",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                OutlinedTextField(
                                    value = mensagem,
                                    onValueChange = { mensagem = it.take(1500) },
                                    label = { Text("Recado (opcional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (textoHabitos != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.Checkbox(checked = incluirHabitos, onCheckedChange = { incluirHabitos = it })
                                        Text(
                                            "Incluir hábitos e check-in da semana",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        val r = repo.resumoDe(treinoId)?.copy(mensagem = mensagemFinal())
                                        if (r != null) {
                                            val codigo = CodigoTexto.codificar(ContratoJson.serializar(r))
                                            val envio = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Meu resumo de treino:\n$codigo")
                                            }
                                            ctx.startActivity(Intent.createChooser(envio, "Enviar resumo"))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Enviar resumo")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val r = repo.resumoDe(treinoId)?.copy(mensagem = mensagemFinal())
                                        if (r != null) {
                                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(
                                                ClipData.newPlainText("Resumo", CodigoTexto.codificar(ContratoJson.serializar(r))),
                                            )
                                            versaoMensagem = "Código copiado."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Copiar código")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A=0, B=1, C=2, D=3 (qualquer outra divisão cai na primeira cor). */
@Suppress("FunctionName")
private fun CoresDivisaoIndice(divisao: String): Int = when (divisao.uppercase(Locale.ROOT)) {
    "B" -> 1
    "C" -> 2
    "D" -> 3
    else -> 0
}
