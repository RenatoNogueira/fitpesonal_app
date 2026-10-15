package br.com.fitpersonal.app.ui.busca

import br.com.fitpersonal.app.core.estadoSeguro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.fitpersonal.app.data.files.FotoStorage
import br.com.fitpersonal.app.data.local.TreinoComAluno
import br.com.fitpersonal.app.data.repo.AgendaRepository
import br.com.fitpersonal.app.data.repo.AlunoRepository
import br.com.fitpersonal.app.data.repo.AvaliacaoRepository
import br.com.fitpersonal.app.data.repo.TreinoRepository
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.rules.BuscaRules
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.CampoBusca
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.TituloSecao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

data class ItemBuscaAvaliacao(val id: Long, val nomeAluno: String, val data: LocalDate, val resumo: String)

data class ItemBuscaAgenda(
    val id: Long,
    val nomeAluno: String,
    val data: LocalDate,
    val horario: String,
    val tipo: String,
    val status: String,
    val anotacao: String?,
)

data class ResultadoBusca(
    val termo: String = "",
    val alunos: List<Aluno> = emptyList(),
    val treinos: List<TreinoComAluno> = emptyList(),
    val avaliacoes: List<ItemBuscaAvaliacao> = emptyList(),
    val agendamentos: List<ItemBuscaAgenda> = emptyList(),
) {
    val vazio: Boolean get() = alunos.isEmpty() && treinos.isEmpty() && avaliacoes.isEmpty() && agendamentos.isEmpty()
}

@HiltViewModel
class BuscaViewModel @Inject constructor(
    alunos: AlunoRepository,
    treinos: TreinoRepository,
    avaliacoes: AvaliacaoRepository,
    agenda: AgendaRepository,
    private val fotos: FotoStorage,
) : ViewModel() {

    private val _termo = MutableStateFlow("")

    private val base = combine(alunos.observar(""), treinos.todosComAluno()) { a, t -> a to t }
    private val registros = combine(avaliacoes.todas(), agenda.todos()) { av, ag -> av to ag }

    val resultado: StateFlow<ResultadoBusca> = combine(_termo, base, registros) { termo, (listaAlunos, listaTreinos), (listaAv, listaAg) ->
        if (termo.isBlank()) {
            ResultadoBusca(termo)
        } else {
            val nomes = listaAlunos.associate { it.id to it.nome }
            val fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            ResultadoBusca(
                termo = termo,
                alunos = listaAlunos.filter { BuscaRules.alunoCorresponde(termo, it) },
                treinos = listaTreinos.filter { BuscaRules.corresponde(termo, it.nome, it.objetivo, it.nomeAluno) },
                avaliacoes = listaAv
                    .filter {
                        BuscaRules.corresponde(
                            termo, nomes[it.idAluno], it.data.format(fmt), it.anamnese, it.historicoSaude,
                            it.metas, it.postura, it.estiloVida, it.cardio,
                        )
                    }
                    .take(30)
                    .map {
                        ItemBuscaAvaliacao(
                            it.id, nomes[it.idAluno].orEmpty(), it.data,
                            listOfNotNull(
                                it.pesoKg?.let { p -> "%.1f kg".format(java.util.Locale("pt", "BR"), p) },
                                it.percentualGordura?.let { g -> "%.1f%% gordura".format(java.util.Locale("pt", "BR"), g) },
                            ).joinToString(" • ").ifEmpty { "Avaliação" },
                        )
                    },
                agendamentos = listaAg
                    .filter {
                        BuscaRules.corresponde(
                            termo, nomes[it.idAluno], it.data.format(fmt), it.tipo.rotulo, it.status.rotulo, it.anotacao,
                        )
                    }
                    .take(30)
                    .map {
                        ItemBuscaAgenda(
                            it.id, nomes[it.idAluno].orEmpty(), it.data, it.horario.toString(),
                            it.tipo.rotulo, it.status.rotulo, it.anotacao,
                        )
                    },
            )
        }
    }.estadoSeguro(viewModelScope, ResultadoBusca())

    val termo: StateFlow<String> = _termo
    fun buscar(texto: String) { _termo.value = texto }
    fun arquivoFoto(rel: String?): File? = rel?.let(fotos::arquivo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscaScreen(
    onVoltar: () -> Unit,
    onAbrirAluno: (Long) -> Unit,
    onAbrirTreino: (Long) -> Unit,
    onAbrirAvaliacao: (Long) -> Unit,
    onAbrirAgendamento: (Long) -> Unit,
    vm: BuscaViewModel = hiltViewModel(),
) {
    val termo by vm.termo.collectAsStateWithLifecycle()
    val r by vm.resultado.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Buscar", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CampoBusca(
                valor = termo,
                onValorChange = vm::buscar,
                placeholder = "Aluno, objetivo, telefone ou treino",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                termo.isBlank() -> EstadoVazio(
                    icone = Icons.Filled.Search,
                    titulo = "Busque em tudo",
                    texto = "Digite parte do nome, objetivo, telefone ou o nome de um treino.",
                )
                r.vazio -> EstadoVazio(
                    icone = Icons.Filled.Search,
                    titulo = "Nada encontrado",
                    texto = "Confira a grafia — a busca ignora acentos e maiúsculas.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (r.alunos.isNotEmpty()) {
                        item { TituloSecao("Alunos (${r.alunos.size})") }
                        items(r.alunos, key = { "a${it.id}" }) { a ->
                            CartaoItem(
                                modifier = Modifier.animateItem(),
                                onClick = { onAbrirAluno(a.id) },
                                titulo = a.nome,
                                subtitulo = a.objetivo?.takeIf { it.isNotBlank() },
                                inicio = { AvatarAluno(a.nome, vm.arquivoFoto(a.fotoPerfil), tamanho = 44.dp) },
                            )
                        }
                    }
                    if (r.treinos.isNotEmpty()) {
                        item { TituloSecao("Treinos (${r.treinos.size})") }
                        items(r.treinos, key = { "t${it.id}" }) { t ->
                            CartaoItem(
                                modifier = Modifier.animateItem(),
                                onClick = { onAbrirTreino(t.id) },
                                titulo = t.nome,
                                subtitulo = "Aluno: ${t.nomeAluno}",
                            )
                        }
                    }
                    if (r.avaliacoes.isNotEmpty()) {
                        item { TituloSecao("Avaliações (${r.avaliacoes.size})") }
                        items(r.avaliacoes, key = { "av${it.id}" }) { a ->
                            CartaoItem(
                                modifier = Modifier.animateItem(),
                                onClick = { onAbrirAvaliacao(a.id) },
                                titulo = a.nomeAluno,
                                subtitulo = "${a.data.format(DATA)} • ${a.resumo}",
                            )
                        }
                    }
                    if (r.agendamentos.isNotEmpty()) {
                        item { TituloSecao("Agendamentos (${r.agendamentos.size})") }
                        items(r.agendamentos, key = { "ag${it.id}" }) { g ->
                            CartaoItem(
                                modifier = Modifier.animateItem(),
                                onClick = { onAbrirAgendamento(g.id) },
                                titulo = g.nomeAluno,
                                subtitulo = "${g.data.format(DATA)} às ${g.horario} • ${g.tipo} • ${g.status}" +
                                    (g.anotacao?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                            )
                        }
                    }
                }
            }
        }
    }
}
