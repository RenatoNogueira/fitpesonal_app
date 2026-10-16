package br.com.fitpersonal.app.ui.treinos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.ItemComAcento
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import br.com.fitpersonal.app.ui.theme.Paleta
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.fitpersonal.app.domain.model.ModeloTreino
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.SeletorAluno
import java.time.format.DateTimeFormatter

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun TreinosScreen(
    onNovo: (idAluno: Long) -> Unit,
    onAbrir: (idTreino: Long) -> Unit,
    vm: TreinosViewModel = hiltViewModel(),
) {
    val alunos by vm.alunos.collectAsStateWithLifecycle()
    val selecionado by vm.alunoSelecionado.collectAsStateWithLifecycle()
    val treinos by vm.treinos.collectAsStateWithLifecycle()
    val modelos by vm.modelos.collectAsStateWithLifecycle()
    val totais by vm.totais.collectAsStateWithLifecycle()
    var biblioteca by remember { mutableStateOf(false) }
    var excluindoModelo by remember { mutableStateOf<ModeloTreino?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val nomeAluno = alunos.firstOrNull { it.id == selecionado }?.nome
            val totalExercicios = treinos.sumOf { totais[it.id] ?: 0 }
            CabecalhoTela(
                titulo = "Treinos",
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Coral,
                subtitulo = nomeAluno ?: "Fichas A/B/C/D por aluno",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                acoes = {
                    selecionado?.let { id -> br.com.fitpersonal.app.ui.common.AcaoCabecalho(Icons.Filled.Add, "Novo treino", { onNovo(id) }) }
                    TextButton(onClick = { biblioteca = true }) {
                        Text("Modelos (${modelos.size})", color = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto)
                    }
                },
            ) {
                if (selecionado != null) {
                    LinhaPilulas(
                        listOf(
                            "${treinos.size} treino(s)",
                            "$totalExercicios exercício(s)",
                        ),
                    )
                }
            }
            SeletorAluno(
                alunos = alunos,
                selecionadoId = selecionado,
                onSelecionar = vm::selecionar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            when {
                alunos.isEmpty() -> EstadoVazio(
                    icone = Icons.Filled.People,
                    titulo = "Cadastre um aluno primeiro",
                    texto = "Os treinos são montados para cada aluno.",
                )
                selecionado == null -> EstadoVazio(
                    icone = Icons.Filled.FitnessCenter,
                    titulo = "Escolha um aluno",
                    texto = "Selecione acima para ver ou criar as fichas de treino.",
                )
                treinos.isEmpty() -> EstadoVazio(
                    icone = Icons.Filled.FitnessCenter,
                    titulo = "Nenhum treino ainda",
                    texto = "Toque em \"Novo treino\" para montar a primeira ficha.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(treinos, key = { _, t -> t.id }) { indice, t ->
                        val cores = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            Paleta.Rosa600,
                        )
                        val cor = cores[indice % cores.size]
                        val qtd = totais[t.id] ?: 0
                        val corPasta = listOf(
                            br.com.fitpersonal.app.ui.common.PaletaPasta.Lima,
                            br.com.fitpersonal.app.ui.common.PaletaPasta.Verde,
                            br.com.fitpersonal.app.ui.common.PaletaPasta.Teal,
                            br.com.fitpersonal.app.ui.common.PaletaPasta.Coral,
                        )[indice % 4]
                        br.com.fitpersonal.app.ui.common.CartaoPastaItem(
                            cor = corPasta,
                            onClick = { onAbrir(t.id) },
                            modifier = Modifier.animateItem(),
                            inicio = {
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape)
                                        .background(br.com.fitpersonal.app.ui.common.PaletaPasta.Preto),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = corPasta)
                                }
                            },
                            titulo = t.nome,
                            subtitulo = listOfNotNull(
                                if (qtd == 0) "sem exercícios" else qtd.toString() + " exercício(s)",
                                t.objetivo?.takeIf { it.isNotBlank() },
                                "criado em " + t.dataCriacao.format(DATA_BR),
                            ).joinToString(" • "),
                        )
                    }
                }
            }
        }
    }

    if (biblioteca) {
        AlertDialog(
            onDismissRequest = { biblioteca = false },
            title = { Text("Modelos de treino") },
            text = {
                if (modelos.isEmpty()) {
                    Text(
                        "Nenhum modelo ainda. No editor de um treino, use \"Salvar como modelo\" no menu.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selecionado == null) {
                            Text(
                                "Escolha um aluno na tela de treinos para aplicar um modelo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        LazyColumn(
                            Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(modelos, key = { it.id }) { m ->
                                CartaoItem(
                                    onClick = {
                                        if (selecionado != null) {
                                            biblioteca = false
                                            vm.aplicarModelo(m.id, onAbrir)
                                        }
                                    },
                                    titulo = m.nome,
                                    subtitulo = listOfNotNull(
                                        "${m.totalExercicios} exercício(s)",
                                        m.objetivo?.takeIf { it.isNotBlank() },
                                    ).joinToString(" • "),
                                    fim = {
                                        TextButton(onClick = { excluindoModelo = m }) { Text("Excluir") }
                                    },
                                )
                            }
                        }
                        if (selecionado != null) {
                            Text(
                                "Toque em um modelo para criar um treino novo para o aluno selecionado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { biblioteca = false }) { Text("Fechar") } },
        )
    }

    excluindoModelo?.let { m ->
        AlertDialog(
            onDismissRequest = { excluindoModelo = null },
            title = { Text("Excluir modelo?") },
            text = { Text("\"${m.nome}\" será removido da biblioteca. Treinos já criados a partir dele não são afetados.") },
            confirmButton = {
                TextButton(onClick = { vm.excluirModelo(m.id); excluindoModelo = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { excluindoModelo = null }) { Text("Cancelar") } },
        )
    }
}
