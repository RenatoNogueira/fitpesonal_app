package br.com.fitpersonal.app.ui.avaliacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import br.com.fitpersonal.app.domain.rules.ClasseImc
import br.com.fitpersonal.app.ui.common.CabecalhoTela
import br.com.fitpersonal.app.ui.common.ItemComAcento
import br.com.fitpersonal.app.ui.common.LinhaPilulas
import br.com.fitpersonal.app.ui.theme.Paleta
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.fitpersonal.app.domain.model.AvaliacaoFisica
import br.com.fitpersonal.app.domain.rules.AvaliacaoRules
import br.com.fitpersonal.app.ui.common.BlocoHorario
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.EstadoVazio
import br.com.fitpersonal.app.ui.common.SeletorAluno
import br.com.fitpersonal.app.ui.common.TomChip
import java.util.Locale

@Composable
fun AvaliacoesScreen(
    onNova: (idAluno: Long) -> Unit,
    onAbrir: (idAvaliacao: Long) -> Unit,
    vm: AvaliacoesViewModel = hiltViewModel(),
) {
    val alunos by vm.alunos.collectAsStateWithLifecycle()
    val selecionado by vm.alunoSelecionado.collectAsStateWithLifecycle()
    val avaliacoes by vm.avaliacoes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            selecionado?.let { id ->
                ExtendedFloatingActionButton(
                    onClick = { onNova(id) },
                    modifier = Modifier.padding(bottom = 72.dp),   // acima da barra de vidro flutuante
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nova avaliação") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val nomeAluno = alunos.firstOrNull { it.id == selecionado }?.nome
            val cronologicas = avaliacoes.sortedWith(compareBy({ it.data }, { it.id }))
            val primeiroPeso = cronologicas.firstNotNullOfOrNull { it.pesoKg }
            val ultimoPeso = cronologicas.lastNotNullOfOrNull { it.pesoKg }
            CabecalhoTela(
                titulo = "Avaliações",
                cor = br.com.fitpersonal.app.ui.common.PaletaPasta.Rosa,
                subtitulo = nomeAluno ?: "Medidas, saúde, dobras e fotos",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            ) {
                if (avaliacoes.isNotEmpty()) {
                    LinhaPilulas(
                        listOfNotNull(
                            "${avaliacoes.size} avaliação(ões)",
                            "última em %02d/%02d/%d".format(
                                cronologicas.last().data.dayOfMonth,
                                cronologicas.last().data.monthValue,
                                cronologicas.last().data.year,
                            ),
                            if (primeiroPeso != null && ultimoPeso != null && cronologicas.size > 1) {
                                val delta = ultimoPeso - primeiroPeso
                                (if (delta > 0) "+" else "") + delta.formatar() + " kg desde a primeira"
                            } else null,
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
                    texto = "As avaliações são registradas para cada aluno.",
                )
                selecionado == null -> EstadoVazio(
                    icone = Icons.Filled.Assessment,
                    titulo = "Escolha um aluno",
                    texto = "Selecione acima para ver o histórico ou registrar uma nova avaliação.",
                )
                avaliacoes.isEmpty() -> EstadoVazio(
                    icone = Icons.Filled.Assessment,
                    titulo = "Nenhuma avaliação ainda",
                    texto = "Toque em \"Nova avaliação\" para registrar a primeira.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // A lista vem do mais recente ao mais antigo: o "anterior" é o próximo da lista.
                    itemsIndexed(avaliacoes, key = { _, a -> a.id }) { indice, a ->
                        ItemAvaliacao(
                            a = a,
                            anterior = avaliacoes.getOrNull(indice + 1),
                            onClick = { onAbrir(a.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

private fun Double.formatar(casas: Int = 1) = String.format(Locale("pt", "BR"), "%.${casas}f", this)

/** Último elemento não nulo (equivalente a firstNotNullOfOrNull, de trás para frente). */
private fun <T, R : Any> List<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
    for (i in indices.reversed()) transform(this[i])?.let { return it }
    return null
}

@Composable
private fun ItemAvaliacao(
    a: AvaliacaoFisica,
    anterior: AvaliacaoFisica?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imc = AvaliacaoRules.imc(a.pesoKg, a.alturaM)
    val classeImc = imc?.let(AvaliacaoRules::classificarImc)
    val pressao = AvaliacaoRules.classificarPressao(a.pressaoSistolica, a.pressaoDiastolica)
    val fotos = listOfNotNull(a.fotoFrente, a.fotoCostas, a.fotoLateralDir, a.fotoLateralEsq).size

    val titulo = listOfNotNull(
        a.pesoKg?.let { "${it.formatar()} kg" },
        imc?.let { "IMC ${it.formatar()}" },
    ).joinToString(" • ").ifEmpty { "Avaliação" }

    val subtitulo = listOfNotNull(
        a.percentualGordura?.let { "${it.formatar()}% de gordura" },
        a.cinturaCm?.let { "cintura ${it.formatar()} cm" },
        if (fotos > 0) "$fotos foto(s)" else null,
    ).joinToString(" • ").ifEmpty { null }

    val (corClasse, tomClasse) = when (classeImc) {
        ClasseImc.NORMAL -> Paleta.Sucesso to TomChip.Sucesso
        ClasseImc.BAIXO_PESO, ClasseImc.SOBREPESO -> MaterialTheme.colorScheme.tertiary to TomChip.Atencao
        null -> MaterialTheme.colorScheme.outline to TomChip.Neutro
        else -> MaterialTheme.colorScheme.error to TomChip.Erro
    }

    val deltaPeso = if (a.pesoKg != null && anterior?.pesoKg != null) a.pesoKg - anterior.pesoKg else null

    val corPasta = when (classeImc) {
        ClasseImc.NORMAL -> br.com.fitpersonal.app.ui.common.PaletaPasta.Verde
        ClasseImc.BAIXO_PESO, ClasseImc.SOBREPESO -> br.com.fitpersonal.app.ui.common.PaletaPasta.Lima
        null -> br.com.fitpersonal.app.ui.common.PaletaPasta.Teal
        else -> br.com.fitpersonal.app.ui.common.PaletaPasta.Coral
    }
    br.com.fitpersonal.app.ui.common.CartaoPastaItem(
        cor = corPasta,
        onClick = onClick,
        modifier = modifier,
        titulo = titulo,
        subtitulo = subtitulo,
        inicio = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d/%02d".format(a.data.dayOfMonth, a.data.monthValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto,
                )
                Text(
                    a.data.year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = br.com.fitpersonal.app.ui.common.PaletaPasta.Preto.copy(alpha = 0.7f),
                )
            }
        },
        fim = {
            classeImc?.let { br.com.fitpersonal.app.ui.common.EtiquetaPreta(it.rotulo) }
            if (deltaPeso != null && kotlin.math.abs(deltaPeso) >= 0.05) {
                br.com.fitpersonal.app.ui.common.EtiquetaPreta(
                    (if (deltaPeso > 0) "+" else "") + deltaPeso.formatar() + " kg",
                )
            }
            if (pressao?.alerta == true) br.com.fitpersonal.app.ui.common.EtiquetaPreta("PA")
        },
    )
}
