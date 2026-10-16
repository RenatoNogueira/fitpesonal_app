package br.com.fitpersonal.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import br.com.fitpersonal.app.ui.common.vidro
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Barra de abas flutuante: uma "pílula" arredondada, com sombra, solta das bordas da tela. A aba ativa
 * vira uma cápsula na cor da marca com ícone e nome; as demais mostram só o ícone.
 */
@Composable
fun BarraInferior(rotaSelecionada: String?, onSelecionar: (Destino) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .vidro(RoundedCornerShape(32.dp), elevacao = 16.dp, escuro = esquema.background.luminance() < 0.5f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Destino.entries.forEach { d ->
                    ItemDaBarra(d, ativo = d.rota == rotaSelecionada, onClick = { onSelecionar(d) })
                }
            }
        }
    }
}

@Composable
private fun ItemDaBarra(destino: Destino, ativo: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    // 0 → 1: a cápsula ativa surge como uma "gota" de vidro colorido (cor da marca + brilho + borda de luz)
    val presenca by animateFloatAsState(if (ativo) 1f else 0f, animationSpec = tween(220), label = "presencaAba")
    val conteudo by animateColorAsState(
        if (ativo) esquema.onPrimary else esquema.onSurfaceVariant,
        animationSpec = tween(180),
        label = "corAba",
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(esquema.primary.copy(alpha = 0.92f * presenca))
            .drawWithContent {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.42f * presenca),
                        0.55f to Color.White.copy(alpha = 0f),
                        endY = size.height,
                    ),
                )
                drawContent()
            }
            .border(1.dp, Color.White.copy(alpha = 0.45f * presenca), RoundedCornerShape(24.dp))
            .selectable(selected = ativo, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (ativo) 16.dp else 12.dp, vertical = 10.dp)
            .animateContentSize(animationSpec = tween(180)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (ativo) destino.iconeSelecionado else destino.icone,
            contentDescription = destino.rotulo,
            tint = conteudo,
            modifier = Modifier.size(24.dp),
        )
        if (ativo) {
            Text(
                destino.rotulo,
                style = MaterialTheme.typography.labelLarge,
                color = conteudo,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
