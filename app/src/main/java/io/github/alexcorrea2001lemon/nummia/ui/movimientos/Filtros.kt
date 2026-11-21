package io.github.alexcorrea2001lemon.nummia.ui.movimientos

import io.github.alexcorrea2001lemon.nummia.ui.comun.EtiquetaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
import java.text.Normalizer
import java.time.LocalDate

enum class Pestana(val nombre: String) { TODOS("Todos"), GASTOS("Gastos"), INGRESOS("Ingresos"), PENDIENTES("Pendientes") }
enum class Orden(val nombre: String) { FECHA("Fecha"), IMPORTE("Importe"), TIENDA("Tienda") }

data class Filtros(
    val pestana: Pestana = Pestana.TODOS,
    val texto: String = "",
    val orden: Orden = Orden.FECHA,
    val conReembolso: Boolean? = null,
    val minimo: Long? = null,
    val maximo: Long? = null,
    /** Id de categoría, id de persona y nombre de tienda (cualquier grafía). */
    val categoria: String? = null,
    val persona: String? = null,
    val tienda: String? = null,
) {
    val avanzados: Int get() = listOfNotNull(conReembolso, minimo, maximo, categoria, persona, tienda).size
}

data class GrupoDia(val fecha: LocalDate, val total: Long, val filas: List<FilaMov>)

/** Sin tildes ni mayúsculas: «Café» → «cafe». */
fun normalizar(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

fun aplicar(filas: List<FilaMov>, f: Filtros): List<FilaMov> {
    val q = normalizar(f.texto.trim())
    val tienda = f.tienda?.let { normalizar(it.trim()) }
    return filas.asSequence()
        .filter {
            when (f.pestana) {
                Pestana.TODOS -> true
                Pestana.GASTOS -> it.esGasto
                Pestana.INGRESOS -> !it.esGasto
                Pestana.PENDIENTES -> it.etiqueta is EtiquetaMov.TeDeben || it.etiqueta == EtiquetaMov.Previsto
            }
        }
        .filter {
            q.isEmpty() || normalizar(it.titulo).contains(q) || normalizar(it.tienda).contains(q) ||
                normalizar(it.subtitulo).contains(q) || coincideImporte(it, q) || coincideFecha(it.fecha, q)
        }
        .filter { f.conReembolso == null || (it.esGasto && (it.pagado != null || it.etiqueta is EtiquetaMov.AFavor) == f.conReembolso) }
        .filter { f.minimo == null || kotlin.math.abs(it.importe) >= f.minimo }
        .filter { f.maximo == null || kotlin.math.abs(it.importe) <= f.maximo }
        .filter { f.categoria == null || it.categoriaId == f.categoria }
        .filter { f.persona == null || it.personaId == f.persona }
        .filter { tienda == null || normalizar(it.tienda.trim()) == tienda }
        .toList()
}

private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
private val DIA_MES = Regex("""^(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?$""")
private val NOMBRE_MES = Regex("""^([a-z]+)(?:\s+(?:de\s+)?(\d{4}))?$""")

/** «15/03», «15/3/2026», «marzo», «marzo de 2026» o «2026». */
fun coincideFecha(f: LocalDate, q: String): Boolean {
    DIA_MES.find(q)?.let { m ->
        val anio = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
        return f.dayOfMonth == m.groupValues[1].toInt() && f.monthValue == m.groupValues[2].toInt() && (anio == null || f.year == anio)
    }
    NOMBRE_MES.find(q)?.let { m ->
        val mes = MESES.indexOf(m.groupValues[1]).takeIf { it >= 0 } ?: return false
        return f.monthValue == mes + 1 && (m.groupValues[2].isEmpty() || f.year == m.groupValues[2].toInt())
    }
    return q.length == 4 && q.all { it.isDigit() } && f.year == q.toInt()
}

private fun coincideImporte(f: FilaMov, q: String): Boolean {
    val limpio = q.replace(".", "").replace(",", "")
    if (limpio.isEmpty() || !limpio.all { it.isDigit() }) return false
    val centimos = kotlin.math.abs(f.importe).toString()
    val pagado = f.pagado?.toString().orEmpty()
    return centimos == limpio || centimos == limpio + "00" || pagado == limpio || pagado == limpio + "00"
}

/** Agrupa por día. El total del día es lo ingresado − el gasto real. */
fun agrupar(filas: List<FilaMov>, orden: Orden): List<GrupoDia> = when (orden) {
    Orden.FECHA -> filas.sortedWith(compareByDescending<FilaMov> { it.fecha }.thenByDescending { it.creado })
        .groupBy { it.fecha }
        .map { (fecha, fs) -> GrupoDia(fecha, fs.sumOf { if (it.esGasto) -it.importe else it.importe }, fs) }
    Orden.IMPORTE -> listOf(GrupoDia(LocalDate.MIN, 0, filas.sortedByDescending { kotlin.math.abs(it.importe) }))
    Orden.TIENDA -> listOf(GrupoDia(LocalDate.MIN, 0, filas.sortedWith(compareBy<FilaMov> { normalizar(it.tienda.ifBlank { it.titulo }) }.thenByDescending { it.fecha })))
}
