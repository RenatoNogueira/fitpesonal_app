package br.com.fitpersonal.app.ui.evolucao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import br.com.fitpersonal.app.domain.rules.MetaComposicao
import br.com.fitpersonal.app.domain.rules.MetaComposicaoRules
import br.com.fitpersonal.app.domain.rules.ProgressoMeta
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.PaletaPasta
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val PT_BR = Locale("pt", "BR")

private val METRICAS_PAINEL = listOf(
    Metrica.PESO, Metrica.PERCENTUAL_GORDURA, Metrica.MASSA_MAGRA,
    Metrica.MASSA_GORDA, Metrica.IMC, Metrica.CINTURA, Metrica.RCQ,
)

private fun Double.fmt(casas: Int) = String.format(PT_BR, "%.${casas}f", this)

/** % de gordura da avaliação: o informado ou, na falta dele, massa gorda ÷ peso. */
private fun percentualGordura(a: AvaliacaoFisica): Double? {
    a.percentualGordura?.let { return it }
    val peso = a.pesoKg
    val gorda = a.massaGordaKg
    return if (peso != null && gorda != null && peso > 0) gorda / peso * 100.0 else null
}

/**
 * Painel de composição corporal de uma avaliação: anel gordura × massa magra e cartões com cada medida,
 * a variação desde a avaliação anterior e a trajetória (mini-gráfico) de todas as avaliações até ela.
 *
 * [atual] é a avaliação mostrada (pode ainda não estar salva, como no formulário); [historico] são as
 * demais avaliações do mesmo aluno, em qualquer ordem — só entram as anteriores a [atual].
 */
@Composable
fun PainelComposicao(
    atual: AvaliacaoFisica?,
    historico: List<AvaliacaoFisica>,
    modifier: Modifier = Modifier,
    titulo: String = "Composição corporal",
    metas: List<MetaComposicao> = emptyList(),
    /** Se informado, tocar numa medida com meta possível abre o editor; valor null = remover a meta. */
    onDefinirMeta: ((Metrica, Double?) -> Unit)? = null,
) {
    var editando by remember { mutableStateOf<Metrica?>(null) }
    CartaoSecao(modifier = modifier, titulo = titulo) {
        if (atual == null) {
            Text(
                "Sem avaliação para mostrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@CartaoSecao
        }
        val anteriores = historico
            .filter { it.id != atual.id && (it.data < atual.data || (it.data == atual.data && it.id < atual.id)) }
            .sortedWith(compareBy({ it.data }, { it.id }))
        val anterior = anteriores.lastOrNull()
        val trajetoria = anteriores + atual

        val disponiveis = METRICAS_PAINEL.filter { EvolucaoRules.valor(atual, it) != null }
        if (disponiveis.isEmpty()) {
            Text(
                "Informe peso, % de gordura ou massas para ver o painel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@CartaoSecao
        }

        Text(
            "Avaliação de " + atual.data.format(DATA),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (anterior != null) "Variação desde " + anterior.data.format(DATA) + " • " + trajetoria.size + " avaliações"
            else "Primeira avaliação do aluno",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val gordura = percentualGordura(atual)
        if (gordura != null) {
            AnelComposicao(
                gordura = gordura.coerceIn(0.0, 100.0),
                pesoKg = atual.pesoKg,
                massaGordaKg = atual.massaGordaKg,
                massaMagraKg = atual.massaMagraKg,
            )
        }

        disponiveis.chunked(2).forEach { par ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                par.forEach { m ->
                    val valor = EvolucaoRules.valor(atual, m)!!
                    val antes = anterior?.let { EvolucaoRules.valor(it, m) }
                    val serie = trajetoria.mapNotNull { EvolucaoRules.valor(it, m) }
                    val meta = metas.firstOrNull { it.metrica == m }
                    val progresso = meta?.let { MetaComposicaoRules.progresso(m, serie.first(), valor, it.alvo) }
                    val podeEditar = onDefinirMeta != null && m in MetaComposicaoRules.METRICAS
                    CartaoMedida(
                        metrica = m,
                        valor = valor,
                        antes = antes,
                        serie = serie,
                        progresso = progresso,
                        dicaMeta = podeEditar && progresso == null,
                        onClick = if (podeEditar) ({ editando = m }) else null,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (par.size == 1) Box(Modifier.weight(1f))
            }
        }
    }

    editando?.let { m ->
        EditorMeta(
            metrica = m,
            atual = metas.firstOrNull { it.metrica == m }?.alvo,
            onSalvar = { v -> onDefinirMeta?.invoke(m, v); editando = null },
            onFechar = { editando = null },
        )
    }
}

@Composable
private fun EditorMeta(metrica: Metrica, atual: Double?, onSalvar: (Double?) -> Unit, onFechar: () -> Unit) {
    var texto by remember { mutableStateOf(atual?.fmt(metrica.casas).orEmpty()) }
    val valor = texto.trim().replace(',', '.').toDoubleOrNull()
    val valido = valor != null && valor > 0.0 && valor < 1000.0
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Meta de " + metrica.rotulo.lowercase()) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                label = { Text("Valor desejado" + if (metrica.unidade.isNotEmpty()) " (" + metrica.unidade + ")" else "") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onSalvar(valor) }, enabled = valido) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (atual != null) androidx.compose.material3.TextButton(onClick = { onSalvar(null) }) { Text("Remover meta") }
                androidx.compose.material3.TextButton(onClick = onFechar) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun AnelComposicao(gordura: Double, pesoKg: Double?, massaGordaKg: Double?, massaMagraKg: Double?) {
    val corGordura = PaletaPasta.Coral
    val corMagra = PaletaPasta.Teal
    val trilho = MaterialTheme.colorScheme.surfaceContainerHigh
    val magra = 100.0 - gordura
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(116.dp)) {
                val espessura = 18.dp.toPx()
                val lado = size.minDimension - espessura
                val topo = Offset(espessura / 2f, espessura / 2f)
                val tamanho = Size(lado, lado)
                drawArc(trilho, 0f, 360f, false, topo, tamanho, style = Stroke(espessura))
                val varreduraGordura = (gordura / 100.0 * 360.0).toFloat()
                drawArc(corMagra, -90f + varreduraGordura, 360f - varreduraGordura, false, topo, tamanho, style = Stroke(espessura, cap = StrokeCap.Butt))
                drawArc(corGordura, -90f, varreduraGordura, false, topo, tamanho, style = Stroke(espessura, cap = StrokeCap.Butt))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(gordura.fmt(1) + "%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("gordura", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendaAnel(corGordura, "Massa gorda", gordura.fmt(1) + "%" + (massaGordaKg?.let { " • " + it.fmt(1) + " kg" } ?: ""))
            LegendaAnel(corMagra, "Massa magra", magra.fmt(1) + "%" + (massaMagraKg?.let { " • " + it.fmt(1) + " kg" } ?: ""))
            pesoKg?.let {
                Text("Peso " + it.fmt(1) + " kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendaAnel(cor: Color, rotulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(cor))
        Column {
            Text(rotulo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(valor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CartaoMedida(
    metrica: Metrica,
    valor: Double,
    antes: Double?,
    serie: List<Double>,
    progresso: ProgressoMeta? = null,
    dicaMeta: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val variacao = antes?.let { EvolucaoRules.variacao(metrica, it, valor) }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(metrica.rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(valor.fmt(metrica.casas), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (metrica.unidade.isNotEmpty()) {
                Text(
                    metrica.unidade,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            if (variacao != null) PilulaVariacao(EvolucaoRules.formatarDelta(variacao, metrica), variacao.sentido)
            else Text("—", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MiniGrafico(serie, Modifier.width(56.dp).height(24.dp))
        }
        if (progresso != null) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progresso.fracao },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = if (progresso.atingida) PaletaPasta.Verde else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text(
                if (progresso.atingida) "Meta de " + progresso.alvo.fmt(metrica.casas) + " atingida"
                else "Meta " + progresso.alvo.fmt(metrica.casas) + " • faltam " + progresso.faltam.fmt(metrica.casas),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (dicaMeta) {
            Text("Toque para definir meta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PilulaVariacao(texto: String, sentido: SentidoVariacao) {
    val fundo = when (sentido) {
        SentidoVariacao.MELHOROU -> PaletaPasta.Verde
        SentidoVariacao.PIOROU -> PaletaPasta.Coral
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val tinta = when (sentido) {
        SentidoVariacao.MELHOROU, SentidoVariacao.PIOROU -> PaletaPasta.Preto
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        if (sentido == SentidoVariacao.IGUAL) "= igual" else texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = tinta,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(fundo).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Linha de tendência sem eixos: mostra só a direção; o último ponto fica destacado. */
@Composable
private fun MiniGrafico(valores: List<Double>, modifier: Modifier = Modifier) {
    val cor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (valores.size < 2) {
            drawCircle(cor, radius = 3.dp.toPx(), center = Offset(size.width - 3.dp.toPx(), size.height / 2f))
            return@Canvas
        }
        val min = valores.min()
        val max = valores.max()
        val faixa = (max - min).takeIf { it > 0.0 } ?: 1.0
        val passo = size.width / (valores.size - 1)
        val margem = 3.dp.toPx()
        fun y(v: Double) = (size.height - margem) - ((v - min) / faixa).toFloat() * (size.height - 2 * margem)
        val caminho = Path()
        valores.forEachIndexed { i, v ->
            val p = Offset(i * passo, y(v))
            if (i == 0) caminho.moveTo(p.x, p.y) else caminho.lineTo(p.x, p.y)
        }
        drawPath(caminho, cor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(cor, 3.dp.toPx(), Offset((valores.size - 1) * passo, y(valores.last())))
    }
}

/**
 * Atalho para telas que não têm espaço para o painel inteiro (ex.: treino): um cartão compacto com a
 * última avaliação do aluno que, ao tocar, abre o painel completo numa folha.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComposicaoNoTreino(
    avaliacoes: List<AvaliacaoFisica>,
    modifier: Modifier = Modifier,
    metas: List<MetaComposicao> = emptyList(),
) {
    var aberto by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val ordenadas = avaliacoes.sortedWith(compareBy({ it.data }, { it.id }))
    val ultima = ordenadas.lastOrNull()
    val anterior = ordenadas.dropLast(1).lastOrNull()

    val resumo = if (ultima == null) "Sem avaliação registrada para este aluno" else buildList {
        percentualGordura(ultima)?.let { add(it.fmt(1) + "% de gordura") }
        ultima.pesoKg?.let { p ->
            val delta = anterior?.pesoKg?.let { p - it }
            add(p.fmt(1) + " kg" + (delta?.takeIf { kotlin.math.abs(it) >= 0.05 }?.let { " (" + (if (it > 0) "+" else "−") + kotlin.math.abs(it).fmt(1) + ")" } ?: ""))
        }
        add("de " + ultima.data.format(DATA))
    }.joinToString(" • ")

    androidx.compose.material3.Surface(
        onClick = { if (ultima != null) aberto = true },
        enabled = ultima != null,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(PaletaPasta.Lima))
            Column(Modifier.weight(1f)) {
                Text("Composição corporal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(resumo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ultima != null) Text("Ver painel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (aberto && ultima != null) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { aberto = false }) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                PainelComposicao(atual = ultima, historico = ordenadas.dropLast(1), titulo = "Evolução do aluno", metas = metas)
            }
        }
    }
}
