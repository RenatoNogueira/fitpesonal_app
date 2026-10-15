package br.com.fitpersonal.app

import br.com.fitpersonal.app.core.TratadorDeErros
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.fitpersonal.app.aluno.AlunoApp
import br.com.fitpersonal.app.aluno.AlunoTema
import br.com.fitpersonal.app.aluno.Recebido
import br.com.fitpersonal.app.aluno.Repositorio
import br.com.fitpersonal.app.data.repo.Configuracoes
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.domain.rules.TemaApp
import br.com.fitpersonal.app.domain.rules.UsoApp
import br.com.fitpersonal.app.ui.EscolhaDeUso
import br.com.fitpersonal.app.ui.common.AberturaApp
import br.com.fitpersonal.app.ui.lgpd.AppRaiz
import br.com.fitpersonal.app.ui.protecao.BloqueioApp
import br.com.fitpersonal.app.ui.theme.FitTheme
import br.com.fitpersonal.app.widget.AgendaWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Única porta de entrada. Na primeira abertura a pessoa escolhe professor ou aluno ([EscolhaDeUso]);
 * a partir daí o app mostra só a metade escolhida. Arquivos de treino (.rjetreino, .rjetreinopack) e
 * códigos recebidos por "Compartilhar" são tratados no modo aluno.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var bloqueio: BloqueioApp
    @Inject lateinit var configuracoes: ConfiguracoesRepository
    @Inject lateinit var trabalhos: br.com.fitpersonal.app.work.TrabalhosAgendados

    private lateinit var repoAluno: Repositorio

    /** Aviso do último treino recebido por arquivo/compartilhamento, mostrado uma vez na lista do aluno. */
    private var aviso by mutableStateOf<String?>(null)

    /** Enquanto false, a splash do sistema segura a tela (evita piscar a escolha antes de ler a configuração). */
    @Volatile private var configLida = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Precisa vir antes do super.onCreate: troca o tema de splash pelo tema normal do app.
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !configLida }
        super.onCreate(savedInstanceState)
        repoAluno = Repositorio(applicationContext)

        // Capturas de tela / miniatura em "recentes" bloqueadas quando o usuário pede (dados de saúde).
        lifecycleScope.launch(TratadorDeErros) {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                configuracoes.config.map { it.bloquearCapturaTela }.distinctUntilChanged().collect { bloquear ->
                    if (bloquear) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        tratarIntent(intent)

        setContent {
            val cfg: Configuracoes? by configuracoes.config
                .collectAsStateWithLifecycle<Configuracoes?>(initialValue = null)
            val atual = cfg
            if (atual != null) configLida = true

            val escuro = when (atual?.tema ?: TemaApp.SISTEMA) {
                TemaApp.SISTEMA -> isSystemInDarkTheme()
                TemaApp.CLARO -> false
                TemaApp.ESCURO -> true
            }
            // Ícones das barras do sistema acompanham o tema escolhido, não só o do sistema.
            DisposableEffect(escuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { escuro },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { escuro },
                )
                onDispose {}
            }
            var abertura by rememberSaveable { mutableStateOf(true) }

            FitTheme(dark = escuro) {
                Box {
                    when (atual?.usoApp) {
                        null -> if (atual != null) {
                            EscolhaDeUso(onEscolher = { uso -> lifecycleScope.launch(TratadorDeErros) { configuracoes.definirUsoApp(uso) } })
                        }
                        UsoApp.ALUNO -> AlunoTema {
                            AlunoApp(
                                repo = repoAluno,
                                aviso = aviso,
                                onAvisoLido = { aviso = null },
                                onTrocarModo = { lifecycleScope.launch(TratadorDeErros) { configuracoes.definirUsoApp(UsoApp.PROFESSOR) } },
                                lembrete = atual?.lembreteAluno ?: br.com.fitpersonal.app.domain.rules.LembreteAluno(),
                                onLembrete = { novo ->
                                    lifecycleScope.launch(TratadorDeErros) {
                                        configuracoes.definirLembreteAluno(novo)
                                        trabalhos.sincronizarLembreteAluno(novo)
                                    }
                                },
                            )
                        }
                        UsoApp.PROFESSOR -> AppRaiz()
                    }
                    if (abertura) AberturaApp(onConcluir = { abertura = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tratarIntent(intent)
    }

    /**
     * Aceita VIEW (arquivo .rjetreino / .rjetreinopack) e SEND (arquivo ou texto com o código).
     * Se a pessoa ainda não escolheu o modo, receber um treino já define "aluno".
     * No modo professor o conteúdo é ignorado (quem gera os treinos é o professor).
     */
    private fun tratarIntent(i: Intent?) {
        i ?: return
        val uri: Uri? = when (i.action) {
            Intent.ACTION_VIEW -> i.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }
        val textoDireto: String? = if (i.action == Intent.ACTION_SEND) i.getStringExtra(Intent.EXTRA_TEXT) else null
        if (uri == null && textoDireto.isNullOrBlank()) return

        lifecycleScope.launch(TratadorDeErros) {
            val uso = configuracoes.config.first().usoApp
            if (uso == UsoApp.PROFESSOR) {
                aviso = null
                return@launch
            }
            if (uso == null) configuracoes.definirUsoApp(UsoApp.ALUNO)

            val resultado = withContext(Dispatchers.IO) {
                when {
                    uri != null && ehPacoteComVideos(uri) -> runCatching {
                        contentResolver.openInputStream(uri)?.use { repoAluno.receberPacoteComVideos(it) }
                    }.getOrNull() ?: Recebido.Erro("Não foi possível abrir o arquivo.")
                    else -> {
                        val texto = uri?.let(::lerUri) ?: textoDireto
                        if (texto.isNullOrBlank()) null else repoAluno.receber(texto)
                    }
                }
            } ?: return@launch
            aviso = mensagemDe(resultado)
        }
    }

    private fun mensagemDe(r: Recebido): String = when (r) {
        is Recebido.Novo -> "Treino \"${r.pacote.treino.nome}\" recebido."
        is Recebido.Atualizado -> "Treino \"${r.pacote.treino.nome}\" atualizado (revisão ${r.pacote.revisao})."
        is Recebido.JaTemIgualOuMaisNovo -> "Você já tem esta versão (ou uma mais nova) de \"${r.pacote.treino.nome}\"."
        is Recebido.Erro -> r.mensagem
    }

    private fun ehPacoteComVideos(uri: Uri): Boolean {
        if (uri.toString().lowercase().endsWith(".rjetreinopack")) return true
        return runCatching {
            contentResolver.openInputStream(uri)?.use { s ->
                val a = s.read()
                val b = s.read()
                a == 'P'.code && b == 'K'.code
            } == true
        }.getOrDefault(false)
    }

    private fun lerUri(uri: Uri): String? = runCatching {
        contentResolver.openInputStream(uri)?.use { it.readBytes().take(600_000).toByteArray().toString(Charsets.UTF_8) }
    }.getOrNull()

    override fun onStart() {
        super.onStart()
        bloqueio.aoVoltarAoPrimeiroPlano()
    }

    override fun onStop() {
        bloqueio.aoIrParaSegundoPlano()
        // O usuário pode ter mudado a agenda: atualiza o widget ao sair do app.
        val app = applicationContext
        lifecycleScope.launch(TratadorDeErros) { runCatching { AgendaWidget().updateAll(app) } }
        super.onStop()
    }
}
