package io.github.alexcorrea2001lemon.nummia.domain.dinero

/**
 * Todo el dinero de Nummia son céntimos en un [Long]. Nunca Double ni Float.
 */

private const val NBSP = ' '
private const val MENOS = '−'
private const val MAXIMO_CENTIMOS = 9_999_999_999L // 99.999.999,99 €

class ImporteInvalido(val motivo: String) : IllegalArgumentException(motivo)

/** «1.520,75 €». Los negativos llevan siempre «−»; los positivos «+» solo si [conSigno]. */
fun formatearEuros(centimos: Long, conSigno: Boolean = false, discreto: Boolean = false): String {
    if (discreto) return "••••$NBSP€"
    val abs = if (centimos == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(centimos)
    val signo = when {
        centimos < 0 -> MENOS.toString()
        conSigno && centimos > 0 -> "+"
        else -> ""
    }
    return "$signo${agruparMiles(abs / 100)},${(abs % 100).toString().padStart(2, '0')}$NBSP€"
}

/** Porcentaje guardado ×10 (un decimal): 907 → «90,7 %». `null` → «—». */
fun formatearPorMil(porMil: Int?): String {
    if (porMil == null) return "—"
    val abs = kotlin.math.abs(porMil)
    val signo = if (porMil < 0) MENOS.toString() else ""
    return "$signo${agruparMiles((abs / 10).toLong())},${abs % 10}$NBSP%"
}

private fun agruparMiles(n: Long): String {
    val s = n.toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(c)
    }
    return sb.toString()
}

/**
 * Lee un importe escrito por la persona: «2,7», «1.520,75», «1520.75», «12,50 €».
 * La coma es el separador decimal; un punto seguido de exactamente 3 cifras es separador de miles.
 */
fun leerImporte(texto: String): Result<Long> {
    val limpio = texto.replace("€", "").replace(" ", "").replace(NBSP.toString(), "").trim()
    if (limpio.isEmpty()) return fallo("Escribe un importe")
    if (limpio.startsWith("-") || limpio.startsWith(MENOS)) return fallo("El importe no puede ser negativo")
    if (limpio.any { !it.isDigit() && it != ',' && it != '.' }) return fallo("Escribe solo números")

    val entero: String
    val decimales: String
    if (limpio.count { it == ',' } > 1) return fallo("Escribe solo números")
    if (',' in limpio) {
        entero = limpio.substringBefore(',').replace(".", "")
        decimales = limpio.substringAfter(',')
        if ('.' in decimales) return fallo("Escribe solo números")
    } else if ('.' in limpio) {
        val partes = limpio.split('.')
        val ultima = partes.last()
        if (partes.size == 2 && ultima.length != 3) {
            entero = partes[0]
            decimales = ultima
        } else {
            if (partes.drop(1).any { it.length != 3 }) return fallo("Escribe solo números")
            entero = partes.joinToString("")
            decimales = ""
        }
    } else {
        entero = limpio
        decimales = ""
    }

    if (decimales.length > 2) return fallo("Como máximo 2 decimales")
    val enteroNorm = entero.ifEmpty { "0" }.trimStart('0').ifEmpty { "0" }
    if (enteroNorm.length > 8) return fallo("El importe es demasiado grande")
    val centimos = enteroNorm.toLong() * 100 + decimales.padEnd(2, '0').ifEmpty { "00" }.toLong()
    if (centimos > MAXIMO_CENTIMOS) return fallo("El importe es demasiado grande")
    if (centimos == 0L) return fallo("El importe debe ser mayor que 0")
    return Result.success(centimos)
}

private fun fallo(motivo: String): Result<Long> = Result.failure(ImporteInvalido(motivo))
