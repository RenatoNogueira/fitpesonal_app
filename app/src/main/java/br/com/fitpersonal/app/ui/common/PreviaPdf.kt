package br.com.fitpersonal.app.ui.common

import android.graphics.Bitmap
import android.graphics.Color as CorAndroid
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_PAGINAS = 12
private const val LARGURA_PX = 1080

/** Renderiza as primeiras páginas do PDF em bitmaps (null = falha ao abrir). */
private fun renderizar(arquivo: File): List<Bitmap>? = runCatching {
    ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            (0 until minOf(renderer.pageCount, MAX_PAGINAS)).map { i ->
                renderer.openPage(i).use { pagina ->
                    val escala = LARGURA_PX.toFloat() / pagina.width
                    val bmp = Bitmap.createBitmap(LARGURA_PX, (pagina.height * escala).toInt(), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(CorAndroid.WHITE)
                    pagina.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }
}.getOrNull()

/** Pré-visualização em tela cheia do PDF gerado, com ações de compartilhar e de enviar pelo WhatsApp. */
@Composable
fun PreviaPdf(
    arquivo: File,
    onFechar: () -> Unit,
    onCompartilhar: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    // null = renderizando; lista vazia = falha
    val paginas by produceState<List<Bitmap>?>(initialValue = null, arquivo) {
        value = withContext(Dispatchers.IO) { renderizar(arquivo) } ?: emptyList()
    }

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.statusBarsPadding().fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Pré-visualização",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onFechar) { Icon(Icons.Filled.Close, "Fechar") }
                }

                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val lista = paginas
                    when {
                        lista == null -> CircularProgressIndicator()
                        lista.isEmpty() -> Text(
                            "Não foi possível exibir o arquivo, mas você ainda pode compartilhá-lo.",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        else -> LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(lista) { i, bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Página ${i + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.White),
                                )
                            }
                        }
                    }
                }

                Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(
                        Modifier.navigationBarsPadding().fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = onWhatsApp, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                        Button(onClick = onCompartilhar, modifier = Modifier.weight(1f)) { Text("Compartilhar") }
                    }
                }
            }
        }
    }
}
