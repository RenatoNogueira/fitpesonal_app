package br.com.fitpersonal.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Abertura do app: a logo entra com um leve "pulo" (mola) e fade, segura um instante e some deixando
 * o app aparecer. Toque em qualquer ponto para pular. Só usa transformações de camada (sem recomposição).
 */
@Composable
fun AberturaApp(onConcluir: () -> Unit) {
    val escala = remember { Animatable(0.72f) }
    val alfaLogo = remember { Animatable(0f) }
    val alfaTela = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { alfaLogo.animateTo(1f, tween(320)) }
            launch { escala.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        }
        delay(450)
        alfaTela.animateTo(0f, tween(260))
        onConcluir()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = alfaTela.value }
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onConcluir() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = "FitPersonal",
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = escala.value
                    scaleY = escala.value
                    alpha = alfaLogo.value
                }
                .clip(RoundedCornerShape(52.dp)),
        )
    }
}
