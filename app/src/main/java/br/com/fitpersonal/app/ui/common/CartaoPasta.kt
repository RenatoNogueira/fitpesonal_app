package br.com.fitpersonal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Cores vivas dos cartões "pasta" (texto preto por cima, nos temas claro e escuro). */
object PaletaPasta {
    val Lima = Color(0xFFD9F24F)
    val Verde = Color(0xFF8CEB5E)
    val Coral = Color(0xFFFF7A5C)
    val Teal = Color(0xFF34D1BF)
    val Rosa = Color(0xFFFFA3C7)
    val Preto = Color(0xFF0A0D08)

    /** Ponta mais clara do degradê (30% de branco). */
    fun claro(cor: Color): Color = androidx.compose.ui.graphics.lerp(cor, Color.White, 0.30f)

    /** Ponta mais escura do degradê (14% de preto). */
    fun escuro(cor: Color): Color = androidx.compose.ui.graphics.lerp(cor, Color.Black, 0.14f)

    /** Degradê diagonal claro → cor → escuro, para fundos de cartões e cabeçalhos. */
    fun degrade(cor: Color): androidx.compose.ui.graphics.Brush =
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(claro(cor), cor, escuro(cor)))

    private fun contraste(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    /**
     * Cor de texto (preto ou branco) com o MAIOR contraste mínimo sobre as três cores do degradê de [cor]:
     * a leitura fica boa em qualquer ponto do cartão, não só no meio.
     */
    fun tinta(cor: Color): Color {
        val pontos = listOf(claro(cor), cor, escuro(cor))
        val comPreto = pontos.minOf { contraste(Preto, it) }
        val comBranco = pontos.minOf { contraste(Color.White, it) }
        return if (comPreto >= comBranco) Preto else Color.White
    }

    /** Texto secundário: a mesma tinta, um pouco mais suave (sem perder o contraste mínimo de leitura). */
    fun tintaSuave(cor: Color): Color = tinta(cor).copy(alpha = 0.82f)
}

/**
 * Forma de "pasta": retângulo arredondado com a aba alta à esquerda e a borda direita mais baixa,
 * ligadas por uma curva suave.
 */
class FolderShape(
    private val raio: Dp = 22.dp,
    private val recorte: Dp = 18.dp,
    private val fracaoAba: Float = 0.58f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { raio.toPx() }.coerceAtMost(size.height / 3f)
        val n = with(density) { recorte.toPx() }.coerceAtMost(size.height / 3f)
        val w = size.width
        val h = size.height
        val xAba = (w * fracaoAba).coerceIn(r * 2f, w - r * 3f)

        val p = Path().apply {
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, 2f * r, 2f * r), 180f, 90f, false)           // canto superior esquerdo
            lineTo(xAba - r, 0f)
            cubicTo(xAba, 0f, xAba, n, xAba + r, n)                          // degrau suave
            lineTo(w - r, n)
            arcTo(Rect(w - 2f * r, n, w, n + 2f * r), 270f, 90f, false)      // canto superior direito
            lineTo(w, h - r)
            arcTo(Rect(w - 2f * r, h - 2f * r, w, h), 0f, 90f, false)       // canto inferior direito
            lineTo(r, h)
            arcTo(Rect(0f, h - 2f * r, 2f * r, h), 90f, 90f, false)         // canto inferior esquerdo
            close()
        }
        return Outline.Generic(p)
    }
}

/** Indicador do início no estilo pasta: ícone em disco preto, valor grande e rótulo. */
@Composable
fun KpiPasta(
    icone: ImageVector,
    valor: String,
    rotulo: String,
    fundo: Color,
    modifier: Modifier = Modifier,
) {
    val tinta = PaletaPasta.tinta(fundo)
    Column(
        modifier
            .clip(FolderShape())
            .background(PaletaPasta.degrade(fundo))
            .padding(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tinta),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = if (tinta == PaletaPasta.Preto) fundo else PaletaPasta.Preto, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            valor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tinta,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = PaletaPasta.tintaSuave(fundo),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Etiqueta preta com texto branco, para usar sobre os cartões coloridos. */
@Composable
fun EtiquetaPreta(texto: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), color = PaletaPasta.Preto) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
        )
    }
}

/**
 * Item de lista no estilo pasta: fundo colorido, texto preto, [inicio] à esquerda (avatar, horário…)
 * e [fim] à direita (etiquetas e ações). Ícones e botões dentro herdam a cor preta.
 */
@Composable
fun CartaoPastaItem(
    cor: Color,
    onClick: () -> Unit,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    inicio: @Composable () -> Unit = {},
    fim: @Composable ColumnScope.() -> Unit = {},
) {
    val base = androidx.compose.ui.graphics.lerp(cor, Color.White, 0.25f)
    val tinta = PaletaPasta.tinta(base)
    CompositionLocalProvider(LocalContentColor provides tinta) {
        Row(
            modifier
                .fillMaxWidth()
                .clip(FolderShape(raio = 20.dp, recorte = 14.dp, fracaoAba = 0.5f))
                .background(PaletaPasta.degrade(base))
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = 10.dp, top = 22.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            inicio()
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tinta,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = PaletaPasta.tintaSuave(base),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp), content = fim)
        }
    }
}
