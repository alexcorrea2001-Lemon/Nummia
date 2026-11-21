package io.github.alexcorrea2001lemon.nummia.data.importar

import io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Un gasto leído del Excel. [celda] es la celda del nombre (para los avisos). */
data class GastoExcel(val fecha: LocalDate, val nombre: String, val importe: Long, val reembolso: Long?, val celda: String)

data class IngresoExcel(val fecha: LocalDate, val concepto: String, val importe: Long, val esSueldo: Boolean, val fechaAproximada: Boolean)

data class MesExcel(val mes: Int, val gastoRealCalculado: Long, val gastoRealEnExcel: Long?) {
    val coincide get() = gastoRealEnExcel == null || gastoRealEnExcel == gastoRealCalculado
}

data class AnalisisExcel(
    val anio: Int,
    val gastos: List<GastoExcel>,
    val ingresos: List<IngresoExcel>,
    val meses: List<MesExcel>,
    val avisos: List<String>,
) {
    val reembolsos get() = gastos.count { it.reembolso != null }
    val vacio get() = gastos.isEmpty() && ingresos.isEmpty()
}

/**
 * Lee el Excel anual que se llevaba a mano:
 * - Bloques de enero a junio en las filas 9–23 y de julio a diciembre en las 38–52.
 * - Columnas C/G/K/O/S/W: nombre «Concepto(dd/mm)»; la siguiente, el precio; y la siguiente, el reembolso.
 * - Sueldo en las filas 3 y 32 (columna del precio). Gasto real del mes (fórmula) en las filas 2 y 31.
 * - Ganancias extraordinarias desde la fila 56, columnas C:D. Se ignoran H56:I60 y todas las fórmulas como datos.
 */
object ExcelAnual {
    private val COLUMNAS = listOf("C", "G", "K", "O", "S", "W")
    val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    private val CON_FECHA = Regex("""^(.*?)\s*\(\s*(\d{1,2})\s*/\s*(\d{1,2})(?:\s*/\s*\d{2,4})?\s*\)\s*$""")

    private fun siguiente(col: String, n: Int) = (col[0] + n).toString()

    /** Euros (número del Excel) → céntimos. */
    fun centimos(n: BigDecimal): Long = n.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

    /** Año en el nombre del archivo («2026.xlsx», «Cuentas 2026.xlsx»…). */
    fun anioDelNombre(nombre: String?): Int? = nombre?.let { Regex("""(?<!\d)(20\d{2})(?!\d)""").find(it)?.groupValues?.get(1)?.toInt() }

    fun analizar(celdas: Map<String, Celda>, anio: Int): AnalisisExcel {
        val avisos = ArrayList<String>()
        val brutos = ArrayList<GastoExcel>()
        val ingresos = ArrayList<IngresoExcel>()
        val meses = ArrayList<MesExcel>()

        fun numero(ref: String): BigDecimal? = celdas[ref]?.takeIf { !it.formula }?.numero
        fun texto(ref: String): String? = celdas[ref]?.takeIf { !it.formula }?.texto?.trim()?.takeIf { it.isNotEmpty() }

        for (bloque in 0..1) {
            val filas = if (bloque == 0) 9..23 else 38..52
            val filaSueldo = if (bloque == 0) 3 else 32
            val filaTotal = if (bloque == 0) 2 else 31
            COLUMNAS.forEachIndexed { i, col ->
                val mes = bloque * 6 + i + 1
                val ym = YearMonth.of(anio, mes)
                val cPrecio = siguiente(col, 1)
                val cReembolso = siguiente(col, 2)
                val delMes = ArrayList<GastoExcel>()
                for (f in filas) {
                    val nombreCelda = texto("$col$f")
                    val precio = numero("$cPrecio$f")
                    if (nombreCelda == null) {
                        if (precio != null) avisos += "$cPrecio$f: hay un precio sin nombre; no se importa."
                        continue
                    }
                    if (precio == null || precio.signum() <= 0) {
                        avisos += "$col$f («$nombreCelda»): no tiene precio; no se importa."
                        continue
                    }
                    val m = CON_FECHA.find(nombreCelda)
                    var nombre = nombreCelda
                    var fecha = ym.atDay(1)
                    if (m != null) {
                        nombre = m.groupValues[1].trim().ifEmpty { nombreCelda }
                        val dia = m.groupValues[2].toInt()
                        val mesTexto = m.groupValues[3].toInt()
                        val f2 = runCatching { LocalDate.of(anio, mesTexto, dia) }.getOrNull()
                        if (f2 == null) {
                            avisos += "$col$f («$nombreCelda»): fecha no válida; se pone el día 1."
                        } else {
                            fecha = f2
                            if (mesTexto != mes) avisos += "$col$f («$nombreCelda»): la fecha es de otro mes; se usa la fecha escrita."
                        }
                    } else {
                        avisos += "$col$f («$nombreCelda»): sin fecha; se pone el día 1."
                    }
                    val reembolso = numero("$cReembolso$f")?.takeIf { it.signum() > 0 }?.let(::centimos)
                    delMes += GastoExcel(fecha, nombre, centimos(precio), reembolso, "$col$f")
                }
                brutos += delMes
                numero("$cPrecio$filaSueldo")?.takeIf { it.signum() > 0 }?.let {
                    ingresos += IngresoExcel(ym.atEndOfMonth(), "Sueldo", centimos(it), esSueldo = true, fechaAproximada = true)
                }
                val enExcel = celdas["$cPrecio$filaTotal"]?.numero?.let(::centimos)
                val calculado = delMes.sumOf { it.importe - (it.reembolso ?: 0) }
                meses += MesExcel(mes, calculado, enExcel)
                if (enExcel != null && enExcel != calculado) {
                    avisos += "${MESES[mes - 1]}: el Excel dice ${formatearEuros(enExcel)} de gasto real y se han leído ${formatearEuros(calculado)}."
                }
            }
        }

        // Ganancias extraordinarias: C56 en adelante (la cabecera no tiene importe).
        for (f in 56..200) {
            val concepto = texto("C$f") ?: continue
            val importe = numero("D$f")?.takeIf { it.signum() > 0 } ?: continue
            val m = CON_FECHA.find(concepto)
            val fecha = m?.let { runCatching { LocalDate.of(anio, it.groupValues[3].toInt(), it.groupValues[2].toInt()) }.getOrNull() }
            if (fecha == null) avisos += "C$f («$concepto»): ganancia sin fecha; se pone el 1 de enero. Puedes cambiarla después."
            ingresos += IngresoExcel(fecha ?: LocalDate.of(anio, 1, 1), m?.groupValues?.get(1)?.trim()?.ifEmpty { null } ?: concepto, centimos(importe), esSueldo = false, fechaAproximada = fecha == null)
        }

        val nombres = unificarNombres(brutos.map { it.nombre })
        return AnalisisExcel(anio, brutos.map { it.copy(nombre = nombres.getValue(it.nombre)) }, ingresos, meses, avisos)
    }

    /**
     * Une variantes del mismo nombre (mayúsculas, tildes, espacios o una errata) y se queda con la grafía más frecuente.
     * Devuelve un mapa de cada nombre original a su nombre unificado.
     */
    fun unificarNombres(nombres: List<String>): Map<String, String> {
        val frecuencia = nombres.map { it.trim().replace(Regex("\\s+"), " ") }.groupingBy { it }.eachCount()
        // Grupos por clave (sin tildes ni mayúsculas ni espacios), de más a menos frecuentes.
        val porClave = frecuencia.keys.groupBy { clave(it) }
        val claves = porClave.keys.sortedByDescending { k -> porClave.getValue(k).sumOf { frecuencia.getValue(it) } }
        val representante = HashMap<String, String>() // clave → clave del grupo al que se une
        for (k in claves) {
            representante[k] = claves.firstOrNull { otra -> otra in representante.values && otra != k && parecidas(k, otra) } ?: k
        }
        val grupos = frecuencia.keys.groupBy { representante.getValue(clave(it)) }
        val mejor = grupos.mapValues { (_, variantes) -> variantes.maxWith(compareBy<String> { frecuencia.getValue(it) }.thenBy { v -> v.count { it.isUpperCase() } }) }
        return nombres.associateWith { mejor.getValue(representante.getValue(clave(it.trim().replace(Regex("\\s+"), " ")))) }
    }

    private fun clave(s: String) = claveTexto(s).replace(Regex("[^a-z0-9]"), "")

    /** Una sola letra de diferencia (cambiada, de más o de menos), solo en nombres de 5 letras o más. */
    private fun parecidas(a: String, b: String): Boolean {
        if (minOf(a.length, b.length) < 5 || kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var dif = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++dif > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        return dif + (a.length - i) + (b.length - j) <= 1
    }
}
