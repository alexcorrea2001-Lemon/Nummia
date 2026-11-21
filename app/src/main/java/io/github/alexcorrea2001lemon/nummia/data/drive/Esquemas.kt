package io.github.alexcorrea2001lemon.nummia.data.drive

import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Cómo se guarda cada pestaña de la hoja de Drive. Cada fila se pasa a una forma «canónica»
 * (lista de textos normalizados) que es igual venga del móvil o de la hoja: así se comparan.
 */
enum class TipoCelda { TEXTO, FECHA, IMPORTE, IMPORTE_OPCIONAL, SI_NO }

data class Columna(val titulo: String, val tipo: TipoCelda, val obligatoria: Boolean = false)

class FilaInvalida(val motivo: String) : Exception(motivo)

/** Valores canónicos de una fila (sin el id). */
typealias Canon = List<String>

fun firma(c: Canon): String = c.joinToString("\u001F")
fun desdeFirma(f: String): Canon = f.split("\u001F")

enum class Pestana(val nombre: String, val columnas: List<Columna>) {
    GASTOS(
        "Gastos",
        listOf(
            Columna("Código", TipoCelda.TEXTO), Columna("Fecha", TipoCelda.FECHA, true), Columna("Tienda", TipoCelda.TEXTO),
            Columna("Concepto", TipoCelda.TEXTO, true), Columna("Importe", TipoCelda.IMPORTE, true), Columna("Categoría", TipoCelda.TEXTO),
            Columna("¿Me lo devuelven?", TipoCelda.SI_NO), Columna("Quién", TipoCelda.TEXTO), Columna("Esperado", TipoCelda.IMPORTE_OPCIONAL),
            Columna("Notas", TipoCelda.TEXTO),
        ),
    ),
    INGRESOS(
        "Ingresos",
        listOf(
            Columna("Código", TipoCelda.TEXTO), Columna("Fecha", TipoCelda.FECHA, true), Columna("Concepto", TipoCelda.TEXTO, true),
            Columna("Tipo", TipoCelda.TEXTO), Columna("Importe", TipoCelda.IMPORTE, true), Columna("Notas", TipoCelda.TEXTO),
        ),
    ),
    REEMBOLSOS(
        "Reembolsos",
        listOf(
            Columna("Código", TipoCelda.TEXTO), Columna("Código del gasto", TipoCelda.TEXTO, true), Columna("Fecha", TipoCelda.FECHA),
            Columna("Importe", TipoCelda.IMPORTE, true), Columna("Quién", TipoCelda.TEXTO), Columna("Nota", TipoCelda.TEXTO),
        ),
    ),
    ;

    /** La columna del id va justo después de las visibles (y se oculta en la hoja). */
    val columnaId: Int get() = columnas.size
    val letraFinal: Char get() = 'A' + columnaId
}

/** Días entre el 30/12/1899 (origen de las hojas de cálculo) y el 01/01/1970. */
private const val DESFASE_SERIAL = 25569L
private val FECHA_TEXTO = DateTimeFormatter.ofPattern("d/M/yyyy")

/** Celda de la hoja (lo que devuelve la API sin formato) → texto canónico. */
fun leerCelda(c: JsonElement?, col: Columna): String {
    val p = (c as? JsonPrimitive)?.takeUnless { it is JsonNull }
    val texto = p?.contentOrNull?.trim().orEmpty()
    if (texto.isEmpty()) {
        if (col.obligatoria) throw FilaInvalida("Falta «${col.titulo}»")
        return ""
    }
    return when (col.tipo) {
        TipoCelda.TEXTO -> texto
        TipoCelda.FECHA -> {
            val serial = if (p!!.isString) null else texto.toBigDecimalOrNull()
            val f = if (serial != null) LocalDate.ofEpochDay(serial.toLong() - DESFASE_SERIAL)
            else runCatching { LocalDate.parse(texto, FECHA_TEXTO) }.getOrElse { throw FilaInvalida("Fecha no válida: «$texto»") }
            f.toEpochDay().toString()
        }
        TipoCelda.IMPORTE, TipoCelda.IMPORTE_OPCIONAL -> {
            val centimos = if (!p!!.isString) {
                BigDecimal(texto).movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
            } else {
                leerImporte(texto).getOrElse { throw FilaInvalida("Importe no válido: «$texto»") }
            }
            if (centimos < 0 || (col.tipo == TipoCelda.IMPORTE && centimos == 0L)) throw FilaInvalida("Importe no válido: «$texto»")
            centimos.toString()
        }
        TipoCelda.SI_NO -> if (texto.lowercase() in setOf("sí", "si", "s", "x", "true", "verdadero", "1")) "Sí" else "No"
    }
}

/** Texto canónico → valor para escribir en la hoja (números y fechas como números). */
fun escribirCelda(valor: String, col: Columna): JsonElement = when {
    valor.isEmpty() -> JsonPrimitive("")
    col.tipo == TipoCelda.FECHA -> JsonPrimitive(valor.toLong() + DESFASE_SERIAL)
    col.tipo == TipoCelda.IMPORTE || col.tipo == TipoCelda.IMPORTE_OPCIONAL -> JsonPrimitive(BigDecimal(valor.toLong()).movePointLeft(2))
    else -> JsonPrimitive(valor)
}

/** Fila leída de la hoja: número de fila (1 = cabecera), id si lo tiene y su forma canónica o el motivo del error. */
data class FilaRemota(val fila: Int, val id: String?, val canon: Canon?, val error: String? = null)

fun leerFilas(p: Pestana, valores: List<List<JsonElement>>): List<FilaRemota> =
    valores.withIndex().drop(1).mapNotNull { (i, celdas) ->
        val fila = i + 1
        val visibles = (0 until p.columnaId).map { celdas.getOrNull(it) }
        val id = celdas.getOrNull(p.columnaId)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        if (id == null && visibles.all { (it as? JsonPrimitive)?.contentOrNull.isNullOrBlank() }) return@mapNotNull null // fila vacía
        try {
            FilaRemota(fila, id, p.columnas.mapIndexed { j, col -> leerCelda(visibles[j], col) })
        } catch (e: FilaInvalida) {
            FilaRemota(fila, id, null, e.motivo)
        }
    }

fun filaParaEscribir(p: Pestana, id: String, c: Canon): List<JsonElement> =
    p.columnas.mapIndexed { j, col -> escribirCelda(c[j], col) } + JsonPrimitive(id)
