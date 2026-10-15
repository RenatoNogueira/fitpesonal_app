package br.com.fitpersonal.app.ui.espera

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.ListaEsperaRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.ListaEspera
import br.com.fitpersonal.app.domain.rules.AgendamentoValidator
import br.com.fitpersonal.app.domain.rules.FrequenciaRules
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.MaskTransformation
import br.com.fitpersonal.app.ui.common.SeletorAluno
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ListaEsperaViewModel @Inject constructor(
    alunosRepo: AlunoRepository,
    private val repo: ListaEsperaRepository,
) : ViewModel() {

    val alunos: StateFlow<List<Aluno>> = alunosRepo.observar("")
        .estadoSeguro(viewModelScope, emptyList())

    val itens: StateFlow<List<ListaEspera>> = repo.todos
        .estadoSeguro(viewModelScope, emptyList())

    fun adicionar(idAluno: Long, dia: DayOfWeek, horarioDigitos: String, observacao: String) {
        val horario = AgendamentoValidator.parseHorario(horarioDigitos)
        viewModelScope.launch(TratadorDeErros) {
            repo.adicionar(
                ListaEspera(
                    idAluno = idAluno,
                    diaSemana = dia,
                    horario = horario,
                    observacao = observacao.trim().ifEmpty { null },
                    criadoEm = LocalDate.now(),
                )
            )
        }
    }

    fun remover(id: Long) {
        viewModelScope.launch(TratadorDeErros) { repo.remover(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaEsperaScreen(
    onVoltar: () -> Unit,
    onAgendar: (Long) -> Unit,
    vm: ListaEsperaViewModel = hiltViewModel(),
) {
    val alunos by vm.alunos.collectAsStateWithLifecycle()
    val itens by vm.itens.collectAsStateWithLifecycle()
    var adicionando by remember { mutableStateOf(false) }
    val nomes = alunos.associate { it.id to it.nome }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Lista de espera", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adicionando = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Adicionar") },
            )
        },
    ) { padding ->
        if (itens.isEmpty()) {
            EstadoVazio(
                icone = Icons.Filled.Schedule,
                titulo = "Ninguém na espera",
                texto = "Anote quem quer um horário que está cheio e agende quando abrir uma vaga.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(itens, key = { it.id }) { e ->
                    CartaoItem(
                        onClick = { onAgendar(e.idAluno) },
                        titulo = nomes[e.idAluno] ?: "Aluno removido",
                        subtitulo = listOfNotNull(
                            FrequenciaRules.rotuloDia(e.diaSemana) + (e.horario?.let { " às $it" } ?: " (qualquer horário)"),
                            e.observacao,
                        ).joinToString(" • "),
                        fim = {
                            Row {
                                TextButton(onClick = { onAgendar(e.idAluno) }) { Text("Agendar") }
                                TextButton(onClick = { vm.remover(e.id) }) { Text("Remover") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (adicionando) {
        var aluno by remember { mutableStateOf<Long?>(null) }
        var dia by remember { mutableStateOf(DayOfWeek.MONDAY) }
        var hora by remember { mutableStateOf("") }
        var obs by remember { mutableStateOf("") }
        val horaValida = hora.isEmpty() || AgendamentoValidator.parseHorario(hora) != null

        AlertDialog(
            onDismissRequest = { adicionando = false },
            title = { Text("Adicionar à espera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeletorAluno(alunos = alunos, selecionadoId = aluno, onSelecionar = { aluno = it })
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DayOfWeek.entries.forEach { d ->
                            FilterChip(
                                selected = dia == d,
                                onClick = { dia = d },
                                label = { Text(FrequenciaRules.rotuloDia(d).substringBefore('-')) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it.filter(Char::isDigit).take(4) },
                        label = { Text("Horário preferido (opcional)") },
                        placeholder = { Text("HH:MM") },
                        visualTransformation = MaskTransformation("##:##"),
                        isError = !horaValida,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it.take(120) },
                        label = { Text("Observação (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = aluno != null && horaValida,
                    onClick = {
                        vm.adicionar(aluno!!, dia, hora, obs)
                        adicionando = false
                    },
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { adicionando = false }) { Text("Cancelar") } },
        )
    }
}
