package br.com.fitpersonal.app.ui.lgpd

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.core.Analitica
import br.com.fitpersonal.app.core.TratadorDeErros
import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.rules.TermoLgpd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnaliticaViewModel @Inject constructor(
    private val config: ConfiguracoesRepository,
) : ViewModel() {

    /**
     * Pergunta uma única vez, e só quando faz sentido: o Firebase está configurado neste build, a pessoa já escolheu
     * o modo de uso e, no modo professor, já aceitou o termo de privacidade (a pergunta vem depois dele).
     */
    val devePerguntar: StateFlow<Boolean> = config.config
        .map { c ->
            Analitica.disponivel && !c.analiticaPerguntada && c.usoApp != null &&
                (c.usoApp == br.com.fitpersonal.app.domain.rules.UsoApp.ALUNO || c.consentimentoVersao >= TermoLgpd.VERSAO)
        }
        .estadoSeguro(viewModelScope, false)

    fun responder(aceitou: Boolean) {
        viewModelScope.launch(TratadorDeErros) { config.definirAnalitica(aceitou) }
    }
}

/** Pergunta inicial, uma vez: "Ajudar a melhorar o app com estatísticas de uso anônimas?". Recusar é a opção neutra. */
@Composable
fun PerguntaAnalitica(vm: AnaliticaViewModel = hiltViewModel()) {
    val perguntar by vm.devePerguntar.collectAsStateWithLifecycle()
    if (!perguntar) return
    AlertDialog(
        onDismissRequest = { vm.responder(false) },
        title = { Text("Ajude a melhorar o FitPersonal") },
        text = {
            Text(
                "Podemos coletar estatísticas de uso anônimas, como quais telas são mais usadas e em qual momento as pessoas " +
                    "desistem de uma tarefa. Nunca enviamos nomes, medidas, fotos, mensagens nem qualquer dado de alunos. " +
                    "Você pode mudar isso depois em Perfil → Segurança e privacidade.",
            )
        },
        confirmButton = { TextButton(onClick = { vm.responder(true) }) { Text("Aceitar") } },
        dismissButton = { TextButton(onClick = { vm.responder(false) }) { Text("Agora não") } },
    )
}
