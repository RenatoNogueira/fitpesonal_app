package br.com.fitpersonal.app.ui.alunos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import br.com.fitpersonal.app.domain.rules.FiltroAluno
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.rules.SituacaoReavaliacao
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.ItemComAcento
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import androidx.compose.ui.Alignment
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.CampoBusca
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.TomChip
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun AlunosScreen(
    onNovo: () -> Unit,
    onAbrir: (Long) -> Unit,
    vm: AlunosViewModel = hiltViewModel(),
) {
    val alunos by vm.alunos.collectAsStateWithLifecycle()
    val busca by vm.busca.collectAsStateWithLifecycle()
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val resumo by vm.resumo.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNovo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Novo aluno") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CabecalhoTela(
                titulo = "Alunos",
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Verde,
                subtitulo = if (resumo.total == 0) "Cadastre seu primeiro aluno"
                else "${alunos.size} de ${resumo.total} aluno${if (resumo.total != 1) "s" else ""}",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                LinhaPilulas(
                    listOfNotNull(
                        if (resumo.reavaliacao > 0) "${resumo.reavaliacao} para reavaliar" else null,
                        if (resumo.aniversarioNoMes > 0) "${resumo.aniversarioNoMes} aniversário(s) no mês" else null,
                        if (resumo.semAvaliacao > 0) "${resumo.semAvaliacao} sem avaliação" else null,
                    ),
                )
            }
            CampoBusca(
                valor = busca,
                onValorChange = vm::buscar,
                placeholder = "Nome, objetivo, telefone ou e-mail",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroAluno.entries.forEach { f ->
                    FilterChip(
                        selected = f == filtro,
                        onClick = { vm.filtrar(f) },
                        label = { Text(f.rotulo) },
                    )
                }
            }

            if (alunos.isEmpty()) {
                if (busca.isBlank() && filtro == FiltroAluno.TODOS) {
                    EstadoVazio(
                        icone = Icons.Filled.People,
                        titulo = "Nenhum aluno cadastrado",
                        texto = "Toque em \"Novo aluno\" para começar. Você poderá registrar avaliações, treinos e agendamentos.",
                    )
                } else {
                    EstadoVazio(
                        icone = Icons.Filled.Search,
                        titulo = "Nenhum aluno encontrado",
                        texto = "Ajuste a busca ou escolha outro filtro.",
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(alunos, key = { it.aluno.id }) { item ->
                        val a = item.aluno
                        val cor = when (item.reavaliacao) {
                            is SituacaoReavaliacao.Atrasada -> MaterialTheme.colorScheme.error
                            is SituacaoReavaliacao.Proxima, SituacaoReavaliacao.SemAvaliacao -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val corPasta = when (item.reavaliacao) {
                            is SituacaoReavaliacao.Atrasada -> br.com.fitpersonal.app.ui.common.PaletaPasta.Coral
                            is SituacaoReavaliacao.Proxima, SituacaoReavaliacao.SemAvaliacao -> br.com.fitpersonal.app.ui.common.PaletaPasta.Lima
                            else -> br.com.fitpersonal.app.ui.common.PaletaPasta.Verde
                        }
                        br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                            cor = corPasta,
                            onClick = { onAbrir(a.id) },
                            modifier = Modifier.animateItem(),
                            inicio = { AvatarAluno(a.nome, vm.arquivoFoto(a.fotoPerfil), tamanho = 52.dp) },
                            titulo = a.nome + if (item.aniversarioHoje) "  🎂" else "",
                            subtitulo = resumo(item),
                            fim = {
                                when (val r = item.reavaliacao) {
                                    is SituacaoReavaliacao.Atrasada -> br.com.fitpersonal.app.ui.common.EtiquetaPreta("Reavaliar")
                                    is SituacaoReavaliacao.Proxima -> br.com.fitpersonal.app.ui.common.EtiquetaPreta(
                                        if (r.diasRestantes == 0) "Reavaliar hoje" else "Reaval. em " + r.diasRestantes + "d",
                                    )
                                    SituacaoReavaliacao.SemAvaliacao -> br.com.fitpersonal.app.ui.common.EtiquetaPreta("Sem avaliação")
                                    else -> Unit
                                }
                                if (a.consentimentoLgpdEm == null) br.com.fitpersonal.app.ui.common.EtiquetaPreta("Sem consentimento")
                            },
                        )
                    }
                }
            }
        }
    }
}

private val DATA_CURTA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy")

private fun resumo(i: AlunoItemUi): String? = listOfNotNull(
    i.aluno.dataNascimento?.let { "${ChronoUnit.YEARS.between(it, LocalDate.now())} anos" },
    i.aluno.objetivo?.takeIf { it.isNotBlank() },
    i.ultimaAvaliacao?.let { "avaliado em ${it.format(DATA_CURTA)}" },
).joinToString(" • ").ifEmpty { null }
