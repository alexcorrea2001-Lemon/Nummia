package io.github.alexcorrea2001lemon.nummia.domain.calculo

import java.time.LocalDate

enum class Estado { CONFIRMADO, PREVISTO }

enum class TipoIngreso { SUELDO, EXTRA, VENTA, TRABAJO_EXTRA, OTRO }

/** Vista mínima de un gasto para los cálculos. Los borrados nunca llegan aquí. */
data class MovGasto(
    val id: String,
    val fecha: LocalDate,
    val importe: Long,
    val meLoDevuelven: Boolean = false,
    val esperado: Long? = null,
    val estado: Estado = Estado.CONFIRMADO,
)

data class MovReembolso(val gastoId: String, val importe: Long)

data class MovIngreso(
    val fecha: LocalDate,
    val importe: Long,
    val tipo: TipoIngreso = TipoIngreso.OTRO,
    val estado: Estado = Estado.CONFIRMADO,
)

/** Resumen de un periodo. [ahorroPorMil] es el % de ahorro ×10 (un decimal), `null` sin ingresos. */
data class Resumen(
    val ingresos: Long,
    val pagado: Long,
    val reembolsos: Long,
    val gastoReal: Long,
    val balance: Long,
    val ahorroPorMil: Int?,
) {
    companion object {
        val VACIO = Resumen(0, 0, 0, 0, 0, null)
    }
}
