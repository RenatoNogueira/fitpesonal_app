package br.com.fitpersonal.app.ui.home

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.local.AgendamentoComAluno
import br.com.fitpersonal.app.data.local.AlunoDao
import br.com.fitpersonal.app.data.local.AvaliacaoDao
import br.com.fitpersonal.app.data.local.TreinoDao
import br.com.fitpersonal.app.data.local.toDomain
import br.com.fitpersonal.app.data.remote.ResultadoClima
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.ClimaRepository
import br.com.fitpersonal.app.data.repo.ConfiguracoesRepository
import br.com.fitpersonal.app.data.repo.PagamentoRepository
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.NegocioRules
import br.com.fitpersonal.app.domain.rules.PacoteRules
import br.com.fitpersonal.app.domain.rules.ResumoNegocio
import br.com.fitpersonal.app.domain.rules.ResumoPacote
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.domain.rules.SituacaoPagamento
import br.com.fitpersonal.app.ui.common.WhatsApp
import br.com.fitpersonal.app.data.repo.PerfilRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.ClimaRules
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.AniversarioRules
import br.com.fitpersonal.app.domain.rules.ReavaliacaoRules
import br.com.fitpersonal.app.domain.rules.Saudacao
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import br.com.fitpersonal.app.ui.ficha.textoReavaliacao
import br.com.fitpersonal.app.domain.rules.SituacaoAniversario
import br.com.fitpersonal.app.domain.rules.Validators
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.BlocoHorario
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.IndicadorKpi
import br.com.fitpersonal.app.ui.common.TituloSecao
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.common.tomDoStatus
import br.com.fitpersonal.app.ui.theme.Paleta
import br.com.fitpersonal.app.ui.theme.ehEscuro
import br.com.fitpersonal.app.ui.theme.gradienteFesta
import br.com.fitpersonal.app.ui.theme.gradienteMarca
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import br.com.fitpersonal.app.ui.theme.corSobre

data class HomeState(
    val totalAlunos: Int = 0,
    val agendamentosHoje: Int = 0,
    val avaliacoesNoMes: Int = 0,
    val totalTreinos: Int = 0,
    val proximos: List<AgendamentoComAluno> = emptyList(),
    /** Alunos que fazem aniversário no mês corrente, em ordem de dia. */
    val aniversariantes: List<Aluno> = emptyList(),
)

/** Estado do cartão "Primeiros passos" (aparece até tudo ser feito ou o usuário dispensar). */
data class PrimeirosPassos(val perfil: Boolean, val aluno: Boolean, val avaliacao: Boolean)

/** Aluno com pacote na última sessão ou esgotado. */
data class PacoteAlerta(val aluno: Aluno, val resumo: ResumoPacote)

/** Aluno com mensalidade vencida e sem pagamento. */
data class Inadimplente(
    val aluno: Aluno,
    val atrasadas: List<java.time.YearMonth>,
    val valorEmAbertoCentavos: Long,
)

/** Aluno com reavaliação vencendo, atrasada ou nunca feita. */
data class ReavaliacaoPendente(val aluno: Aluno, val situacao: SituacaoReavaliacao)

/** Espelha DashboardView.refresh_data do desktop, reativo via Flow (sem timer de 60 s). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    alunos: AlunoDao, avaliacoes: AvaliacaoDao, treinos: TreinoDao, agenda: AgendaRepository,
    private val fotos: FotoStorage,
    private val climaRepo: ClimaRepository,
    private val perfilRepo: PerfilRepository,
    private val configRepo: ConfiguracoesRepository,
    pagamentos: PagamentoRepository,
) : ViewModel() {

    val hoje: LocalDate = LocalDate.now()

    private val concluidos = agenda.concluidos()

    /** Pacotes esgotados primeiro, depois os que estão na última sessão. */
    val pacotes: StateFlow<List<PacoteAlerta>> = combine(alunos.observar(""), concluidos) { lista, feitos ->
        val porAluno = feitos.groupBy { it.idAluno }
        lista.map { it.toDomain() }.mapNotNull { a ->
            val r = PacoteRules.resumir(a.pacoteSessoes, a.pacoteInicio, porAluno[a.id].orEmpty())
            if (r.precisaAtencao) PacoteAlerta(a, r) else null
        }.sortedBy { it.resumo.saldo }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).estadoSeguro(viewModelScope, emptyList())

    /** Sessões e receita do mês contra a meta e o mês anterior. */
    val negocio: StateFlow<ResumoNegocio> = combine(
        concluidos, pagamentos.todos(), configRepo.config,
    ) { feitos, pagos, cfg ->
        NegocioRules.resumir(
            sessoesConcluidas = feitos.map { it.data },
            recebimentos = pagos.map { it.competencia to it.valorCentavos },
            hoje = LocalDate.now(),
            metaSessoes = cfg.metaSessoesMes,
            metaReceitaCentavos = cfg.metaReceitaMesCentavos,
        )
    }.estadoSeguro(
        viewModelScope,
        ResumoNegocio(0, 0, 0L, 0L, null, null),
    )

    fun salvarMetasNegocio(sessoes: Int, receitaCentavos: Long) {
        viewModelScope.launch(TratadorDeErros) {
            configRepo.definirMetasNegocio(sessoes, receitaCentavos)
            br.com.fitpersonal.app.core.Avisos.sucesso("Metas do mês salvas")
        }
    }

    /** Mensalidades vencidas e não pagas (últimos 3 meses), maior valor em aberto primeiro. */
    val inadimplentes: StateFlow<List<Inadimplente>> = combine(
        alunos.observar(""),
        pagamentos.todos(),
    ) { lista, pagos ->
        val hojeData = LocalDate.now()
        val porAluno = pagos.groupBy { it.idAluno }
        lista.map { it.toDomain() }.mapNotNull { a ->
            val pagas = porAluno[a.id].orEmpty().map { it.competencia }.toSet()
            val r = FinanceiroRules.resumir(a.mensalidadeCentavos, a.diaVencimento, a.dataCadastro, pagas, hojeData)
            val valor = a.mensalidadeCentavos
            if (r.situacao == SituacaoPagamento.ATRASADO && valor != null) {
                Inadimplente(a, r.atrasadas, valor * r.atrasadas.size)
            } else null
        }.sortedByDescending { it.valorEmAbertoCentavos }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).estadoSeguro(viewModelScope, emptyList())

    /** null = nada a mostrar (carregando, dispensado ou concluído). */
    val primeirosPassos: StateFlow<PrimeirosPassos?> = combine(
        perfilRepo.perfil.map { it.nome.isNotBlank() },
        alunos.total(),
        avaliacoes.total(),
        configRepo.config.map { it.onboardingConcluido },
    ) { perfil, totalAlunos, totalAvaliacoes, dispensado ->
        val p = PrimeirosPassos(perfil, totalAlunos > 0, totalAvaliacoes > 0)
        if (dispensado || (p.perfil && p.aluno && p.avaliacao)) null else p
    }.estadoSeguro<PrimeirosPassos?>(viewModelScope, null)

    /** Atrasadas primeiro (mais atrasadas antes), depois as que vencem logo; nunca avaliadas por último. */
    val reavaliacoes: StateFlow<List<ReavaliacaoPendente>> = combine(
        alunos.observar(""),
        avaliacoes.ultimasPorAluno(),
    ) { lista, ultimas ->
        val porAluno = ultimas.associate { it.idAluno to LocalDate.parse(it.ultima) }
        lista.map { it.toDomain() }
            .mapNotNull { a ->
                val s = ReavaliacaoRules.situacao(porAluno[a.id], a.reavaliacaoDias, LocalDate.now())
                if (ReavaliacaoRules.pedeAtencao(s)) ReavaliacaoPendente(a, s!!) else null
            }
            .sortedBy { p ->
                when (val s = p.situacao) {
                    is SituacaoReavaliacao.Atrasada -> -s.diasAtraso
                    is SituacaoReavaliacao.Proxima -> s.diasRestantes
                    is SituacaoReavaliacao.EmDia -> s.diasRestantes
                    SituacaoReavaliacao.SemAvaliacao -> Int.MAX_VALUE
                }
            }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).estadoSeguro(viewModelScope, emptyList())

    fun dispensarPrimeirosPassos() {
        viewModelScope.launch(TratadorDeErros) { configRepo.concluirOnboarding() }
    }

    private val aniversariantes = alunos.aniversariantes("%02d".format(hoje.monthValue))
        .map { lista -> lista.map { it.toDomain() } }

    val state: StateFlow<HomeState> = combine(
        alunos.total(),
        avaliacoes.totalNoMes(hoje.toString().substring(0, 7)),
        treinos.total(),
        agenda.proximos(hoje),
        aniversariantes,
    ) { a, av, t, prox, aniv ->
        HomeState(a, prox.count { it.data == hoje.toString() }, av, t, prox, aniv)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).estadoSeguro(viewModelScope, HomeState())

    /** Primeiro nome do profissional (do perfil) para a saudação; vazio se ainda não informado. */
    val nomeProfissional: StateFlow<String> = perfilRepo.perfil
        .map { Saudacao.primeiroNome(it.nome) }
        .estadoSeguro(viewModelScope, "")

    /** Perfil completo (nome, cidade e chave Pix) para montar o Pix das cobranças. */
    val perfilCompleto: StateFlow<br.com.fitpersonal.app.domain.model.PerfilProfissional> = perfilRepo.perfil
        .estadoSeguro(viewModelScope, br.com.fitpersonal.app.domain.model.PerfilProfissional())

    fun arquivoFoto(rel: String?): File? = rel?.let(fotos::arquivo)

    // ── clima (cidade vem do perfil do profissional; null = carregando) ──
    /** false quando o app foi compilado sem OPENWEATHER_API_KEY: o cartão nem é composto (sem "piscar"). */
    val climaDisponivel: Boolean = climaRepo.disponivel

    private val _clima = MutableStateFlow<ResultadoClima?>(null)
    val clima: StateFlow<ResultadoClima?> = _clima

    init {
        // Recarrega sempre que a cidade do perfil muda; o cache do repositório evita consultas repetidas.
        if (climaDisponivel) viewModelScope.launch(TratadorDeErros) {
            perfilRepo.perfil.map { it.cidade.trim() }.distinctUntilChanged().collectLatest { cidade ->
                _clima.value = null
                _clima.value = climaRepo.atual(cidade)
            }
        }
    }

    fun atualizarClima() {
        viewModelScope.launch(TratadorDeErros) {
            val cidade = perfilRepo.perfil.first().cidade
            _clima.value = null
            _clima.value = climaRepo.atual(cidade, forcar = true)
        }
    }
}

@Composable
private fun PassoInicial(texto: String, feito: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (feito) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (feito) "Feito" else "Pendente",
            tint = if (feito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Text(
            texto,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (feito) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        if (!feito) TextButton(onClick = onClick) { Text("Fazer") }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPerfil: () -> Unit = {},
    onNovoAluno: () -> Unit = {},
    onNovoAgendamento: () -> Unit = {},
    onNovaAvaliacao: () -> Unit = {},
    onVerAgenda: () -> Unit = {},
    onVerAlunos: () -> Unit = {},
    onAgendarAluno: (Long) -> Unit = {},
    onAbrirAluno: (Long) -> Unit = {},
    onBuscar: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val reavaliacoes by vm.reavaliacoes.collectAsStateWithLifecycle()
    val inadimplentes by vm.inadimplentes.collectAsStateWithLifecycle()
    val pacotes by vm.pacotes.collectAsStateWithLifecycle()
    val negocio by vm.negocio.collectAsStateWithLifecycle()
    var editandoMetas by remember { mutableStateOf(false) }
    var verMetas by remember { mutableStateOf(false) }
    var avisoCobranca by remember { mutableStateOf<String?>(null) }
    val s by vm.state.collectAsStateWithLifecycle()
    val clima by vm.clima.collectAsStateWithLifecycle()
    val nome by vm.nomeProfissional.collectAsStateWithLifecycle()
    val perfilCompleto by vm.perfilCompleto.collectAsStateWithLifecycle()
    val primeiros by vm.primeirosPassos.collectAsStateWithLifecycle()
    val hoje = vm.hoje
    val ctx = LocalContext.current
    val mes = hoje.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
    val saudacao = remember { Saudacao.porHora(LocalTime.now().hour) }

    var sheet by remember { mutableStateOf<String?>(null) }
    val esquema = MaterialTheme.colorScheme
    val escuro = ehEscuro()

    val comData = s.aniversariantes.mapNotNull { a -> a.dataNascimento?.let { a to it } }
    val ordenados = AniversarioRules.ordenar(comData, hoje) { it.second }
    val deHoje = ordenados.filter { AniversarioRules.diasAte(it.second, hoje) == 0 }
    val demais = ordenados - deHoje.toSet()

    fun avisarWhats(aluno: Aluno, msg: String) {
        if (!WhatsApp.enviar(ctx, aluno.telefone, msg)) {
            avisoCobranca = "${aluno.nome}: sem telefone válido ou WhatsApp indisponível."
        }
    }

    // Linhas reutilizadas no carrossel (2 primeiras) e na folha "Ver todos" (lista completa).
    @Composable
    fun linhasPacotes(sobre: Color, max: Int) {
        pacotes.take(max).forEach { p ->
            LinhaResumo(
                nome = p.aluno.nome,
                detalhe = if (p.resumo.saldo <= 0) "Pacote esgotado" else "Última sessão do pacote",
                foto = vm.arquivoFoto(p.aluno.fotoPerfil),
                acao = "Avisar",
                sobre = sobre,
                onClick = { onAbrirAluno(p.aluno.id) },
                onAcao = { avisarWhats(p.aluno, MensagemRules.pacoteAcabando(p.aluno.nome, p.resumo.saldo, nome)) },
            )
        }
    }

    @Composable
    fun linhasInadimplentes(sobre: Color, max: Int) {
        inadimplentes.take(max).forEach { i ->
            LinhaResumo(
                nome = i.aluno.nome,
                detalhe = "${i.atrasadas.size} mês(es) • " + FinanceiroRules.formatarReais(i.valorEmAbertoCentavos),
                foto = vm.arquivoFoto(i.aluno.fotoPerfil),
                acao = "Cobrar",
                sobre = sobre,
                onClick = { onAbrirAluno(i.aluno.id) },
                onAcao = {
                    avisarWhats(
                        i.aluno,
                        MensagemRules.cobranca(
                            i.aluno.nome, i.atrasadas.first(), i.aluno.mensalidadeCentavos ?: 0L, nome,
                            br.com.fitpersonal.app.domain.rules.PixRules.cobranca(
                                perfilCompleto.chavePix, i.aluno.mensalidadeCentavos ?: 0L,
                                perfilCompleto.nome, perfilCompleto.cidade,
                            ),
                        ),
                    )
                },
            )
        }
    }

    @Composable
    fun linhasReavaliacoes(sobre: Color, max: Int) {
        reavaliacoes.take(max).forEach { r ->
            LinhaResumo(
                nome = r.aluno.nome,
                detalhe = textoReavaliacao(r.situacao),
                foto = vm.arquivoFoto(r.aluno.fotoPerfil),
                acao = "Agendar",
                sobre = sobre,
                onClick = { onAbrirAluno(r.aluno.id) },
                onAcao = { onAgendarAluno(r.aluno.id) },
            )
        }
    }

    val paginas = buildList {
        if (deHoje.isNotEmpty()) {
            add(
                PaginaResumo(
                    chave = "aniversario", icone = Icons.Filled.Cake,
                    titulo = if (deHoje.size == 1) "Aniversário hoje!" else "${deHoje.size} aniversários hoje!",
                    fundo = br.com.fitpersonal.app.ui.common.PaletaPasta.Rosa,
                ) { sobre ->
                    deHoje.take(2).forEach { (aluno, _) ->
                        LinhaResumo(
                            nome = aluno.nome, detalhe = "Mande um parabéns!",
                            foto = vm.arquivoFoto(aluno.fotoPerfil), acao = "Parabéns", sobre = sobre,
                            onClick = { onAbrirAluno(aluno.id) }, onAcao = { parabenizar(ctx, aluno) },
                        )
                    }
                },
            )
        }
        primeiros?.let { p ->
            add(
                PaginaResumo("primeiros", Icons.Filled.RocketLaunch, "Primeiros passos", br.com.fitpersonal.app.ui.common.PaletaPasta.Lima) { sobre ->
                    PassoCompacto("Preencha seu perfil profissional", p.perfil, sobre, onPerfil)
                    PassoCompacto("Cadastre o primeiro aluno", p.aluno, sobre, onNovoAluno)
                    PassoCompacto("Faça a primeira avaliação", p.avaliacao, sobre, onNovaAvaliacao)
                    TextButton(onClick = vm::dispensarPrimeirosPassos) { Text("Dispensar", color = sobre) }
                },
            )
        }
        add(
            PaginaResumo("metas", Icons.Filled.TrackChanges, "Metas do mês", br.com.fitpersonal.app.ui.common.PaletaPasta.Verde) { sobre ->
                val meta = negocio.metaSessoes
                Text(
                    if (meta != null) "${negocio.sessoes} de $meta sessões" else "${negocio.sessoes} sessões concluídas",
                    style = MaterialTheme.typography.titleSmall, color = sobre,
                )
                (negocio.fracaoSessoes ?: negocio.fracaoReceita)?.let {
                    LinearProgressIndicator(
                        progress = { it.toFloat() }, modifier = Modifier.fillMaxWidth(),
                        color = sobre, trackColor = sobre.copy(alpha = 0.25f),
                    )
                }
                Text(
                    "Receita: " + FinanceiroRules.formatarReais(negocio.receitaCentavos) +
                        (negocio.metaReceitaCentavos?.let { " de " + FinanceiroRules.formatarReais(it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = sobre,
                )
                TextButton(onClick = { verMetas = true }) { Text("Detalhes e metas", color = sobre) }
            },
        )
        if (inadimplentes.isNotEmpty()) {
            add(
                PaginaResumo(
                    "inadimplentes", Icons.Filled.Payments,
                    "Mensalidades em atraso (${inadimplentes.size})", br.com.fitpersonal.app.ui.common.PaletaPasta.Coral,
                ) { sobre ->
                    Text(
                        "Total em aberto: " + FinanceiroRules.formatarReais(inadimplentes.sumOf { it.valorEmAbertoCentavos }),
                        style = MaterialTheme.typography.bodySmall, color = sobre,
                    )
                    linhasInadimplentes(sobre, 1)
                    if (inadimplentes.size > 1) VerTodos(inadimplentes.size, sobre) { sheet = "inadimplentes" }
                },
            )
        }
        if (reavaliacoes.isNotEmpty()) {
            add(
                PaginaResumo(
                    "reavaliacoes", Icons.Filled.Autorenew,
                    "Reavaliações (${reavaliacoes.size})", br.com.fitpersonal.app.ui.common.PaletaPasta.Teal,
                ) { sobre ->
                    linhasReavaliacoes(sobre, 2)
                    if (reavaliacoes.size > 2) VerTodos(reavaliacoes.size, sobre) { sheet = "reavaliacoes" }
                },
            )
        }
        if (pacotes.isNotEmpty()) {
            add(
                PaginaResumo(
                    "pacotes", Icons.Filled.Inventory2,
                    "Pacotes acabando (${pacotes.size})", br.com.fitpersonal.app.ui.common.PaletaPasta.Lima,
                ) { sobre ->
                    linhasPacotes(sobre, 2)
                    if (pacotes.size > 2) VerTodos(pacotes.size, sobre) { sheet = "pacotes" }
                },
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Cabecalho(
                titulo = if (nome.isNotBlank()) "$saudacao, $nome" else saudacao,
                subtitulo = Saudacao.dataExtensa(hoje),
                onPerfil = onPerfil,
                onBuscar = onBuscar,
            )
        }

        // Sem chave compilada o recurso não existe: o cartão nem aparece.
        if (vm.climaDisponivel) {
            item { CartaoClima(clima, onAtualizar = vm::atualizarClima, onPerfil = onPerfil) }
        }

        // ── números do dia: cartões "pasta" coloridos ──
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    br.com.fitpersonal.app.ui.common.KpiPasta(
                        Icons.Filled.People, s.totalAlunos.toString(), "Alunos",
                        br.com.fitpersonal.app.ui.common.PaletaPasta.Lima, Modifier.weight(1f),
                    )
                    br.com.fitpersonal.app.ui.common.KpiPasta(
                        Icons.Filled.CalendarMonth, s.agendamentosHoje.toString(), "Hoje",
                        br.com.fitpersonal.app.ui.common.PaletaPasta.Verde, Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    br.com.fitpersonal.app.ui.common.KpiPasta(
                        Icons.Filled.Favorite, s.avaliacoesNoMes.toString(), "Avaliações no mês",
                        br.com.fitpersonal.app.ui.common.PaletaPasta.Coral, Modifier.weight(1f),
                    )
                    br.com.fitpersonal.app.ui.common.KpiPasta(
                        Icons.Filled.FitnessCenter, s.totalTreinos.toString(), "Treinos",
                        br.com.fitpersonal.app.ui.common.PaletaPasta.Teal, Modifier.weight(1f),
                    )
                }
            }
        }

        // ── carrossel vertical: um cartão por assunto, rola de baixo para cima ──
        item(key = "carrossel") {
            CarrosselResumo(paginas, Modifier.animateItem())
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AcaoRapida(
                    Icons.Filled.PersonAdd, "Novo aluno", onNovoAluno,
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.weight(1f),
                )
                AcaoRapida(
                    Icons.Filled.Event, "Agendar", onNovoAgendamento,
                    MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f),
                )
                AcaoRapida(
                    Icons.Filled.Assessment, "Avaliar", onNovaAvaliacao,
                    MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, Modifier.weight(1f),
                )
            }
        }

        // ── próximos agendamentos (pilha de pastas) ──
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Próximos agendamentos", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onVerAgenda) { Text("Ver agenda") }
            }
        }
        item {
            PilhaAgenda(
                itens = s.proximos,
                hoje = hoje,
                onAbrir = onVerAgenda,
                onCriar = onNovoAgendamento,
            )
        }

        // ── próximos aniversários (só aparece se houver; os de hoje ficam no carrossel) ──
        if (demais.isNotEmpty()) {
            item {
                TituloSecao("Aniversariantes de $mes", detalhe = demais.size.toString())
            }
            item(key = "an_lista") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.animateItem()) {
                    itemsIndexed(demais, key = { _, par -> "an${par.first.id}" }) { indice, (aluno, nasc) ->
                        CartaoAniversarioPasta(
                            aluno = aluno,
                            nascimento = nasc,
                            hoje = hoje,
                            foto = vm.arquivoFoto(aluno.fotoPerfil),
                            cor = corDoAniversario(nasc, hoje),
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        avisoCobranca?.let { aviso ->
            item { Text(aviso, style = MaterialTheme.typography.bodySmall, color = esquema.error) }
        }
    }

    // Diálogos das metas
    if (verMetas) {
        DetalheMetasNegocio(
            r = negocio,
            onFechar = { verMetas = false },
            onEditar = { verMetas = false; editandoMetas = true },
        )
    }
    if (editandoMetas) {
        DialogoMetasNegocio(
            r = negocio,
            onCancelar = { editandoMetas = false },
            onSalvar = { sess, c -> editandoMetas = false; vm.salvarMetasNegocio(sess, c) },
        )
    }

    // Folha "Ver todos" de cada assunto do carrossel
    sheet?.let { chave ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val sobre = esquema.onSurface
                when (chave) {
                    "inadimplentes" -> {
                        Text("Mensalidades em atraso", style = MaterialTheme.typography.titleLarge)
                        linhasInadimplentes(sobre, Int.MAX_VALUE)
                    }
                    "reavaliacoes" -> {
                        Text("Reavaliações", style = MaterialTheme.typography.titleLarge)
                        linhasReavaliacoes(sobre, Int.MAX_VALUE)
                    }
                    "pacotes" -> {
                        Text("Pacotes acabando", style = MaterialTheme.typography.titleLarge)
                        linhasPacotes(sobre, Int.MAX_VALUE)
                    }
                }
            }
        }
    }
}

// ───────────────────────── carrossel de resumo ─────────────────────────

/** Uma página do carrossel: ícone, título, cor de fundo e o conteúdo (recebe a cor de texto com bom contraste). */
private class PaginaResumo(
    val chave: String,
    val icone: androidx.compose.ui.graphics.vector.ImageVector,
    val titulo: String,
    val fundo: Color,
    val conteudo: @Composable ColumnScope.(Color) -> Unit,
)

/**
 * Resumo em um cartão por vez, trocando com um fade calmo (sem deslocamento). Passa sozinho a cada 8 s;
 * deslizar o dedo na vertical ou tocar nos pontinhos troca de cartão e reinicia a contagem.
 */
@Composable
private fun CarrosselResumo(paginas: List<PaginaResumo>, modifier: Modifier = Modifier) {
    if (paginas.isEmpty()) return
    var indice by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val atual = indice.coerceIn(0, paginas.lastIndex)

    // Reinicia a contagem a cada troca (manual ou automática).
    LaunchedEffect(atual, paginas.size) {
        if (paginas.size > 1) {
            kotlinx.coroutines.delay(8000)
            indice = (atual + 1) % paginas.size
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(262.dp)
            .pointerInput(paginas.size) {
                var acumulado = 0f
                detectVerticalDragGestures(
                    onDragStart = { acumulado = 0f },
                    onDragCancel = { acumulado = 0f },
                    onDragEnd = {
                        val corrente = indice.coerceIn(0, paginas.lastIndex)
                        when {
                            acumulado <= -48f -> indice = (corrente + 1) % paginas.size
                            acumulado >= 48f -> indice = (corrente - 1 + paginas.size) % paginas.size
                        }
                        acumulado = 0f
                    },
                ) { change, dy ->
                    change.consume()
                    acumulado += dy
                }
            },
    ) {
        // camadas atrás do cartão (pastas empilhadas): mostram as cores das próximas páginas
        for (k in 2 downTo 1) {
            if (paginas.size > k) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (k == 2) 0.dp else 12.dp)
                        .fillMaxWidth()
                        .padding(horizontal = (k * 11).dp)
                        .height(200.dp)
                        .clip(br.com.fitpersonal.app.ui.common.FolderShape())
                        .background(
                            br.com.fitpersonal.app.ui.common.PaletaPasta.degrade(paginas[(atual + k) % paginas.size].fundo),
                            alpha = 0.9f,
                        ),
                )
            }
        }
        androidx.compose.animation.Crossfade(
            targetState = atual,
            animationSpec = androidx.compose.animation.core.tween(500),
            label = "carrossel",
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(238.dp),
        ) { i ->
            val p = paginas.getOrNull(i) ?: return@Crossfade
            val sobre = br.com.fitpersonal.app.ui.common.PaletaPasta.tinta(p.fundo)
            Card(
                modifier = Modifier.fillMaxSize().background(
                    br.com.fitpersonal.app.ui.common.PaletaPasta.degrade(p.fundo),
                    br.com.fitpersonal.app.ui.common.FolderShape(),
                ),
                shape = br.com.fitpersonal.app.ui.common.FolderShape(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(start = 16.dp, end = 30.dp, top = 32.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = CircleShape, color = sobre.copy(alpha = 0.15f), modifier = Modifier.size(34.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(p.icone, contentDescription = null, tint = sobre, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            p.titulo, style = MaterialTheme.typography.titleMedium, color = sobre, maxLines = 1,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    p.conteudo(this, sobre)
                }
            }
        }

        // indicador de página, à direita (cada ponto também é um botão)
        val corPonto = br.com.fitpersonal.app.ui.common.PaletaPasta.tinta(paginas[atual].fundo)
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(paginas.size) { k ->
                val ativo = k == atual
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { indice = k },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(if (ativo) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(corPonto.copy(alpha = if (ativo) 0.95f else 0.35f)),
                    )
                }
            }
        }
    }
}

/** Linha compacta (avatar, nome, detalhe e uma ação) usada dentro dos cartões do carrossel e da folha. */
@Composable
private fun LinhaResumo(
    nome: String,
    detalhe: String,
    foto: File?,
    acao: String,
    sobre: Color,
    onClick: () -> Unit,
    onAcao: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AvatarAluno(nome, foto, tamanho = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.titleSmall, color = sobre, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                detalhe, style = MaterialTheme.typography.bodySmall,
                color = sobre.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onAcao) { Text(acao, color = sobre) }
    }
}

@Composable
private fun VerTodos(total: Int, sobre: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Ver todos ($total)", color = sobre) }
}

@Composable
private fun PassoCompacto(texto: String, feito: Boolean, sobre: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            if (feito) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null, tint = sobre, modifier = Modifier.size(20.dp),
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = sobre)
    }
}

@Composable
private fun MiniKpi(icone: androidx.compose.ui.graphics.vector.ImageVector, valor: Int, rotulo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(valor.toString(), style = MaterialTheme.typography.titleLarge)
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// ───────────────────────── componentes da tela ─────────────────────────

@Composable
private fun Cabecalho(titulo: String, subtitulo: String, onPerfil: () -> Unit, onBuscar: () -> Unit) {
    br.com.fitpersonal.app.ui.common.CabecalhoTela(
        titulo = titulo,
        subtitulo = subtitulo,
        cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Lima,
        acoes = {
            br.com.fitpersonal.app.ui.common.AcaoCabecalho(Icons.Filled.Search, "Buscar", onBuscar)
            Spacer(Modifier.width(8.dp))
            br.com.fitpersonal.app.ui.common.AcaoCabecalho(Icons.Filled.Settings, "Perfil e backup", onPerfil)
        },
    )
}

@Composable
private fun AcaoRapida(
    icone: ImageVector,
    rotulo: String,
    onClick: () -> Unit,
    fundo: Color,
    sobre: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = fundo, contentColor = sobre),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(sobre.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = sobre)
            }
            Text(rotulo, style = MaterialTheme.typography.labelLarge, color = sobre)
        }
    }
}

@Composable
private fun CartaoClima(estado: ResultadoClima?, onAtualizar: () -> Unit, onPerfil: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    when (estado) {
        is ResultadoClima.Ok -> {
            val c = estado.clima
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(cs.primary, cs.secondary)))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(ClimaRules.emoji(c.icone), style = MaterialTheme.typography.displaySmall)
                    Column(Modifier.weight(1f)) {
                        Text(
                            ClimaRules.grau(c.temperaturaC),
                            style = MaterialTheme.typography.headlineLarge,
                            color = cs.onPrimary,
                        )
                        Text(c.descricao, style = MaterialTheme.typography.titleSmall, color = cs.onPrimary)
                        val detalhes = listOfNotNull(
                            c.cidade.takeIf { it.isNotBlank() },
                            c.sensacaoC?.let { "sensação ${ClimaRules.grau(it)}" },
                            c.umidade?.let { "umidade $it%" },
                        ).joinToString(" • ")
                        if (detalhes.isNotEmpty()) {
                            Text(detalhes, style = MaterialTheme.typography.bodySmall, color = cs.onPrimary)
                        }
                    }
                    IconButton(onClick = onAtualizar) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Atualizar clima", tint = cs.onPrimary)
                    }
                }
            }
        }
        null -> CartaoSecao {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("Carregando clima…")
            }
        }
        ResultadoClima.SemCidade -> CartaoSecao {
            Text("Informe sua cidade no perfil para ver o clima.")
            TextButton(onClick = onPerfil) { Text("Abrir perfil") }
        }
        ResultadoClima.CidadeNaoEncontrada -> CartaoSecao {
            Text("Cidade não encontrada. Confira o nome no perfil.")
            TextButton(onClick = onPerfil) { Text("Abrir perfil") }
        }
        ResultadoClima.ChaveInvalida -> CartaoSecao {
            Text("Clima indisponível: a chave do serviço foi recusada.")
        }
        is ResultadoClima.Falha -> CartaoSecao {
            Text("Clima indisponível (${estado.motivo.lowercase()}).")
            TextButton(onClick = onAtualizar) { Text("Tentar novamente") }
        }
        ResultadoClima.SemChave -> Unit
    }
}

/** Aniversariantes do dia em destaque: fundo em degradê, emoji pulsando e botão de parabéns por aluno. */
@Composable
private fun DestaqueAniversarioHoje(
    lista: List<Pair<Aluno, LocalDate>>,
    hoje: LocalDate,
    foto: (Aluno) -> File?,
    onParabenizar: (Aluno) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pulso = rememberInfiniteTransition(label = "pulso")
    val escala by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )
    val degrade = gradienteFesta()
    val sobre = Color.White

    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(degrade)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎂", fontSize = 30.sp, modifier = Modifier.graphicsLayer { scaleX = escala; scaleY = escala })
                Column {
                    Text(
                        if (lista.size == 1) "Aniversário hoje!" else "${lista.size} aniversários hoje!",
                        style = MaterialTheme.typography.titleMedium,
                        color = sobre,
                    )
                    Text(
                        "Mande um parabéns e fortaleça a relação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = sobre.copy(alpha = 0.85f),
                    )
                }
            }
            lista.forEach { (aluno, nasc) ->
                val podeEnviar = Validators.somenteDigitos(aluno.telefone).length >= 10
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(sobre.copy(alpha = 0.14f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarAluno(aluno.nome, foto(aluno), tamanho = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            aluno.nome,
                            style = MaterialTheme.typography.titleSmall,
                            color = sobre,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            AniversarioRules.descricaoIdade(nasc, hoje),
                            style = MaterialTheme.typography.bodySmall,
                            color = sobre.copy(alpha = 0.85f),
                        )
                    }
                    if (podeEnviar) {
                        Button(
                            onClick = { onParabenizar(aluno) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = sobre,
                                contentColor = Paleta.Rosa600,
                            ),
                        ) { Text("Parabenizar") }
                    }
                }
            }
        }
    }
}

/** Próximos (e já passados) do mês: dia em destaque, idade, proximidade e visual esmaecido para os que passaram. */
@Composable
private fun CartaoAniversario(
    aluno: Aluno,
    nascimento: LocalDate,
    hoje: LocalDate,
    foto: File?,
    modifier: Modifier = Modifier,
) {
    val passou = AniversarioRules.situacao(nascimento, hoje) == SituacaoAniversario.JA_PASSOU
    val amanha = AniversarioRules.situacao(nascimento, hoje) == SituacaoAniversario.AMANHA
    val dia = AniversarioRules.aniversarioEm(nascimento, hoje.year).dayOfMonth

    Card(
        modifier = modifier.width(150.dp).alpha(if (passou) 0.65f else 1f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (amanha) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                AvatarAluno(aluno.nome, foto, tamanho = 60.dp)
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        dia.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(
                aluno.nome,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                AniversarioRules.descricaoIdade(nascimento, hoje),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChipStatus(
                AniversarioRules.rotuloProximidade(nascimento, hoje),
                when {
                    amanha -> TomChip.Atencao
                    passou -> TomChip.Neutro
                    else -> TomChip.Marca
                },
            )
        }
    }
}

/** Abre o WhatsApp com uma mensagem pronta (wa.me); sem app instalado, não faz nada. */
private fun parabenizar(ctx: Context, aluno: Aluno) {
    WhatsApp.enviar(
        ctx, aluno.telefone,
        "Parabéns, ${Saudacao.primeiroNome(aluno.nome)}! Feliz aniversário! Muita saúde e disposição!",
    )
}
