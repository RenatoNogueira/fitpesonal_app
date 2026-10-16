package br.com.fitpersonal.app.ui.recuperacao

import br.com.fitpersonal.app.core.TratadorDeErros
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.backup.BackupInvalidoException
import br.com.fitpersonal.app.data.backup.BackupManager
import br.com.fitpersonal.app.data.crypto.SenhaIncorretaException
import br.com.fitpersonal.app.data.local.InfoRecuperacao
import br.com.fitpersonal.app.data.local.RegistroRecuperacao
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.DialogoSenha
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.system.exitProcess

data class RecuperacaoState(
    val ocupado: Boolean = false,
    val mensagem: String? = null,
    val pedirSenhaPara: Uri? = null,
    val restauracaoConcluida: Boolean = false,
)

/**
 * Tela exibida quando o banco anterior não pôde ser aberto (ex.: chave do Keystore perdida após
 * restaurar/trocar de aparelho). O banco antigo foi guardado de lado e o app já tem um banco novo, vazio.
 */
@HiltViewModel
class RecuperacaoViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val backup: BackupManager,
) : ViewModel() {

    private val dir: File get() = ctx.noBackupFilesDir

    private val _info = MutableStateFlow(RegistroRecuperacao.ler(dir))
    val info: StateFlow<InfoRecuperacao?> = _info

    private val _state = MutableStateFlow(RecuperacaoState())
    val state: StateFlow<RecuperacaoState> = _state

    fun mensagemLida() = _state.update { it.copy(mensagem = null) }
    fun cancelarSenha() = _state.update { it.copy(pedirSenhaPara = null) }

    /** Segue com o banco novo e vazio; o banco antigo continua guardado de lado. */
    fun comecarDoZero() {
        // As fotos antigas só eram referenciadas pelo banco ilegível: sem ele, ficariam órfãs no disco.
        File(ctx.filesDir, "fotos").deleteRecursively()
        RegistroRecuperacao.limpar(dir)
        _info.value = null
    }

    fun restaurar(origem: Uri, senha: CharArray? = null) {
        _state.update { it.copy(ocupado = true, pedirSenhaPara = null) }
        viewModelScope.launch(TratadorDeErros) {
            val cifrado = runCatching { backup.ehCifrado(origem) }.getOrDefault(false)
            if (cifrado && senha == null) {
                _state.update { it.copy(ocupado = false, pedirSenhaPara = origem) }
                return@launch
            }
            runCatching { backup.restaurar(origem, senha) }
                .onSuccess {
                    RegistroRecuperacao.limpar(dir)
                    _state.update { it.copy(ocupado = false, restauracaoConcluida = true) }
                }
                .onFailure { e ->
                    senha?.fill('0')
                    when (e) {
                        is SenhaIncorretaException ->
                            _state.update { it.copy(ocupado = false, pedirSenhaPara = origem, mensagem = "Senha incorreta.") }
                        is BackupInvalidoException ->
                            _state.update { it.copy(ocupado = false, mensagem = e.message) }
                        else ->
                            _state.update { it.copy(ocupado = false, mensagem = "Falha na restauração: ${e.message}") }
                    }
                }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
fun RecuperacaoScreen(info: InfoRecuperacao, vm: RecuperacaoViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmarZero by remember { mutableStateOf(false) }
    var escolhido by rememberSaveable { mutableStateOf<Uri?>(null) }

    LaunchedEffect(s.mensagem) { s.mensagem?.let { snackbar.showSnackbar(it); vm.mensagemLida() } }

    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        escolhido = uri
    }

    // Quando o arquivo é escolhido, tenta restaurar (pede a senha se estiver cifrado).
    LaunchedEffect(escolhido) {
        escolhido?.let { vm.restaurar(it) }
        escolhido = null
    }

    val quando = if (info.emMillis > 0)
        Instant.ofEpochMilli(info.emMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Text("Não foi possível abrir seus dados", style = MaterialTheme.typography.headlineMedium)
            Text(
                info.motivo + if (quando != null) " (detectado em $quando)" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CartaoSecao(titulo = "O que aconteceu") {
                Text(
                    "Isso costuma acontecer ao restaurar o aparelho ou mudar de celular: a chave que protege " +
                        "o banco fica guardada só no aparelho de origem e não acompanha a restauração do sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            CartaoSecao(titulo = "Como recuperar") {
                Text(
                    "Seus dados continuam protegidos. Restaure um backup (.fitbak) usando a senha definida quando " +
                        "ele foi criado. Uma cópia do arquivo antigo foi mantida no aparelho.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { abrir.launch(arrayOf("*/*")) },
                enabled = !s.ocupado,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar de um backup") }

            OutlinedButton(
                onClick = { confirmarZero = true },
                enabled = !s.ocupado,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Começar do zero") }

            Text(
                "Também é possível trazer dados do aplicativo desktop depois, em Perfil e backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (s.ocupado) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                    Text("Restaurando backup…")
                }
            },
        )
    }

    s.pedirSenhaPara?.let { uri ->
        DialogoSenha(
            titulo = "Senha do backup",
            aviso = "Informe a senha usada quando este backup foi criado.",
            confirmar = false,
            textoConfirmar = "Restaurar",
            onCancelar = vm::cancelarSenha,
            onConfirmar = { senha, _ ->
                vm.restaurar(uri, senha.toCharArray())
                null
            },
        )
    }

    if (confirmarZero) {
        AlertDialog(
            onDismissRequest = { confirmarZero = false },
            title = { Text("Começar do zero?") },
            text = {
                Text(
                    "O app abrirá vazio. Você ainda poderá restaurar um backup depois, em Perfil e backup. " +
                        "Os dados antigos não podem ser lidos sem a chave original.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmarZero = false; vm.comecarDoZero() }) { Text("Começar do zero") } },
            dismissButton = { TextButton(onClick = { confirmarZero = false }) { Text("Voltar") } },
        )
    }

    if (s.restauracaoConcluida) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restauração concluída") },
            text = { Text("Os dados foram restaurados. O aplicativo será fechado — abra-o novamente para continuar.") },
            confirmButton = {
                TextButton(onClick = {
                    ctx.activity()?.finishAffinity()
                    exitProcess(0)
                }) { Text("Fechar app") }
            },
        )
    }
}
