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

/** Parte (0–1) de lo que entró en el mes que se gastó; null si no entró nada. Más de lo que entró → 1. */
fun parteGastada(gasto: Long, ingresos: Long): Float? =
    if (ingresos <= 0) null else (gasto.coerceAtLeast(0).toDouble() / ingresos).coerceIn(0.0, 1.0).toFloat()

/** Porcentaje redondeado de lo que entró que se gastó (puede pasar de 100); null si no entró nada. */
fun porcentajeGastado(gasto: Long, ingresos: Long): Int? =
    if (ingresos <= 0) null else Math.round(gasto.coerceAtLeast(0).toDouble() * 100 / ingresos).toInt()

/** El mes en cifras. [dias]: días ya vividos del mes (todos si ya pasó; 0 si aún no ha llegado). */
data class CifrasMes(
    val dias: Int,
    val mediaDia: Long,
    val gastos: Int,
    val gastoMedio: Long?,
    val diasSinGastar: Int,
    /** Solo en el mes en curso y a partir del día 3: a qué ritmo acabará el mes si sigues así. */
    val prevision: Long?,
)

fun cifrasMes(ym: YearMonth, hoy: LocalDate, gastos: List<MovGasto>, reembolsos: List<MovReembolso>): CifrasMes {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val delMes = gastos.filter { confirmado(it) && YearMonth.from(it.fecha) == ym }
    val dias = when {
        ym == YearMonth.from(hoy) -> hoy.dayOfMonth
        ym.isBefore(YearMonth.from(hoy)) -> ym.lengthOfMonth()
        else -> 0
    }
    val total = delMes.sumOf { it.importe - (devuelto[it.id] ?: 0) }
    val conGasto = delMes.filter { it.fecha.dayOfMonth <= dias }.map { it.fecha }.toSet().size
    val enCurso = ym == YearMonth.from(hoy)
    return CifrasMes(
        dias = dias,
        mediaDia = if (dias > 0) Math.round(total.toDouble() / dias) else 0L,
        gastos = delMes.size,
        gastoMedio = if (delMes.isEmpty()) null else Math.round(total.toDouble() / delMes.size),
        diasSinGastar = (dias - conGasto).coerceAtLeast(0),
        prevision = if (enCurso && dias >= 3) Math.round(total.toDouble() * ym.lengthOfMonth() / dias) else null,
    )
}

/**
 * Lo que te quedará a fin de mes: [ahora] (ingresado − gastado de verdad) + [porCobrar] (ingresos previstos)
 * − [porPagar] (pagos previstos) − [ritmo] (gasto normal, sin pagos fijos, al ritmo de este mes por los días
 * que quedan; desde el día 3).
 */
data class PrevisionMes(val ahora: Long, val porCobrar: Long, val porPagar: Long, val ritmo: Long) {
    val quedara: Long get() = ahora + porCobrar - porPagar - ritmo
}

fun previsionFinDeMes(
    ym: YearMonth, hoy: LocalDate, gastos: List<MovGasto>, reembolsos: List<MovReembolso>, ingresos: List<MovIngreso>, fijos: Set<String>,
): PrevisionMes? {
    if (ym != YearMonth.from(hoy)) return null
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val delMes = gastos.filter { YearMonth.from(it.fecha) == ym }
    val hechos = delMes.filter(::confirmado)
    fun real(g: MovGasto) = g.importe - (devuelto[g.id] ?: 0)
    val ingMes = ingresos.filter { YearMonth.from(it.fecha) == ym }
    val ahora = ingMes.filter { it.estado == Estado.CONFIRMADO }.sumOf { it.importe } - hechos.sumOf(::real)
    val normal = hechos.filter { it.id !in fijos }.sumOf(::real)
    val dia = hoy.dayOfMonth
    val quedan = ym.lengthOfMonth() - dia
    val ritmo = if (dia >= 3 && quedan > 0) Math.round(normal.toDouble() / dia * quedan).coerceAtLeast(0) else 0L
    return PrevisionMes(
        ahora = ahora,
        porCobrar = ingMes.filter { it.estado == Estado.PREVISTO }.sumOf { it.importe },
        porPagar = delMes.filter { it.estado == Estado.PREVISTO }.sumOf { it.importe },
        ritmo = ritmo,
    )
}

/**
 * Precio medio de cada compra, mes a mes (para ver si una tienda sube). Un mes sin compras repite el
 * anterior; los meses antes de la primera compra toman esa primera. Sin compras, lista vacía.
 */
fun precioMedioPorMes(meses: List<YearMonth>, compras: List<Pair<YearMonth, Long>>): List<Long> {
    val porMes = compras.groupBy({ it.first }, { it.second }).mapValues { (_, l) -> Math.round(l.average()) }
    val primero = meses.firstNotNullOfOrNull { porMes[it] } ?: return emptyList()
    var ultimo = primero
    return meses.map { m -> porMes[m]?.also { ultimo = it } ?: ultimo }
}

/** Días que suele tardar alguien en devolver (mediana de gasto → devolución; nunca negativo). Sin datos, null. */
fun diasMediosDevolucion(pares: List<Pair<LocalDate, LocalDate>>): Int? {
    if (pares.isEmpty()) return null
    val dias = pares.map { (g, d) -> java.time.temporal.ChronoUnit.DAYS.between(g, d).coerceAtLeast(0) }.sorted()
    val n = dias.size
    return if (n % 2 == 1) dias[n / 2].toInt() else Math.round((dias[n / 2 - 1] + dias[n / 2]) / 2.0).toInt()
}

fun textoDevolucion(dias: Int): String = "Suele devolverte " + when {
    dias <= 0 -> "el mismo día"
    dias == 1 -> "en 1 día"
    dias < 14 -> "en unos $dias días"
    dias < 60 -> "en unas ${Math.round(dias / 7.0)} semanas"
    else -> "en unos ${Math.round(dias / 30.0)} meses"
}
