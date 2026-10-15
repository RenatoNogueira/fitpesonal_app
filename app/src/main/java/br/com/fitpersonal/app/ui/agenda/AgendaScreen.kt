package br.com.fitpersonal.app.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.ItemComAcento
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.theme.Paleta
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Send
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.ui.common.WhatsApp
import java.time.LocalTime
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.Saudacao
import br.com.fitpersonal.app.ui.common.BlocoHorario
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.CompartilharPdfEffect
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.tomDoStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    onNovo: (dia: LocalDate) -> Unit,
    onAbrir: (id: Long) -> Unit,
    onEspera: () -> Unit = {},
    vm: AgendaViewModel = hiltViewModel(),
) {
    val dia by vm.data.collectAsStateWithLifecycle()
    val itens by vm.agendamentos.collectAsStateWithLifecycle()
    var escolhendoData by remember { mutableStateOf(false) }

    val pendentes = itens.count { it.status == StatusAgendamento.PENDENTE.name }
    val agora = LocalTime.now()
    val proximoId = if (dia == LocalDate.now()) {
        itens.firstOrNull {
            it.status == StatusAgendamento.PENDENTE.name && !LocalTime.parse(it.horario).isBefore(agora)
        }?.id
    } else null
    val hoje = LocalDate.now()

    val profissional by vm.profissional.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pdf by vm.pdf.collectAsStateWithLifecycle()
    val erroPdf by vm.erroPdf.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    CompartilharPdfEffect(pdf, vm::pdfConsumido)
    LaunchedEffect(erroPdf) { erroPdf?.let { snackbar.showSnackbar(it); vm.erroLido() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNovo(dia) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agendar") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val concluidos = itens.count { it.status == StatusAgendamento.CONCLUIDO.name }
            val faltas = itens.count { it.status == StatusAgendamento.FALTOU.name }
            var menuAberto by remember { mutableStateOf(false) }

            CabecalhoTela(
                titulo = "Agenda",
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Teal,
                subtitulo = Saudacao.dataExtensa(dia),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                acoes = {
                    IconButton(onClick = { escolhendoData = true }) {
                        Icon(
                            Icons.Filled.CalendarMonth, contentDescription = "Escolher data",
                            tint = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(
                                Icons.Filled.MoreVert, contentDescription = "Mais opções",
                                tint = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto,
                            )
                        }
                        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                            DropdownMenuItem(
                                text = { Text("Agenda do dia em PDF") },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = { menuAberto = false; vm.gerarPdf() },
                            )
                            DropdownMenuItem(
                                text = { Text("Lista de espera") },
                                leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                                onClick = { menuAberto = false; onEspera() },
                            )
                            DropdownMenuItem(
                                text = { Text("Relatório do mês em PDF") },
                                leadingIcon = { Icon(Icons.Filled.Assessment, null) },
                                onClick = { menuAberto = false; vm.gerarRelatorioMes() },
                            )
                        }
                    }
                },
            ) {
                LinhaPilulas(
                    if (itens.isEmpty()) listOf("Dia livre")
                    else listOfNotNull(
                        "${itens.size} no dia",
                        if (pendentes > 0) "$pendentes pendente(s)" else null,
                        if (concluidos > 0) "$concluidos concluído(s)" else null,
                        if (faltas > 0) "$faltas falta(s)" else null,
                    ),
                )
            }

            FaixaSemana(
                selecionado = dia,
                hoje = hoje,
                onSelecionar = vm::irPara,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )

            if (dia != hoje) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = vm::hoje) { Text("Voltar para hoje") }
                }
            }

            if (itens.isEmpty()) {
                EstadoVazio(
                    icone = Icons.Filled.Event,
                    titulo = "Dia livre",
                    texto = "Nenhum agendamento em ${Saudacao.rotuloData(dia, hoje).lowercase()}. Toque em \"Agendar\" para marcar um horário.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(itens, key = { it.id }) { ag ->
                        val status = (StatusAgendamento.entries.firstOrNull { it.name == ag.status } ?: StatusAgendamento.PENDENTE)
                        val ehProximo = ag.id == proximoId
                        val corAcento = when (status) {
                            StatusAgendamento.PENDENTE ->
                                if (ehProximo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            StatusAgendamento.CONCLUIDO -> Paleta.Sucesso
                            StatusAgendamento.FALTOU -> MaterialTheme.colorScheme.error
                            StatusAgendamento.CANCELADO -> MaterialTheme.colorScheme.outline
                        }
                        val corPasta = when (status) {
                            StatusAgendamento.PENDENTE ->
                                if (ehProximo) br.com.fitpersonal.app.ui.common.PaletaPasta.Lima
                                else br.com.fitpersonal.app.ui.common.PaletaPasta.Verde
                            StatusAgendamento.CONCLUIDO -> br.com.fitpersonal.app.ui.common.PaletaPasta.Teal
                            StatusAgendamento.FALTOU -> br.com.fitpersonal.app.ui.common.PaletaPasta.Coral
                            StatusAgendamento.CANCELADO -> androidx.compose.ui.graphics.Color(0xFFC9D1C2)
                        }
                        br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                            cor = corPasta,
                            onClick = { onAbrir(ag.id) },
                            modifier = Modifier.animateItem(),
                            inicio = {
                                Text(
                                    ag.horario,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                    color = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto,
                                )
                            },
                            titulo = ag.nomeAluno,
                            subtitulo = (TipoAgendamento.entries.firstOrNull { it.name == ag.tipo } ?: TipoAgendamento.TREINO).rotulo,
                            fim = {
                                br.com.fitpersonal.app.ui.common.EtiquetaPreta(if (ehProximo) "Próximo" else status.rotulo)
                                if (status == StatusAgendamento.PENDENTE) {
                                    Row {
                                        IconButton(
                                            modifier = Modifier.size(38.dp),
                                            onClick = {
                                                val msg = MensagemRules.lembreteAgendamento(
                                                    ag.nomeAluno, LocalDate.parse(ag.data), LocalTime.parse(ag.horario),
                                                    (TipoAgendamento.entries.firstOrNull { it.name == ag.tipo } ?: TipoAgendamento.TREINO).rotulo, profissional,
                                                )
                                                if (!WhatsApp.enviar(ctx, ag.telefoneAluno, msg)) {
                                                    vm.avisarErro(ag.nomeAluno + ": sem telefone válido ou WhatsApp indisponível.")
                                                }
                                            },
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Lembrar pelo WhatsApp")
                                        }
                                        IconButton(
                                            modifier = Modifier.size(38.dp),
                                            onClick = { vm.mudarStatus(ag.id, StatusAgendamento.FALTOU) },
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Marcar falta")
                                        }
                                        IconButton(
                                            modifier = Modifier.size(38.dp),
                                            onClick = { vm.mudarStatus(ag.id, StatusAgendamento.CONCLUIDO) },
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = "Marcar como concluído")
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (escolhendoData) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = dia.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let {
                        vm.irPara(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

/** Faixa de 7 dias em torno do dia selecionado, com setas para pular uma semana. */
@Composable
private fun FaixaSemana(
    selecionado: LocalDate,
    hoje: LocalDate,
    onSelecionar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelecionar(selecionado.minusWeeks(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior")
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (-3..3).map { selecionado.plusDays(it.toLong()) }.forEach { d ->
                val ativo = d == selecionado
                val ehHoje = d == hoje
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (ativo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerLowest,
                        )
                        .clickable { onSelecionar(d) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val cor = if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Text(
                        Saudacao.diaSemanaCurto(d),
                        style = MaterialTheme.typography.labelSmall,
                        color = cor,
                    )
                    Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = cor)
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (ehHoje) {
                                    if (ativo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0f),
                            ),
                    )
                }
            }
        }
        IconButton(onClick = { onSelecionar(selecionado.plusWeeks(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próxima semana")
        }
    }
}
