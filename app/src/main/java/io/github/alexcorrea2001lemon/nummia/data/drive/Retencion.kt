package io.github.alexcorrea2001lemon.nummia.data.drive

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/** «Nummia_copia_2030-03-15_21-30.nummia» */
private val FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

fun nombreCopia(f: LocalDateTime) = "Nummia_copia_${FORMATO.format(f)}.nummia"

fun fechaDeCopia(nombre: String): LocalDateTime? =
    Regex("""Nummia_copia_(\d{4}-\d{2}-\d{2}_\d{2}-\d{2})""").find(nombre)?.groupValues?.get(1)
        ?.let { runCatching { LocalDateTime.parse(it, FORMATO) }.getOrNull() }

/**
 * Qué copias se borran: se conservan la más reciente de cada uno de los últimos 7 días con copia,
 * de cada una de las últimas 4 semanas y de cada uno de los últimos 12 meses. Las que no tienen fecha legible no se tocan.
 */
fun copiasABorrar(copias: List<Pair<String, LocalDateTime?>>): List<String> {
    val fechadas = copias.mapNotNull { (id, f) -> f?.let { id to it } }.sortedByDescending { it.second }
    fun ultimasPor(n: Int, clave: (LocalDateTime) -> Any): Set<String> =
        fechadas.groupBy { clave(it.second) }.values.map { it.first().first }.take(n).toSet()
    val conservar = ultimasPor(7) { it.toLocalDate() } +
        ultimasPor(4) { it.get(IsoFields.WEEK_BASED_YEAR) * 100 + it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) } +
        ultimasPor(12) { it.year * 100 + it.monthValue }
    return fechadas.map { it.first }.filter { it !in conservar }
}
