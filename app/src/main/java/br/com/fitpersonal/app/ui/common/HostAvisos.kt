package br.com.fitpersonal.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.core.Aviso
import br.com.fitpersonal.app.core.Avisos
import br.com.fitpersonal.app.core.TipoAviso
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val DURACAO_MS = 2600L

/**
 * Mostra os [Avisos] como um cartão no topo da tela, por alguns segundos. Um aviso novo substitui o que
 * está na tela. Coloque uma vez, sobre o conteúdo (ex.: dentro do Box raiz da navegação).
 */
@Composable
fun HostAvisos(modifier: Modifier = Modifier) {
    var atual by remember { mutableStateOf<Aviso?>(null) }
    var visivel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Avisos.fluxo.collectLatest { novo ->
            atual = novo
            visivel = true
            delay(DURACAO_MS)
            visivel = false
        }
    }

    Box(modifier, contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visivel,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            atual?.let { CartaoAviso(it) }
        }
    }
}

@Composable
private fun CartaoAviso(aviso: Aviso) {
    val (icone, destaque) = when (aviso.tipo) {
        TipoAviso.SUCESSO -> Icons.Filled.CheckCircle to PaletaPasta.Lima
        TipoAviso.INFO -> Icons.Filled.Info to PaletaPasta.Teal
        TipoAviso.ERRO -> Icons.Filled.ErrorOutline to PaletaPasta.Coral
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = PaletaPasta.Preto,
        shadowElevation = 10.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 420.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icone, contentDescription = null, tint = destaque, modifier = Modifier.size(22.dp))
            Text(
                aviso.texto,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
            )
        }
    }
}
