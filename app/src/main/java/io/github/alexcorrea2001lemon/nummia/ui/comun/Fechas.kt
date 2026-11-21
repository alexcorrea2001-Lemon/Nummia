package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val ES: Locale = Locale.forLanguageTag("es-ES")

private fun String.mayuscula() = replaceFirstChar { it.titlecase(ES) }

fun nombreMes(ym: YearMonth): String = "${ym.month.getDisplayName(TextStyle.FULL_STANDALONE, ES).mayuscula()} ${ym.year}"
fun nombreMesMinuscula(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL_STANDALONE, ES).lowercase(ES)

/** «Jueves 15 oct» */
fun fechaCabecera(f: LocalDate, hoy: LocalDate): String = when (f) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> DateTimeFormatter.ofPattern("EEEE d MMM", ES).format(f).mayuscula().removeSuffix(".")
}

/** «15 ene 2026» */
fun fechaCorta(f: LocalDate): String = DateTimeFormatter.ofPattern("d MMM yyyy", ES).format(f).replace(".", "")

/** «Jueves 15 de enero de 2026» */
fun fechaLarga(f: LocalDate): String = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES).format(f).mayuscula()

/**
 * Nombre del mes que siempre cabe: «Noviembre 2026» y, si no hay sitio, «Nov 2026» (nunca se corta el año).
 */
@androidx.compose.runtime.Composable
fun TextoMes(
    ym: YearMonth,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    val medidor = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val completo = nombreMes(ym)
        val corto = "${ym.month.getDisplayName(TextStyle.SHORT_STANDALONE, ES).trimEnd('.').mayuscula()} ${ym.year}"
        val cabe = medidor.measure(completo, style, maxLines = 1).size.width <= constraints.maxWidth
        androidx.compose.material3.Text(
            if (cabe) completo else corto, style = style, color = color, maxLines = 1, softWrap = false,
            modifier = androidx.compose.ui.Modifier.semantics { contentDescription = completo },
        )
    }
}
