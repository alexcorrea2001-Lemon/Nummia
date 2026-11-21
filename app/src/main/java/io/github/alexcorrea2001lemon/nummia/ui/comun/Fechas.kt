package io.github.alexcorrea2001lemon.nummia.ui.comun

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
