package br.com.fitpersonal.app.ui.ficha

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.domain.model.Agendamento
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.domain.rules.PacoteRules
import br.com.fitpersonal.app.domain.rules.SituacaoPacote
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.common.WhatsApp
import java.time.format.DateTimeFormatter

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun tomPacote(s: SituacaoPacote): TomChip = when (s) {
    SituacaoPacote.ESGOTADO -> TomChip.Erro
    SituacaoPacote.ULTIMA_SESSAO -> TomChip.Atencao
    SituacaoPacote.EM_USO -> TomChip.Sucesso
    SituacaoPacote.SEM_PACOTE -> TomChip.Neutro
}

/** Pacote de sessões do aluno: saldo, aviso por WhatsApp e renovação. */
@Composable
fun CartaoPacote(
    aluno: Aluno,
    agendamentos: List<Agendamento>,
    profissional: String,
    onSalvar: (Int?) -> Unit,
    onMensagem: (String) -> Unit,
) {
    val ctx = LocalContext.current
    var editando by remember { mutableStateOf(false) }
    val r = PacoteRules.resumir(aluno.pacoteSessoes, aluno.pacoteInicio, agendamentos)

    CartaoSecao(titulo = "Pacote de sessões") {
        if (r.situacao == SituacaoPacote.SEM_PACOTE) {
            Text(
                "Sem pacote. Defina um pacote (ex.: 12 sessões) para acompanhar o saldo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Saldo: ${r.saldo} de ${r.total}", style = MaterialTheme.typography.titleMedium)
                ChipStatus(r.situacao.rotulo, tomPacote(r.situacao))
            }
            LinearProgressIndicator(
                progress = { (r.usadas.toFloat() / r.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${r.usadas} usada(s) desde ${aluno.pacoteInicio?.format(DATA_BR)}" +
                    if (r.excedentes > 0) " • ${r.excedentes} além do pacote" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Avaliações físicas não consomem o pacote.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (r.precisaAtencao) {
                OutlinedButton(
                    onClick = {
                        val msg = MensagemRules.pacoteAcabando(aluno.nome, r.saldo, profissional)
                        if (!WhatsApp.enviar(ctx, aluno.telefone, msg)) {
                            onMensagem("Cadastre um telefone válido ou instale o WhatsApp.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Avisar renovação por WhatsApp") }
            }
        }
        OutlinedButton(onClick = { editando = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (r.situacao == SituacaoPacote.SEM_PACOTE) "Definir pacote" else "Renovar / alterar pacote")
        }
    }

    if (editando) {
        var escolhido by remember { mutableStateOf(aluno.pacoteSessoes) }
        var outro by remember { mutableStateOf("") }
        val digitado = outro.toIntOrNull()?.takeIf { it in 1..200 }
        val valor = if (outro.isNotBlank()) digitado else escolhido

        AlertDialog(
            onDismissRequest = { editando = false },
            title = { Text("Pacote de sessões") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Um pacote novo começa a ser contado hoje (as sessões já feitas não entram).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = outro.isBlank() && escolhido == null,
                            onClick = { escolhido = null; outro = "" },
                            label = { Text("Sem pacote") },
                        )
                        PacoteRules.OPCOES.forEach { n ->
                            FilterChip(
                                selected = outro.isBlank() && escolhido == n,
                                onClick = { escolhido = n; outro = "" },
                                label = { Text("$n sessões") },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = outro,
                        onValueChange = { outro = it.filter(Char::isDigit).take(3) },
                        label = { Text("Outra quantidade") },
                        singleLine = true,
                        isError = outro.isNotBlank() && digitado == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = outro.isBlank() || digitado != null,
                    onClick = { editando = false; onSalvar(valor) },
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editando = false }) { Text("Cancelar") } },
        )
    }
}
