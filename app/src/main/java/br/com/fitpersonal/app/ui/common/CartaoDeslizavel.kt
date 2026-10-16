package br.com.fitpersonal.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val LARGURA_ACAO = 72.dp
private val MOLA = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

/**
 * Cartão que, ao ser arrastado para a esquerda, revela os botões **Editar** e **Excluir**.
 *  - Soltar além da metade fixa os botões abertos; antes disso o cartão volta.
 *  - Tocar no cartão aberto só o fecha (não aciona o clique do cartão).
 *  - Arrastar para a direita com o cartão fechado não é consumido: continua trocando de aba.
 *  - Acessibilidade: os botões têm descrição e o cartão expõe "Editar" e "Excluir" como ações de leitor de tela
 *    (TalkBack), então quem não usa o gesto também alcança as duas ações.
 */
@Composable
fun CartaoDeslizavel(
    onEditar: () -> Unit,
    onExcluir: () -> Unit,
    modifier: Modifier = Modifier,
    nome: String = "item",
    conteudo: @Composable () -> Unit,
) {
    val escopo = rememberCoroutineScope()
    val largura = with(LocalDensity.current) { (LARGURA_ACAO * 2).toPx() }
    val deslocamento = remember { Animatable(0f) }            // de 0 (fechado) até -largura (aberto)
    val aberto = deslocamento.value < -largura / 2f
    val progresso = (-deslocamento.value / largura).coerceIn(0f, 1f)

    fun fechar() { escopo.launch { deslocamento.animateTo(0f, MOLA) } }

    Box(
        modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Editar $nome") { onEditar(); true },
                CustomAccessibilityAction("Excluir $nome") { onExcluir(); true },
            )
        },
    ) {
        // Botões por trás: só aparecem (e só recebem toque) conforme o cartão é puxado.
        Row(
            Modifier.matchParentSize().alpha(progresso),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotaoDeslizante(
                icone = Icons.Filled.Edit,
                rotulo = "Editar",
                fundo = PaletaPasta.Preto,
                tinta = PaletaPasta.Lima,
                onClick = { fechar(); onEditar() },
            )
            BotaoDeslizante(
                icone = Icons.Filled.Delete,
                rotulo = "Excluir",
                fundo = MaterialTheme.colorScheme.error,
                tinta = MaterialTheme.colorScheme.onError,
                onClick = { fechar(); onExcluir() },
            )
        }

        Box(
            Modifier
                .offset { IntOffset(deslocamento.value.roundToInt(), 0) }
                .pointerInput(largura) {
                    awaitEachGesture {
                        val toque = awaitFirstDown(requireUnconsumed = false)
                        var aceitou = false
                        // Só assume o gesto se for para a esquerda (ou se já estiver aberto): o resto passa adiante.
                        val primeiro = awaitHorizontalTouchSlopOrCancellation(toque.id) { mudanca, excesso ->
                            if (excesso < 0f || deslocamento.value < 0f) {
                                mudanca.consume()
                                aceitou = true
                            }
                        }
                        if (primeiro != null && aceitou) {
                            horizontalDrag(primeiro.id) { mudanca ->
                                val destino = (deslocamento.value + mudanca.positionChange().x).coerceIn(-largura, 0f)
                                escopo.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { deslocamento.snapTo(destino) }
                                mudanca.consume()
                            }
                            // Soltou: abre ou fecha conforme passou da metade
                            escopo.launch {
                                deslocamento.animateTo(if (deslocamento.value < -largura / 2f) -largura else 0f, MOLA)
                            }
                        }
                    }
                },
        ) {
            conteudo()
            // Aberto: um toque no cartão apenas fecha, sem abrir o item.
            if (aberto) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fechar() },
                )
            }
        }
    }
}

@Composable
private fun BotaoDeslizante(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    rotulo: String,
    fundo: Color,
    tinta: Color,
    onClick: () -> Unit,
) {
    Box(Modifier.size(LARGURA_ACAO), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(fundo)
                .clickable(onClick = onClick)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icone, contentDescription = rotulo, tint = tinta, modifier = Modifier.size(22.dp))
                Text(rotulo, style = MaterialTheme.typography.labelSmall, color = tinta, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
