package br.com.fitpersonal.app.ui.agenda

import androidx.compose.foundation.horizontalScroll
import br.com.fitpersonal.app.ui.theme.corSobre
import androidx.compose.foundation.layout.*
import br.com.fitpersonal.app.domain.rules.RecorrenciaRules
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.StatusAgendamento
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.CampoAgendamento
import br.com.fitpersonal.app.ui.common.MaskTransformation
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import br.com.fitpersonal.app.domain.rules.AgendamentoValidator
import br.com.fitpersonal.app.domain.rules.Saudacao
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val HORA = MaskTransformation("##:##")
private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendamentoFormScreen(onVoltar: () -> Unit, vm: AgendamentoFormViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmarExclusao by remember { mutableStateOf(false) }
    var escolhendoData by remember { mutableStateOf(false) }

    LaunchedEffect(s.concluido) { if (s.concluido) onVoltar() }

    Scaffold(
        bottomBar = {
            if (!s.carregando) {
                Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Button(
                        onClick = vm::salvar,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text(if (s.ocorrencias > 1 && !s.editando) "Salvar série (${s.ocorrencias})" else "Salvar") }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(if (s.editando) "Editar agendamento" else "Novo agendamento") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                actions = {
                    if (s.editando) IconButton(onClick = { confirmarExclusao = true }) {
                        Icon(Icons.Default.Delete, "Excluir agendamento")
                    }
                },
            )
        },
    ) { padding ->
        if (s.carregando) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val i = s.input
        val alunoSel = s.alunos.firstOrNull { it.id == i.idAluno }
        val hoje = LocalDate.now()
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            var alunoAberto by remember { mutableStateOf(false) }

            // Resumo ao vivo do que será agendado
            val horaTexto = AgendamentoValidator.parseHorario(i.horario)?.toString() ?: "--:--"
            CabecalhoTela(
                titulo = Saudacao.rotuloData(i.data, hoje),
                subtitulo = "às $horaTexto • ${i.tipo.rotulo}",
            ) {
                LinhaPilulas(
                    listOfNotNull(
                        alunoSel?.nome ?: "Escolha o aluno",
                        i.status.rotulo,
                        if (!s.editando && s.ocorrencias > 1) "repete ${s.ocorrencias} semanas" else null,
                    ),
                )
            }

            CartaoSecao(titulo = "Aluno") {
                ExposedDropdownMenuBox(expanded = alunoAberto, onExpandedChange = { alunoAberto = it }) {
                    OutlinedTextField(
                        value = alunoSel?.nome.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aluno *") },
                        placeholder = { Text("Selecione um aluno") },
                        leadingIcon = alunoSel?.let { a -> { AvatarAluno(a.nome, null, tamanho = 32.dp) } },
                        isError = s.erros.containsKey(CampoAgendamento.ALUNO),
                        supportingText = s.erros[CampoAgendamento.ALUNO]?.let { { Text(it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(alunoAberto) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = alunoAberto, onDismissRequest = { alunoAberto = false }) {
                        s.alunos.forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.nome) },
                                leadingIcon = { AvatarAluno(a.nome, null, tamanho = 32.dp) },
                                onClick = { vm.alterar(CampoAgendamento.ALUNO) { it.copy(idAluno = a.id) }; alunoAberto = false },
                            )
                        }
                    }
                }
            }

            CartaoSecao(titulo = "Quando") {
                // Atalhos de data
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Hoje" to hoje,
                        "Amanhã" to hoje.plusDays(1),
                        "Em 1 semana" to hoje.plusWeeks(1),
                    ).forEach { (rotulo, dia) ->
                        FilterChip(
                            selected = i.data == dia,
                            onClick = { vm.alterar { it.copy(data = dia) } },
                            label = { Text(rotulo) },
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = { escolhendoData = true },
                        label = { Text("Outra data…") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) },
                    )
                }
                Text(
                    i.data.format(DATA_BR),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Atalhos de horário
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("0600", "0700", "0800", "0900", "1000", "1700", "1800", "1900").forEach { h ->
                        FilterChip(
                            selected = i.horario == h,
                            onClick = { vm.alterar(CampoAgendamento.HORARIO) { it.copy(horario = h) } },
                            label = { Text(h.substring(0, 2) + ":" + h.substring(2)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = i.horario,
                    onValueChange = { v ->
                        vm.alterar(CampoAgendamento.HORARIO) { it.copy(horario = v.filter(Char::isDigit).take(4)) }
                    },
                    label = { Text("Horário *") },
                    placeholder = { Text("HH:MM") },
                    isError = s.erros.containsKey(CampoAgendamento.HORARIO),
                    supportingText = s.erros[CampoAgendamento.HORARIO]?.let { { Text(it) } },
                    visualTransformation = HORA,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CartaoSecao(titulo = "Tipo") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TipoAgendamento.entries.forEach { t ->
                        val (icone, cor) = when (t) {
                            TipoAgendamento.TREINO -> Icons.Filled.FitnessCenter to MaterialTheme.colorScheme.primary
                            TipoAgendamento.AVALIACAO -> Icons.Filled.Assessment to MaterialTheme.colorScheme.tertiary
                            TipoAgendamento.CONSULTORIA -> Icons.Filled.Chat to MaterialTheme.colorScheme.secondary
                        }
                        val selecionado = i.tipo == t
                        Surface(
                            onClick = { vm.alterar { it.copy(tipo = t) } },
                            shape = MaterialTheme.shapes.large,
                            color = if (selecionado) cor else cor.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                Modifier.padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(icone, contentDescription = null, tint = if (selecionado) corSobre(cor) else cor)
                                Text(
                                    t.rotulo,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selecionado) corSobre(cor) else cor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            CartaoSecao(titulo = "Status") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusAgendamento.entries.forEach { st ->
                        FilterChip(
                            selected = i.status == st,
                            onClick = { vm.alterar { it.copy(status = st) } },
                            label = { Text(st.rotulo) },
                        )
                    }
                }
            }

            CartaoSecao(titulo = "Anotação da sessão") {
                OutlinedTextField(
                    value = i.anotacao,
                    onValueChange = { v -> vm.alterar { it.copy(anotacao = v.take(1000)) } },
                    label = { Text("O que foi feito, cargas, observações") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Só ao criar: série semanal no mesmo dia da semana e horário.
            if (!s.editando) {
                CartaoSecao(titulo = "Repetição") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecorrenciaRules.OPCOES.forEach { n ->
                            FilterChip(
                                selected = s.ocorrencias == n,
                                onClick = { vm.definirOcorrencias(n) },
                                label = { Text(RecorrenciaRules.rotulo(n)) },
                            )
                        }
                    }
                    if (s.ocorrencias > 1) {
                        Text(
                            "Cria ${s.ocorrencias} agendamentos, um por semana, até " +
                                s.input.data.plusWeeks((s.ocorrencias - 1).toLong()).format(DATA_BR) + ".",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    s.avisoSerie?.let { aviso ->
        AlertDialog(
            onDismissRequest = vm::avisoSerieLido,
            title = { Text("Série criada") },
            text = { Text(aviso) },
            confirmButton = { TextButton(onClick = vm::avisoSerieLido) { Text("OK") } },
        )
    }

    if (escolhendoData) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = s.input.data.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.alterar { it.copy(data = d) }
                    }
                    escolhendoData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }

    s.conflito?.let { c ->
        AlertDialog(
            onDismissRequest = vm::cancelarConflito,
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Conflito de horário") },
            text = {
                Text(
                    "Já existe um agendamento em ${s.input.data.format(DATA_BR)} às ${c.horario} para ${c.nomeAluno}.\n\n" +
                        "Deseja salvar mesmo assim?",
                )
            },
            confirmButton = { TextButton(onClick = vm::confirmarConflito) { Text("Salvar mesmo assim") } },
            dismissButton = { TextButton(onClick = vm::cancelarConflito) { Text("Voltar e ajustar") } },
        )
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir agendamento?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}
