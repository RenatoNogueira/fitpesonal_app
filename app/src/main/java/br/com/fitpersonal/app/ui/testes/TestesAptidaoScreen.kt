package br.com.fitpersonal.app.ui.testes

import br.com.fitpersonal.app.core.estadoSeguro
import br.com.fitpersonal.app.core.TratadorDeErros
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.TesteAptidaoRepository
import br.com.fitpersonal.app.domain.rules.AptidaoRules
import br.com.fitpersonal.app.domain.rules.SentidoVariacao
import br.com.fitpersonal.app.domain.rules.TesteAptidao
import br.com.fitpersonal.app.domain.rules.TipoTeste
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.evolucao.GraficoLinha
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class TestesState(val nomeAluno: String = "", val testes: List<TesteAptidao> = emptyList())

@HiltViewModel
class TestesAptidaoViewModel @Inject constructor(
    alunos: AlunoRepository,
    private val repo: TesteAptidaoRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val idAluno: Long = savedState.get<Long>("id") ?: 0L

    val state: StateFlow<TestesState> = combine(
        alunos.observar("").map { l -> l.firstOrNull { it.id == idAluno }?.nome.orEmpty() },
        repo.doAluno(idAluno),
    ) { nome, testes -> TestesState(nome, testes) }
        .estadoSeguro(viewModelScope, TestesState())

    fun registrar(tipo: TipoTeste, valor: Double, observacao: String) {
        viewModelScope.launch(TratadorDeErros) {
            repo.registrar(
                TesteAptidao(
                    idAluno = idAluno, data = LocalDate.now(), tipo = tipo, valor = valor,
                    observacao = observacao.trim().ifEmpty { null },
                )
            )
        }
    }

    fun excluir(id: Long) {
        viewModelScope.launch(TratadorDeErros) { repo.excluir(id) }
    }
}

private val PT_BR = Locale("pt", "BR")
private val DATA = DateTimeFormatter.ofPattern("dd/MM/yy")

private fun fmt(v: Double, casas: Int) = String.format(PT_BR, "%.${casas}f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestesAptidaoScreen(onVoltar: () -> Unit, vm: TestesAptidaoViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tipo by remember { mutableStateOf(TipoTeste.COOPER_12MIN) }
    var valor by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    val numero = valor.trim().replace(',', '.').toDoubleOrNull()
    val valido = numero != null && AptidaoRules.valido(tipo, numero)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Testes de aptidão", style = MaterialTheme.typography.titleLarge)
                        if (s.nomeAluno.isNotEmpty()) {
                            Text(
                                s.nomeAluno,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CartaoSecao(titulo = "Novo resultado") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoTeste.entries.forEach { t ->
                        FilterChip(selected = t == tipo, onClick = { tipo = t; valor = "" }, label = { Text(t.rotulo) })
                    }
                }
                Text(
                    tipo.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it.filter { c -> c.isDigit() || c == ',' || c == '.' || c == '-' } },
                    label = { Text("Resultado (${tipo.unidade})") },
                    isError = valor.isNotBlank() && !valido,
                    supportingText = {
                        if (valor.isNotBlank() && !valido) {
                            Text("Valor fora do esperado (${fmt(tipo.minimo, 0)} a ${fmt(tipo.maximo, 0)} ${tipo.unidade}).")
                        } else if (tipo == TipoTeste.COOPER_12MIN && numero != null) {
                            AptidaoRules.vo2Cooper(numero)?.let { Text("VO₂ máx estimado: ${fmt(it, 1)} ml/kg/min") }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it.take(200) },
                    label = { Text("Observação (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = valido,
                    onClick = {
                        vm.registrar(tipo, numero!!, obs)
                        valor = ""
                        obs = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Registrar hoje") }
            }

            if (s.testes.isEmpty()) {
                Text(
                    "Nenhum teste registrado ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TipoTeste.entries.forEach { t ->
                val serie = AptidaoRules.serie(s.testes, t)
                if (serie.isEmpty()) return@forEach
                val daqueleTipo = s.testes.filter { it.tipo == t }
                CartaoSecao(titulo = t.rotulo) {
                    val ultimo = serie.last()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${fmt(ultimo.valor, t.casas)} ${t.unidade}", style = MaterialTheme.typography.headlineSmall)
                        if (serie.size >= 2) {
                            val anterior = serie[serie.size - 2].valor
                            val sentido = AptidaoRules.sentido(anterior, ultimo.valor, t.casas)
                            val delta = ultimo.valor - anterior
                            ChipStatus(
                                (if (delta > 0) "+" else "") + fmt(delta, t.casas) + " ${t.unidade}",
                                when (sentido) {
                                    SentidoVariacao.MELHOROU -> TomChip.Sucesso
                                    SentidoVariacao.PIOROU -> TomChip.Atencao
                                    else -> TomChip.Neutro
                                },
                            )
                        }
                    }
                    if (t == TipoTeste.COOPER_12MIN) {
                        AptidaoRules.vo2Cooper(ultimo.valor)?.let {
                            Text(
                                "VO₂ máx estimado: ${fmt(it, 1)} ml/kg/min",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (serie.size >= 2) {
                        GraficoLinha(
                            pontos = serie,
                            cor = MaterialTheme.colorScheme.primary,
                            grade = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                        )
                    }
                    daqueleTipo.take(5).forEach { r ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${r.data.format(DATA)} • ${fmt(r.valor, t.casas)} ${t.unidade}" +
                                    (r.observacao?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { vm.excluir(r.id) }) { Text("Excluir") }
                        }
                    }
                }
            }

            Text(
                "Os testes são registro de desempenho. A classificação por faixa etária depende da tabela do " +
                    "protocolo adotado e deve ser interpretada pelo profissional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
