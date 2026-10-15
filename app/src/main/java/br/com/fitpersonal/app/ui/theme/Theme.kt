package br.com.fitpersonal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

/**
 * Identidade visual do FitPersonal.
 *
 * Paleta da logo FitPersonal: verde-limão sobre preto. Todas as cores ficam aqui.
 */
object Paleta {
    // Marca
    val Verde700 = Color(0xFF3A7D0A)
    val Verde300 = Color(0xFFA3F53A)
    val Verde100 = Color(0xFFE4FBC4)
    val Verde900 = Color(0xFF2A4D0B)
    val Verde950 = Color(0xFF10250A)

    // Apoio
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo900 = Color(0xFF312E81)
    val Amber700 = Color(0xFFB45309)
    val Amber100 = Color(0xFFFEF3C7)
    val Amber300 = Color(0xFFFCD34D)
    val Amber900 = Color(0xFF78350F)

    // Vibrantes (gradientes de destaque e variações de cartões)
    val Verde600 = Color(0xFF3A8208)
    val VerdeVivo600 = Color(0xFF4D9A0F)
    val VerdeProfundo700 = Color(0xFF16300A)
    val Laranja700 = Color(0xFFC2410C)
    val Rosa700 = Color(0xFFBE185D)
    val Violeta700 = Color(0xFF6D28D9)
    val Lima300 = Color(0xFFBEF264)
    val Laranja600 = Color(0xFFEA580C)
    val Rosa600 = Color(0xFFDB2777)
    val Violeta600 = Color(0xFF7C3AED)
    val RosaFundo = Color(0xFFFCE7F3)
    val RosaTexto = Color(0xFF831843)
    val RosaFundoEscuro = Color(0xFF831843)
    val RosaTextoEscuro = Color(0xFFFBCFE8)

    // Estados semânticos usados em chips e alertas
    val Sucesso = Color(0xFF166534)
    val SucessoFundo = Color(0xFFDCFCE7)
    val SucessoFundoEscuro = Color(0xFF14532D)
}

private val ClaroEsquema = lightColorScheme(
    primary = Paleta.Verde600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F99D),
    onPrimaryContainer = Paleta.Verde950,
    secondary = Paleta.Indigo600,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7D2FE),
    onSecondaryContainer = Paleta.Indigo900,
    tertiary = Paleta.Amber700,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDE68A),
    onTertiaryContainer = Paleta.Amber900,
    background = Color(0xFFF1F6EC),
    onBackground = Color(0xFF0F160B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F160B),
    surfaceVariant = Color(0xFFE5EBDD),
    onSurfaceVariant = Color(0xFF4A5642),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FBF6),
    surfaceContainer = Color(0xFFF2F6EE),
    surfaceContainerHigh = Color(0xFFEAF0E4),
    surfaceContainerHighest = Color(0xFFE1E8DA),
    outline = Color(0xFF7C8A72),
    outlineVariant = Color(0xFFCDD7C3),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFCE4E1),
    onErrorContainer = Color(0xFF410E0B),
)

private val EscuroEsquema = darkColorScheme(
    primary = Paleta.Verde300,
    onPrimary = Paleta.Verde950,
    primaryContainer = Paleta.Verde900,
    onPrimaryContainer = Paleta.Verde100,
    secondary = Paleta.Indigo200,
    onSecondary = Paleta.Indigo900,
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Paleta.Amber300,
    onTertiary = Paleta.Amber900,
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Paleta.Amber100,
    background = Color(0xFF0A0D08),
    onBackground = Color(0xFFE6ECE0),
    surface = Color(0xFF11160E),
    onSurface = Color(0xFFE6ECE0),
    surfaceVariant = Color(0xFF263020),
    onSurfaceVariant = Color(0xFFBAC4B0),
    surfaceContainerLowest = Color(0xFF0A0D08),
    surfaceContainerLow = Color(0xFF141A10),
    surfaceContainer = Color(0xFF1A2115),
    surfaceContainerHigh = Color(0xFF222B1C),
    surfaceContainerHighest = Color(0xFF2B3624),
    outline = Color(0xFF8C9980),
    outlineVariant = Color(0xFF3E4A35),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val Base = Typography()

/** Hierarquia mais marcada: títulos em negrito/semibold, rótulos em médio. */
private val FitTipografia = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.Medium),
)

private val FitFormas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Tema escuro em uso (inclui a escolha manual do usuário, não só a do sistema). */
@Composable
fun ehEscuro(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Degradê da marca (verde para verde-escuro) para cabeçalhos e destaques; o texto por cima usa onPrimary. */
@Composable
fun gradienteMarca(): Brush = Brush.linearGradient(
    listOf(Paleta.Verde700, Paleta.VerdeProfundo700),
)

/** Degradê festivo (laranja, rosa, violeta) dos aniversários do dia; o texto por cima é branco. */
@Composable
fun gradienteFesta(): Brush = Brush.linearGradient(
    listOf(Paleta.Laranja700, Paleta.Rosa700, Paleta.Violeta700),
)

/**
 * Cor de texto/ícone com melhor contraste (WCAG) sobre [fundo]: branco ou quase preto.
 * Use sempre que o fundo vier do tema (que muda entre claro e escuro) em vez de fixar Color.White.
 */
fun corSobre(fundo: Color): Color {
    val l = fundo.luminance()
    val comBranco = 1.05f / (l + 0.05f)
    val comPreto = (l + 0.05f) / 0.056f
    return if (comBranco >= comPreto) Color.White else Color(0xFF0A0D08)
}

/** Espaçamentos padrão usados pelos componentes e telas. */
object Dimens {
    val Tela = 16.dp
    val Secao = 20.dp
    val Item = 12.dp
    val Cartao = 16.dp
}

/**
 * @param dynamic cores do papel de parede (Android 12+). Desligado por padrão para manter a identidade da marca.
 */
@Composable
fun FitTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> EscuroEsquema
        else -> ClaroEsquema
    }
    MaterialTheme(colorScheme = scheme, typography = FitTipografia, shapes = FitFormas, content = content)
}
