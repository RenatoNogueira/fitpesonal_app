package br.com.fitpersonal.app.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import br.com.fitpersonal.app.domain.model.Aluno
import br.com.fitpersonal.app.domain.model.Pagamento
import br.com.fitpersonal.app.domain.rules.FinanceiroRules
import br.com.fitpersonal.app.domain.rules.MensagemRules
import br.com.fitpersonal.app.domain.rules.SituacaoPagamento
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.TomChip
import br.com.fitpersonal.app.ui.common.WhatsApp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DIA_MES = DateTimeFormatter.ofPattern("dd/MM")

fun tomPagamento(s: SituacaoPagamento): TomChip = when (s) {
    SituacaoPagamento.ATRASADO -> TomChip.Erro
    SituacaoPagamento.A_VENCER -> TomChip.Atencao
    SituacaoPagamento.EM_DIA -> TomChip.Sucesso
    SituacaoPagamento.SEM_MENSALIDADE -> TomChip.Neutro
}

/** Mensalidade do aluno: situação, registrar pagamento, cobrar por WhatsApp e editar valor/vencimento. */
@Composable
fun CartaoMensalidade(
    aluno: Aluno,
    pagamentos: List<Pagamento>,
    profissional: String,
    perfil: br.com.fitpersonal.app.domain.model.PerfilProfissional,
    onSalvar: (Long?, Int?) -> Unit,
    onRegistrar: (YearMonth, Long) -> Unit,
    onExcluirPagamento: (Long) -> Unit,
    onRecibo: (Long) -> Unit,
    onMensagem: (String) -> Unit,
) {
    val ctx = LocalContext.current
    var editando by remember { mutableStateOf(false) }
    var mostrandoQr by remember { mutableStateOf(false) }
    val hoje = LocalDate.now()
    val valor = aluno.mensalidadeCentavos
    val resumo = FinanceiroRules.resumir(
        valor, aluno.diaVencimento, aluno.dataCadastro, pagamentos.map { it.competencia }.toSet(), hoje,
    )
    val competencia = resumo.competenciaParaPagar(hoje)

    CartaoSecao(titulo = "Mensalidade") {
        if (valor == null || aluno.diaVencimento == null) {
            Text(
                "Sem mensalidade cadastrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(FinanceiroRules.formatarReais(valor), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Vence todo dia ${aluno.diaVencimento}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ChipStatus(
                    if (resumo.situacao == SituacaoPagamento.ATRASADO) {
                        "${resumo.atrasadas.size} em atraso"
                    } else resumo.situacao.rotulo,
                    tomPagamento(resumo.situacao),
                )
            }

            if (competencia != null) {
                Button(
                    onClick = { onRegistrar(competencia, valor) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Registrar pagamento de ${FinanceiroRules.rotuloCompetencia(competencia)}") }
            }
            if (resumo.situacao == SituacaoPagamento.ATRASADO) {
                OutlinedButton(
                    onClick = {
                        val pix = br.com.fitpersonal.app.domain.rules.PixRules.cobranca(
                            perfil.chavePix, valor, perfil.nome, perfil.cidade,
                        )
                        val msg = MensagemRules.cobranca(aluno.nome, resumo.atrasadas.first(), valor, profissional, pix)
                        if (!WhatsApp.enviar(ctx, aluno.telefone, msg)) {
                            onMensagem("Cadastre um telefone válido ou instale o WhatsApp para cobrar.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cobrar por WhatsApp") }
            }

            // QR Code do Pix: só aparece com chave Pix cadastrada e algo a receber (mês a pagar ou em atraso)
            val pixQr = br.com.fitpersonal.app.domain.rules.PixRules.cobranca(
                perfil.chavePix, valor, perfil.nome, perfil.cidade,
            )
            if (pixQr != null && (competencia != null || resumo.situacao == SituacaoPagamento.ATRASADO)) {
                OutlinedButton(onClick = { mostrandoQr = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("QR Code Pix")
                }
            }
            if (mostrandoQr && pixQr != null) {
                PixQrDialog(
                    pix = pixQr,
                    valorCentavos = valor,
                    nomeAluno = aluno.nome,
                    onFechar = { mostrandoQr = false },
                    onMensagem = onMensagem,
                )
            }

            pagamentos.take(3).forEach { p ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 0.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            FinanceiroRules.rotuloCompetencia(p.competencia),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            FinanceiroRules.formatarReais(p.valorCentavos),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Text(
                        "Pago em ${p.pagoEm.format(DIA_MES)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onRecibo(p.id) }) { Text("Recibo", maxLines = 1) }
                        TextButton(onClick = { onExcluirPagamento(p.id) }) { Text("Desfazer", maxLines = 1) }
                    }
                }
            }
        }

        OutlinedButton(onClick = { editando = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (valor == null) "Definir mensalidade" else "Editar mensalidade")
        }
    }

    if (editando) {
        DialogoMensalidade(
            valorAtual = valor,
            diaAtual = aluno.diaVencimento,
            onCancelar = { editando = false },
            onSalvar = { c, d -> editando = false; onSalvar(c, d) },
        )
    }
}

@Composable
private fun DialogoMensalidade(
    valorAtual: Long?,
    diaAtual: Int?,
    onCancelar: () -> Unit,
    onSalvar: (Long?, Int?) -> Unit,
) {
    var valor by remember {
        mutableStateOf(valorAtual?.let { String.format(java.util.Locale("pt", "BR"), "%d,%02d", it / 100, it % 100) }.orEmpty())
    }
    var dia by remember { mutableStateOf(diaAtual?.toString().orEmpty()) }

    val centavos = FinanceiroRules.parseReais(valor)
    val diaNum = dia.toIntOrNull()?.takeIf { it in 1..31 }
    val vazio = valor.isBlank() && dia.isBlank()
    val valido = vazio || (centavos != null && diaNum != null)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Mensalidade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    isError = valor.isNotBlank() && centavos == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dia,
                    onValueChange = { dia = it.filter(Char::isDigit).take(2) },
                    label = { Text("Dia do vencimento (1 a 31)") },
                    singleLine = true,
                    isError = dia.isNotBlank() && diaNum == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Deixe os dois campos vazios para remover a cobrança.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = { onSalvar(if (vazio) null else centavos, if (vazio) null else diaNum) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
