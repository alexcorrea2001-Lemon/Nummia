package io.github.alexcorrea2001lemon.nummia.ui.theme

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.data.ajustes.Ajustes

/** Colores con significado fijo en toda la app (pilar 4). */
@Immutable
data class ColoresNummia(
    val ingreso: Color,
    val gasto: Color,
    val aviso: Color,
    val avisoFondo: Color,
    val ingresoFondo: Color,
    val heroe: Color,
    val borde: Color,
    val textoSuave: Color,
    val esOscuro: Boolean,
)

val LocalColoresNummia = staticCompositionLocalOf {
    ColoresNummia(Color.Green, Color.Red, Color.Yellow, Color.Black, Color.Black, Color.Black, Color.Gray, Color.Gray, true)
}

/** Tokens de espaciado (múltiplos de 4 dp). */
object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

/** Forma de las tarjetas, a gusto de cada uno (Apariencia). */
enum class FormaTarjetas(val nombre: String, val radio: Int) { REDONDA("Redondas", 28), NORMAL("Normales", 20), RECTA("Rectas", 8) }

private fun formas(f: FormaTarjetas) = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape((f.radio * 0.6f).dp),
    medium = RoundedCornerShape(f.radio.dp),
    large = RoundedCornerShape((f.radio + 8).dp),
    extraLarge = RoundedCornerShape((f.radio + 12).dp),
)

/** Preferencias de la interfaz que leen los componentes (vibración y listas compactas). */
data class PreferenciasUi(val vibracion: Boolean = true, val compacto: Boolean = false, val radio: Int = 20)
val LocalPreferenciasUi = androidx.compose.runtime.staticCompositionLocalOf { PreferenciasUi() }

/** Cifras tabulares en todos los estilos: los céntimos quedan en columna. */
private fun TextStyle.tabular() = copy(fontFeatureSettings = "tnum")

private val tipografia = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.tabular(), displayMedium = t.displayMedium.tabular(), displaySmall = t.displaySmall.tabular(),
        headlineLarge = t.headlineLarge.tabular(), headlineMedium = t.headlineMedium.tabular(), headlineSmall = t.headlineSmall.tabular(),
        titleLarge = t.titleLarge.tabular(), titleMedium = t.titleMedium.tabular(), titleSmall = t.titleSmall.tabular(),
        bodyLarge = t.bodyLarge.tabular(), bodyMedium = t.bodyMedium.tabular(), bodySmall = t.bodySmall.tabular(),
        labelLarge = t.labelLarge.tabular(), labelMedium = t.labelMedium.tabular(), labelSmall = t.labelSmall.tabular(),
    )
}

/** La misma escala de estilos con otra familia de letra (la del sistema es la de [tipografia]). */
internal fun tipografiaDe(letra: TipoLetra): Typography {
    val f = when (letra) {
        TipoLetra.SISTEMA -> return tipografia
        TipoLetra.SERIFA -> FontFamily.Serif
        TipoLetra.MONO -> FontFamily.Monospace
    }
    val t = tipografia
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = f), displayMedium = t.displayMedium.copy(fontFamily = f), displaySmall = t.displaySmall.copy(fontFamily = f),
        headlineLarge = t.headlineLarge.copy(fontFamily = f), headlineMedium = t.headlineMedium.copy(fontFamily = f), headlineSmall = t.headlineSmall.copy(fontFamily = f),
        titleLarge = t.titleLarge.copy(fontFamily = f), titleMedium = t.titleMedium.copy(fontFamily = f), titleSmall = t.titleSmall.copy(fontFamily = f),
        bodyLarge = t.bodyLarge.copy(fontFamily = f), bodyMedium = t.bodyMedium.copy(fontFamily = f), bodySmall = t.bodySmall.copy(fontFamily = f),
        labelLarge = t.labelLarge.copy(fontFamily = f), labelMedium = t.labelMedium.copy(fontFamily = f), labelSmall = t.labelSmall.copy(fontFamily = f),
    )
}

/**
 * Colores de ingreso y de gasto. Con [accesibles] van en azul y naranja (se distinguen sin necesidad de ver el
 * rojo y el verde); todos pasan el contraste AA sobre los fondos de la app.
 */
internal fun coloresMovimiento(oscuro: Boolean, accesibles: Boolean): Pair<Color, Color> = when {
    oscuro && accesibles -> Color(0xFF60A5FA) to Color(0xFFFB923C)
    oscuro -> Color(0xFF4ADE80) to Color(0xFFF87171)
    accesibles -> Color(0xFF1D4ED8) to Color(0xFFC2410C)
    else -> Color(0xFF15803D) to Color(0xFFB91C1C)
}

private fun oscurecer(c: Color, f: Float) = Color.Black.copy(alpha = f).compositeOver(c)
private fun sobre(c: Color) = if (c.luminance() > 0.4f) Color(0xFF16121F) else Color.White

/** Un toque del color de la app sobre una superficie (solo con «Fondo con tu color»). */
private fun tinte(acento: Color, base: Color, alfa: Float, activo: Boolean) = if (activo) acento.copy(alpha = alfa).compositeOver(base) else base

private fun esquemaOscuro(acento: Color, amoled: Boolean, tinte: Boolean = false): ColorScheme {
    // Con AMOLED el fondo sigue siendo negro puro (ahorra batería); el tinte solo llega a las tarjetas.
    val fondo = if (amoled) Color(0xFF000000) else tinte(acento, Color(0xFF0E0F13), 0.05f, tinte)
    // Tarjetas algo más claras que el fondo y textos secundarios más legibles (contraste AA).
    val sup = tinte(acento, if (amoled) Color(0xFF121214) else Color(0xFF1A1C23), 0.07f, tinte)
    val sup2 = tinte(acento, if (amoled) Color(0xFF1C1C20) else Color(0xFF242731), 0.09f, tinte)
    return darkColorScheme(
        primary = acento, onPrimary = sobre(acento),
        primaryContainer = acento.copy(alpha = 0.22f).compositeOver(sup), onPrimaryContainer = Color(0xFFF2EEFF),
        secondary = acento, onSecondary = sobre(acento),
        secondaryContainer = acento.copy(alpha = 0.16f).compositeOver(sup), onSecondaryContainer = Color(0xFFEDEBF5),
        background = fondo, onBackground = Color(0xFFECEDF1),
        surface = fondo, onSurface = Color(0xFFECEDF1),
        surfaceVariant = sup2, onSurfaceVariant = Color(0xFFB3B8C4),
        surfaceContainerLowest = fondo, surfaceContainerLow = sup, surfaceContainer = sup,
        surfaceContainerHigh = sup2, surfaceContainerHighest = sup2,
        outline = Color(0xFF424754), outlineVariant = Color(0xFF2E323C),
        error = Color(0xFFF87171), onError = Color(0xFF2B0A0A),
    )
}

private fun esquemaClaro(acento: Color, tinte: Boolean = false): ColorScheme {
    val texto = oscurecer(acento, 0.38f)
    val fondo = tinte(acento, Color(0xFFF6F6F9), 0.10f, tinte)
    val tarjeta = tinte(acento, Color.White, 0.04f, tinte)
    return lightColorScheme(
        primary = texto, onPrimary = Color.White,
        primaryContainer = acento.copy(alpha = 0.22f).compositeOver(Color.White), onPrimaryContainer = oscurecer(acento, 0.65f),
        secondary = texto, onSecondary = Color.White,
        secondaryContainer = acento.copy(alpha = 0.16f).compositeOver(Color.White), onSecondaryContainer = oscurecer(acento, 0.65f),
        background = fondo, onBackground = Color(0xFF14151A),
        surface = fondo, onSurface = Color(0xFF14151A),
        surfaceVariant = Color(0xFFECEDF2), onSurfaceVariant = Color(0xFF555A66),
        surfaceContainerLowest = tarjeta, surfaceContainerLow = tarjeta, surfaceContainer = tarjeta,
        surfaceContainerHigh = tinte(acento, Color(0xFFF0F1F5), 0.07f, tinte), surfaceContainerHighest = tinte(acento, Color(0xFFE8E9EF), 0.09f, tinte),
        outline = Color(0xFFC9CBD3), outlineVariant = Color(0xFFE2E4EA),
        error = Color(0xFFB91C1C), onError = Color.White,
    )
}

@Composable
fun NummiaTheme(ajustes: Ajustes, contenido: @Composable () -> Unit) {
    val oscuro = when (ajustes.tema) {
        Tema.AUTOMATICO -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO, Tema.AMOLED -> true
    }
    val amoled = ajustes.tema == Tema.AMOLED
    val acento = when (ajustes.modoAcento) {
        ModoAcento.PALETA -> ajustes.acento.color
        ModoAcento.PERSONALIZADO -> Color(ajustes.acentoPersonalizado)
        ModoAcento.FONDO_PANTALLA -> ajustes.acento.color
    }
    val ctx = LocalContext.current
    val esquema = if (ajustes.modoAcento == ModoAcento.FONDO_PANTALLA && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (oscuro) {
            dynamicDarkColorScheme(ctx).let { if (amoled) it.copy(background = Color.Black, surface = Color.Black) else it }
        } else {
            dynamicLightColorScheme(ctx)
        }
    } else if (oscuro) {
        esquemaOscuro(acento, amoled, ajustes.tinteFondo)
    } else {
        esquemaClaro(acento, ajustes.tinteFondo)
    }
    val (ingreso, gasto) = coloresMovimiento(oscuro, ajustes.coloresAccesibles)
    val colores = if (oscuro) {
        ColoresNummia(
            ingreso = ingreso, gasto = gasto, aviso = Color(0xFFFBBF24),
            avisoFondo = Color(0x33FBBF24), ingresoFondo = Color(0x294ADE80),
            heroe = esquema.primary.copy(alpha = 0.10f).compositeOver(esquema.background),
            borde = esquema.outlineVariant, textoSuave = esquema.onSurfaceVariant, esOscuro = true,
        )
    } else {
        ColoresNummia(
            ingreso = ingreso, gasto = gasto, aviso = Color(0xFF92400E),
            avisoFondo = Color(0xFFFDF0D5), ingresoFondo = Color(0xFFDCF5E5),
            heroe = acento.copy(alpha = 0.14f).compositeOver(Color.White),
            borde = esquema.outlineVariant, textoSuave = esquema.onSurfaceVariant, esOscuro = false,
        )
    }
    val densidad = LocalDensity.current
    CompositionLocalProvider(
        LocalColoresNummia provides colores,
        LocalPreferenciasUi provides PreferenciasUi(ajustes.vibracion, ajustes.compacto, ajustes.formaTarjetas.radio),
        LocalDensity provides Density(densidad.density, densidad.fontScale * ajustes.tamanoTexto.escala),
    ) {
        MaterialTheme(colorScheme = esquema, typography = remember(ajustes.letra) { tipografiaDe(ajustes.letra) }, shapes = formas(ajustes.formaTarjetas), content = contenido)
    }
}
