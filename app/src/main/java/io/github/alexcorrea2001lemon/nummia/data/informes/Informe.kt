package io.github.alexcorrea2001lemon.nummia.data.informes

import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.aMov
import io.github.alexcorrea2001lemon.nummia.data.repo.devuelto
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.data.repo.reembolsosMov
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GastoEtiquetado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.porCategoria
import io.github.alexcorrea2001lemon.nummia.domain.calculo.porTienda
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
import java.time.LocalDate
import java.time.YearMonth

enum class Contenido(val nombre: String) {
    RESUMEN("Resumen"), MESES("Mes a mes"), MOVIMIENTOS("Movimientos"), TIENDAS("Por tienda"), CATEGORIAS("Por categoría"), DEBEN("Lo que me deben")
}

enum class Formato(val extension: String, val mime: String) {
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
}

data class FilaMovimiento(
    val fecha: LocalDate, val esGasto: Boolean, val codigo: String, val concepto: String, val tienda: String, val categoria: String,
    val importe: Long, val devuelto: Long, val real: Long,
)

data class FilaDeben(val persona: String, val fecha: LocalDate, val concepto: String, val esperado: Long, val devuelto: Long, val falta: Long)

data class Informe(
    val titulo: String,
    val periodo: String,
    val contenidos: Set<Contenido>,
    val resumen: Resumen,
    val meses: List<Pair<String, Resumen>>,
    val movimientos: List<FilaMovimiento>,
    val tiendas: List<GrupoGasto>,
    val categorias: List<GrupoGasto>,
    val deben: List<FilaDeben>,
) {
    val totalDeben get() = deben.sumOf { it.falta }
    val vacio get() = movimientos.isEmpty() && deben.isEmpty() && resumen == Resumen.VACIO
}

private val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

fun nombreMes(ym: YearMonth) = "${MESES[ym.monthValue - 1]} ${ym.year}"

/**
 * Prepara el informe de [desde]..[hasta] con lo CONFIRMADO. «Lo que me deben» es lo pendiente a día de hoy
 * (de cualquier fecha), y se puede limitar a una [persona].
 */
fun construirInforme(
    titulo: String,
    desde: LocalDate,
    hasta: LocalDate,
    contenidos: Set<Contenido>,
    gastos: List<GastoConReembolsos>,
    ingresos: List<IngresoEntity>,
    personas: Map<String, String>,
    categorias: Map<String, String>,
    persona: String? = null,
): Informe {
    val en = { f: LocalDate -> !f.isBefore(desde) && !f.isAfter(hasta) }
    val gs = gastos.filter { it.gasto.deletedAt == null && it.gasto.estado == Estado.CONFIRMADO }
    val gp = gs.filter { en(it.gasto.fecha) }
    val ip = ingresos.filter { it.deletedAt == null && it.estado == Estado.CONFIRMADO && en(it.fecha) }
    val reemb = gp.flatMap { it.reembolsosMov() }

    val r = resumen(desde, hasta, gp.map { it.aMov() }, reemb, ip.map { it.aMov() })

    val meses = if (Contenido.MESES in contenidos) {
        generateSequence(YearMonth.from(desde)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(hasta)) }.map { ym ->
            val a = maxOf(ym.atDay(1), desde)
            val b = minOf(ym.atEndOfMonth(), hasta)
            val g = gp.filter { !it.gasto.fecha.isBefore(a) && !it.gasto.fecha.isAfter(b) }
            nombreMes(ym) to resumen(a, b, g.map { it.aMov() }, g.flatMap { it.reembolsosMov() }, ip.filter { !it.fecha.isBefore(a) && !it.fecha.isAfter(b) }.map { it.aMov() })
        }.toList()
    } else emptyList()

    val nombreCat = { id: String? -> id?.let(categorias::get) ?: "Sin categoría" }
    val movimientos = if (Contenido.MOVIMIENTOS in contenidos) {
        (gp.map { g ->
            FilaMovimiento(
                g.gasto.fecha, true, g.gasto.codigo, g.gasto.concepto, g.gasto.tienda, nombreCat(g.gasto.categoriaId),
                g.gasto.importe, g.devuelto, g.real,
            )
        } + ip.map { i ->
            FilaMovimiento(i.fecha, false, i.codigo, i.concepto, tipoIngreso(i.tipo), "", i.importe, 0, i.importe)
        }).sortedWith(compareBy<FilaMovimiento> { it.fecha }.thenBy { it.codigo })
    } else emptyList()

    val etiquetados = gp.map { GastoEtiquetado(it.aMov(), it.gasto.tienda.ifBlank { it.gasto.concepto }, it.gasto.categoriaId) }
    val tiendas = if (Contenido.TIENDAS in contenidos) porTienda(etiquetados, reemb) else emptyList()
    val cats = if (Contenido.CATEGORIAS in contenidos) porCategoria(etiquetados, reemb) { nombreCat(it) } else emptyList()

    val deben = if (Contenido.DEBEN in contenidos) {
        gs.filter { it.gasto.meLoDevuelven && it.falta > 0 && (persona == null || it.gasto.personaId == persona) }
            .map { g ->
                FilaDeben(
                    g.gasto.personaId?.let(personas::get) ?: "Sin persona", g.gasto.fecha, g.gasto.concepto,
                    g.gasto.importeEsperado ?: g.gasto.importe, g.devuelto, g.falta,
                )
            }
            .sortedWith(compareBy<FilaDeben> { it.persona.lowercase() }.thenBy { it.fecha })
    } else emptyList()

    val periodo = when {
        desde == YearMonth.from(desde).atDay(1) && hasta == YearMonth.from(desde).atEndOfMonth() -> nombreMes(YearMonth.from(desde))
        desde == LocalDate.of(desde.year, 1, 1) && hasta == LocalDate.of(desde.year, 12, 31) -> "Año ${desde.year}"
        else -> "Del ${fechaCorta(desde)} al ${fechaCorta(hasta)}"
    }
    return Informe(titulo, periodo, contenidos, r, meses, movimientos, tiendas, cats, deben)
}

fun fechaCorta(f: LocalDate): String = "%02d/%02d/%d".format(f.dayOfMonth, f.monthValue, f.year)

private fun tipoIngreso(t: TipoIngreso) = when (t) {
    TipoIngreso.SUELDO -> "Sueldo"; TipoIngreso.EXTRA -> "Extra"; TipoIngreso.VENTA -> "Venta"
    TipoIngreso.TRABAJO_EXTRA -> "Trabajo extra"; TipoIngreso.OTRO -> "Otro"
}
