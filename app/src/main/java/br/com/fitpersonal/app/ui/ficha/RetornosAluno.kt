package br.com.fitpersonal.app.ui.ficha

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.data.contrato.PreviaResumo
import br.com.fitpersonal.app.data.local.RetornoAlunoEntity
import br.com.fitpersonal.app.ui.common.CartaoItem
import br.com.fitpersonal.app.ui.common.CartaoSecao
import br.com.fitpersonal.app.ui.common.ChipStatus
import br.com.fitpersonal.app.ui.common.TomChip
import java.util.Locale

/** Retornos do aluno (resumos importados) e a entrada para importar um novo. */
@Composable
fun CartaoRetornos(
    retornos: List<RetornoAlunoEntity>,
    previa: PreviaResumo?,
    onAnalisar: (String) -> Unit,
    onImportar: () -> Unit,
    onLimpar: () -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }

    CartaoSecao(titulo = "Retornos do aluno") {
        if (retornos.isEmpty()) {
            Text(
                "Nenhum resumo importado. O aluno envia um código ou arquivo pelo app FitPersonal Treino ou pela página do treino.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            retornos.take(3).forEach { r ->
                CartaoItem(
                    onClick = {},
                    titulo = "${r.de} a ${r.ate}",
                    subtitulo = listOfNotNull(
                        "${r.sessoes} sessão(ões)",
                        r.esforcoMedio?.let { String.format(Locale("pt", "BR"), "esforço médio %.1f", it) },
                        r.mensagem,
                    ).joinToString(" • "),
                )
            }
        }
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Importar resumo do aluno")
        }
    }

    if (aberto) {
        DialogoImportarResumo(
            previa = previa,
            onAnalisar = onAnalisar,
            onImportar = { onImportar(); aberto = false },
            onFechar = { aberto = false; onLimpar() },
        )
    }
}

@Composable
private fun DialogoImportarResumo(
    previa: PreviaResumo?,
    onAnalisar: (String) -> Unit,
    onImportar: () -> Unit,
    onFechar: () -> Unit,
) {
    val ctx = LocalContext.current
    var texto by remember { mutableStateOf("") }

    val escolherArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val lido = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (lido != null) { texto = lido; onAnalisar(lido) }
        }
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Importar resumo") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Cole o código ou a mensagem do aluno") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAnalisar(texto) }, enabled = texto.isNotBlank()) { Text("Analisar") }
                    OutlinedButton(onClick = { escolherArquivo.launch(arrayOf("*/*")) }) { Text("Escolher arquivo") }
                }
                when (previa) {
                    null -> Unit
                    is PreviaResumo.Erro -> Text(previa.mensagem, color = MaterialTheme.colorScheme.error)
                    is PreviaResumo.Ok -> {
                        Text(
                            "Período: ${previa.resumo.de} a ${previa.resumo.ate}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${previa.sessoesNovas} sessão(ões) nova(s)" +
                                if (previa.sessoesJaImportadas > 0) " • ${previa.sessoesJaImportadas} já importada(s)" else "",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (previa.nomeCargasNovas > 0) {
                            Text("${previa.nomeCargasNovas} carga(s) vão para o histórico de progressão.")
                        }
                        previa.resumo.mensagem?.let {
                            Text("Mensagem do aluno: \"$it\"", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (previa.resumoJaImportado && previa.sessoesNovas == 0) {
                            ChipStatus("Já importado antes", TomChip.Neutro)
                        }
                        if (!previa.confere) {
                            Text(
                                "Atenção: o identificador do aluno neste resumo não confere com o desta ficha " +
                                    "(o treino pode ter sido enviado a outro aluno). Importe só se tiver certeza.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val pode = previa is PreviaResumo.Ok && (previa.sessoesNovas > 0 || !previa.resumoJaImportado)
            TextButton(enabled = pode, onClick = onImportar) { Text("Importar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
    )
}
