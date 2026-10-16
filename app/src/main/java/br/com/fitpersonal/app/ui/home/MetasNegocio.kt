package br.com.fitpersonal.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.ResumoNegocio

private fun deltaInt(d: Int): String = (if (d > 0) "+" else "") + d

private fun deltaReais(centavos: Long): String =
    (if (centavos > 0) "+" else if (centavos < 0) "-" else "") + FinanceiroRules.formatarReais(kotlin.math.abs(centavos))

/** Resumo compacto das metas do mês; toque abre o detalhe em um modal ([DetalheMetasNegocio]). */
@Composable
fun CartaoMetasNegocio(r: ResumoNegocio, onAbrir: () -> Unit) {
    val progresso = r.fracaoSessoes ?: r.fracaoReceita
    val resumo = buildString {
        append(r.sessoes)
        r.metaSessoes?.let { append(" de ").append(it) }
        append(if (r.sessoes == 1 && r.metaSessoes == null) " sessão" else " sessões")
        append(" • ").append(FinanceiroRules.formatarReais(r.receitaCentavos))
    }
    Card(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Metas do mês", style = MaterialTheme.typography.titleSmall)
                    Text(
                        resumo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text("Detalhes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            progresso?.let { LinearProgressIndicator(progress = { it.toFloat() }, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

/** Modal com sessões e receita contra as metas e o mês anterior, e o atalho para editar as metas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheMetasNegocio(r: ResumoNegocio, onFechar: () -> Unit, onEditar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onFechar) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Metas do mês", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sessões concluídas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (r.metaSessoes != null) "${r.sessoes} de ${r.metaSessoes}" else "${r.sessoes}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                r.fracaoSessoes?.let { LinearProgressIndicator(progress = { it.toFloat() }, modifier = Modifier.fillMaxWidth()) }
                Text(
                    "${deltaInt(r.deltaSessoes)} em relação ao mês anterior (${r.sessoesMesAnterior})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Receita recebida", style = MaterialTheme.typography.titleSmall)
                    Text(
                        FinanceiroRules.formatarReais(r.receitaCentavos) +
                            (r.metaReceitaCentavos?.let { " de " + FinanceiroRules.formatarReais(it) } ?: ""),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                r.fracaoReceita?.let { LinearProgressIndicator(progress = { it.toFloat() }, modifier = Modifier.fillMaxWidth()) }
                Text(
                    "${deltaReais(r.deltaReceitaCentavos)} em relação ao mês anterior " +
                        "(${FinanceiroRules.formatarReais(r.receitaMesAnteriorCentavos)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
                Text(if (r.metaSessoes == null && r.metaReceitaCentavos == null) "Definir metas" else "Editar metas")
            }
        }
    }
}

@Composable
fun DialogoMetasNegocio(
    r: ResumoNegocio,
    onCancelar: () -> Unit,
    onSalvar: (Int, Long) -> Unit,
) {
    var sessoes by remember { mutableStateOf(r.metaSessoes?.toString().orEmpty()) }
    var receita by remember {
        mutableStateOf(
            r.metaReceitaCentavos?.let { String.format(java.util.Locale("pt", "BR"), "%d,%02d", it / 100, it % 100) }.orEmpty()
        )
    }
    val sessoesNum = if (sessoes.isBlank()) 0 else sessoes.toIntOrNull()
    val receitaNum = if (receita.isBlank()) 0L else FinanceiroRules.parseReais(receita)
    val valido = sessoesNum != null && receitaNum != null

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Metas do mês") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sessoes,
                    onValueChange = { sessoes = it.filter(Char::isDigit).take(4) },
                    label = { Text("Sessões concluídas no mês") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = receita,
                    onValueChange = { receita = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Receita do mês (R$)") },
                    singleLine = true,
                    isError = receita.isNotBlank() && receitaNum == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Deixe em branco para não usar a meta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = { onSalvar(sessoesNum ?: 0, receitaNum ?: 0L) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
