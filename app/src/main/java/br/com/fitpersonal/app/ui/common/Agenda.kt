package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.model.StatusAgendamento

/** Cor do chip conforme o status do agendamento. */
fun tomDoStatus(status: StatusAgendamento): TomChip = when (status) {
    StatusAgendamento.PENDENTE -> TomChip.Atencao
    StatusAgendamento.CONCLUIDO -> TomChip.Sucesso
    StatusAgendamento.CANCELADO -> TomChip.Neutro
    StatusAgendamento.FALTOU -> TomChip.Erro
}

/** Bloco com o horário (e uma legenda como "Hoje") à esquerda do cartão de agendamento. */
@Composable
fun BlocoHorario(horario: String, legenda: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .widthIn(min = 64.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            horario,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (legenda != null) {
            Text(
                legenda,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
