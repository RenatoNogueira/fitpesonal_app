package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.rules.AniversarioRules
import coil.compose.AsyncImage
import java.io.File

/** Foto circular do aluno; sem foto (ou arquivo ausente) mostra as iniciais sobre a cor do tema. */
@Composable
fun AvatarAluno(nome: String, arquivo: File?, tamanho: Dp = 40.dp, modifier: Modifier = Modifier) {
    // Começa tentando carregar a foto; a checagem em disco roda fora da thread principal.
    val temFoto by produceState(initialValue = arquivo != null, arquivo) {
        value = arquivo != null && withContext(Dispatchers.IO) { arquivo.isFile }
    }
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (temFoto) {
            AsyncImage(
                model = arquivo,
                contentDescription = "Foto de $nome",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamanho),
            )
        } else {
            Text(
                text = AniversarioRules.iniciais(nome),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = if (tamanho >= 64.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            )
        }
    }
}
