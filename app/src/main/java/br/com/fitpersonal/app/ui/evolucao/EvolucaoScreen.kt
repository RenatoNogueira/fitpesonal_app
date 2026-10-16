package br.com.fitpersonal.app.ui.evolucao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.LinhaComparativo
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.PontoSerie
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.CompartilharPdfEffect
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.TomChip
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yy")
private val PT_BR = Locale("pt", "BR")

private fun Double.fmt(casas: Int) = String.format(PT_BR, "%.${casas}f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolucaoScreen(onVoltar: () -> Unit, vm: EvolucaoViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    CompartilharPdfEffect(s.pdf, vm::pdfConsumido)
    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Evolução", style = MaterialTheme.typography.titleLarge)
                        s.aluno?.let {
                            Text(
                                it.nome,
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
        if (s.avaliacoes.isEmpty()) {
            EstadoVazio(
                icone = Icons.Filled.Assessment,
                titulo = "Sem avaliações ainda",
                texto = "Registre avaliações para acompanhar a evolução do aluno ao longo do tempo.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        // Métricas que têm pelo menos um ponto
        val disponiveis = Metrica.entries.filter { EvolucaoRules.serie(s.avaliacoes, it).isNotEmpty() }
        val metrica = if (s.metrica in disponiveis) s.metrica else disponiveis.firstOrNull() ?: Metrica.PESO
        val serie = EvolucaoRules.serie(s.avaliacoes, metrica)

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Seletor de métrica
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                disponiveis.forEach { m ->
                    FilterChip(
                        selected = m == metrica,
                        onClick = { vm.selecionarMetrica(m) },
                        label = { Text(m.rotulo) },
                    )
                }
            }

            CartaoSecao(titulo = metrica.rotulo) {
                if (serie.isEmpty()) {
                    Text("Sem medidas de ${metrica.rotulo.lowercase()} nas avaliações.")
                } else {
                    ResumoSerie(serie, metrica)
                    GraficoLinha(
                        pontos = serie,
                        cor = MaterialTheme.colorScheme.primary,
                        grade = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth().height(170.dp),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            serie.first().data.format(DATA_BR),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            serie.last().data.format(DATA_BR),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Comparativo
            val antes = s.avaliacoes.firstOrNull { it.id == s.idAntes }
            val depois = s.avaliacoes.firstOrNull { it.id == s.idDepois }
            CartaoSecao(titulo = "Comparar avaliações") {
                if (s.avaliacoes.size < 2) {
                    Text(
                        "Registre ao menos duas avaliações para comparar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SeletorAvaliacao("Antes", s.avaliacoes, s.idAntes, vm::escolherAntes)
                    SeletorAvaliacao("Depois", s.avaliacoes, s.idDepois, vm::escolherDepois)
                    if (antes != null && depois != null && antes.id != depois.id) {
                        TabelaComparativa(EvolucaoRules.comparar(antes, depois))
                        androidx.compose.material3.Button(
                            onClick = vm::gerarComparativo,
                            enabled = !s.gerandoPdf,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text(if (s.gerandoPdf) "  Gerando…" else "  Compartilhar comparativo (PDF)")
                        }
                    } else {
                        Text(
                            "Escolha duas avaliações diferentes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (antes != null && depois != null && antes.id != depois.id) {
                val ordenadas = if (antes.data <= depois.data) antes to depois else depois to antes
                FotosLadoALado(ordenadas.first, ordenadas.second, vm::arquivoFoto)
            }
        }
    }
}

@Composable
private fun FotosLadoALado(
    antes: AvaliacaoFisica,
    depois: AvaliacaoFisica,
    arquivo: (String) -> java.io.File,
) {
    val posicoes = listOf(
        Triple("Frente", antes.fotoFrente, depois.fotoFrente),
        Triple("Costas", antes.fotoCostas, depois.fotoCostas),
        Triple("Lateral direita", antes.fotoLateralDir, depois.fotoLateralDir),
        Triple("Lateral esquerda", antes.fotoLateralEsq, depois.fotoLateralEsq),
    ).filter { it.second != null || it.third != null }
    if (posicoes.isEmpty()) return

    CartaoSecao(titulo = "Fotos lado a lado") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Antes • ${antes.data.format(DATA_CURTA)}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text("Depois • ${depois.data.format(DATA_CURTA)}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        }
        posicoes.forEach { (nome, a, d) ->
            Text(nome, style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FotoOuVazio(a, arquivo, Modifier.weight(1f))
                FotoOuVazio(d, arquivo, Modifier.weight(1f))
            }
            // Com as duas fotos, oferece o comparador deslizante (antes/depois sobrepostos)
            if (a != null && d != null) {
                var deslizar by remember(nome) { mutableStateOf(false) }
                androidx.compose.material3.TextButton(onClick = { deslizar = !deslizar }) {
                    Text(if (deslizar) "Fechar comparação" else "Comparar deslizando")
                }
                if (deslizar) ComparadorDeslizante(arquivo(a), arquivo(d))
            }
        }
        Text(
            "O PDF do comparativo inclui as posições que têm foto nas duas avaliações.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FotoOuVazio(rel: String?, arquivo: (String) -> java.io.File, modifier: Modifier) {
    val forma = MaterialTheme.shapes.medium
    Box(
        modifier.aspectRatio(3f / 4f).clip(forma).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (rel == null) {
            Text("sem foto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AsyncImage(
                model = arquivo(rel),
                contentDescription = "Foto postural",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ResumoSerie(serie: List<PontoSerie>, m: Metrica) {
    val ultimo = serie.last().valor
    val un = if (m.unidade.isNotEmpty()) " ${m.unidade}" else ""
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(ultimo.fmt(m.casas) + un, style = MaterialTheme.typography.headlineMedium)
        if (serie.size >= 2) {
            val v = EvolucaoRules.variacao(m, serie.first().valor, ultimo)
            ChipStatus(
                EvolucaoRules.formatarDelta(v, m) + " desde o início",
                when (v.sentido) {
                    SentidoVariacao.MELHOROU -> TomChip.Sucesso
                    SentidoVariacao.PIOROU -> TomChip.Atencao
                    else -> TomChip.Neutro
                },
            )
        }
    }
}

@Composable
private fun TabelaComparativa(linhas: List<LinhaComparativo>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        linhas.forEach { l ->
            val un = if (l.metrica.unidade.isNotEmpty()) " ${l.metrica.unidade}" else ""
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(l.metrica.rotulo, style = MaterialTheme.typography.titleSmall)
                    Text(
                        (l.antes?.let { it.fmt(l.metrica.casas) + un } ?: "-") + "  →  " +
                            (l.depois?.let { it.fmt(l.metrica.casas) + un } ?: "-"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                l.variacao?.let { v ->
                    ChipStatus(
                        EvolucaoRules.formatarDelta(v, l.metrica),
                        when (v.sentido) {
                            SentidoVariacao.MELHOROU -> TomChip.Sucesso
                            SentidoVariacao.PIOROU -> TomChip.Atencao
                            else -> TomChip.Neutro
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorAvaliacao(
    rotulo: String,
    avaliacoes: List<AvaliacaoFisica>,
    selecionadoId: Long?,
    onSelecionar: (Long) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    fun texto(a: AvaliacaoFisica) =
        a.data.format(DATA_BR) + (a.pesoKg?.let { " • ${it.fmt(1)} kg" } ?: "")

    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }) {
        OutlinedTextField(
            value = avaliacoes.firstOrNull { it.id == selecionadoId }?.let(::texto).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(aberto) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            avaliacoes.forEach { a ->
                DropdownMenuItem(
                    text = { Text(texto(a)) },
                    onClick = { onSelecionar(a.id); aberto = false },
                )
            }
        }
    }
}

/**
 * Gráfico de linha simples em Canvas: eixo X proporcional às datas, Y ajustado ao intervalo dos valores.
 * Com um único ponto desenha só o marcador.
 */
@Composable
internal fun GraficoLinha(
    pontos: List<PontoSerie>,
    cor: Color,
    grade: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        if (pontos.isEmpty()) return@Canvas
        val margem = 14.dp.toPx()
        val larg = size.width - 2 * margem
        val alt = size.height - 2 * margem

        val minV = pontos.minOf { it.valor }
        val maxV = pontos.maxOf { it.valor }
        val folga = if (maxV - minV < 1e-9) 1.0 else (maxV - minV) * 0.15
        val baixo = minV - folga
        val cima = maxV + folga

        val d0 = pontos.first().data.toEpochDay()
        val d1 = pontos.last().data.toEpochDay()

        fun x(i: Int): Float =
            if (d1 == d0) margem + larg / 2f
            else margem + larg * ((pontos[i].data.toEpochDay() - d0).toFloat() / (d1 - d0).toFloat())

        fun y(v: Double): Float = margem + alt * (1f - ((v - baixo) / (cima - baixo)).toFloat())

        // grade horizontal (3 linhas)
        for (k in 0..2) {
            val yy = margem + alt * k / 2f
            drawLine(grade, Offset(margem, yy), Offset(size.width - margem, yy), strokeWidth = 1.dp.toPx())
        }

        if (pontos.size >= 2) {
            val caminho = Path()
            pontos.forEachIndexed { i, p ->
                val px = x(i)
                val py = y(p.valor)
                if (i == 0) caminho.moveTo(px, py) else caminho.lineTo(px, py)
            }
            drawPath(caminho, cor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        pontos.forEachIndexed { i, p ->
            drawCircle(cor, radius = 5.dp.toPx(), center = Offset(x(i), y(p.valor)))
        }
    }
}
