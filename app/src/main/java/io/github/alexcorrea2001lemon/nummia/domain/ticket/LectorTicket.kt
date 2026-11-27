package io.github.alexcorrea2001lemon.nummia.domain.ticket

import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import java.text.Normalizer
import java.time.LocalDate

/** Lo que se ha podido leer de un ticket. Cada dato puede faltar: siempre se revisa antes de guardar. */
data class TicketLeido(val importe: Long?, val tienda: String?, val fecha: LocalDate?) {
    val vacio get() = importe == null && tienda == null && fecha == null
}

private val IMPORTE = Regex("""(?<!\d|\d[.,])(\d{1,3}(?:\.\d{3})+,\d{2}|\d+[.,]\d{2})(?!\d|[.,]\d)""")
private val FECHA_DMA = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4}|\d{2})(?!\d)""")
private val FECHA_AMD = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")
private val PALABRA_TOTAL = Regex("""(?i)\btotal\b""")
private val PALABRA_IMPORTE = Regex("""(?i)\bimporte\b|a pagar|a cobrar""")
private val PREFERIDA = Regex("""(?i)pagar|abonar|cobrar""")
private val NO_ES_EL_TOTAL = Regex("""(?i)sub\s*total|iva|descuento|dto|ahorro|cambio|entregad|efectivo|base|cuota|propina|puntos|saldo|art[ií]culos|unidades|\buds\b""")
private val NO_ES_COMPRA = Regex("""(?i)caduc|v[aá]lid|hasta|vence|devoluci""")
private val NO_ES_NOMBRE = Regex("""(?i)ticket|factura|simplificada|\bcif\b|\bnif\b|\btel|tfno|www\.|https?:|@|\bc/|calle|avda|avenida|plaza|\bc\.?p\b|bienvenid|gracias|operaci|fecha|hora|\d{5}""")
private val FORMA_LEGAL = Regex("""(?i)[,\s]+(s\.?\s?[al]\.?(\s?u\.?)?|s\.?\s?coop\.?)\.?$""")

private fun sinTildes(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
private fun soloLetrasYCifras(s: String) = sinTildes(s).filter { it.isLetterOrDigit() }

/** Los importes de una línea (sin contar las fechas), en céntimos. Los ceros no valen. */
private fun importesDe(linea: String): List<Long> =
    IMPORTE.findAll(FECHA_DMA.replace(linea, " ")).mapNotNull { leerImporte(it.value).getOrNull() }.toList()

/**
 * Saca el importe, la tienda y la fecha del texto que el móvil ha reconocido en un ticket.
 * Es una lectura aproximada: el texto de un ticket puede venir con errores, así que cada dato
 * es opcional y la persona lo revisa antes de guardar.
 * [tiendasConocidas] sirve para escribir la tienda con el mismo nombre que ya usas.
 */
fun leerTicket(texto: String, hoy: LocalDate, tiendasConocidas: List<String> = emptyList()): TicketLeido {
    val lineas = texto.replace('\u00A0', ' ').lines().map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.isNotEmpty() }
    return TicketLeido(importe(lineas), tienda(lineas, tiendasConocidas), fecha(lineas, hoy))
}

private fun importe(lineas: List<String>): Long? {
    // 1) Líneas «TOTAL …», con el importe en la misma línea o en la siguiente.
    fun porPalabra(palabra: Regex): List<Pair<String, Long>> = lineas.mapIndexedNotNull { i, l ->
        if (!palabra.containsMatchIn(l) || NO_ES_EL_TOTAL.containsMatchIn(l)) return@mapIndexedNotNull null
        val aqui = importesDe(l).lastOrNull()
        val siguiente = lineas.getOrNull(i + 1)?.takeIf { it.none { c -> c.isLetter() } }?.let { importesDe(it).lastOrNull() }
        (aqui ?: siguiente)?.let { l to it }
    }
    for (palabra in listOf(PALABRA_TOTAL, PALABRA_IMPORTE)) {
        val candidatos = porPalabra(palabra)
        if (candidatos.isNotEmpty()) return (candidatos.lastOrNull { PREFERIDA.containsMatchIn(it.first) } ?: candidatos.last()).second
    }
    // 2) Sin palabra clave: el mayor importe que no sea IVA, cambio, etc.
    return lineas.filterNot { NO_ES_EL_TOTAL.containsMatchIn(it) }.flatMap { importesDe(it) }.maxOrNull()
}

private fun fecha(lineas: List<String>, hoy: LocalDate): LocalDate? {
    val limite = hoy.minusYears(2)
    for (l in lineas) {
        if (NO_ES_COMPRA.containsMatchIn(l)) continue
        val halladas = buildList {
            FECHA_DMA.findAll(l).forEach { m ->
                val (d, mes, a) = m.destructured
                val anio = if (a.length == 2) 2000 + a.toInt() else a.toInt()
                add(m.range.first to runCatching { LocalDate.of(anio, mes.toInt(), d.toInt()) }.getOrNull())
            }
            FECHA_AMD.findAll(l).forEach { m ->
                val (a, mes, d) = m.destructured
                add(m.range.first to runCatching { LocalDate.of(a.toInt(), mes.toInt(), d.toInt()) }.getOrNull())
            }
        }.sortedBy { it.first }.mapNotNull { it.second }
        halladas.firstOrNull { !it.isAfter(hoy.plusDays(1)) && !it.isBefore(limite) }?.let { return it }
    }
    return null
}

private fun tienda(lineas: List<String>, conocidas: List<String>): String? {
    val linea = lineas.take(8).firstOrNull { l ->
        val letras = l.count { it.isLetter() }
        letras >= 3 && letras >= 0.6 * l.count { !it.isWhitespace() } && !NO_ES_NOMBRE.containsMatchIn(l)
    } ?: return null
    val claveLinea = soloLetrasYCifras(linea)
    conocidas.filter { soloLetrasYCifras(it).length >= 4 && soloLetrasYCifras(it) in claveLinea }
        .maxByOrNull { soloLetrasYCifras(it).length }?.let { return it }
    val limpia = linea.trim { !it.isLetterOrDigit() }.replace(FORMA_LEGAL, "").trim { !it.isLetterOrDigit() }.ifEmpty { return null }
    return if (limpia.any { it.isLowerCase() }) limpia
    else limpia.lowercase().split(' ').joinToString(" ") { p -> p.replaceFirstChar { it.uppercase() } }
}
