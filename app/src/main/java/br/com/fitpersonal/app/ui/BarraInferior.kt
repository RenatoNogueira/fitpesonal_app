package br.com.fitpersonal.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = esquema.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, esquema.outlineVariant.copy(alpha = 0.6f)),
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
    val fundo by animateColorAsState(
        if (ativo) esquema.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "fundoAba",
    )
    val conteudo by animateColorAsState(
        if (ativo) esquema.onPrimary else esquema.onSurfaceVariant,
        animationSpec = tween(180),
        label = "corAba",
    )
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(fundo)
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
