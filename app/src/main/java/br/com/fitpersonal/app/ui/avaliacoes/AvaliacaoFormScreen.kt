package br.com.fitpersonal.app.ui.avaliacoes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.CondicaoSaude
import br.com.fitpersonal.app.domain.model.ProtocoloDobras
import br.com.fitpersonal.app.domain.model.SexoBiologico
import br.com.fitpersonal.app.domain.rules.*
import androidx.compose.material.icons.filled.Share
import br.com.fitpersonal.app.ui.common.CompartilharPdfEffect
import br.com.fitpersonal.app.ui.common.IndicadorEtapas
import br.com.fitpersonal.app.ui.common.Masks
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoFormScreen(onVoltar: () -> Unit, vm: AvaliacaoFormViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmarExclusao by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(s.concluido) { if (s.concluido) onVoltar() }
    CompartilharPdfEffect(s.pdf, vm::pdfConsumido)
    LaunchedEffect(s.mensagem) {
        s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (s.editando) "Editar avaliação" else "Nova avaliação") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    if (s.editando) {
                        IconButton(onClick = vm::gerarPdf, enabled = !s.gerandoPdf) {
                            Icon(Icons.Default.Share, "Compartilhar PDF da avaliação salva")
                        }
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Default.Delete, "Excluir avaliação")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { vm.irPara(s.passo - 1) },
                        enabled = s.passo > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text("Voltar") }
                    if (s.passo < PASSOS - 1) {
                        Button(onClick = { vm.irPara(s.passo + 1) }, modifier = Modifier.weight(1f)) {
                            Text("Próximo: ${TITULOS_PASSO[s.passo + 1]}", maxLines = 1)
                        }
                    } else {
                        Button(onClick = vm::salvar, modifier = Modifier.weight(1f)) { Text("Salvar avaliação") }
                    }
                }
            }
        },
    ) { padding ->
        if (s.carregando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            CabecalhoTela(
                titulo = s.aluno?.nome ?: if (s.editando) "Editar avaliação" else "Nova avaliação",
                subtitulo = "Etapa ${s.passo + 1} de $PASSOS • ${TITULOS_PASSO[s.passo]}",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { (s.passo + 1f) / PASSOS },
                    modifier = Modifier.fillMaxWidth(),
                    color = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto,
                    trackColor = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto.copy(alpha = 0.2f),
                )
                LinhaPilulas(
                    listOfNotNull(
                        s.resumo.imc?.let { "IMC " + String.format(java.util.Locale("pt", "BR"), "%.1f", it) },
                        s.resumo.classeImc?.rotulo,
                        s.resumo.pressao?.takeIf { it.alerta }?.let { "PA: ${it.rotulo}" },
                        if (s.resumo.alertas.isNotEmpty()) "${s.resumo.alertas.size} alerta(s)" else null,
                    ),
                )
            }
            IndicadorEtapas(
                atual = s.passo,
                titulos = TITULOS_PASSO,
                onIr = vm::irPara,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            AnimatedContent(
                targetState = s.passo,
                transitionSpec = {
                    val avancando = targetState > initialState
                    (slideInHorizontally(tween(200)) { w -> if (avancando) w / 8 else -w / 8 } + fadeIn(tween(160))) togetherWith
                        (slideOutHorizontally(tween(200)) { w -> if (avancando) -w / 8 else w / 8 } + fadeOut(tween(90)))
                },
                modifier = Modifier.weight(1f),
                label = "etapa",
            ) { passo ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CartaoSecao(titulo = TITULOS_PASSO[passo]) {
                        when (passo) {
                            0 -> PassoMedidas(s, vm)
                            1 -> PassoSaude(s, vm)
                            2 -> PassoFisica(s, vm)
                            else -> PassoFotos(s, vm)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir avaliação?") },
            text = { Text("A avaliação e suas fotos serão removidas. Esta ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}

// ───────────────────────── Etapas ─────────────────────────

@Composable
private fun PassoMedidas(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    val i = s.input
    Campo("Data", i.data, s.erros[CampoAvaliacao.DATA], Masks.DATA, KeyboardType.Number, max = 8, placeholder = "DD/MM/AAAA") { v ->
        vm.alterar(CampoAvaliacao.DATA) { it.copy(data = v) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("Peso (kg)", i.peso, s.erros[CampoAvaliacao.PESO], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.PESO) { it.copy(peso = v) }
        }
        Campo(
            "Altura (m)", i.altura, s.erros[CampoAvaliacao.ALTURA],
            teclado = KeyboardType.Decimal, placeholder = "1,75", modifier = Modifier.weight(1f),
            cursorNoFim = true,
        ) { v ->
            vm.alterar(CampoAvaliacao.ALTURA) { it.copy(altura = AvaliacaoValidator.formatarAltura(v, i.altura)) }
        }
    }
    Campo("% de gordura", i.percentualGordura, s.erros[CampoAvaliacao.PERCENTUAL], teclado = KeyboardType.Decimal) { v ->
        vm.alterar(CampoAvaliacao.PERCENTUAL) { it.copy(percentualGordura = v) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("Massa magra (kg)", i.massaMagra, s.erros[CampoAvaliacao.MASSA_MAGRA], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.MASSA_MAGRA) { it.copy(massaMagra = v) }
        }
        Campo("Massa gorda (kg)", i.massaGorda, s.erros[CampoAvaliacao.MASSA_GORDA], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.MASSA_GORDA) { it.copy(massaGorda = v) }
        }
    }
    CartaoResumo(s.resumo)
    br.com.fitpersonal.app.ui.evolucao.PainelComposicao(
        atual = avaliacaoAoVivo(s),
        historico = s.historico,
    )
}

/** Avaliação montada com o que já foi digitado (campos inválidos ficam de fora), para o painel ao vivo. */
private fun avaliacaoAoVivo(s: AvaliacaoFormState): AvaliacaoFisica {
    val i = s.input
    fun d(t: String) = AvaliacaoValidator.parseDecimal(t)
    return AvaliacaoFisica(
        id = Long.MAX_VALUE,
        idAluno = s.aluno?.id ?: 0L,
        data = AlunoValidator.parseData(i.data) ?: java.time.LocalDate.now(),
        pesoKg = d(i.peso),
        alturaM = d(i.altura),
        percentualGordura = d(i.percentualGordura),
        massaMagraKg = d(i.massaMagra) ?: s.resumo.massaMagraKg,
        massaGordaKg = d(i.massaGorda) ?: s.resumo.massaGordaKg,
        cinturaCm = d(i.cintura),
        quadrilCm = d(i.quadril),
    )
}

@Composable
private fun PassoSaude(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    val i = s.input
    Text("Pressão arterial (mmHg)", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("Sistólica", i.pressaoSistolica, s.erros[CampoAvaliacao.PAS], teclado = KeyboardType.Number, max = 3, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.PAS) { it.copy(pressaoSistolica = v) }
        }
        Campo("Diastólica", i.pressaoDiastolica, s.erros[CampoAvaliacao.PAD], teclado = KeyboardType.Number, max = 3, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.PAD) { it.copy(pressaoDiastolica = v) }
        }
    }
    s.resumo.pressao?.let { p ->
        val popular = s.resumo.pressaoPopular?.let { " ($it)" }.orEmpty()
        Text("${p.rotulo}$popular", color = if (p.alerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }

    Text("Perfil lipídico (mg/dL)", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("LDL", i.ldl, s.erros[CampoAvaliacao.LDL], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.LDL) { it.copy(ldl = v) }
        }
        Campo("HDL", i.hdl, s.erros[CampoAvaliacao.HDL], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.HDL) { it.copy(hdl = v) }
        }
    }

    Text("Condições", style = MaterialTheme.typography.titleSmall)
    FlowCondicoes(i.condicoes) { c, marcado ->
        vm.alterar { it.copy(condicoes = if (marcado) it.condicoes + c else it.condicoes - c) }
    }

    SecaoParq(i.parq, vm)

    Campo("Histórico de saúde", i.historicoSaude, null, linhas = 3) { v -> vm.alterar { it.copy(historicoSaude = v) } }
    Campo("Anamnese", i.anamnese, null, linhas = 3) { v -> vm.alterar { it.copy(anamnese = v) } }
    Campo("Estilo de vida", i.estiloVida, null, linhas = 3) { v -> vm.alterar { it.copy(estiloVida = v) } }
    Campo("Metas", i.metas, null, linhas = 3) { v -> vm.alterar { it.copy(metas = v) } }

    SecaoAssinatura(s.assinatura, vm)

    ListaAvisos(s.resumo.avisos, mostrarTudoCerto = false)
}

@Composable
private fun SecaoParq(codigo: String, vm: AvaliacaoFormViewModel) {
    val respostas = ParqRules.decodificar(codigo)
    Text("PAR-Q (prontidão para atividade física)", style = MaterialTheme.typography.titleSmall)
    Text(
        "Opcional. Qualquer \"sim\" sugere liberação médica antes de iniciar ou intensificar o treino.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ParqRules.PERGUNTAS.forEachIndexed { idx, pergunta ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${idx + 1}. $pergunta", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = respostas[idx] == true,
                    onClick = { vm.responderParq(idx, true) },
                    label = { Text("Sim") },
                )
                FilterChip(
                    selected = respostas[idx] == false,
                    onClick = { vm.responderParq(idx, false) },
                    label = { Text("Não") },
                )
            }
        }
    }
}

@Composable
private fun SecaoAssinatura(assinatura: String?, vm: AvaliacaoFormViewModel) {
    Text("Assinatura do aluno", style = MaterialTheme.typography.titleSmall)
    if (assinatura != null) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(120.dp)
                .androidxBackgroundWhite(),
        ) {
            AsyncImage(
                model = vm.arquivoFoto(assinatura),
                contentDescription = "Assinatura do aluno",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        OutlinedButton(onClick = vm::removerAssinatura) { Text("Refazer assinatura") }
    } else {
        AssinaturaPad(onConfirmar = vm::definirAssinatura)
    }
}

private fun Modifier.androidxBackgroundWhite(): Modifier =
    this.then(Modifier.background(androidx.compose.ui.graphics.Color.White))

@Composable
private fun PassoFisica(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    val i = s.input
    SecaoDobras(s, vm)
    Campo("Dobras — observações", i.dobrasCutaneas, null, linhas = 2, placeholder = "Ex.: medidas do lado direito") { v ->
        vm.alterar { it.copy(dobrasCutaneas = v) }
    }
    SecaoCircunferencias(s, vm)
    Campo("Perímetros", i.perimetros, null, linhas = 4, placeholder = "Ex.: Cintura 82 cm; Quadril 98 cm") { v ->
        vm.alterar { it.copy(perimetros = v) }
    }
    Campo("Postura", i.postura, null, linhas = 3) { v -> vm.alterar { it.copy(postura = v) } }
    Campo("Funcional e mobilidade", i.funcionalMobilidade, null, linhas = 3) { v -> vm.alterar { it.copy(funcionalMobilidade = v) } }
    Campo("Cardiorrespiratório", i.cardio, null, linhas = 3) { v -> vm.alterar { it.copy(cardio = v) } }
    Campo("Força e resistência", i.forcaResistencia, null, linhas = 3) { v -> vm.alterar { it.copy(forcaResistencia = v) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecaoDobras(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    val i = s.input
    Text("Dobras cutâneas (mm) — Pollock", style = MaterialTheme.typography.titleSmall)

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ProtocoloDobras.entries.forEachIndexed { idx, p ->
            SegmentedButton(
                selected = i.protocolo == p,
                onClick = { vm.alterar { it.copy(protocolo = p) } },
                shape = SegmentedButtonDefaults.itemShape(idx, ProtocoloDobras.entries.size),
            ) { Text(p.rotulo) }
        }
    }

    // Sexo: vem do cadastro; se ausente ("Outro"/vazio) o profissional escolhe a equação.
    if (SexoBiologico.deTexto(s.aluno?.sexo) == null) {
        Text("Sexo para a equação", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SexoBiologico.entries.forEach { sx ->
                FilterChip(
                    selected = s.sexoCalculo == sx,
                    onClick = { vm.definirSexoCalculo(sx) },
                    label = { Text(sx.rotulo) },
                )
            }
        }
    }

    val sexo = s.sexoCalculo
    if (sexo != null) {
        val campos = Pollock.dobrasNecessarias(i.protocolo, sexo)
        campos.chunked(2).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { d ->
                    Campo(
                        d.rotulo, i.dobras[d].orEmpty(), s.erros[d.campo()],
                        teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                    ) { v -> vm.alterar(d.campo()) { it.copy(dobras = it.dobras + (d to v)) } }
                }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    when (val a = s.pollock) {
        is AnalisePollock.Pendente -> Text(a.motivo, style = MaterialTheme.typography.bodySmall)
        is AnalisePollock.Calculada -> {
            val r = a.resultado
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Resultado — ${r.protocolo.rotulo}", style = MaterialTheme.typography.titleSmall)
                    Text("Soma das dobras: %.1f mm • Idade: %d anos".format(r.somaDobrasMm, r.idade))
                    Text("Densidade corporal: %.4f g/mL".format(r.densidadeCorporal))
                    Text("% de gordura (Siri): %.1f%%".format(r.percentualGordura), style = MaterialTheme.typography.titleMedium)
                    a.avisos.forEach {
                        Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = vm::usarPercentualPollock, modifier = Modifier.fillMaxWidth()) {
                        Text("Usar como % de gordura")
                    }
                }
            }
        }
    }
}

/** Cintura e quadril (cm) com o RCQ e a classificação de referência da OMS. */
@Composable
private fun SecaoCircunferencias(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    val i = s.input
    Text("Circunferências (cm)", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Campo("Cintura", i.cintura, s.erros[CampoAvaliacao.CINTURA], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.CINTURA) { it.copy(cintura = v) }
        }
        Campo("Quadril", i.quadril, s.erros[CampoAvaliacao.QUADRIL], teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { v ->
            vm.alterar(CampoAvaliacao.QUADRIL) { it.copy(quadril = v) }
        }
    }
    val rcq = EvolucaoRules.rcq(
        AvaliacaoValidator.parseDecimal(i.cintura),
        AvaliacaoValidator.parseDecimal(i.quadril),
    )
    if (rcq != null) {
        val classe = EvolucaoRules.classificarRcq(rcq, s.sexoCalculo)
        val texto = "RCQ: " + String.format(java.util.Locale("pt", "BR"), "%.2f", rcq) +
            (classe?.let { " — " + it.rotulo } ?: "")
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (classe?.atencao == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PassoFotos(s: AvaliacaoFormState, vm: AvaliacaoFormViewModel) {
    var tipoPendente by rememberSaveable { mutableStateOf<TipoFoto?>(null) }
    var uriCamera by rememberSaveable { mutableStateOf<Uri?>(null) }

    var recortando by rememberSaveable { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriCamera
        if (ok && tipoPendente != null && uri != null) {
            recortando = uri
        } else {
            tipoPendente = null
        }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && tipoPendente != null) {
            recortando = uri
        } else {
            tipoPendente = null
        }
    }
    recortando?.let { uri ->
        val tipo = tipoPendente
        br.com.fitpersonal.app.ui.common.RecortarImagemDialog(
            origem = uri,
            proporcaoInicial = null,
            onConfirmar = { r ->
                tipo?.let { vm.definirFoto(it, uri, r) }
                recortando = null; tipoPendente = null
            },
            onUsarOriginal = {
                tipo?.let { vm.definirFoto(it, uri) }
                recortando = null; tipoPendente = null
            },
            onCancelar = { recortando = null; tipoPendente = null },
        )
    }

    TipoFoto.entries.chunked(2).forEach { linha ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            linha.forEach { tipo ->
                CartaoFoto(
                    tipo = tipo,
                    caminho = s.fotos[tipo]?.let { vm.arquivoFoto(it) },
                    modifier = Modifier.weight(1f),
                    onCamera = {
                        tipoPendente = tipo
                        uriCamera = vm.novaUriCamera().also { camera.launch(it) }
                    },
                    onGaleria = {
                        tipoPendente = tipo
                        galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemover = { vm.removerFoto(tipo) },
                )
            }
        }
    }
}

// ───────────────────────── Componentes ─────────────────────────

@Composable
private fun CartaoResumo(r: ResumoAvaliacao) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Resumo", style = MaterialTheme.typography.titleSmall)
            Text(
                r.imc?.let { "IMC: %.1f — %s".format(it, r.classeImc?.rotulo.orEmpty()) } ?: "IMC: informe peso e altura",
            )
            r.massaMagraKg?.let { Text("Massa magra: %.1f kg".format(it)) }
            r.massaGordaKg?.let { Text("Massa gorda: %.1f kg".format(it)) }
        }
    }
    ListaAvisos(r.avisos, mostrarTudoCerto = r.imc != null)
}

/** Um cartão por aviso, na cor da gravidade; sem avisos e com dados suficientes mostra "tudo certo". */
@Composable
private fun ListaAvisos(avisos: List<AvisoAvaliacao>, mostrarTudoCerto: Boolean) {
    if (avisos.isEmpty()) {
        if (mostrarTudoCerto) CartaoAviso(
            AvisoAvaliacao("Sem alertas", "Nenhum alerta com os dados informados até aqui.", Gravidade.INFO),
        )
        return
    }
    avisos.forEach { CartaoAviso(it) }
}

@Composable
private fun CartaoAviso(aviso: AvisoAvaliacao) {
    val cores = MaterialTheme.colorScheme
    val (fundo, sobre) = when (aviso.gravidade) {
        Gravidade.ALTA -> cores.errorContainer to cores.onErrorContainer
        Gravidade.ATENCAO -> cores.tertiaryContainer to cores.onTertiaryContainer
        Gravidade.INFO -> cores.primaryContainer to cores.onPrimaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = fundo, contentColor = sobre),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (aviso.gravidade == Gravidade.INFO) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = sobre,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(aviso.titulo, style = MaterialTheme.typography.titleSmall, color = sobre)
                Text(aviso.detalhe, style = MaterialTheme.typography.bodySmall, color = sobre)
            }
        }
    }
}

@Composable
private fun FlowCondicoes(selecionadas: Set<CondicaoSaude>, onToggle: (CondicaoSaude, Boolean) -> Unit) {
    CondicaoSaude.entries.chunked(2).forEach { linha ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            linha.forEach { c ->
                FilterChip(
                    selected = c in selecionadas,
                    onClick = { onToggle(c, c !in selecionadas) },
                    label = { Text(c.rotulo) },
                )
            }
        }
    }
}

@Composable
private fun CartaoFoto(
    tipo: TipoFoto,
    caminho: java.io.File?,
    modifier: Modifier = Modifier,
    onCamera: () -> Unit,
    onGaleria: () -> Unit,
    onRemover: () -> Unit,
) {
    Card(modifier) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tipo.rotulo, style = MaterialTheme.typography.labelLarge)
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center,
            ) {
                if (caminho != null) {
                    AsyncImage(
                        model = caminho,
                        contentDescription = "Foto ${tipo.rotulo}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("Sem foto", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onCamera, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f)) {
                    Text("Câmera")
                }
                OutlinedButton(onClick = onGaleria, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.weight(1f)) {
                    Text("Galeria")
                }
            }
            if (caminho != null) TextButton(onClick = onRemover, modifier = Modifier.fillMaxWidth()) { Text("Remover") }
        }
    }
}

@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    erro: String?,
    transform: VisualTransformation = VisualTransformation.None,
    teclado: KeyboardType = KeyboardType.Text,
    max: Int = Int.MAX_VALUE,
    linhas: Int = 1,
    placeholder: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    /** Campos que o app reformata ao digitar (ex.: altura): mantém o cursor sempre no fim do texto. */
    cursorNoFim: Boolean = false,
    onChange: (String) -> Unit,
) {
    val digitos = teclado == KeyboardType.Number
    val decimal = teclado == KeyboardType.Decimal
    val tratar: (String) -> Unit = { novo ->
        val v = when {
            digitos -> novo.filter(Char::isDigit)
            decimal -> novo.filter { it.isDigit() || it == ',' || it == '.' }
            else -> novo
        }
        onChange(v.take(if (decimal) 7 else max))
    }
    if (cursorNoFim) {
        OutlinedTextField(
            value = androidx.compose.ui.text.input.TextFieldValue(
                valor, androidx.compose.ui.text.TextRange(valor.length),
            ),
            onValueChange = { tratar(it.text) },
            label = { Text(rotulo) },
            placeholder = placeholder?.let { { Text(it) } },
            isError = erro != null,
            supportingText = erro?.let { { Text(it) } },
            visualTransformation = transform,
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            singleLine = linhas == 1,
            minLines = linhas,
            modifier = modifier,
        )
        return
    }
    OutlinedTextField(
        value = valor,
        onValueChange = tratar,
        label = { Text(rotulo) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        visualTransformation = transform,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = linhas == 1,
        minLines = linhas,
        modifier = modifier,
    )
}
