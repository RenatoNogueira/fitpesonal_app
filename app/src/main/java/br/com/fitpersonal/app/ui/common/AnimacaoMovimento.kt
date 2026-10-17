package br.com.fitpersonal.app.ui.common

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.com.fitpersonal.app.domain.rules.CatalogoImagens
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private const val INTERVALO_MS = 900L

/**
 * Mostra como o exercício é executado: as duas fotos do catálogo (posição inicial e final) se alternam em ciclo,
 * dando a ideia do movimento. As fotos vêm do catálogo gratuito (baixadas só na primeira vez e guardadas no cache)
 * e não somem do aparelho do aluno. Não aparece se o exercício não tem foto ou se o download falhar (ex.: sem internet
 * na primeira vez). Com as animações do sistema desligadas, mostra só a posição inicial.
 */
@Composable
fun AnimacaoMovimento(catalogoId: String?, descricao: String, modifier: Modifier = Modifier) {
    val urls = remember(catalogoId) { CatalogoImagens.movimento(catalogoId) }
    if (urls.size < 2) return
    var falhou by remember(catalogoId) { mutableStateOf(false) }
    if (falhou) return

    val ctx = LocalContext.current
    val reduzirMovimento = remember {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
    var indice by remember(catalogoId) { mutableIntStateOf(0) }
    LaunchedEffect(catalogoId, reduzirMovimento) {
        if (reduzirMovimento) return@LaunchedEffect
        while (true) {
            delay(INTERVALO_MS)
            indice = (indice + 1) % urls.size
        }
    }

    Box(
        modifier
            .aspectRatio(1.4f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics { contentDescription = descricao },
    ) {
        // As duas fotos ficam carregadas ao mesmo tempo e só trocam de opacidade (sem piscar a cada volta).
        urls.forEachIndexed { i, url ->
            val opacidade by animateFloatAsState(if (indice == i) 1f else 0f, tween(250), label = "movimento")
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { falhou = true },
                modifier = Modifier.fillMaxSize().alpha(opacidade),
            )
        }
    }
}
