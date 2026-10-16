package br.com.fitpersonal.app.ui.treinos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.data.files.VideoExercicio
import br.com.fitpersonal.app.domain.rules.CargaRules
import br.com.fitpersonal.app.domain.rules.VideoRules

/** Tudo que a tela precisa para trabalhar com vídeos próprios, sem conhecer o serviço. */
class VideosAcoes(
    val mapa: Map<String, VideoExercicio>,
    val novaUriGravacao: () -> Uri,
    val importar: (nome: String, uri: Uri) -> Unit,
    val remover: (chave: String) -> Unit,
    val assistir: (VideoExercicio) -> Unit,
    val compartilhar: (VideoExercicio) -> Unit,
    val abrirLink: (String) -> Unit,
    val ocupado: Boolean,
) {
    fun doExercicio(nome: String): VideoExercicio? =
        CargaRules.chaveExercicio(nome).takeIf { it.isNotEmpty() }?.let { mapa[it] }
}

/**
 * Seção "Vídeo" do exercício: link (YouTube etc.) e/ou vídeo próprio gravado ou escolhido da galeria.
 * O vídeo próprio vale para todos os treinos que tiverem exercício com o mesmo nome.
 */
@Composable
fun SecaoVideo(
    nome: String,
    link: String,
    onLink: (String) -> Unit,
    acoes: VideosAcoes,
) {
    var uriGravacao by rememberSaveable { mutableStateOf<Uri?>(null) }
    val gravar = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        if (ok) uriGravacao?.let { acoes.importar(nome, it) }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) acoes.importar(nome, uri)
    }
    val erro = VideoRules.erro(link)
    val local = acoes.doExercicio(nome)
    val nomeOk = CargaRules.chaveExercicio(nome).isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vídeo de demonstração", style = MaterialTheme.typography.labelLarge)

        OutlinedTextField(
            value = link,
            onValueChange = { onLink(it.take(VideoRules.MAX)) },
            label = { Text("Link do vídeo (YouTube ou outro)") },
            placeholder = { Text("https://youtube.com/...") },
            isError = erro != null,
            supportingText = { Text(erro ?: "O link vai junto do treino para o aluno.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        VideoRules.normalizar(link)?.let { url ->
            TextButton(onClick = { acoes.abrirLink(url) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Testar link (${VideoRules.origem(url)})")
            }
        }

        if (local != null) {
            Text(
                "Vídeo próprio: ${local.duracaoTexto} • ${local.tamanhoTexto}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { acoes.assistir(local) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Assistir")
                }
                OutlinedButton(onClick = { acoes.compartilhar(local) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Enviar")
                }
            }
            TextButton(onClick = { acoes.remover(local.chave) }) { Text("Remover vídeo próprio") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { uriGravacao = acoes.novaUriGravacao().also { gravar.launch(it) } },
                enabled = nomeOk && !acoes.ocupado,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (local != null) "Gravar outro" else "Gravar")
            }
            OutlinedButton(
                onClick = {
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                enabled = nomeOk && !acoes.ocupado,
                modifier = Modifier.weight(1f),
            ) { Text("Da galeria") }
        }
        Text(
            if (!nomeOk) "Digite o nome do exercício para adicionar um vídeo próprio."
            else if (acoes.ocupado) "Salvando vídeo…"
            else "Vídeo próprio: até 5 min e 150 MB, guardado neste aparelho e incluído só no backup manual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
