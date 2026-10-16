package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Textura de vidro (estilo "liquid glass") sem depender de desfoque do que está atrás:
 *  - corpo translúcido com degradê vertical e dois "reflexos" de cor (lima e turquesa) dentro do vidro;
 *  - brilho superior (a luz batendo na face) e borda de luz mais forte no canto superior esquerdo;
 *  - sombra suave e baixa, para o vidro parecer suspenso.
 * Funciona em tema claro e escuro.
 */
@Composable
fun Modifier.vidro(
    forma: Shape,
    elevacao: Dp = 14.dp,
    escuro: Boolean = isSystemInDarkTheme(),
): Modifier {
    val corpoTopo = if (escuro) Color(0xFF232A1F).copy(alpha = 0.80f) else Color.White.copy(alpha = 0.90f)
    val corpoBase = if (escuro) Color(0xFF151A12).copy(alpha = 0.66f) else Color(0xFFF1F5E8).copy(alpha = 0.70f)
    val luz = if (escuro) 0.22f else 0.95f
    val sombra = if (escuro) Color.Black else Color(0xFF5B6B1F)

    return this
        .shadow(elevacao, forma, ambientColor = sombra.copy(alpha = 0.35f), spotColor = sombra.copy(alpha = 0.45f))
        .clip(forma)
        .background(Brush.verticalGradient(listOf(corpoTopo, corpoBase)))
        .drawWithContent {
            // reflexos de cor dentro do vidro
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFD9F24F).copy(alpha = if (escuro) 0.16f else 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 1.05f),
                    radius = size.width * 0.55f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF34D1BF).copy(alpha = if (escuro) 0.12f else 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.5f,
                ),
            )
            // brilho da face superior (desenhado antes do conteúdo para não esmaecer ícones e textos)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (escuro) 0.14f else 0.55f),
                    0.5f to Color.White.copy(alpha = 0f),
                    endY = size.height,
                ),
            )
            drawContent()
        }
        .border(
            1.2.dp,
            Brush.linearGradient(
                0f to Color.White.copy(alpha = luz),
                0.45f to Color.White.copy(alpha = luz * 0.12f),
                1f to Color.White.copy(alpha = luz * 0.45f),
            ),
            forma,
        )
}
