package br.com.fitpersonal.app.ui.protecao

import br.com.fitpersonal.app.core.estadoSeguro
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProtecaoViewModel @Inject constructor(
    config: ConfiguracoesRepository,
    private val bloqueio: BloqueioApp,
) : ViewModel() {
    /** null enquanto a configuração ainda não foi lida. */
    val bloqueioAtivo: StateFlow<Boolean?> = config.config
        .map { it.bloqueioAtivo }
        .estadoSeguro<Boolean?>(viewModelScope, null)

    val desbloqueado: StateFlow<Boolean> = bloqueio.desbloqueado

    fun desbloquear() = bloqueio.desbloquear()
}

/** Mostra [conteudo] só depois do desbloqueio quando o bloqueio do app está ligado. */
@Composable
fun PortaoBloqueio(vm: ProtecaoViewModel = hiltViewModel(), conteudo: @Composable () -> Unit) {
    val ativo by vm.bloqueioAtivo.collectAsStateWithLifecycle()
    val desbloqueado by vm.desbloqueado.collectAsStateWithLifecycle()
    when {
        ativo == null ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ativo == false || desbloqueado -> conteudo()
        else -> BloqueioScreen(onDesbloqueado = vm::desbloquear)
    }
}

@Composable
private fun BloqueioScreen(onDesbloqueado: () -> Unit) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) onDesbloqueado()
    }

    fun pedir() {
        val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = km.createConfirmDeviceCredentialIntent(
            "FitPersonal",
            "Confirme sua identidade para abrir o app",
        )
        // null = aparelho sem bloqueio de tela: não há como autenticar, então não prende o usuário.
        if (intent == null) onDesbloqueado() else launcher.launch(intent)
    }

    LaunchedEffect(Unit) { pedir() }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text("App bloqueado", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Use a biometria, PIN ou padrão do aparelho para entrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = ::pedir) { Text("Desbloquear") }
        }
    }
}
