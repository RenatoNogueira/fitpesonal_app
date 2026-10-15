package br.com.fitpersonal.app.ui.lgpd

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.rules.TermoLgpd
import br.com.fitpersonal.app.ui.FitNavHost
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.protecao.PortaoBloqueio
import br.com.fitpersonal.app.ui.recuperacao.RecuperacaoScreen
import br.com.fitpersonal.app.ui.recuperacao.RecuperacaoViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EstadoConsentimento {
    data object Carregando : EstadoConsentimento
    data object Pendente : EstadoConsentimento
    data object Aceito : EstadoConsentimento
}

@HiltViewModel
class ConsentimentoViewModel @Inject constructor(
    private val config: ConfiguracoesRepository,
) : ViewModel() {

    val estado: StateFlow<EstadoConsentimento> = config.config
        .map { if (it.consentimentoVersao >= TermoLgpd.VERSAO) EstadoConsentimento.Aceito else EstadoConsentimento.Pendente }
        .estadoSeguro(viewModelScope, EstadoConsentimento.Carregando)

    fun aceitar() {
        viewModelScope.launch(TratadorDeErros) { config.registrarConsentimento(TermoLgpd.VERSAO, System.currentTimeMillis()) }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Raiz do app: o conteúdo só aparece depois do aceite da versão atual do termo. */
@Composable
fun AppRaiz(vm: ConsentimentoViewModel = hiltViewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    when (estado) {
        EstadoConsentimento.Carregando ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        EstadoConsentimento.Pendente ->
            ConsentimentoScreen(onAceitar = vm::aceitar, onRecusar = { ctx.activity()?.finishAffinity() })
        EstadoConsentimento.Aceito -> {
            // Banco ilegível (ex.: chave do Keystore perdida) foi posto de lado: guia o usuário antes do app.
            val rec: RecuperacaoViewModel = hiltViewModel()
            val info by rec.info.collectAsStateWithLifecycle()
            val pendente = info
            if (pendente != null) RecuperacaoScreen(pendente, rec) else PortaoBloqueio { FitNavHost() }
        }
    }
}

@Composable
fun ConsentimentoScreen(onAceitar: () -> Unit, onRecusar: () -> Unit) {
    var li by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(TermoLgpd.TITULO, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Antes de começar, leia como os dados dos seus alunos são tratados neste aplicativo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TermoConteudo(emCartoes = true)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = li, onCheckedChange = { li = it })
                        Text(
                            "Li e concordo com o termo e assumo a responsabilidade de coletar o consentimento dos meus alunos.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(onClick = onAceitar, enabled = li, modifier = Modifier.fillMaxWidth()) {
                        Text("Aceitar e continuar")
                    }
                    TextButton(onClick = onRecusar, modifier = Modifier.fillMaxWidth()) { Text("Recusar e sair") }
                }
            }
        }
    }
}

/**
 * As seções do termo. Na tela de aceite usa cartões ([emCartoes] = true); no diálogo
 * "Ver termo" do Perfil usa texto corrido.
 */
@Composable
fun TermoConteudo(emCartoes: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TermoLgpd.SECOES.forEach { secao ->
            if (emCartoes) {
                CartaoSecao(titulo = secao.titulo) {
                    Text(secao.texto, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(secao.titulo, style = MaterialTheme.typography.titleSmall)
                    Text(secao.texto, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text(
            "Versão ${TermoLgpd.VERSAO} do termo.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
