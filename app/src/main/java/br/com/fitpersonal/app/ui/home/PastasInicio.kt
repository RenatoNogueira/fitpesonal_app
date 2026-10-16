package br.com.fitpersonal.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.data.local.AgendamentoComAluno
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.TipoAgendamento
import br.com.fitpersonal.app.domain.rules.AniversarioRules
import br.com.fitpersonal.app.domain.rules.Saudacao
import br.com.fitpersonal.app.domain.rules.SituacaoAniversario
import br.com.fitpersonal.app.ui.common.AvatarAluno
import br.com.fitpersonal.app.ui.common.FolderShape
import br.com.fitpersonal.app.ui.common.PaletaPasta
import java.io.File
import java.time.LocalDate

/** Quantos atendimentos aparecem na pilha do Início; o restante vira "+ N na agenda". */
const val MAX_NA_PILHA = 4

/** Cor do cartão de aniversariante pela proximidade: hoje/amanhã em coral, até 7 dias em lima, depois verde. */
fun corDoAniversario(nascimento: LocalDate, hoje: LocalDate): Color = when (AniversarioRules.situacao(nascimento, hoje)) {
    SituacaoAniversario.HOJE, SituacaoAniversario.AMANHA -> PaletaPasta.Coral
    SituacaoAniversario.JA_PASSOU -> PaletaPasta.Teal
    SituacaoAniversario.PROXIMO ->
        if (AniversarioRules.diasAte(nascimento, hoje) <= 7) PaletaPasta.Lima else PaletaPasta.Verde
}

/**
 * Próximos agendamentos como uma pilha de pastas: duas camadas coloridas atrás e a pasta da frente
 * com uma linha por atendimento (ícone, aluno, quando e horário). Tocar abre a agenda.
 */
@Composable
fun PilhaAgenda(
    itens: List<AgendamentoComAluno>,
    hoje: LocalDate,
    onAbrir: () -> Unit,
    onCriar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tinta = PaletaPasta.Preto
    Box(modifier.fillMaxWidth()) {
        // camadas de trás
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(90.dp)
                .clip(FolderShape())
                .background(PaletaPasta.degrade(PaletaPasta.Coral)),
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .fillMaxWidth()
                .padding(horizontal = 11.dp)
                .height(90.dp)
                .clip(FolderShape())
                .background(PaletaPasta.degrade(PaletaPasta.Verde)),
        )

        // pasta da frente
        Column(
            Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .clip(FolderShape())
                .background(PaletaPasta.degrade(PaletaPasta.Lima))
                .clickable(onClick = if (itens.isEmpty()) onCriar else onAbrir)
                .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (itens.isEmpty()) {
                Text("Nada agendado por enquanto.", style = MaterialTheme.typography.titleSmall, color = tinta)
                TextButton(onClick = onCriar) {
                    Text("Criar agendamento", color = tinta, fontWeight = FontWeight.Bold)
                }
            } else {
                itens.take(MAX_NA_PILHA).forEach { ag -> LinhaAgendaPasta(ag, hoje) }
                if (itens.size > MAX_NA_PILHA) {
                    Text(
                        "+ " + (itens.size - MAX_NA_PILHA) + " na agenda",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = tinta,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaAgendaPasta(ag: AgendamentoComAluno, hoje: LocalDate) {
    val tinta = PaletaPasta.Preto
    val tipo = runCatching { (TipoAgendamento.entries.firstOrNull { it.name == ag.tipo } ?: TipoAgendamento.TREINO) }.getOrNull()
    val data = runCatching { LocalDate.parse(ag.data) }.getOrNull()
    val quando = data?.let { Saudacao.rotuloData(it, hoje) }
    val icone = when (tipo) {
        TipoAgendamento.AVALIACAO -> Icons.Filled.Assessment
        TipoAgendamento.TREINO -> Icons.Filled.FitnessCenter
        else -> Icons.Filled.Person
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tinta),
            contentAlignment = Alignment.Center,
        ) { Icon(icone, contentDescription = null, tint = PaletaPasta.Lima, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(
                ag.nomeAluno,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(quando, tipo?.rotulo ?: ag.tipo).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = tinta.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            ag.horario,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tinta,
        )
    }
}

/** Aniversariante no formato pasta: foto, dia em disco preto, nome, idade e proximidade. */
@Composable
fun CartaoAniversarioPasta(
    aluno: Aluno,
    nascimento: LocalDate,
    hoje: LocalDate,
    foto: File?,
    cor: Color,
    modifier: Modifier = Modifier,
) {
    val tinta = PaletaPasta.tinta(cor)
    val situacao = AniversarioRules.situacao(nascimento, hoje)
    val passou = situacao == SituacaoAniversario.JA_PASSOU
    val dia = AniversarioRules.aniversarioEm(nascimento, hoje.year).dayOfMonth

    Column(
        modifier
            .width(168.dp)
            .alpha(if (passou) 0.7f else 1f)
            .clip(FolderShape())
            .background(PaletaPasta.degrade(cor))
            .padding(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            AvatarAluno(aluno.nome, foto, tamanho = 56.dp)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(tinta),
                contentAlignment = Alignment.Center,
            ) {
                Text(dia.toString(), style = MaterialTheme.typography.labelMedium, color = if (tinta == PaletaPasta.Preto) cor else PaletaPasta.Preto, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            aluno.nome,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tinta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            AniversarioRules.descricaoIdade(nascimento, hoje),
            style = MaterialTheme.typography.bodySmall,
            color = PaletaPasta.tintaSuave(cor),
            maxLines = 1,
        )
        Text(
            AniversarioRules.rotuloProximidade(nascimento, hoje),
            style = MaterialTheme.typography.labelLarge,
            color = tinta,
            maxLines = 1,
        )
    }
}
