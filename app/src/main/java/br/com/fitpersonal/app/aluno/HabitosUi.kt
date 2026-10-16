package br.com.fitpersonal.app.aluno

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.rules.CheckIn
import br.com.fitpersonal.app.domain.rules.Habito
import br.com.fitpersonal.app.domain.rules.HabitosRules
import java.time.LocalDate

private val LETRAS_SEMANA = listOf("S", "T", "Q", "Q", "S", "S", "D")

/**
 * Hábitos do dia, sequência da semana e check-in semanal do aluno. Tudo guardado no aparelho
 * ([HabitosStore]); o texto da semana segue junto do resumo enviado ao professor.
 */
@Composable
fun CartaoHabitos(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val store = remember { HabitosStore(ctx.applicationContext) }
    var dados by remember { mutableStateOf(store.ler()) }
    var checkinAberto by remember { mutableStateOf(false) }
    val hoje = remember { LocalDate.now() }

    val marcadosHoje = dados.dias[hoje].orEmpty()
    val sequencia = HabitosRules.sequencia(dados.dias, hoje)
    val semana = HabitosRules.semana(dados.dias, hoje)
    val precisaCheckIn = HabitosRules.precisaCheckIn(dados.checkins, hoje)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Hábitos de hoje", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (sequencia >= 2) "Sequência de $sequencia dias" else "Marque ao menos ${HabitosRules.MINIMO_DIA} para contar o dia",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Semana (segunda a domingo): círculo cheio = dia cumprido; número em negrito = hoje
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                semana.forEachIndexed { i, (dia, cumprido) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(LETRAS_SEMANA[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (cumprido) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                dia.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cumprido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (dia == hoje) FontWeight.ExtraBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            Habito.entries.forEach { h ->
                FilterChip(
                    selected = h.id in marcadosHoje,
                    onClick = { store.alternar(hoje, h.id); dados = store.ler() },
                    label = { Text(h.rotulo) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (precisaCheckIn) {
                Button(onClick = { checkinAberto = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fazer check-in da semana")
                }
            } else {
                OutlinedButton(onClick = { checkinAberto = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Check-in da semana feito — refazer")
                }
            }
        }
    }

    if (checkinAberto) {
        CheckInDialog(
            onSalvar = { c -> store.registrarCheckIn(c); dados = store.ler(); checkinAberto = false },
            onFechar = { checkinAberto = false },
        )
    }
}

@Composable
private fun CheckInDialog(onSalvar: (CheckIn) -> Unit, onFechar: () -> Unit) {
    var energia by remember { mutableFloatStateOf(3f) }
    var sono by remember { mutableFloatStateOf(3f) }
    var dor by remember { mutableFloatStateOf(1f) }
    var obs by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Check-in da semana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EscalaCheckIn("Energia", "baixa", "alta", energia) { energia = it }
                EscalaCheckIn("Sono", "ruim", "ótimo", sono) { sono = it }
                EscalaCheckIn("Dor ou desconforto", "nenhuma", "muita", dor) { dor = it }
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it.take(300) },
                    label = { Text("Observação (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSalvar(
                        CheckIn(
                            data = LocalDate.now(),
                            energia = energia.toInt().coerceIn(1, 5),
                            sono = sono.toInt().coerceIn(1, 5),
                            dor = dor.toInt().coerceIn(1, 5),
                            obs = obs.trim().ifEmpty { null },
                        )
                    )
                },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } },
    )
}

@Composable
private fun EscalaCheckIn(titulo: String, minimo: String, maximo: String, valor: Float, onValor: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(titulo, style = MaterialTheme.typography.labelLarge)
            Text(valor.toInt().toString() + "/5", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Slider(value = valor, onValueChange = { onValor(it.toInt().toFloat()) }, valueRange = 1f..5f, steps = 3)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minimo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(maximo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
