package br.com.fitpersonal.app.ui.ficha

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.rules.EvolucaoRules
import br.com.fitpersonal.app.domain.rules.FrequenciaRules
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.domain.rules.Metrica
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.ui.common.AcaoCartao
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.CompartilharPdfEffect
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.GrupoAnimado
import br.com.fitpersonal.app.ui.common.PilulaResumo
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.common.WhatsApp
import br.com.fitpersonal.app.ui.theme.ehEscuro
import br.com.fitpersonal.app.ui.theme.gradienteMarca
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val PT_BR = Locale("pt", "BR")

private val ABAS_FICHA = listOf("Resumo", "Histórico", "Financeiro")

private fun Double.fmt(casas: Int) = String.format(PT_BR, "%.${casas}f", this)

private fun soDigitos(s: String?) = s.orEmpty().filter { it.isDigit() }

private fun abrir(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // sem app que trate a ação: nada a fazer
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaScreen(
    onVoltar: () -> Unit,
    onEditar: (Long) -> Unit,
    onEvolucao: (Long) -> Unit,
    onNovaAvaliacao: (Long) -> Unit,
    onAbrirAvaliacao: (Long) -> Unit,
    onNovoTreino: (Long) -> Unit,
    onAbrirTreino: (Long) -> Unit,
    onAgendar: (Long) -> Unit,
    onAbrirAgendamento: (Long) -> Unit,
    onProgressao: (Long) -> Unit,
    onTestes: (Long) -> Unit,
    onFotos: (Long) -> Unit,
    vm: FichaViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val profissional by vm.profissional.collectAsStateWithLifecycle()
    val perfilCompleto by vm.perfilCompleto.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val aluno = s.aluno
    val snackbar = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()
    var aba by rememberSaveable { mutableStateOf(0) }
    fun avisar(texto: String) { escopo.launch { snackbar.showSnackbar(texto) } }

    val pdf by vm.pdf.collectAsStateWithLifecycle()
    val erro by vm.erro.collectAsStateWithLifecycle()
    val retornos by vm.retornos.collectAsStateWithLifecycle()
    val previa by vm.previa.collectAsStateWithLifecycle()
    CompartilharPdfEffect(pdf, vm::pdfConsumido)
    LaunchedEffect(erro) { erro?.let { avisar(it); vm.erroLido() } }

    // Aluno excluído (ex.: pelo formulário de edição): a ficha não tem mais o que mostrar.
    LaunchedEffect(s.carregado, aluno) { if (s.carregado && aluno == null) onVoltar() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ficha do aluno", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    if (aluno != null) IconButton(onClick = { onEditar(aluno.id) }) {
                        Icon(Icons.Filled.Edit, "Editar aluno")
                    }
                },
            )
        },
    ) { padding ->
        if (aluno == null) {
            if (s.carregado) EstadoVazio(
                icone = Icons.Filled.PersonOff,
                titulo = "Aluno não encontrado",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        val fone = soDigitos(aluno.telefone)
        val temFone = fone.length >= 10

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroAluno(
                aluno = aluno,
                foto = s.foto,
                temTelefone = temFone,
                onLigar = { abrir(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$fone"))) },
                onWhatsApp = {
                    val numero = if (fone.startsWith("55")) fone else "55$fone"
                    abrir(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero")))
                },
            )

            // Atalhos coloridos
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AcaoCartao(
                    Icons.Filled.Assessment, "Avaliar", { onNovaAvaliacao(aluno.id) },
                    MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, Modifier.weight(1f),
                )
                AcaoCartao(
                    Icons.Filled.FitnessCenter, "Treino", { onNovoTreino(aluno.id) },
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.weight(1f),
                )
                AcaoCartao(
                    Icons.Filled.Event, "Agendar", { onAgendar(aluno.id) },
                    MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, Modifier.weight(1f),
                )
            }

            TabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.background) {
                ABAS_FICHA.forEachIndexed { i, titulo ->
                    br.com.fitpersonal.app.ui.common.AbaCompacta(selecionada = aba == i, onClick = { aba = i }, titulo = titulo)
                }
            }

            // ── Resumo ──
            GrupoAnimado(aba == 0) {
                ResumoMedidas(s.avaliacoes)

                val frequencia = FrequenciaRules.resumir(s.agendamentos, LocalDate.now())
                if (frequencia.concluidos + frequencia.faltas > 0) {
                    CartaoSecao(titulo = "Frequência") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniMetrica("Sessões", frequencia.concluidos.toString(), Modifier.weight(1f))
                            MiniMetrica("Faltas", frequencia.faltas.toString(), Modifier.weight(1f))
                            MiniMetrica(
                                "Taxa de faltas",
                                frequencia.taxaFaltas?.let { String.format(PT_BR, "%.0f%%", it * 100) } ?: "—",
                                Modifier.weight(1f),
                            )
                        }
                        if (FrequenciaRules.pedeAtencao(frequencia)) {
                            ChipStatus(
                                if (frequencia.faltasSeguidas >= 2) "${frequencia.faltasSeguidas} faltas seguidas"
                                else "Taxa de faltas alta",
                                TomChip.Atencao,
                            )
                        }
                    }
                }

                CartaoPlano(aluno, s.avaliacoes, onSalvar = vm::salvarPlano)

                CartaoPacote(
                    aluno = aluno,
                    agendamentos = s.agendamentos,
                    profissional = profissional,
                    onSalvar = vm::salvarPacote,
                    onMensagem = ::avisar,
                )
            }

            // ── Histórico ──
            GrupoAnimado(aba == 1) {
                CartaoSecao(titulo = "Avaliações") {
                    if (s.avaliacoes.isEmpty()) {
                        Text("Nenhuma avaliação registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        s.avaliacoes.take(3).forEach { a ->
                            CartaoItem(
                                titulo = a.data.format(DATA_BR),
                                subtitulo = listOfNotNull(
                                    a.pesoKg?.let { "${it.fmt(1)} kg" },
                                    a.percentualGordura?.let { "${it.fmt(1)} % gordura" },
                                ).joinToString(" • ").ifEmpty { "Sem medidas" },
                                onClick = { onAbrirAvaliacao(a.id) },
                            )
                        }
                        TextButton(onClick = { onEvolucao(aluno.id) }) { Text("Ver evolução e comparativo") }
                    }
                    TextButton(onClick = { onFotos(aluno.id) }) { Text("Linha do tempo de fotos") }
                    TextButton(onClick = { onTestes(aluno.id) }) { Text("Testes de aptidão") }
                }

                CartaoSecao(titulo = "Treinos") {
                    if (s.treinos.isEmpty()) {
                        Text("Nenhum treino criado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        s.treinos.take(3).forEach { t ->
                            CartaoItem(
                                titulo = t.nome,
                                subtitulo = t.objetivo ?: "Criado em ${t.dataCriacao.format(DATA_BR)}",
                                onClick = { onAbrirTreino(t.id) },
                                fim = {
                                    TextButton(
                                        onClick = {
                                            val msg = MensagemRules.treinoPronto(aluno.nome, t.nome, profissional)
                                            if (!WhatsApp.enviar(ctx, aluno.telefone, msg)) {
                                                avisar("Cadastre um telefone válido ou instale o WhatsApp.")
                                            }
                                        },
                                    ) { Text("Avisar") }
                                },
                            )
                        }
                        TextButton(onClick = { onProgressao(aluno.id) }) { Text("Ver progressão de carga") }
                    }
                }

                CartaoRetornos(
                    retornos = retornos,
                    previa = previa,
                    onAnalisar = vm::analisarResumo,
                    onImportar = vm::importarResumo,
                    onLimpar = vm::limparPrevia,
                )

                val comNota = s.agendamentos
                    .filter { it.status == StatusAgendamento.CONCLUIDO && !it.anotacao.isNullOrBlank() }
                    .sortedWith(compareByDescending<Agendamento> { it.data }.thenByDescending { it.horario })
                if (comNota.isNotEmpty()) {
                    CartaoSecao(titulo = "Histórico de sessões") {
                        comNota.take(5).forEach { g ->
                            CartaoItem(
                                titulo = "${g.data.format(DATA_BR)} • ${g.tipo.rotulo}",
                                subtitulo = g.anotacao,
                                onClick = { onAbrirAgendamento(g.id) },
                            )
                        }
                        if (comNota.size > 5) {
                            Text(
                                "e mais ${comNota.size - 5} sessão(ões) com anotação",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                CartaoSecao(titulo = "Próximos agendamentos") {
                    val hoje = LocalDate.now()
                    val futuros = s.agendamentos
                        .filter { it.data >= hoje && it.status == StatusAgendamento.PENDENTE }
                        .sortedWith(compareBy({ it.data }, { it.horario }))
                    if (futuros.isEmpty()) {
                        Text("Nenhum agendamento pendente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        futuros.take(3).forEach { g ->
                            CartaoItem(
                                titulo = "${g.data.format(DATA_BR)} às ${g.horario}",
                                subtitulo = g.tipo.rotulo,
                                onClick = { onAbrirAgendamento(g.id) },
                                fim = {
                                    TextButton(
                                        onClick = {
                                            val msg = MensagemRules.lembreteAgendamento(
                                                aluno.nome, g.data, g.horario, g.tipo.rotulo, profissional,
                                            )
                                            if (!WhatsApp.enviar(ctx, aluno.telefone, msg)) {
                                                avisar("Cadastre um telefone válido ou instale o WhatsApp.")
                                            }
                                        },
                                    ) { Text("Lembrar") }
                                },
                            )
                        }
                    }
                }
            }

            // ── Financeiro ──
            GrupoAnimado(aba == 2) {
                CartaoMensalidade(
                    aluno = aluno,
                    pagamentos = s.pagamentos,
                    profissional = profissional,
                    perfil = perfilCompleto,
                    onSalvar = vm::salvarMensalidade,
                    onRegistrar = vm::registrarPagamento,
                    onExcluirPagamento = vm::excluirPagamento,
                    onRecibo = vm::gerarRecibo,
                    onMensagem = ::avisar,
                )
            }
        }
    }
}

/** Cabeçalho da ficha: avatar com anel, nome, idade/sexo, objetivo e botões redondos de contato. */
@Composable
private fun HeroAluno(
    aluno: Aluno,
    foto: java.io.File?,
    temTelefone: Boolean,
    onLigar: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val sobre = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto
    val idade = aluno.dataNascimento?.let { Period.between(it, LocalDate.now()).years }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(br.com.fitpersonal.app.ui.common.FolderShape(raio = 26.dp, recorte = 20.dp, fracaoAba = 0.6f))
            .background(br.com.fitpersonal.app.ui.common.PaletaPasta.Lima)
            .padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.border(3.dp, sobre.copy(alpha = 0.85f), CircleShape).padding(3.dp),
            ) {
                AvatarAluno(aluno.nome, foto, tamanho = 76.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(aluno.nome, style = MaterialTheme.typography.headlineSmall, color = sobre, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    idade?.let { PilulaResumo("$it anos") }
                    aluno.sexo?.takeIf { it.isNotBlank() }?.let { PilulaResumo(it) }
                }
            }
        }
        aluno.objetivo?.takeIf { it.isNotBlank() }?.let {
            Text("Objetivo: $it", style = MaterialTheme.typography.bodyMedium, color = sobre.copy(alpha = 0.92f))
        }
        if (temTelefone) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotaoRedondo(Icons.Filled.Call, "Ligar", onLigar, sobre)
                BotaoRedondo(Icons.Filled.Chat, "WhatsApp", onWhatsApp, sobre)
            }
        }
    }
}

@Composable
private fun BotaoRedondo(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    descricao: String,
    onClick: () -> Unit,
    sobre: Color,
) {
    Surface(onClick = onClick, shape = CircleShape, color = sobre.copy(alpha = 0.2f)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icone, contentDescription = descricao, tint = sobre, modifier = Modifier.size(20.dp))
            Text(descricao, style = MaterialTheme.typography.labelLarge, color = sobre)
        }
    }
}

@Composable
private fun MiniMetrica(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(valor, style = MaterialTheme.typography.titleLarge)
        Text(
            rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Peso, % de gordura e IMC da última avaliação em blocos coloridos, com a variação em relação à anterior. */
@Composable
private fun ResumoMedidas(avaliacoes: List<AvaliacaoFisica>) {
    if (avaliacoes.isEmpty()) return
    val cronologicas = avaliacoes.sortedWith(compareBy({ it.data }, { it.id }))
    val ultima = cronologicas.last()
    val anterior = cronologicas.dropLast(1).lastOrNull()
    val escuro = ehEscuro()

    CartaoSecao(titulo = "Última avaliação • ${ultima.data.format(DATA_BR)}") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Metrica.PESO, Metrica.PERCENTUAL_GORDURA, Metrica.IMC).forEachIndexed { indice, m ->
                val atual = EvolucaoRules.valor(ultima, m)
                val antes = anterior?.let { EvolucaoRules.valor(it, m) }
                val (fundo, texto) = when (indice) {
                    0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    1 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = fundo, contentColor = texto),
                    border = if (escuro) BorderStroke(1.dp, texto.copy(alpha = 0.2f)) else null,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            atual?.let { it.fmt(m.casas) } ?: "—",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            m.rotulo + if (m.unidade.isNotEmpty()) " (${m.unidade})" else "",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                        if (atual != null && antes != null) {
                            val v = EvolucaoRules.variacao(m, antes, atual)
                            ChipStatus(
                                EvolucaoRules.formatarDelta(v, m),
                                when (v.sentido) {
                                    SentidoVariacao.MELHOROU -> TomChip.Sucesso
                                    SentidoVariacao.PIOROU -> TomChip.Atencao
                                    else -> TomChip.Neutro
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
