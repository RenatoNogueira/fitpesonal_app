package br.com.fitpersonal.app.ui.common

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Fonte do desfoque: o conteúdo que fica "atrás do vidro" é gravado numa camada gráfica e o vidro desenha
 * uma cópia borrada dela. O conteúdo e o vidro ficam em irmãos dentro do mesmo Box.
 */
class FundoDesfocavel internal constructor(internal val camada: GraphicsLayer) {
    /** Posição (na raiz) do conteúdo gravado; o vidro usa para achar o trecho que está atrás dele. */
    internal var origem by mutableStateOf(Offset.Zero)

    /** Sobe a cada nova gravação para o vidro redesenhar (só o vidro lê; quem grava nunca lê). */
    internal var versao by mutableIntStateOf(0)
    /** Ligado se a gravação/desfoque falhar neste aparelho: dali em diante o vidro usa só as camadas de cor e luz. */
    @Volatile internal var desativado = false

    internal fun desativar(e: Throwable) {
        desativado = true
        br.com.fitpersonal.app.core.Falhas.registrar(e, contexto = "vidro: desfoque desativado")
    }

    private var contador = 0
    internal fun avisarNovaGravacao() {
        contador++
        versao = contador
    }
}

@Composable
fun rememberFundoDesfocavel(): FundoDesfocavel {
    val camada = rememberGraphicsLayer()
    return remember(camada) { FundoDesfocavel(camada) }
}

/** Marca o conteúdo que o vidro vai desfocar: desenha normalmente e grava uma cópia na camada. */
fun Modifier.fonteDeFundo(fundo: FundoDesfocavel): Modifier = this
    .onGloballyPositioned { fundo.origem = it.positionInRoot() }
    .drawWithContent {
        if (fundo.desativado) {
            drawContent()
        } else {
            val tamanho = IntSize(size.width.toInt(), size.height.toInt())
            val gravou = runCatching {
                fundo.camada.record(this, layoutDirection, tamanho) { this@drawWithContent.drawContent() }
                drawLayer(fundo.camada)
            }.onFailure { fundo.desativar(it) }.isSuccess
            if (gravou) fundo.avisarNovaGravacao() else drawContent()
        }
    }

/** O desfoque de camadas exige Android 12 (API 31); antes disso o vidro usa só as camadas de cor e luz. */
private val DESFOQUE_DISPONIVEL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Textura de vidro (estilo "liquid glass"):
 *  - com [fundo] (Android 12+): o que está atrás do vidro aparece desfocado, como vidro fosco de verdade;
 *  - corpo translúcido com degradê vertical e dois reflexos de cor (lima e turquesa);
 *  - brilho da face superior e borda de luz mais forte no canto superior esquerdo;
 *  - sombra suave e baixa, para o vidro parecer suspenso.
 * Funciona em tema claro e escuro.
 */
@Composable
fun Modifier.vidro(
    forma: Shape,
    elevacao: Dp = 14.dp,
    escuro: Boolean = isSystemInDarkTheme(),
    fundo: FundoDesfocavel? = null,
): Modifier {
    val desfocar = fundo != null && DESFOQUE_DISPONIVEL
    // Com desfoque o corpo fica mais transparente para o fundo aparecer; sem ele, mais opaco para ler bem.
    val a1 = if (desfocar) 0.58f else 0.90f
    val a2 = if (desfocar) 0.46f else 0.70f
    val corpoTopo = if (escuro) Color(0xFF232A1F).copy(alpha = a1) else Color.White.copy(alpha = a1)
    val corpoBase = if (escuro) Color(0xFF151A12).copy(alpha = a2) else Color(0xFFF1F5E8).copy(alpha = a2)
    val luz = if (escuro) 0.22f else 0.95f
    val sombra = if (escuro) Color.Black else Color(0xFF5B6B1F)

    val borrada = rememberGraphicsLayer()
    var posicao by remember { mutableStateOf(Offset.Zero) }

    return this
        .shadow(elevacao, forma, ambientColor = sombra.copy(alpha = 0.35f), spotColor = sombra.copy(alpha = 0.45f))
        .clip(forma)
        .onGloballyPositioned { posicao = it.positionInRoot() }
        .drawWithContent {
            if (desfocar && fundo != null && !fundo.desativado) {
                fundo.versao   // lido de propósito: redesenha quando o conteúdo de trás muda
                runCatching {
                    val raio = 26.dp.toPx()
                    borrada.renderEffect = BlurEffect(raio, raio, TileMode.Clamp)
                    val tamanho = IntSize(size.width.toInt(), size.height.toInt())
                    borrada.record(this, layoutDirection, tamanho) {
                        translate(fundo.origem.x - posicao.x, fundo.origem.y - posicao.y) { drawLayer(fundo.camada) }
                    }
                    drawLayer(borrada)
                }.onFailure { fundo.desativar(it) }
            }
            // corpo translúcido por cima do desfoque
            drawRect(Brush.verticalGradient(listOf(corpoTopo, corpoBase)))
            // reflexos de cor dentro do vidro
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFD9F24F).copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 1.05f),
                    radius = size.width * 0.55f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFF34D1BF).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.5f,
                ),
            )
            // brilho da face superior (antes do conteúdo, para não esmaecer ícones e textos)
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (escuro) 0.14f else 0.55f),
                    0.5f to Color.White.copy(alpha = 0f),
                    endY = size.height,
                ),
            )
            drawContent()
        }
        .border(
            1.2.dp,
            Brush.linearGradient(
                0f to Color.White.copy(alpha = luz),
                0.45f to Color.White.copy(alpha = luz * 0.12f),
                1f to Color.White.copy(alpha = luz * 0.45f),
            ),
            forma,
        )
}
