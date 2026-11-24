package io.github.alexcorrea2001lemon.nummia.domain.calculo

import io.github.alexcorrea2001lemon.nummia.data.avisos.Aviso
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * ¿Toca pagar en [ym]? Mensual (o sin mes de inicio): siempre. Si no, cada [cadaMeses] meses contando
 * desde [desde] (el mes del primer cobro); antes de ese mes, nunca.
 */
fun tocaEnMes(cadaMeses: Int, desde: YearMonth?, ym: YearMonth): Boolean {
    if (cadaMeses <= 1 || desde == null) return true
    val n = ChronoUnit.MONTHS.between(desde, ym)
    return n >= 0 && n % cadaMeses == 0L
}

/** Primera fecha de cobro a partir de [hoy] (incluido). El día 31 en un mes corto es el último día. */
fun proximoCobro(dia: Int, cadaMeses: Int, desde: YearMonth?, hoy: LocalDate): LocalDate {
    var ym = YearMonth.from(hoy)
    repeat(12 * 10 + 1) {
        if (tocaEnMes(cadaMeses, desde, ym)) {
            val f = ym.atDay(dia.coerceIn(1, ym.lengthOfMonth()))
            if (!f.isBefore(hoy)) return f
        }
        ym = ym.plusMonths(1)
    }
    return hoy
}

/** Lo que cuesta en un año (en céntimos). */
fun costeAnual(importe: Long, cadaMeses: Int): Long = importe * 12 / cadaMeses.coerceAtLeast(1)

data class PagoProximo(val id: String, val concepto: String, val importe: Long, val fecha: LocalDate, val cadaMeses: Int)

/**
 * Los pagos trimestrales y anuales avisan 3 días antes (los mensuales ya avisan el mismo día):
 * así no te pilla por sorpresa un cargo grande.
 */
fun avisosAntesDelCobro(hoy: LocalDate, pagos: List<PagoProximo>): List<Aviso> =
    pagos.filter { it.cadaMeses > 1 && it.fecha.isAfter(hoy) && !it.fecha.isAfter(hoy.plusDays(3)) }.map { p ->
        val cuando = when (ChronoUnit.DAYS.between(hoy, p.fecha)) { 1L -> "mañana"; else -> "dentro de ${ChronoUnit.DAYS.between(hoy, p.fecha)} días" }
        Aviso("antes-${p.id}-${YearMonth.from(p.fecha)}", "Se acerca un pago: ${p.concepto}", "Se cobra $cuando: ${formatearEuros(p.importe)}.")
    }
