package io.github.alexcorrea2001lemon.nummia.domain.calculo

import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/** Un gasto con su tienda y su categoría, para agrupar. */
data class GastoEtiquetado(val mov: MovGasto, val tienda: String, val categoria: String?)

data class GrupoGasto(val nombre: String, val total: Long, val veces: Int) {
    val media: Long get() = if (veces == 0) 0 else total / veces
}

data class DiaCalendario(val gasto: Long, val ingreso: Long, val previsto: Boolean)

/** Resumen de un año: 12 meses (enero = índice 0) y totales. Las medias, sobre los meses con datos. */
data class ResumenAnual(
    val anio: Int,
    val meses: List<Resumen>,
    val sueldos: Long,
    val otrosIngresos: Long,
    val total: Resumen,
    val mediaGasto: Long,
    val mediaIngreso: Long,
    /** Mes (1–12) con más / menos gasto real, entre los que tienen datos. */
    val mesMasGasto: Int?,
    val mesMenosGasto: Int?,
)

private fun confirmado(g: MovGasto) = g.estado == Estado.CONFIRMADO

fun resumenAnual(anio: Int, gastos: List<MovGasto>, reembolsos: List<MovReembolso>, ingresos: List<MovIngreso>): ResumenAnual {
    val meses = (1..12).map { m ->
        val ym = YearMonth.of(anio, m)
        resumen(ym.atDay(1), ym.atEndOfMonth(), gastos, reembolsos, ingresos)
    }
    val conDatos = (1..12).filter { m ->
        gastos.any { confirmado(it) && it.fecha.year == anio && it.fecha.monthValue == m } ||
            ingresos.any { it.estado == Estado.CONFIRMADO && it.fecha.year == anio && it.fecha.monthValue == m }
    }
    val delAnio = ingresos.filter { it.estado == Estado.CONFIRMADO && it.fecha.year == anio }
    val total = resumen(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), gastos, reembolsos, ingresos)
    val n = conDatos.size
    return ResumenAnual(
        anio = anio,
        meses = meses,
        sueldos = delAnio.filter { it.tipo == TipoIngreso.SUELDO }.sumOf { it.importe },
        otrosIngresos = delAnio.filter { it.tipo != TipoIngreso.SUELDO }.sumOf { it.importe },
        total = total,
        mediaGasto = if (n == 0) 0 else total.gastoReal / n,
        mediaIngreso = if (n == 0) 0 else total.ingresos / n,
        mesMasGasto = conDatos.maxByOrNull { meses[it - 1].gastoReal }?.let { mx -> conDatos.first { meses[it - 1].gastoReal == meses[mx - 1].gastoReal } },
        mesMenosGasto = conDatos.minByOrNull { meses[it - 1].gastoReal }?.let { mn -> conDatos.first { meses[it - 1].gastoReal == meses[mn - 1].gastoReal } },
    )
}

/** Gasto real acumulado día a día (índice 0 = día 1). Los reembolsos cuentan el día del gasto. */
fun acumuladoDiario(ym: YearMonth, gastos: List<MovGasto>, reembolsos: List<MovReembolso>): List<Long> {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val porDia = LongArray(ym.lengthOfMonth())
    gastos.filter { confirmado(it) && YearMonth.from(it.fecha) == ym }
        .forEach { porDia[it.fecha.dayOfMonth - 1] += it.importe - (devuelto[it.id] ?: 0) }
    var suma = 0L
    return porDia.map { suma += it; suma }
}

/** «Cafetería» y «cafeteria» son la misma clave. */
fun claveTexto(s: String): String =
    Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

private fun agrupar(gastos: List<GastoEtiquetado>, reembolsos: List<MovReembolso>, nombre: (GastoEtiquetado) -> String): List<GrupoGasto> {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    return gastos.filter { confirmado(it.mov) }
        .groupBy { claveTexto(nombre(it)) }
        .map { (_, gs) ->
            val grafia = gs.groupingBy { nombre(it).trim() }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).first().key
            GrupoGasto(grafia, gs.sumOf { it.mov.importe - (devuelto[it.mov.id] ?: 0) }, gs.size)
        }
        .sortedByDescending { it.total }
}

fun porTienda(gastos: List<GastoEtiquetado>, reembolsos: List<MovReembolso>): List<GrupoGasto> =
    agrupar(gastos, reembolsos) { it.tienda.ifBlank { "Sin tienda" } }

fun porCategoria(gastos: List<GastoEtiquetado>, reembolsos: List<MovReembolso>, nombre: (String?) -> String): List<GrupoGasto> =
    agrupar(gastos, reembolsos) { nombre(it.categoria) }

/** Día con más gasto real, o `null` si no hay gastos. */
fun diaMasGasto(gastos: List<MovGasto>, reembolsos: List<MovReembolso>): Pair<LocalDate, Long>? {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    return gastos.filter(::confirmado)
        .groupBy { it.fecha }
        .mapValues { (_, gs) -> gs.sumOf { it.importe - (devuelto[it.id] ?: 0) } }
        .entries.sortedWith(compareByDescending<Map.Entry<LocalDate, Long>> { it.value }.thenBy { it.key })
        .firstOrNull()?.let { it.key to it.value }
}

/** Resumen por día para el calendario. Lo previsto solo marca el día; no suma. */
fun porDia(gastos: List<MovGasto>, reembolsos: List<MovReembolso>, ingresos: List<MovIngreso>): Map<LocalDate, DiaCalendario> {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val dias = HashMap<LocalDate, DiaCalendario>()
    fun con(f: LocalDate, cambio: (DiaCalendario) -> DiaCalendario) { dias[f] = cambio(dias[f] ?: DiaCalendario(0, 0, false)) }
    gastos.forEach { g ->
        if (confirmado(g)) con(g.fecha) { it.copy(gasto = it.gasto + g.importe - (devuelto[g.id] ?: 0)) }
        else con(g.fecha) { it.copy(previsto = true) }
    }
    ingresos.forEach { i ->
        if (i.estado == Estado.CONFIRMADO) con(i.fecha) { it.copy(ingreso = it.ingreso + i.importe) }
        else con(i.fecha) { it.copy(previsto = true) }
    }
    return dias
}

/** Año [b] frente al año [a] (referencia). */
data class Comparacion(val a: ResumenAnual, val b: ResumenAnual) {
    val diferenciaGasto get() = b.total.gastoReal - a.total.gastoReal
    val diferenciaIngreso get() = b.total.ingresos - a.total.ingresos
    val diferenciaBalance get() = b.total.balance - a.total.balance
    val porMilGasto get() = variacionPorMil(b.total.gastoReal, a.total.gastoReal) ?: if (a.total.gastoReal == b.total.gastoReal && a.total.gastoReal > 0) 0 else null
    val porMilIngreso get() = variacionPorMil(b.total.ingresos, a.total.ingresos)
    val porMilBalance get() = if (a.total.balance <= 0) null else porMil(b.total.balance - a.total.balance, a.total.balance)
}
