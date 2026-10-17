package br.com.fitpersonal.app.ui.treinos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import br.com.fitpersonal.app.ui.theme.corSobre
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.common.ChipStatus
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.ItemComAcento
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import br.com.fitpersonal.app.ui.theme.Paleta
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.DivisaoTreino
import br.com.fitpersonal.app.domain.model.ExercicioTreino
import br.com.fitpersonal.app.domain.rules.TreinoRules
import br.com.fitpersonal.app.ui.common.CampoBusca
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.CompartilharPdfEffect
import br.com.fitpersonal.app.ui.common.EstadoVazio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreinoEditorScreen(
    onVoltar: () -> Unit,
    onExecutar: (Long) -> Unit = {},
    vm: TreinoFormViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ctxLocal = androidx.compose.ui.platform.LocalContext.current
    val videosMapa by vm.videos.collectAsStateWithLifecycle()
    val videoOcupado by vm.videoOcupado.collectAsStateWithLifecycle()
    val avaliacoesAluno by vm.avaliacoes.collectAsStateWithLifecycle()
    val metasAluno by vm.metasAluno.collectAsStateWithLifecycle()
    var reproduzindo by remember { mutableStateOf<java.io.File?>(null) }
    val videosAcoes = VideosAcoes(
        mapa = videosMapa,
        novaUriGravacao = vm::novaUriVideo,
        importar = vm::importarVideo,
        remover = vm::removerVideo,
        assistir = { v -> reproduzindo = vm.arquivoVideo(v) },
        compartilhar = { v -> br.com.fitpersonal.app.ui.common.VideoIntents.compartilhar(ctxLocal, vm.uriVideo(v), v.nome) },
        abrirLink = { url ->
            if (!br.com.fitpersonal.app.ui.common.VideoIntents.abrirLink(ctxLocal, url)) {
                vm.avisar("Nenhum aplicativo para abrir o link.")
            }
        },
        ocupado = videoOcupado,
    )
    var menu by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var confirmarSaida by remember { mutableStateOf(false) }
    var catalogoAberto by remember { mutableStateOf(false) }
    /** Texto do diálogo "Salvar como modelo"; null = fechado. */
    var nomeModelo by remember { mutableStateOf<String?>(null) }
    // Exercício aberto no diálogo: Pair(uid?, exercício). uid null = novo.
    var dialogo by remember { mutableStateOf<Pair<Long?, ExercicioTreino>?>(null) }

    LaunchedEffect(s.concluido) { if (s.concluido) onVoltar() }
    CompartilharPdfEffect(s.pdf, vm::pdfConsumido)

    val ctxEntrega = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(s.html) {
        s.html?.let {
            br.com.fitpersonal.app.ui.common.PdfSharer.compartilhar(ctxEntrega, it, "Seu treino", "text/html")
            vm.entregaConsumida()
        }
    }
    LaunchedEffect(s.arquivoAluno) {
        s.arquivoAluno?.let {
            br.com.fitpersonal.app.ui.common.PdfSharer.compartilhar(
                ctxEntrega, it, "Seu treino", "application/octet-stream",
            )
            vm.entregaConsumida()
        }
    }
    LaunchedEffect(s.texto) {
        s.texto?.let {
            br.com.fitpersonal.app.ui.common.PdfSharer.compartilharTexto(ctxEntrega, it, "Seu treino")
            vm.entregaConsumida()
        }
    }
    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }

    fun tentarSair() { if (s.alterado) confirmarSaida = true else onVoltar() }
    BackHandler(enabled = s.alterado) { confirmarSaida = true }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (s.editando) "Editar treino" else "Novo treino") },
                navigationIcon = { IconButton(onClick = ::tentarSair) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    TextButton(onClick = vm::salvar) { Text("Salvar") }
                    if (s.editando) {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Modo treino (salve antes)" else "Iniciar modo treino") },
                                    enabled = !s.alterado && s.exercicios.isNotEmpty(),
                                    onClick = { menu = false; onExecutar(vm.idTreino) },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Duplicar (salve antes)" else "Duplicar treino") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; vm.duplicar() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Compartilhar PDF (salve antes)" else "Compartilhar PDF") },
                                    enabled = !s.alterado && !s.gerandoPdf,
                                    onClick = { menu = false; vm.gerarPdf() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Enviar ao aluno (salve antes)" else "Enviar ao aluno (página)") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; vm.enviarComoPagina() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "App do aluno (salve antes)" else "Enviar para o app do aluno") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; vm.enviarParaAppDoAluno(comoCodigo = false) },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Treino com vídeos (salve antes)" else "Enviar treino com vídeos") },
                                    enabled = !s.alterado && !videoOcupado,
                                    onClick = { menu = false; vm.enviarPacoteComVideos() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Código (salve antes)" else "Enviar código do app do aluno") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; vm.enviarParaAppDoAluno(comoCodigo = true) },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Enviar texto (salve antes)" else "Enviar como texto") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; vm.enviarComoTexto() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (s.alterado) "Salvar como modelo (salve antes)" else "Salvar como modelo") },
                                    enabled = !s.alterado,
                                    onClick = { menu = false; nomeModelo = s.nome },
                                )
                                DropdownMenuItem(
                                    text = { Text("Excluir treino") },
                                    onClick = { menu = false; confirmarExclusao = true },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (s.carregando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val porDivisao = TreinoRules.agruparPorDivisao(s.exercicios.map { it.ex })
        val daAba = s.exercicios.filter { it.ex.divisao == s.divisao }

        val corDaDivisao = corDivisao(s.divisao)

        Column(Modifier.padding(padding).fillMaxSize()) {
            CabecalhoTela(
                titulo = s.nome.ifBlank { if (s.editando) "Treino" else "Novo treino" },
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Coral,
                subtitulo = s.objetivo.ifBlank { null },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                LinhaPilulas(
                    buildList {
                        add("${s.exercicios.size} exercício(s)")
                        DivisaoTreino.entries.forEach { d ->
                            val n = porDivisao[d]?.size ?: 0
                            if (n > 0) add("${d.name} · $n")
                        }
                        if (s.alterado) add("não salvo")
                    },
                )
            }

            CartaoSecao(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = s.nome, onValueChange = vm::alterarNome,
                    label = { Text("Nome do treino *") },
                    isError = s.erroNome != null,
                    supportingText = s.erroNome?.let { { Text(it) } },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = s.objetivo, onValueChange = vm::alterarObjetivo,
                    label = { Text("Objetivo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }

            br.com.fitpersonal.app.ui.evolucao.ComposicaoNoTreino(
                avaliacoes = avaliacoesAluno,
                metas = metasAluno,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )

            // Divisões A–D: blocos coloridos com a contagem de exercícios
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DivisaoTreino.entries.forEach { d ->
                    val cor = corDivisao(d)
                    val selecionada = s.divisao == d
                    val n = porDivisao[d]?.size ?: 0
                    Surface(
                        onClick = { vm.selecionarDivisao(d) },
                        shape = MaterialTheme.shapes.large,
                        color = if (selecionada) cor else cor.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                d.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selecionada) corSobre(cor) else cor,
                            )
                            Text(
                                "$n exerc.",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selecionada) corSobre(cor).copy(alpha = 0.9f) else cor,
                            )
                        }
                    }
                }
            }

            if (daAba.isEmpty()) {
                EstadoVazio(
                    icone = Icons.Filled.FitnessCenter,
                    titulo = "Divisão ${s.divisao.name} vazia",
                    texto = "Adicione exercícios do catálogo ou crie um manualmente.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(daAba, key = { _, d -> d.uid }) { indice, d ->
                        ItemComAcento(cor = corDaDivisao, modifier = Modifier.animateItem()) {
                            CartaoItem(
                                onClick = { dialogo = d.uid to d.ex },
                                titulo = d.ex.nome,
                                subtitulo = listOfNotNull(
                                    br.com.fitpersonal.app.domain.rules.MetodoTreino.de(d.ex.metodo)?.rotulo,
                                    TreinoRules.resumo(d.ex).takeIf { it.isNotEmpty() },
                                ).joinToString(" • ").takeIf { it.isNotEmpty() },
                                inicio = {
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape).background(corDaDivisao.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${indice + 1}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = corDaDivisao,
                                        )
                                    }
                                },
                                fim = {
                                    IconButton(onClick = { vm.removerExercicio(d.uid) }) {
                                        Icon(Icons.Default.Close, "Remover ${d.ex.nome}")
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { dialogo = null to ExercicioTreino(idTreino = 0, nome = "", divisao = s.divisao) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Manual") }
                    Button(onClick = { catalogoAberto = true }, modifier = Modifier.weight(1f)) {
                        Text("Do catálogo")
                    }
                }
            }
        }
    }

    if (catalogoAberto) CatalogoSheet(vm = vm, divisao = s.divisao, onFechar = { catalogoAberto = false })

    reproduzindo?.let { br.com.fitpersonal.app.ui.common.PlayerVideoDialog(it, onFechar = { reproduzindo = null }) }

    dialogo?.let { (uid, ex) ->
        ExercicioDialog(
            inicial = ex,
            acoes = videosAcoes,
            onConfirmar = { vm.salvarExercicio(uid, it); dialogo = null },
            onCancelar = { dialogo = null },
        )
    }

    nomeModelo?.let { texto ->
        AlertDialog(
            onDismissRequest = { nomeModelo = null },
            title = { Text("Salvar como modelo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "O modelo guarda os exercícios, sem vínculo com o aluno, e pode ser aplicado a outros alunos.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nomeModelo = it },
                        label = { Text("Nome do modelo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = texto.isNotBlank(),
                    onClick = { vm.salvarComoModelo(texto); nomeModelo = null },
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { nomeModelo = null }) { Text("Cancelar") } },
        )
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir treino?") },
            text = { Text("O treino e todos os seus exercícios serão removidos.") },
            confirmButton = { TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Descartar alterações?") },
            text = { Text("Há alterações não salvas neste treino.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; onVoltar() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar editando") } },
        )
    }
}

// ───────────────────────── Catálogo ─────────────────────────

/** Grupos musculares mais usados como atalhos de busca. */
private val ATALHOS_CATALOGO = listOf(
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Pernas", "Glúteos", "Abdômen", "Cardio",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogoSheet(vm: TreinoFormViewModel, divisao: DivisaoTreino, onFechar: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = s.catalogo
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cor = corDivisao(divisao)
    var expandido by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = { vm.limparSelecaoCatalogo(); onFechar() }, sheetState = sheet) {
        Column(Modifier.fillMaxHeight(0.92f)) {
            // Cabeçalho
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Catálogo de exercícios", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Adicionando à divisão ${divisao.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (c.selecionados.isNotEmpty()) {
                    Surface(shape = MaterialTheme.shapes.large, color = cor) {
                        Text(
                            "${c.selecionados.size} selecionado(s)",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = corSobre(cor),
                        )
                    }
                }
            }

            CampoBusca(
                valor = c.termo,
                onValorChange = vm::buscarCatalogo,
                placeholder = "Nome, grupo muscular ou equipamento",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Atalhos por grupo muscular
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ATALHOS_CATALOGO.forEach { grupo ->
                    FilterChip(
                        selected = c.termo.equals(grupo, ignoreCase = true),
                        onClick = { vm.buscarCatalogo(grupo) },
                        label = { Text(grupo) },
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                when {
                    c.termo.isBlank() -> EstadoVazio(
                        icone = Icons.Filled.FitnessCenter,
                        titulo = "Busque um exercício",
                        texto = "Digite um termo ou toque em um grupo muscular acima.",
                    )
                    c.carregando && c.resultados.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    c.resultados.isEmpty() -> EstadoVazio(
                        icone = Icons.Filled.FitnessCenter,
                        titulo = "Nenhum exercício encontrado",
                        texto = "Tente outro termo (ex.: o nome do músculo ou do equipamento).",
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(c.resultados, key = { it.id }) { item ->
                            val marcado = item.id in c.selecionados
                            Card(
                                onClick = { vm.alternarSelecao(item) },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (marcado) cor.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                                ),
                                border = BorderStroke(
                                    if (marcado) 2.dp else 1.dp,
                                    if (marcado) cor else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.fillMaxWidth().animateContentSize(),
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = marcado, onCheckedChange = { vm.alternarSelecao(item) })
                                        Column(Modifier.weight(1f)) {
                                            Text(item.nome, style = MaterialTheme.typography.titleMedium)
                                            if (item.musculosPrimarios.isNotEmpty()) {
                                                Text(
                                                    item.musculosPrimarios.joinToString(", "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        if (item.instrucoes.isNotEmpty()) {
                                            IconButton(
                                                onClick = { expandido = if (expandido == item.id) null else item.id },
                                            ) {
                                                Icon(
                                                    if (expandido == item.id) Icons.Filled.KeyboardArrowUp
                                                    else Icons.Filled.KeyboardArrowDown,
                                                    contentDescription = "Instruções",
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        listOf(item.categoria, item.equipamento, item.nivel)
                                            .filter { it.isNotBlank() }
                                            .forEach { ChipStatus(it, TomChip.Neutro) }
                                    }
                                    if (expandido == item.id) {
                                        br.com.fitpersonal.app.ui.common.AnimacaoMovimento(item.id, "Como executar: " + item.nome, Modifier.fillMaxWidth())
                                        val fotos = br.com.fitpersonal.app.domain.rules.CatalogoImagens.urls(item.imagens)
                                        if (fotos.isNotEmpty()) {
                                            Row(
                                                Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                fotos.forEach { url ->
                                                    coil.compose.AsyncImage(
                                                        model = url,
                                                        contentDescription = "Demonstração: " + item.nome,
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(150.dp)
                                                            .clip(MaterialTheme.shapes.medium)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                                    )
                                                }
                                            }
                                        }
                                        item.instrucoes.take(4).forEachIndexed { i, passo ->
                                            Text(
                                                "${i + 1}. $passo",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (c.temMais) item {
                            TextButton(onClick = vm::maisResultados, modifier = Modifier.fillMaxWidth()) {
                                Text(if (c.carregando) "Carregando…" else "Carregar mais")
                            }
                        }
                    }
                }
            }

            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Button(
                    onClick = { vm.adicionarSelecionados(); onFechar() },
                    enabled = c.selecionados.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = cor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(
                        if (c.selecionados.isEmpty()) "Selecione exercícios"
                        else "Adicionar ${c.selecionados.size} à divisão ${divisao.name}",
                    )
                }
            }
        }
    }
}

// ───────────────────────── Diálogo de exercício ─────────────────────────

/** Fileira de atalhos que preenche um campo de texto (séries, repetições, descanso). */
@Composable
private fun Atalhos(opcoes: List<String>, atual: String, onEscolher: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opcoes.forEach { o ->
            FilterChip(selected = atual.trim() == o, onClick = { onEscolher(o) }, label = { Text(o) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercicioDialog(
    inicial: ExercicioTreino,
    acoes: VideosAcoes,
    onConfirmar: (ExercicioTreino) -> Unit,
    onCancelar: () -> Unit,
) {
    var nome by remember { mutableStateOf(inicial.nome) }
    var series by remember { mutableStateOf(inicial.series.orEmpty()) }
    var reps by remember { mutableStateOf(inicial.repeticoes.orEmpty()) }
    var carga by remember { mutableStateOf(inicial.carga.orEmpty()) }
    var descanso by remember { mutableStateOf(inicial.descanso.orEmpty()) }
    var obs by remember { mutableStateOf(inicial.observacoes.orEmpty()) }
    var video by remember { mutableStateOf(inicial.video.orEmpty()) }
    var metodo by remember { mutableStateOf(inicial.metodo) }
    var divisao by remember { mutableStateOf(inicial.divisao) }
    var erro by remember { mutableStateOf<String?>(null) }
    val novo = inicial.nome.isEmpty()

    fun salvar() {
        val e = TreinoRules.validarExercicio(nome)
        if (e != null) { erro = e; return }
        if (br.com.fitpersonal.app.domain.rules.VideoRules.erro(video) != null) return
        fun String.ou() = trim().ifEmpty { null }
        onConfirmar(
            inicial.copy(
                nome = nome.trim(), series = series.ou(), repeticoes = reps.ou(), carga = carga.ou(),
                descanso = descanso.ou(), observacoes = obs.ou(), divisao = divisao,
                video = br.com.fitpersonal.app.domain.rules.VideoRules.normalizar(video),
                metodo = metodo,
            ),
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancelar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 640.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Faixa de título na cor da divisão escolhida
                val corAtual = corDivisao(divisao)
                Row(
                    Modifier.fillMaxWidth().background(corAtual).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (novo) "Novo exercício" else "Editar exercício",
                            style = MaterialTheme.typography.titleLarge,
                            color = corSobre(corAtual),
                        )
                        Text(
                            "Divisão ${divisao.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = corSobre(corAtual).copy(alpha = 0.9f),
                        )
                    }
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.Close, "Fechar", tint = corSobre(corAtual))
                    }
                }

                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        nome, { nome = it; erro = null }, label = { Text("Exercício *") },
                        isError = erro != null, supportingText = erro?.let { { Text(it) } },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )

                    br.com.fitpersonal.app.ui.common.AnimacaoMovimento(inicial.catalogoId, "Como executar: " + nome, Modifier.fillMaxWidth())

                    // Divisão em blocos coloridos
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DivisaoTreino.entries.forEach { d ->
                            val c = corDivisao(d)
                            val sel = divisao == d
                            Surface(
                                onClick = { divisao = d },
                                shape = MaterialTheme.shapes.medium,
                                color = if (sel) c else c.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    d.name,
                                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (sel) corSobre(c) else c,
                                )
                            }
                        }
                    }

                    // Método de execução: bi-set e circuito agrupam exercícios vizinhos com o mesmo método
                    Text("Método", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = metodo == null, onClick = { metodo = null }, label = { Text("Normal") })
                        br.com.fitpersonal.app.domain.rules.MetodoTreino.entries.forEach { m ->
                            FilterChip(selected = metodo == m.id, onClick = { metodo = m.id }, label = { Text(m.rotulo) })
                        }
                    }
                    br.com.fitpersonal.app.domain.rules.MetodoTreino.de(metodo)?.let {
                        Text(
                            it.explicacao + if (it.agrupa) " Marque o mesmo método no exercício vizinho para formar o bloco." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text("Séries", style = MaterialTheme.typography.labelLarge)
                    Atalhos(listOf("2", "3", "4", "5"), series) { series = it }
                    OutlinedTextField(series, { series = it }, label = { Text("Séries") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text("Repetições", style = MaterialTheme.typography.labelLarge)
                    Atalhos(listOf("6", "8", "10", "12", "15", "20"), reps) { reps = it }
                    OutlinedTextField(reps, { reps = it }, label = { Text("Reps (ex.: 10-12)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text("Descanso", style = MaterialTheme.typography.labelLarge)
                    Atalhos(listOf("30 s", "45 s", "60 s", "90 s", "2 min"), descanso) { descanso = it }
                    OutlinedTextField(descanso, { descanso = it }, label = { Text("Descanso") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        carga, { carga = it }, label = { Text("Carga (ex.: 20 kg)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        obs, { obs = it }, label = { Text("Observações") },
                        minLines = 2, modifier = Modifier.fillMaxWidth(),
                    )

                    SecaoVideo(nome = nome, link = video, onLink = { video = it }, acoes = acoes)
                }

                Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            onClick = ::salvar,
                            colors = ButtonDefaults.buttonColors(containerColor = corAtual),
                            modifier = Modifier.weight(1f),
                        ) { Text(if (novo) "Adicionar" else "Salvar") }
                    }
                }
            }
        }
    }
}

/** Cor própria de cada divisão (A, B, C, D) para blocos, barras e numeração. */
@Composable
private fun corDivisao(d: DivisaoTreino): Color = when (d) {
    DivisaoTreino.A -> MaterialTheme.colorScheme.primary
    DivisaoTreino.B -> MaterialTheme.colorScheme.secondary
    DivisaoTreino.C -> MaterialTheme.colorScheme.tertiary
    DivisaoTreino.D -> Paleta.Rosa600
}
