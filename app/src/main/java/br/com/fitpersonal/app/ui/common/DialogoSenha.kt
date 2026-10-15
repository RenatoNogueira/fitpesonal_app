package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Diálogo de senha. [onConfirmar] devolve uma mensagem de erro para exibir (e manter o diálogo aberto)
 * ou null quando aceitou. Com [confirmar] = true pede a senha duas vezes.
 */
@Composable
fun DialogoSenha(
    titulo: String,
    aviso: String,
    confirmar: Boolean,
    textoConfirmar: String,
    onCancelar: () -> Unit,
    onConfirmar: (senha: String, confirmacao: String) -> String?,
) {
    var senha by remember { mutableStateOf("") }
    var conf by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(aviso, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it; erro = null },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = erro != null && !confirmar,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (confirmar) {
                    OutlinedTextField(
                        value = conf,
                        onValueChange = { conf = it; erro = null },
                        label = { Text("Confirmar senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = senha.isNotEmpty(),
                onClick = { erro = onConfirmar(senha, conf) },
            ) { Text(textoConfirmar) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
