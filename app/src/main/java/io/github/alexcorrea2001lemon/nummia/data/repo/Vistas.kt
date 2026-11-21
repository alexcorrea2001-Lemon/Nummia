package io.github.alexcorrea2001lemon.nummia.data.repo

import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.MovGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.MovIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.MovReembolso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.faltaPorRecuperar
import io.github.alexcorrea2001lemon.nummia.domain.calculo.gastoReal
import java.time.LocalDate

fun GastoConReembolsos.aMov(): MovGasto = MovGasto(
    id = gasto.id,
    fecha = gasto.fecha,
    importe = gasto.importe,
    meLoDevuelven = gasto.meLoDevuelven,
    esperado = gasto.importeEsperado,
    estado = gasto.estado,
)

fun GastoConReembolsos.reembolsosMov(): List<MovReembolso> = activos.map { MovReembolso(it.gastoId, it.importe) }

val GastoConReembolsos.devuelto: Long get() = activos.sumOf { it.importe }
val GastoConReembolsos.real: Long get() = gastoReal(aMov(), reembolsosMov())
val GastoConReembolsos.falta: Long get() = faltaPorRecuperar(aMov(), reembolsosMov())

/** Fecha del último reembolso si llegó en un mes posterior al del gasto (para el aviso «Actualizado»). */
val GastoConReembolsos.reembolsoTardio: LocalDate?
    get() = activos.mapNotNull { it.fecha }
        .filter { it.year * 12 + it.monthValue > gasto.fecha.year * 12 + gasto.fecha.monthValue }
        .maxOrNull()

fun IngresoEntity.aMov(): MovIngreso = MovIngreso(fecha, importe, tipo, estado)

/** Datos completos de un periodo, listos para calcular y pintar. */
data class DatosPeriodo(
    val gastos: List<GastoConReembolsos>,
    val ingresos: List<IngresoEntity>,
) {
    val movGastos get() = gastos.map { it.aMov() }
    val movReembolsos get() = gastos.flatMap { it.reembolsosMov() }
    val movIngresos get() = ingresos.map { it.aMov() }
}
