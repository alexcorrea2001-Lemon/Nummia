package io.github.alexcorrea2001lemon.nummia.domain.calculo

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Importe − Σ reembolsos del gasto. Puede ser negativo (te devolvieron de más). */
fun gastoReal(g: MovGasto, rs: List<MovReembolso>): Long =
    g.importe - rs.filter { it.gastoId == g.id }.sumOf { it.importe }

/** Lo que aún te deben por un gasto marcado «me lo devuelven». Nunca negativo. */
fun faltaPorRecuperar(g: MovGasto, rs: List<MovReembolso>): Long {
    if (!g.meLoDevuelven) return 0
    val esperado = g.esperado ?: g.importe
    val devuelto = rs.filter { it.gastoId == g.id }.sumOf { it.importe }
    return (esperado - devuelto).coerceAtLeast(0)
}

/**
 * Resumen de [desde]..[hasta] (ambos incluidos). Solo cuenta lo CONFIRMADO.
 * Los reembolsos cuentan en el mes del GASTO, no en el de su propia fecha.
 */
fun resumen(
    desde: LocalDate,
    hasta: LocalDate,
    gastos: List<MovGasto>,
    reembolsos: List<MovReembolso>,
    ingresos: List<MovIngreso>,
): Resumen {
    fun enRango(f: LocalDate) = !f.isBefore(desde) && !f.isAfter(hasta)
    val gs = gastos.filter { it.estado == Estado.CONFIRMADO && enRango(it.fecha) }
    val ids = gs.mapTo(HashSet()) { it.id }
    val pagado = gs.sumOf { it.importe }
    val devuelto = reembolsos.filter { it.gastoId in ids }.sumOf { it.importe }
    val ingresado = ingresos.filter { it.estado == Estado.CONFIRMADO && enRango(it.fecha) }.sumOf { it.importe }
    val real = pagado - devuelto
    val balance = ingresado - real
    return Resumen(
        ingresos = ingresado,
        pagado = pagado,
        reembolsos = devuelto,
        gastoReal = real,
        balance = balance,
        ahorroPorMil = porMil(balance, ingresado),
    )
}

/** (parte / total) ×1000 redondeado a la mitad hacia arriba; `null` si total ≤ 0. */
fun porMil(parte: Long, total: Long): Int? {
    if (total <= 0) return null
    return BigDecimal(parte).multiply(BigDecimal(1000))
        .divide(BigDecimal(total), 0, RoundingMode.HALF_UP).toInt()
}

/** Variación de [actual] respecto a [anterior], ×10. `null` si no hay referencia. */
fun variacionPorMil(actual: Long, anterior: Long): Int? {
    if (anterior <= 0) return null
    return porMil(actual - anterior, anterior)
}

/** Lo ahorrado en el año: suma de los balances de sus meses (= balance del año entero). */
fun ahorradoEnAnio(
    anio: Int,
    gastos: List<MovGasto>,
    reembolsos: List<MovReembolso>,
    ingresos: List<MovIngreso>,
): Long = resumen(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), gastos, reembolsos, ingresos).balance

/** Lo que te deben, agrupado por persona (clave `null` = sin persona asignada). */
fun teDeben(
    gastos: List<MovGasto>,
    reembolsos: List<MovReembolso>,
    personaDe: (gastoId: String) -> String?,
): Map<String?, Long> =
    gastos.asSequence()
        .filter { it.estado == Estado.CONFIRMADO && it.meLoDevuelven }
        .map { personaDe(it.id) to faltaPorRecuperar(it, reembolsos) }
        .filter { it.second > 0 }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, v) -> v.sum() }
