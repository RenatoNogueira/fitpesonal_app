package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.fitpersonal.app.ui.theme.Dimens
import br.com.fitpersonal.app.ui.theme.Paleta

/** Título de seção com contagem/detalhe opcional à direita. */
@Composable
fun TituloSecao(
    titulo: String,
    modifier: Modifier = Modifier,
    detalhe: String? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        if (detalhe != null) {
            Text(detalhe, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Cartão claro com borda sutil: agrupa campos de formulário ou blocos de informação. */
@Composable
fun CartaoSecao(
    modifier: Modifier = Modifier,
    titulo: String? = null,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(Dimens.Cartao),
            verticalArrangement = Arrangement.spacedBy(Dimens.Item),
        ) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleMedium)
            conteudo()
        }
    }
}

/** Indicador numérico com ícone em círculo tonal (KPI do início). */
@Composable
fun IndicadorKpi(
    icone: ImageVector,
    valor: String,
    rotulo: String,
    modifier: Modifier = Modifier,
    fundo: Color = MaterialTheme.colorScheme.primaryContainer,
    conteudo: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = fundo, contentColor = conteudo),
    ) {
        Column(Modifier.padding(Dimens.Cartao), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(conteudo.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text(valor, style = MaterialTheme.typography.headlineMedium)
            Text(rotulo, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Estado vazio amigável: ícone grande, título e uma frase de apoio. */
@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    texto: String? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (texto != null) {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Campo de busca arredondado, com botão de limpar. */
@Composable
fun CampoBusca(
    valor: String,
    onValorChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        modifier = modifier.fillMaxWidth(),
        // Uma linha só: texto longo termina em "…" em vez de cortar ou crescer o campo.
        placeholder = { Text(placeholder, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        textStyle = MaterialTheme.typography.bodyLarge,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { onValorChange("") }) { Icon(Icons.Default.Clear, "Limpar busca") }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/**
 * Item de lista em cartão clicável: [inicio] (avatar/horário), título, subtítulo e [fim] (chip/ação).
 */
@Composable
fun CartaoItem(
    onClick: () -> Unit,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    inicio: (@Composable () -> Unit)? = null,
    fim: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.Cartao, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Item),
        ) {
            if (inicio != null) inicio()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (fim != null) fim()
        }
    }
}

enum class TomChip { Neutro, Marca, Sucesso, Atencao, Erro }

/** Etiqueta compacta de status (Pendente, Concluído, alertas...). */
@Composable
fun ChipStatus(texto: String, tom: TomChip = TomChip.Neutro, modifier: Modifier = Modifier) {
    val escuro = br.com.fitpersonal.app.ui.theme.ehEscuro()
    val cs = MaterialTheme.colorScheme
    val (fundo, letra) = when (tom) {
        TomChip.Neutro -> cs.surfaceVariant to cs.onSurfaceVariant
        TomChip.Marca -> cs.primaryContainer to cs.onPrimaryContainer
        TomChip.Atencao -> cs.tertiaryContainer to cs.onTertiaryContainer
        TomChip.Erro -> cs.errorContainer to cs.onErrorContainer
        TomChip.Sucesso ->
            if (escuro) Paleta.SucessoFundoEscuro to Paleta.SucessoFundo else Paleta.SucessoFundo to Paleta.Sucesso
    }
    Surface(modifier = modifier, shape = CircleShape, color = fundo, contentColor = letra) {
        Text(
            texto,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Indicador de etapas numerado (formulários em passos); tocar numa etapa navega até ela. */
@Composable
fun IndicadorEtapas(
    atual: Int,
    titulos: List<String>,
    onIr: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        titulos.forEachIndexed { i, titulo ->
            val ativo = i == atual
            val feito = i < atual
            Column(
                Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .clickable { onIr(i) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (ativo || feito) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (feito) {
                        Icon(
                            Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ativo) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ativo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
