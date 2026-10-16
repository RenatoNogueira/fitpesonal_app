package br.com.fitpersonal.app.aluno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.rules.MetodoRules
import br.com.fitpersonal.app.domain.rules.MetodoTreino
import kotlinx.coroutines.delay

/**
 * Cronômetro dos métodos com tempo: AMRAP (contagem regressiva do tempo total) e EMOM (a cada minuto
 * o aparelho vibra, avisando que começa a série seguinte).
 */
@Composable
fun CronometroMetodo(metodo: MetodoTreino, modifier: Modifier = Modifier) {
    var minutos by remember { mutableIntStateOf(MetodoRules.minutosPadrao(metodo)) }
    var restante by remember { mutableIntStateOf(0) }
    var rodando by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(rodando) {
        if (!rodando) return@LaunchedEffect
        while (restante > 0) {
            delay(1000)
            restante--
            if (metodo == MetodoTreino.EMOM && restante > 0 && restante % 60 == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        rodando = false
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    if (restante > 0) {
                        val mm = restante / 60
                        val ss = restante % 60
                        "%d:%02d".format(mm, ss) +
                            if (metodo == MetodoTreino.EMOM) "  •  minuto ${minutos - (restante - 1) / 60} de $minutos" else ""
                    } else "${metodo.rotulo} de $minutos min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                if (!rodando) {
                    FilledTonalIconButton(onClick = { if (minutos > 1) minutos-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Menos um minuto")
                    }
                    FilledTonalIconButton(onClick = { if (minutos < 60) minutos++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Mais um minuto")
                    }
                }
            }
            if (restante > 0) {
                LinearProgressIndicator(
                    progress = { (1f - restante / (minutos * 60f)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    if (rodando) {
                        rodando = false
                        restante = 0
                    } else {
                        restante = minutos * 60
                        rodando = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(if (rodando) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Text(if (rodando) "  Parar" else "  Iniciar ${metodo.rotulo}")
            }
        }
    }
}
