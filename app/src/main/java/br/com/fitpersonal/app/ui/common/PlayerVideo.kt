package br.com.fitpersonal.app.ui.common

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

/**
 * Player de vídeo embutido (tela cheia, com controles) para vídeos guardados no aparelho.
 * Usa o VideoView do sistema: não depende de nenhum app de vídeo instalado nem de biblioteca extra.
 */
@Composable
fun PlayerVideoDialog(arquivo: File, onFechar: () -> Unit) {
    Dialog(
        onDismissRequest = onFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val moldura = FrameLayout(ctx)
                        val video = VideoView(ctx)
                        val controles = MediaController(ctx)
                        controles.setAnchorView(moldura)
                        video.setMediaController(controles)
                        video.setOnPreparedListener { video.start() }
                        video.setOnErrorListener { _, _, _ ->
                            Toast.makeText(ctx, "Não foi possível reproduzir este vídeo.", Toast.LENGTH_LONG).show()
                            onFechar()
                            true
                        }
                        video.setVideoPath(arquivo.path)
                        moldura.addView(
                            video,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER,
                            ),
                        )
                        moldura
                    },
                )
                IconButton(
                    onClick = onFechar,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding(),
                ) { Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White) }
            }
        }
    }
}
