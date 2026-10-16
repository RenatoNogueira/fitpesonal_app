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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.draw.clip

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            br.com.fitpersonal.app.ui.common.CabecalhoTela(
                titulo = "Buscar",
                subtitulo = if (termo.isBlank()) "Alunos, treinos, avaliações e agenda"
                else "Resultados para \"" + termo.trim() + "\"",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Lima,
                acoes = {
                    br.com.fitpersonal.app.ui.common.AcaoCabecalho(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onVoltar)
                },
            ) {
                if (termo.isNotBlank() && !r.vazio) {
                    br.com.fitpersonal.app.ui.common.LinhaPilulas(
                        listOfNotNull(
                            if (r.alunos.isNotEmpty()) r.alunos.size.toString() + " aluno(s)" else null,
                            if (r.treinos.isNotEmpty()) r.treinos.size.toString() + " treino(s)" else null,
                            if (r.avaliacoes.isNotEmpty()) r.avaliacoes.size.toString() + " avaliação(ões)" else null,
                            if (r.agendamentos.isNotEmpty()) r.agendamentos.size.toString() + " agendamento(s)" else null,
                        ),
                    )
                }
            }
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
                        item { TituloSecao("Alunos (" + r.alunos.size + ")") }
                        items(r.alunos, key = { "a" + it.id }) { a ->
                            br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Verde,
                                onClick = { onAbrirAluno(a.id) },
                                modifier = Modifier.animateItem(),
                                titulo = a.nome,
                                subtitulo = a.objetivo?.takeIf { it.isNotBlank() },
                                inicio = { AvatarAluno(a.nome, vm.arquivoFoto(a.fotoPerfil), tamanho = 48.dp) },
                            )
                        }
                    }
                    if (r.treinos.isNotEmpty()) {
                        item { TituloSecao("Treinos (" + r.treinos.size + ")") }
                        items(r.treinos, key = { "t" + it.id }) { t ->
                            br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Coral,
                                onClick = { onAbrirTreino(t.id) },
                                modifier = Modifier.animateItem(),
                                titulo = t.nome,
                                subtitulo = "Aluno: " + t.nomeAluno,
                                inicio = {
                                    DiscoDeIcone(Icons.Filled.FitnessCenter, br.com.fitpersonal.app.ui.common.PaletaPasta.Coral)
                                },
                            )
                        }
                    }
                    if (r.avaliacoes.isNotEmpty()) {
                        item { TituloSecao("Avaliações (" + r.avaliacoes.size + ")") }
                        items(r.avaliacoes, key = { "av" + it.id }) { a ->
                            br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Rosa,
                                onClick = { onAbrirAvaliacao(a.id) },
                                modifier = Modifier.animateItem(),
                                titulo = a.nomeAluno,
                                subtitulo = a.data.format(DATA) + " • " + a.resumo,
                                inicio = {
                                    DiscoDeIcone(Icons.Filled.Assessment, br.com.fitpersonal.app.ui.common.PaletaPasta.Rosa)
                                },
                            )
                        }
                    }
                    if (r.agendamentos.isNotEmpty()) {
                        item { TituloSecao("Agendamentos (" + r.agendamentos.size + ")") }
                        items(r.agendamentos, key = { "ag" + it.id }) { g ->
                            br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Teal,
                                onClick = { onAbrirAgendamento(g.id) },
                                modifier = Modifier.animateItem(),
                                titulo = g.nomeAluno,
                                subtitulo = g.data.format(DATA) + " às " + g.horario + " • " + g.tipo + " • " + g.status +
                                    (g.anotacao?.takeIf { it.isNotBlank() }?.let { " • " + it } ?: ""),
                                inicio = {
                                    DiscoDeIcone(Icons.Filled.Event, br.com.fitpersonal.app.ui.common.PaletaPasta.Teal)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Disco preto com o ícone na cor do cartão (mesmo desenho dos itens de Treinos e Agenda). */
@Composable
private fun DiscoDeIcone(icone: androidx.compose.ui.graphics.vector.ImageVector, cor: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(44.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(br.com.fitpersonal.app.ui.common.PaletaPasta.Preto),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(24.dp))
    }
}
