package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.fitpersonal.app.domain.model.Aluno

/** Seletor de aluno em campo arredondado (usado em Avaliações e Treinos). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeletorAluno(
    alunos: List<Aluno>,
    selecionadoId: Long?,
    onSelecionar: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = it }, modifier = modifier) {
        OutlinedTextField(
            value = alunos.firstOrNull { it.id == selecionadoId }?.nome.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Aluno") },
            placeholder = { Text("Selecione um aluno") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(aberto) },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            alunos.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.nome) },
                    onClick = { onSelecionar(a.id); aberto = false },
                )
            }
        }
    }
}
