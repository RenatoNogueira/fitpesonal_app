package br.com.fitpersonal.app.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import br.com.fitpersonal.app.ui.common.fonteDeFundo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.fitpersonal.app.ui.common.acompanharArrasto
import br.com.fitpersonal.app.ui.common.efeitoElastico
import br.com.fitpersonal.app.ui.common.rememberArrastoHorizontal
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import br.com.fitpersonal.app.ui.alunos.AlunoFormScreen
import br.com.fitpersonal.app.ui.agenda.AgendaScreen
import br.com.fitpersonal.app.ui.agenda.AgendamentoFormScreen
import br.com.fitpersonal.app.ui.alunos.AlunosScreen
import br.com.fitpersonal.app.ui.avaliacoes.AvaliacaoFormScreen
import br.com.fitpersonal.app.ui.avaliacoes.AvaliacoesScreen
import br.com.fitpersonal.app.ui.busca.BuscaScreen
import br.com.fitpersonal.app.ui.espera.ListaEsperaScreen
import br.com.fitpersonal.app.ui.fotos.LinhaDoTempoFotosScreen
import br.com.fitpersonal.app.ui.testes.TestesAptidaoScreen
import br.com.fitpersonal.app.ui.progressao.ProgressaoScreen
import br.com.fitpersonal.app.ui.evolucao.EvolucaoScreen
import br.com.fitpersonal.app.ui.ficha.FichaScreen
import br.com.fitpersonal.app.ui.home.HomeScreen
import br.com.fitpersonal.app.ui.perfil.PerfilScreen
import br.com.fitpersonal.app.ui.treinos.TreinoEditorScreen
import br.com.fitpersonal.app.ui.treinos.TreinoExecucaoScreen
import br.com.fitpersonal.app.ui.treinos.TreinosScreen

enum class Destino(
    val rota: String,
    val rotulo: String,
    val icone: ImageVector,
    val iconeSelecionado: ImageVector,
) {
    INICIO("inicio", "Início", Icons.Outlined.Home, Icons.Filled.Home),
    ALUNOS("alunos", "Alunos", Icons.Outlined.People, Icons.Filled.People),
    AGENDA("agenda", "Agenda", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TREINOS("treinos", "Treinos", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    AVALIACOES("avaliacoes", "Avaliações", Icons.Outlined.Assessment, Icons.Filled.Assessment),
}

private const val DURACAO_NAV = 220

private val ROTAS_ABAS: Set<String> = Destino.entries.map { it.rota }.toSet()

private fun indiceAba(rota: String?): Int = Destino.entries.indexOfFirst { it.rota == rota }

/**
 * Arrastar na horizontal troca de aba (esquerda = próxima, direita = anterior).
 * O deslocamento só conta se for predominantemente horizontal e passar do limiar; listas roláveis
 * na horizontal (chips, carrosséis) consomem o gesto antes e não disparam a troca.
 */
@Composable
private fun Modifier.gestoEntreAbas(
    habilitado: Boolean,
    chave: Any?,
    onProxima: () -> Unit,
    onAnterior: () -> Unit,
): Modifier {
    if (!habilitado) return this
    val arrasto = rememberArrastoHorizontal()
    val escopo = rememberCoroutineScope()
    return this
        .acompanharArrasto(arrasto)
        .pointerInput(chave) {
            var acumulado = 0f
            detectHorizontalDragGestures(
                onDragStart = { acumulado = 0f },
                onDragCancel = {
                    acumulado = 0f
                    escopo.launch { arrasto.soltar() }
                },
                onDragEnd = {
                    val limiar = size.width * 0.28f
                    val trocou = acumulado <= -limiar || acumulado >= limiar
                    when {
                        acumulado <= -limiar -> onProxima()
                        acumulado >= limiar -> onAnterior()
                    }
                    acumulado = 0f
                    // trocou de aba: a nova tela começa parada; senão a tela volta com quique
                    escopo.launch { if (trocou) arrasto.zerar() else arrasto.soltar() }
                },
                onHorizontalDrag = { change, dx ->
                    change.consume()
                    acumulado += dx
                    escopo.launch(start = CoroutineStart.UNDISPATCHED) { arrasto.definir(acumulado) }
                },
            )
        }
}

@Composable
fun FitNavHost() {
    val nav = rememberNavController()
    val atual by nav.currentBackStackEntryAsState()
    val rotaAtual = atual?.destination?.route

    fun irParaAba(d: Destino) {
        nav.navigate(d.rota) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // A barra flutua POR CIMA do conteúdo (que rola por baixo dela); o vidro desfoca o que passa atrás.
    val fundoBarra = br.com.fitpersonal.app.ui.common.rememberFundoDesfocavel()
    Scaffold { padding ->
      Box(Modifier.padding(padding).fillMaxSize()) {
        Box(Modifier.fillMaxSize().fonteDeFundo(fundoBarra)) {
        NavHost(
            nav,
            startDestination = Destino.INICIO.rota,
            modifier = Modifier.fillMaxSize().efeitoElastico().gestoEntreAbas(
                habilitado = rotaAtual in ROTAS_ABAS,
                chave = rotaAtual,
                onProxima = { indiceAba(rotaAtual).let { i -> if (i in 0 until Destino.entries.size - 1) irParaAba(Destino.entries[i + 1]) } },
                onAnterior = { indiceAba(rotaAtual).let { i -> if (i > 0) irParaAba(Destino.entries[i - 1]) } },
            ),
            // Leve: abas só trocam com fade curto (sem deslocar duas telas pesadas ao mesmo tempo);
            // detalhes entram com um deslize pequeno e a tela de baixo some rápido.
            enterTransition = {
                val de = indiceAba(initialState.destination.route)
                val para = indiceAba(targetState.destination.route)
                if (de >= 0 && para >= 0) {
                    androidx.compose.animation.EnterTransition.None
                } else {
                    slideInHorizontally(tween(DURACAO_NAV, easing = FastOutSlowInEasing)) { w -> w / 10 } +
                        fadeIn(tween(160, delayMillis = 30, easing = LinearOutSlowInEasing))
                }
            },
            exitTransition = {
                val de = indiceAba(initialState.destination.route)
                val para = indiceAba(targetState.destination.route)
                if (de >= 0 && para >= 0) androidx.compose.animation.ExitTransition.None
                else fadeOut(tween(90, easing = LinearEasing))
            },
            popEnterTransition = { fadeIn(tween(160, delayMillis = 30, easing = LinearOutSlowInEasing)) },
            popExitTransition = {
                slideOutHorizontally(tween(180, easing = FastOutLinearInEasing)) { w -> w / 10 } +
                    fadeOut(tween(130, easing = LinearEasing))
            },
        ) {
            composable(Destino.INICIO.rota) {
                HomeScreen(
                    onPerfil = { nav.navigate("perfil") },
                    onNovoAluno = { nav.navigate("aluno/0") },
                    onNovoAgendamento = { nav.navigate("agendamento/0") },
                    onNovaAvaliacao = { irParaAba(Destino.AVALIACOES) },
                    onVerAgenda = { irParaAba(Destino.AGENDA) },
                    onVerAlunos = { irParaAba(Destino.ALUNOS) },
                    onAgendarAluno = { id -> nav.navigate("agendamento/0?aluno=$id") },
                    onAbrirAluno = { id -> nav.navigate("ficha/$id") },
                    onBuscar = { nav.navigate("busca") },
                )
            }
            composable("busca") {
                BuscaScreen(
                    onVoltar = { nav.popBackStack() },
                    onAbrirAluno = { id -> nav.navigate("ficha/$id") },
                    onAbrirTreino = { id -> nav.navigate("treino/$id") },
                    onAbrirAvaliacao = { id -> nav.navigate("avaliacao/$id") },
                    onAbrirAgendamento = { id -> nav.navigate("agendamento/$id") },
                )
            }
            composable("perfil") { PerfilScreen(onVoltar = { nav.popBackStack() }) }
            composable(Destino.ALUNOS.rota) {
                AlunosScreen(
                    onNovo = { nav.navigate("aluno/0") },
                    onAbrir = { id -> nav.navigate("ficha/$id") },
                )
            }
            composable(
                "ficha/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                FichaScreen(
                    onVoltar = { nav.popBackStack() },
                    onEditar = { id -> nav.navigate("aluno/$id") },
                    onEvolucao = { id -> nav.navigate("evolucao/$id") },
                    onNovaAvaliacao = { id -> nav.navigate("avaliacao/0?aluno=$id") },
                    onAbrirAvaliacao = { id -> nav.navigate("avaliacao/$id") },
                    onNovoTreino = { id -> nav.navigate("treino/0?aluno=$id") },
                    onAbrirTreino = { id -> nav.navigate("treino/$id") },
                    onAgendar = { id -> nav.navigate("agendamento/0?aluno=$id") },
                    onAbrirAgendamento = { id -> nav.navigate("agendamento/$id") },
                    onProgressao = { id -> nav.navigate("progressao/$id") },
                    onTestes = { id -> nav.navigate("testes/$id") },
                    onFotos = { id -> nav.navigate("fotos/$id") },
                )
            }
            composable(
                "progressao/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { ProgressaoScreen(onVoltar = { nav.popBackStack() }) }
            composable(
                "aluno/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { AlunoFormScreen(onVoltar = { nav.popBackStack() }) }
            composable(
                "evolucao/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { EvolucaoScreen(onVoltar = { nav.popBackStack() }) }
            composable(Destino.AGENDA.rota) {
                AgendaScreen(
                    onNovo = { dia -> nav.navigate("agendamento/0?data=${dia.toEpochDay()}") },
                    onAbrir = { id -> nav.navigate("agendamento/$id") },
                    onEspera = { nav.navigate("espera") },
                )
            }
            composable("espera") {
                ListaEsperaScreen(
                    onVoltar = { nav.popBackStack() },
                    onAgendar = { id -> nav.navigate("agendamento/0?aluno=$id") },
                )
            }
            composable(
                "testes/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { TestesAptidaoScreen(onVoltar = { nav.popBackStack() }) }
            composable(
                "fotos/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { LinhaDoTempoFotosScreen(onVoltar = { nav.popBackStack() }) }
            composable(
                "agendamento/{id}?data={data}&aluno={aluno}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("data") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("aluno") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { AgendamentoFormScreen(onVoltar = { nav.popBackStack() }) }
            composable(Destino.TREINOS.rota) {
                TreinosScreen(
                    onNovo = { idAluno -> nav.navigate("treino/0?aluno=$idAluno") },
                    onAbrir = { id -> nav.navigate("treino/$id") },
                )
            }
            composable(
                "treino/{id}?aluno={aluno}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("aluno") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) {
                TreinoEditorScreen(
                    onVoltar = { nav.popBackStack() },
                    onExecutar = { id -> nav.navigate("treino/$id/executar") },
                )
            }
            composable(
                "treino/{id}/executar",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { TreinoExecucaoScreen(onVoltar = { nav.popBackStack() }) }
            composable(Destino.AVALIACOES.rota) {
                AvaliacoesScreen(
                    onNova = { idAluno -> nav.navigate("avaliacao/0?aluno=$idAluno") },
                    onAbrir = { id -> nav.navigate("avaliacao/$id") },
                )
            }
            composable(
                "avaliacao/{id}?aluno={aluno}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("aluno") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { AvaliacaoFormScreen(onVoltar = { nav.popBackStack() }) }
        }
        }
        // Feedback de "salvo / excluído": cartão no topo, sobrevive à navegação
        br.com.fitpersonal.app.ui.common.HostAvisos(Modifier.align(Alignment.TopCenter).fillMaxWidth())
        // Telas de formulário ocupam a tela inteira (sem barra inferior)
        if (Destino.entries.any { it.rota == rotaAtual }) {
            BarraInferior(
                rotaSelecionada = rotaAtual,
                onSelecionar = { irParaAba(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
                fundo = fundoBarra,
            )
        }
      }
    }
}
