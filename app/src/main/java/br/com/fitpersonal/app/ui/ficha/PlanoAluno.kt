package br.com.fitpersonal.app.ui.ficha

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.MetaRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.ReavaliacaoRules
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.TomChip
import java.time.LocalDate
import java.util.Locale

private val PT_BR = Locale("pt", "BR")

/** Métricas que fazem sentido como meta (IMC e RCQ derivam das demais). */
private val METRICAS_META = listOf(
    Metrica.PESO, Metrica.PERCENTUAL_GORDURA, Metrica.MASSA_MAGRA, Metrica.MASSA_GORDA, Metrica.CINTURA,
)

private fun Double.fmt(casas: Int) = String.format(PT_BR, "%.${casas}f", this)

fun textoReavaliacao(s: SituacaoReavaliacao): String = when (s) {
    SituacaoReavaliacao.SemAvaliacao -> "Nenhuma avaliação feita ainda"
    is SituacaoReavaliacao.EmDia -> "Próxima avaliação em ${s.diasRestantes} dias"
    is SituacaoReavaliacao.Proxima ->
        if (s.diasRestantes == 0) "Reavaliação vence hoje" else "Reavaliação em ${s.diasRestantes} dias"
    is SituacaoReavaliacao.Atrasada ->
        "Reavaliação atrasada há ${s.diasAtraso} " + if (s.diasAtraso == 1) "dia" else "dias"
}

fun tomReavaliacao(s: SituacaoReavaliacao): TomChip = when (s) {
    is SituacaoReavaliacao.Atrasada -> TomChip.Erro
    is SituacaoReavaliacao.Proxima, SituacaoReavaliacao.SemAvaliacao -> TomChip.Atencao
    is SituacaoReavaliacao.EmDia -> TomChip.Sucesso
}

/** Meta com barra de progresso e situação da reavaliação, com edição por diálogo. */
@Composable
fun CartaoPlano(
    aluno: Aluno,
    avaliacoes: List<AvaliacaoFisica>,
    onSalvar: (Metrica?, Double?, Int?) -> Unit,
) {
    var editando by remember { mutableStateOf(false) }
    val meta = MetaRules.meta(aluno.metaMetrica, aluno.metaValor)
    val situacao = ReavaliacaoRules.situacao(
        ultimaAvaliacao = avaliacoes.maxOfOrNull { it.data },
        intervaloDias = aluno.reavaliacaoDias,
        hoje = LocalDate.now(),
    )

    CartaoSecao(titulo = "Meta e acompanhamento") {
        if (meta == null) {
            Text(
                "Defina uma meta (ex.: peso ou % de gordura) para acompanhar o progresso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val serie = EvolucaoRules.serie(avaliacoes, meta.metrica)
            val un = if (meta.metrica.unidade.isNotEmpty()) " ${meta.metrica.unidade}" else ""
            Text(
                "${meta.metrica.rotulo}: meta de ${meta.valor.fmt(meta.metrica.casas)}$un",
                style = MaterialTheme.typography.titleSmall,
            )
            if (serie.isEmpty()) {
                Text(
                    "Sem medidas de ${meta.metrica.rotulo.lowercase()} para calcular o progresso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val p = MetaRules.progresso(serie.first().valor, serie.last().valor, meta.valor)
                LinearProgressIndicator(
                    progress = { p.fracao.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Início ${p.inicial.fmt(meta.metrica.casas)} • Agora ${p.atual.fmt(meta.metrica.casas)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (p.atingida) ChipStatus("Meta atingida", TomChip.Sucesso)
                    else Text(
                        "Faltam ${kotlin.math.abs(p.falta).fmt(meta.metrica.casas)}$un",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (situacao != null) ChipStatus(textoReavaliacao(situacao), tomReavaliacao(situacao))
        else Text(
            "Sem reavaliação programada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(onClick = { editando = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (meta == null && situacao == null) "Definir meta e reavaliação" else "Editar")
        }
    }

    if (editando) {
        DialogoPlano(
            metaAtual = meta?.metrica,
            valorAtual = meta?.valor,
            diasAtual = aluno.reavaliacaoDias,
            onCancelar = { editando = false },
            onSalvar = { m, v, d -> editando = false; onSalvar(m, v, d) },
        )
    }
}

@Composable
private fun DialogoPlano(
    metaAtual: Metrica?,
    valorAtual: Double?,
    diasAtual: Int?,
    onCancelar: () -> Unit,
    onSalvar: (Metrica?, Double?, Int?) -> Unit,
) {
    var metrica by remember { mutableStateOf(metaAtual) }
    var valor by remember { mutableStateOf(valorAtual?.let { it.fmt(metaAtual?.casas ?: 1) } ?: "") }
    var dias by remember { mutableStateOf(diasAtual) }

    val numero = valor.trim().replace(',', '.').toDoubleOrNull()
    // Meta só vale com métrica e valor positivo; ambos vazios = sem meta.
    val metaValida = (metrica == null && valor.isBlank()) || (metrica != null && numero != null && numero > 0)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Meta e reavaliação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Meta", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = metrica == null, onClick = { metrica = null; valor = "" }, label = { Text("Sem meta") })
                    METRICAS_META.forEach { m ->
                        FilterChip(selected = metrica == m, onClick = { metrica = m }, label = { Text(m.rotulo) })
                    }
                }
                if (metrica != null) {
                    OutlinedTextField(
                        value = valor,
                        onValueChange = { valor = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                        label = { Text("Valor-alvo" + metrica?.unidade?.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()) },
                        singleLine = true,
                        isError = !metaValida,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Reavaliação", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = dias == null, onClick = { dias = null }, label = { Text("Nenhuma") })
                    ReavaliacaoRules.OPCOES_DIAS.forEach { d ->
                        FilterChip(selected = dias == d, onClick = { dias = d }, label = { Text("$d dias") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = metaValida,
                onClick = { onSalvar(metrica, if (metrica != null) numero else null, dias) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
