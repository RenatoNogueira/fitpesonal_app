package br.com.fitpersonal.app.ui.evolucao

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Compara duas fotos da mesma posição: a de "depois" fica por baixo e a de "antes" aparece só à
 * esquerda do marcador. Arraste na horizontal (ou toque) para revelar mais de uma ou da outra.
 */
@Composable
fun ComparadorDeslizante(antes: File, depois: File, modifier: Modifier = Modifier) {
    var fracao by remember { mutableFloatStateOf(0.5f) }
    var largura by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onSizeChanged { largura = it.width }
            .semantics { contentDescription = "Comparação antes e depois: arraste para os lados" }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> if (size.width > 0) fracao = (o.x / size.width).coerceIn(0f, 1f) },
                ) { change, _ ->
                    change.consume()
                    if (size.width > 0) fracao = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
    ) {
        AsyncImage(
            model = depois,
            contentDescription = "Foto depois",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model = antes,
            contentDescription = "Foto antes",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * fracao) { this@drawWithContent.drawContent() }
                },
        )

        // marcador vertical + alça
        Box(
            Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset { IntOffset((largura * fracao).toInt() - 1, 0) }
                .background(Color.White),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((largura * fracao).toInt() - 18.dp.roundToPx(), 0) }
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("⇆", color = Color.Black, style = MaterialTheme.typography.titleMedium)
        }

        EtiquetaFoto("Antes", Modifier.align(Alignment.TopStart).padding(8.dp))
        EtiquetaFoto("Depois", Modifier.align(Alignment.TopEnd).padding(8.dp))
    }
}

@Composable
private fun EtiquetaFoto(texto: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}
