package br.com.fitpersonal.app.ui.avaliacoes

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

/**
 * Quadro de assinatura: o dedo desenha traços; "Confirmar" gera um PNG (traço preto, fundo branco)
 * com o tamanho do quadro. Fica sempre branco, independente do tema, para o PDF sair legível.
 */
@Composable
fun AssinaturaPad(onConfirmar: (Bitmap) -> Unit, modifier: Modifier = Modifier) {
    val tracos = remember { mutableStateListOf<List<Offset>>() }
    var atual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tamanho by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val espessura = 4f

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .onSizeChanged { tamanho = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { atual = listOf(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            atual = atual + change.position
                        },
                        onDragEnd = {
                            if (atual.isNotEmpty()) tracos.add(atual)
                            atual = emptyList()
                        },
                        onDragCancel = { atual = emptyList() },
                    )
                },
        ) {
            (tracos + listOf(atual)).forEach { pts ->
                if (pts.size == 1) {
                    drawCircle(Color.Black, radius = espessura / 2, center = pts[0])
                }
                for (k in 1 until pts.size) {
                    drawLine(Color.Black, pts[k - 1], pts[k], strokeWidth = espessura, cap = StrokeCap.Round)
                }
            }
        }
        Text(
            if (tracos.isEmpty()) "Assine com o dedo no quadro acima." else "Toque em Confirmar para salvar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { tracos.clear(); atual = emptyList() }) { Text("Limpar") }
            Button(
                enabled = tracos.isNotEmpty() && tamanho.width > 0 && tamanho.height > 0,
                onClick = {
                    val bmp = Bitmap.createBitmap(tamanho.width, tamanho.height, Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(bmp)
                    c.drawColor(android.graphics.Color.WHITE)
                    val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        strokeWidth = espessura
                        strokeCap = Paint.Cap.ROUND
                        style = Paint.Style.STROKE
                    }
                    tracos.forEach { pts ->
                        if (pts.size == 1) {
                            c.drawPoint(pts[0].x, pts[0].y, tinta)
                        }
                        for (k in 1 until pts.size) {
                            c.drawLine(pts[k - 1].x, pts[k - 1].y, pts[k].x, pts[k].y, tinta)
                        }
                    }
                    onConfirmar(bmp)
                    tracos.clear()
                },
            ) { Text("Confirmar assinatura") }
        }
    }
}
