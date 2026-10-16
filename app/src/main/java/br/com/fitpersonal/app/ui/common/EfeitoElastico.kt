package br.com.fitpersonal.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** Quanto do arrasto do dedo vira deslocamento na tela (quanto menor, mais "pesado" o elástico). */
private const val RESISTENCIA = 0.32f
private const val LIMITE_PX = 700f

/** Mola do retorno: leve quique, como o "rubber-banding" do iOS. */
private val MolaElastica = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

/**
 * Desliga o esticar padrão do Android nas listas debaixo deste bloco; o [efeitoElastico] faz o
 * papel dele. Sem isso o Android consome a sobra do gesto e o elástico nunca recebe nada.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SemOverscrollPadrao(conteudo: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollConfiguration provides null, content = conteudo)
}

/**
 * Elástico vertical: ao puxar além do começo ou do fim de qualquer lista rolável dentro do bloco,
 * o conteúdo acompanha o dedo com resistência e volta com um quique ao soltar.
 */
@Composable
fun Modifier.efeitoElastico(): Modifier {
    val desloc = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    val conexao = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atual = desloc.value
                if (atual == 0f || source != NestedScrollSource.UserInput) return Offset.Zero
                // Já está esticado: o dedo primeiro devolve o elástico, depois a lista volta a rolar.
                val novo = atual + available.y
                return if (atual * novo <= 0f) {
                    escopo.launch(start = CoroutineStart.UNDISPATCHED) { desloc.snapTo(0f) }
                    Offset(0f, -atual)
                } else {
                    escopo.launch(start = CoroutineStart.UNDISPATCHED) { desloc.snapTo(novo) }
                    Offset(0f, available.y)
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val novo = (desloc.value + available.y).coerceIn(-LIMITE_PX, LIMITE_PX)
                escopo.launch(start = CoroutineStart.UNDISPATCHED) { desloc.snapTo(novo) }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (desloc.value == 0f) return Velocity.Zero
                desloc.animateTo(0f, MolaElastica)
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Arremesso que chegou ao fim da lista: dá um pequeno empurrão e volta.
                desloc.animateTo(0f, MolaElastica, initialVelocity = available.y * 0.12f)
                return available
            }
        }
    }
    return this
        .nestedScroll(conexao)
        .graphicsLayer { translationY = desloc.value * RESISTENCIA }
}

/**
 * Estado do arrasto horizontal que mostra o gesto de troca de aba: a tela acompanha o dedo com
 * resistência e volta com quique.
 */
class ArrastoHorizontal internal constructor(private val valor: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>) {
    val px: Float get() = valor.value * RESISTENCIA
    internal suspend fun definir(v: Float) = valor.snapTo(v.coerceIn(-LIMITE_PX, LIMITE_PX))
    internal suspend fun soltar() { valor.animateTo(0f, MolaElastica) }
    internal suspend fun zerar() = valor.snapTo(0f)
}

@Composable
fun rememberArrastoHorizontal(): ArrastoHorizontal = remember { ArrastoHorizontal(Animatable(0f)) }

fun Modifier.acompanharArrasto(arrasto: ArrastoHorizontal): Modifier =
    graphicsLayer { translationX = arrasto.px }
