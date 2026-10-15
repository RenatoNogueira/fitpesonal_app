package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Cabeçalho das telas principais no estilo "pasta" (mesma forma dos cartões): fundo colorido com a aba
 * alta à esquerda, título e subtítulo em preto, [acoes] à direita e, opcionalmente, pílulas ou campos
 * logo abaixo ([conteudo]). Textos e ícones dentro herdam a cor preta.
 */
@Composable
fun CabecalhoTela(
    titulo: String,
    subtitulo: String?,
    modifier: Modifier = Modifier,
    cor: Color = PaletaPasta.Lima,
    acoes: @Composable RowScope.() -> Unit = {},
    conteudo: @Composable ColumnScope.() -> Unit = {},
) {
    val tinta = PaletaPasta.tinta(cor)
    CompositionLocalProvider(LocalContentColor provides tinta) {
        Column(
            modifier
                .fillMaxWidth()
                .clip(FolderShape(raio = 26.dp, recorte = 20.dp, fracaoAba = 0.6f))
                .background(PaletaPasta.degrade(cor))
                .padding(start = 20.dp, end = 12.dp, top = 30.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tinta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaletaPasta.tintaSuave(cor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                acoes()
            }
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = conteudo)
        }
    }
}

/** Botão redondo preto com o ícone na cor da marca, para as ações do cabeçalho. */
@Composable
fun AcaoCabecalho(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(PaletaPasta.Preto)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = descricao, tint = PaletaPasta.Lima, modifier = Modifier.size(22.dp))
    }
}

/** Linha rolável de pílulas para usar dentro do [CabecalhoTela]. */
@Composable
fun LinhaPilulas(itens: List<String>, modifier: Modifier = Modifier) {
    if (itens.isEmpty()) return
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itens.forEach { PilulaResumo(it) }
    }
}

/** Pílula escura translúcida (texto preto) para uso sobre o fundo colorido do cabeçalho. */
@Composable
fun PilulaResumo(texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = PaletaPasta.Preto.copy(alpha = 0.12f),
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = PaletaPasta.Preto,
            maxLines = 1,
        )
    }
}

/** Item de lista com uma barra colorida à esquerda (estado/categoria) que acompanha a altura do cartão. */
@Composable
fun ItemComAcento(
    cor: Color,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 3.dp, bottomEnd = 3.dp))
                .background(cor),
        )
        Box(Modifier.weight(1f).padding(start = 6.dp)) { conteudo() }
    }
}

/** Botão de ação em cartão colorido (ícone em círculo + rótulo), usado em atalhos de ficha e início. */
@Composable
fun AcaoCartao(
    icone: ImageVector,
    rotulo: String,
    onClick: () -> Unit,
    fundo: Color,
    sobre: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = fundo, contentColor = sobre),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(sobre.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = sobre)
            }
            Text(rotulo, style = MaterialTheme.typography.labelLarge, color = sobre)
        }
    }
}

/** Grupo de conteúdo que aparece/some com fade e expansão (usado ao trocar de aba). */
@Composable
fun GrupoAnimado(visivel: Boolean, conteudo: @Composable ColumnScope.() -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visivel,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160)) +
            androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(180)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) +
            androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(150)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = conteudo)
    }
}
