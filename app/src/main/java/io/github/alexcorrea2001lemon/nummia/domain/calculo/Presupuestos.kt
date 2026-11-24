package io.github.alexcorrea2001lemon.nummia.domain.calculo

import java.time.LocalDate
import java.time.YearMonth

enum class Ambito { TOTAL, CATEGORIA, TIENDA }

/** Cada cuánto se renueva un presupuesto. [codigo] es lo que se guarda en la base de datos. */
enum class Periodo(val codigo: Int, val nombre: String, val este: String) {
    MES(0, "Mensual", "este mes"), SEMANA(1, "Semanal", "esta semana"), ANIO(2, "Anual", "este año");
    companion object { fun de(codigo: Int) = entries.firstOrNull { it.codigo == codigo } ?: MES }
}

/** Un límite por semana, mes o año. [ref] es el id de la categoría o el nombre de la tienda (null en TOTAL). */
data class Limite(val id: String, val ambito: Ambito, val ref: String?, val limite: Long, val periodo: Periodo = Periodo.MES)

/**
 * Días que cuenta un presupuesto viendo el mes [ym]. La semana (de lunes a domingo) es la de [hoy] si [ym]
 * es el mes en curso; si no, la última semana de [ym].
 */
fun rangoPresupuesto(p: Periodo, ym: YearMonth, hoy: LocalDate): ClosedRange<LocalDate> = when (p) {
    Periodo.MES -> ym.atDay(1)..ym.atEndOfMonth()
    Periodo.ANIO -> LocalDate.of(ym.year, 1, 1)..LocalDate.of(ym.year, 12, 31)
    Periodo.SEMANA -> {
        val ref = if (YearMonth.from(hoy) == ym) hoy else ym.atEndOfMonth()
        val lunes = ref.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        lunes..lunes.plusDays(6)
    }
}

/** Identifica el periodo (para avisar una sola vez por semana, mes o año). */
fun clavePeriodo(p: Periodo, ym: YearMonth, hoy: LocalDate): String = when (p) {
    Periodo.MES -> ym.toString()
    Periodo.ANIO -> ym.year.toString()
    Periodo.SEMANA -> "s" + rangoPresupuesto(p, ym, hoy).start
}

/** Un gasto con su categoría y tienda, para saber a qué presupuesto pertenece. */
data class GastoDePresupuesto(val mov: MovGasto, val tienda: String, val categoriaId: String?)

enum class Nivel { BIEN, CERCA, PASADO }

data class EstadoPresupuesto(val limite: Limite, val gastado: Long) {
    /** % gastado ×10. */
    val porMil: Int get() = porMil(gastado.coerceAtLeast(0), limite.limite) ?: 0
    val nivel: Nivel get() = when {
        porMil >= 1000 -> Nivel.PASADO
        porMil >= 800 -> Nivel.CERCA
        else -> Nivel.BIEN
    }
    val queda: Long get() = limite.limite - gastado
}

/**
 * Gasto real del periodo que cuenta para cada presupuesto (solo lo CONFIRMADO; los reembolsos restan).
 * Las tiendas se comparan sin mayúsculas ni tildes.
 */
fun estadoPresupuestos(
    limites: List<Limite>, gastos: List<GastoDePresupuesto>, reembolsos: List<MovReembolso>,
    /** Con [ym] y [hoy], cada presupuesto cuenta solo los gastos de su semana, mes o año; sin ellos, todos los que lleguen. */
    ym: YearMonth? = null, hoy: LocalDate? = null,
): List<EstadoPresupuesto> {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val todosConfirmados = gastos.filter { it.mov.estado == Estado.CONFIRMADO }
    fun real(g: GastoDePresupuesto) = g.mov.importe - (devuelto[g.mov.id] ?: 0)
    return limites.map { l ->
        val confirmados = if (ym == null || hoy == null) todosConfirmados else {
            val r = rangoPresupuesto(l.periodo, ym, hoy)
            todosConfirmados.filter { it.mov.fecha in r }
        }
        val suyos = when (l.ambito) {
            Ambito.TOTAL -> confirmados
            Ambito.CATEGORIA -> confirmados.filter { it.categoriaId == l.ref }
            Ambito.TIENDA -> confirmados.filter { l.ref != null && claveTexto(it.tienda) == claveTexto(l.ref) }
        }
        EstadoPresupuesto(l, suyos.sumOf(::real))
    }
}

/** Umbrales que se acaban de cruzar (800 = 80 %, 1000 = 100 %), para avisar una sola vez de cada uno. */
fun umbralesCruzados(e: EstadoPresupuesto): List<Int> = listOf(800, 1000).filter { e.porMil >= it }
