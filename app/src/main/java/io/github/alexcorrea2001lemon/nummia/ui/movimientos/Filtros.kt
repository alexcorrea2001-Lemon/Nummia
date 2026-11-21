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
) {
    val avanzados: Int get() = listOfNotNull(conReembolso, minimo, maximo).size
}

data class GrupoDia(val fecha: LocalDate, val total: Long, val filas: List<FilaMov>)

/** Sin tildes ni mayúsculas: «Café» → «cafe». */
fun normalizar(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

fun aplicar(filas: List<FilaMov>, f: Filtros): List<FilaMov> {
    val q = normalizar(f.texto.trim())
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
                normalizar(it.subtitulo).contains(q) || coincideImporte(it, q)
        }
        .filter { f.conReembolso == null || (it.esGasto && (it.pagado != null || it.etiqueta is EtiquetaMov.AFavor) == f.conReembolso) }
        .filter { f.minimo == null || kotlin.math.abs(it.importe) >= f.minimo }
        .filter { f.maximo == null || kotlin.math.abs(it.importe) <= f.maximo }
        .toList()
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
