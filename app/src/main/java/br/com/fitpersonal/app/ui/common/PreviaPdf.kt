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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

private const val LARGURA_PX = 900

/**
 * Abre o PDF uma única vez e desenha cada página só quando ela aparece na tela (o PdfRenderer só
 * deixa uma página aberta por vez, então o desenho é serializado). Sem isso, um relatório longo
 * ocupava dezenas de MB de bitmaps de uma só vez.
 */
private class LeitorPdf(arquivo: File) : java.io.Closeable {
    private val pfd = ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try { PdfRenderer(pfd) } catch (e: Exception) { pfd.close(); throw e }
    private val trava = Mutex()

    val paginas: Int get() = renderer.pageCount

    /** Proporção altura/largura de cada página (para reservar o espaço antes de desenhar). */
    suspend fun proporcoes(): List<Float> = trava.withLock {
        (0 until renderer.pageCount).map { i ->
            renderer.openPage(i).use { it.height.toFloat() / it.width.toFloat() }
        }
    }

    suspend fun desenhar(indice: Int): Bitmap = trava.withLock {
        renderer.openPage(indice).use { pagina ->
            val escala = LARGURA_PX.toFloat() / pagina.width
            val bmp = Bitmap.createBitmap(LARGURA_PX, (pagina.height * escala).toInt(), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(CorAndroid.WHITE)
            pagina.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}

/** Pré-visualização em tela cheia do PDF gerado, com ações de compartilhar e de enviar pelo WhatsApp. */
@Composable
fun PreviaPdf(
    arquivo: File,
    onFechar: () -> Unit,
    onCompartilhar: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    // null = abrindo; lista vazia = falha. O leitor fica aberto enquanto a prévia estiver na tela.
    val leitor = remember(arquivo) { runCatching { LeitorPdf(arquivo) }.getOrNull() }
    DisposableEffect(leitor) { onDispose { leitor?.close() } }
    val proporcoes by produceState<List<Float>?>(initialValue = null, leitor) {
        value = if (leitor == null) emptyList()
        else withContext(Dispatchers.IO) { runCatching { leitor.proporcoes() }.getOrDefault(emptyList()) }
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

                // Ações logo abaixo do título: sempre visíveis, mesmo que o sistema esconda a parte de baixo do diálogo.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = onWhatsApp, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                    Button(onClick = onCompartilhar, modifier = Modifier.weight(1f)) { Text("Compartilhar") }
                }

                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val lista = proporcoes
                    when {
                        lista == null -> CircularProgressIndicator()
                        lista.isEmpty() -> Text(
                            "Não foi possível exibir o arquivo, mas você ainda pode compartilhá-lo.",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        else -> LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 56.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(lista) { i, proporcao ->
                                PaginaPdf(leitor!!, i, proporcao)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Uma página: reserva o espaço pela proporção e desenha o bitmap só quando entra na composição. */
@Composable
private fun PaginaPdf(leitor: LeitorPdf, indice: Int, proporcao: Float) {
    val bmp by produceState<Bitmap?>(initialValue = null, leitor, indice) {
        value = withContext(Dispatchers.IO) { runCatching { leitor.desenhar(indice) }.getOrNull() }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f / proporcao)
            .background(androidx.compose.ui.graphics.Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "Página ${indice + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
