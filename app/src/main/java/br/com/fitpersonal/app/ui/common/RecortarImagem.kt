package br.com.fitpersonal.app.ui.common

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.fitpersonal.app.data.files.decodificarOrientada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ModoCorte { Nenhum, Mover, SupEsq, SupDir, InfEsq, InfDir }

private val PROPORCOES: List<Pair<String, Float?>> = listOf(
    "Livre" to null,
    "1:1" to 1f,
    "3:4" to 0.75f,
    "4:3" to 4f / 3f,
)

/**
 * Tela cheia para recortar a imagem escolhida antes de salvar. Arraste o interior para mover e os
 * cantos para redimensionar. [onConfirmar] recebe o recorte como fração (0..1) da imagem já orientada.
 */
@Composable
fun RecortarImagemDialog(
    origem: Uri,
    proporcaoInicial: Float?,
    onConfirmar: (RectF) -> Unit,
    onUsarOriginal: () -> Unit,
    onCancelar: () -> Unit,
) {
    val ctx = LocalContext.current
    val carga by produceState<Result<Bitmap>?>(null, origem) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodificarOrientada(ctx.contentResolver, origem, 1024) }
        }
    }
    var proporcao by remember { mutableStateOf(proporcaoInicial) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    val densidade = LocalDensity.current
    val alca = with(densidade) { 36.dp.toPx() }
    val minimo = with(densidade) { 64.dp.toPx() }

    val bmp: Bitmap? = carga?.getOrNull()
    val img: Rect? = if (bmp != null && area.width > 0 && area.height > 0) {
        val sc = min(area.width / bmp.width.toFloat(), area.height / bmp.height.toFloat())
        val dw = bmp.width * sc
        val dh = bmp.height * sc
        Rect(
            (area.width - dw) / 2f, (area.height - dh) / 2f,
            (area.width + dw) / 2f, (area.height + dh) / 2f,
        )
    } else null
    var corte by remember(img, proporcao) { mutableStateOf(img?.let { corteInicial(it, proporcao) } ?: Rect.Zero) }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            // Dentro de Dialog os insets nem sempre chegam: margem fixa garante que os botões fiquem visíveis.
            Column(
                Modifier.statusBarsPadding().navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 32.dp),
            ) {
                Text(
                    "Recortar imagem",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
                Box(
                    Modifier.weight(1f).fillMaxWidth().onSizeChanged { area = it },
                    contentAlignment = Alignment.Center,
                ) {
                    val falha = carga?.exceptionOrNull()
                    when {
                        carga == null -> CircularProgressIndicator(color = Color.White)
                        falha != null -> Text(
                            "Não foi possível abrir a imagem para recortar.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        bmp != null && img != null -> {
                            Canvas(
                                Modifier.fillMaxSize().pointerInput(img, proporcao) {
                                    var modo = ModoCorte.Nenhum
                                    detectDragGestures(
                                        onDragStart = { p -> modo = detectarModo(p, corte, alca) },
                                        onDragEnd = { modo = ModoCorte.Nenhum },
                                        onDragCancel = { modo = ModoCorte.Nenhum },
                                    ) { change, arrasto ->
                                        change.consume()
                                        if (modo != ModoCorte.Nenhum) {
                                            corte = aplicarArrasto(modo, arrasto, corte, img, proporcao, minimo)
                                        }
                                    }
                                },
                            ) {
                                drawImage(
                                    bmp.asImageBitmap(),
                                    dstOffset = IntOffset(img.left.roundToInt(), img.top.roundToInt()),
                                    dstSize = IntSize(img.width.roundToInt(), img.height.roundToInt()),
                                )
                                val sombra = Color.Black.copy(alpha = 0.6f)
                                drawRect(sombra, Offset(img.left, img.top), Size(img.width, corte.top - img.top))
                                drawRect(sombra, Offset(img.left, corte.bottom), Size(img.width, img.bottom - corte.bottom))
                                drawRect(sombra, Offset(img.left, corte.top), Size(corte.left - img.left, corte.height))
                                drawRect(sombra, Offset(corte.right, corte.top), Size(img.right - corte.right, corte.height))

                                val fina = Stroke(1.dp.toPx())
                                val guia = Color.White.copy(alpha = 0.45f)
                                for (k in 1..2) {
                                    val x = corte.left + corte.width * k / 3f
                                    val y = corte.top + corte.height * k / 3f
                                    drawLine(guia, Offset(x, corte.top), Offset(x, corte.bottom), fina.width)
                                    drawLine(guia, Offset(corte.left, y), Offset(corte.right, y), fina.width)
                                }
                                drawRect(Color.White, corte.topLeft, corte.size, style = Stroke(2.dp.toPx()))
                                listOf(corte.topLeft, corte.topRight, corte.bottomLeft, corte.bottomRight).forEach {
                                    drawCircle(Color.White, radius = 9.dp.toPx(), center = it)
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    PROPORCOES.forEach { (rotulo, valor) ->
                        FilterChip(
                            selected = proporcao == valor,
                            onClick = { proporcao = valor },
                            label = { Text(rotulo) },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedContainerColor = Color(0xFF3A8208),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCancelar) { Text("Cancelar", color = Color.White) }
                    OutlinedButton(onClick = onUsarOriginal) { Text("Usar inteira", color = Color.White) }
                    Button(
                        enabled = img != null,
                        onClick = {
                            val i = img ?: return@Button
                            onConfirmar(
                                RectF(
                                    ((corte.left - i.left) / i.width).coerceIn(0f, 1f),
                                    ((corte.top - i.top) / i.height).coerceIn(0f, 1f),
                                    ((corte.right - i.left) / i.width).coerceIn(0f, 1f),
                                    ((corte.bottom - i.top) / i.height).coerceIn(0f, 1f),
                                ),
                            )
                        },
                    ) { Text("Recortar") }
                }
            }
        }
    }
}

private fun corteInicial(img: Rect, proporcao: Float?): Rect {
    if (proporcao == null) return img.deflate(min(img.width, img.height) * 0.1f)
    var w = img.width * 0.9f
    var h = w / proporcao
    if (h > img.height * 0.9f) {
        h = img.height * 0.9f
        w = h * proporcao
    }
    return Rect(Offset(img.center.x - w / 2f, img.center.y - h / 2f), Size(w, h))
}

private fun detectarModo(p: Offset, corte: Rect, alca: Float): ModoCorte {
    val cantos = listOf(
        ModoCorte.SupEsq to corte.topLeft,
        ModoCorte.SupDir to corte.topRight,
        ModoCorte.InfEsq to corte.bottomLeft,
        ModoCorte.InfDir to corte.bottomRight,
    )
    val perto = cantos.minByOrNull { (_, c) -> (c - p).getDistance() }
    if (perto != null && (perto.second - p).getDistance() <= alca) return perto.first
    return if (corte.contains(p)) ModoCorte.Mover else ModoCorte.Nenhum
}

private fun aplicarArrasto(
    modo: ModoCorte,
    d: Offset,
    corte: Rect,
    img: Rect,
    proporcao: Float?,
    minimo: Float,
): Rect {
    if (modo == ModoCorte.Mover) {
        val dx = d.x.coerceIn(img.left - corte.left, img.right - corte.right)
        val dy = d.y.coerceIn(img.top - corte.top, img.bottom - corte.bottom)
        return corte.translate(dx, dy)
    }
    val sx = if (modo == ModoCorte.SupDir || modo == ModoCorte.InfDir) 1f else -1f
    val sy = if (modo == ModoCorte.InfEsq || modo == ModoCorte.InfDir) 1f else -1f
    // Canto oposto fica fixo; o canto arrastado acompanha o dedo.
    val fx = if (sx > 0) corte.left else corte.right
    val fy = if (sy > 0) corte.top else corte.bottom
    val mx = (if (sx > 0) corte.right else corte.left) + d.x
    val my = (if (sy > 0) corte.bottom else corte.top) + d.y
    val folgaH = if (sx > 0) img.right - fx else fx - img.left
    val folgaV = if (sy > 0) img.bottom - fy else fy - img.top

    var w = abs(mx - fx)
    var h = abs(my - fy)
    if (proporcao != null) {
        w = max(w, h * proporcao)
        w = min(w, min(folgaH, folgaV * proporcao))
        w = max(w, min(minimo, min(folgaH, folgaV * proporcao)))
        h = w / proporcao
    } else {
        w = w.coerceIn(min(minimo, folgaH), folgaH)
        h = h.coerceIn(min(minimo, folgaV), folgaV)
    }
    return Rect(
        left = if (sx > 0) fx else fx - w,
        top = if (sy > 0) fy else fy - h,
        right = if (sx > 0) fx + w else fx,
        bottom = if (sy > 0) fy + h else fy,
    )
}
