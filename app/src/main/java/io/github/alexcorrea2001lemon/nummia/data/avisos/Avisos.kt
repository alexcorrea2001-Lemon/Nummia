package io.github.alexcorrea2001lemon.nummia.data.avisos

import io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.umbralesCruzados
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import java.time.LocalDate
import java.time.YearMonth

/** Una notificación. [clave] sirve para no repetirla nunca. */
data class Aviso(val clave: String, val titulo: String, val texto: String, val accion: AccionAviso? = null)

/** Lo que hace el botón «Confirmar» de la notificación. */
data class AccionAviso(val tipo: Tipo, val id: String, val importe: Long) {
    enum class Tipo { PAGO, SUELDO }
}

data class PagoDelDia(val id: String, val concepto: String, val importe: Long, val fecha: LocalDate)

data class PreferenciasAvisos(val sueldo: Boolean = true, val pagos: Boolean = true, val presupuestos: Boolean = true)

private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/**
 * Qué hay que avisar hoy y aún no se ha avisado:
 * - Sueldo previsto sin confirmar, desde el día 27.
 * - Pagos mensuales previstos de hoy (o ya pasados sin confirmar).
 * - Presupuestos al 80 % y al 100 % (una vez por mes y umbral).
 */
fun avisosPendientes(
    hoy: LocalDate,
    sueldoPrevisto: Boolean,
    pagos: List<PagoDelDia>,
    presupuestos: List<Pair<String, EstadoPresupuesto>>,
    hechos: Set<String>,
    pref: PreferenciasAvisos,
    sueldoId: String? = null,
    sueldoImporte: Long = 0,
): List<Aviso> {
    val ym = YearMonth.from(hoy)
    val res = ArrayList<Aviso>()
    if (pref.sueldo && sueldoPrevisto && hoy.dayOfMonth >= 27) {
        res += Aviso(
            "sueldo-$ym", "¿Ya has cobrado?", "Confirma tu sueldo de ${MESES[ym.monthValue - 1]} con el día y el importe reales.",
            sueldoId?.let { AccionAviso(AccionAviso.Tipo.SUELDO, it, sueldoImporte) },
        )
    }
    if (pref.pagos) {
        pagos.filter { !it.fecha.isAfter(hoy) }.forEach { p ->
            res += Aviso(
                "pago-${p.id}", "Hoy se paga ${p.concepto}", "${formatearEuros(p.importe)}. Confírmalo aquí o cambia el importe en Inicio.",
                AccionAviso(AccionAviso.Tipo.PAGO, p.id, p.importe),
            )
        }
    }
    if (pref.presupuestos) {
        presupuestos.forEach { (nombre, e) ->
            val umbral = umbralesCruzados(e).maxOrNull() ?: return@forEach
            val clave = "presupuesto-${e.limite.id}-$ym-$umbral"
            // Si ya se ha pasado del 100 %, no se avisa además del 80 %.
            res += if (umbral >= 1000) {
                Aviso(clave, "Te has pasado: $nombre", "Llevas ${formatearEuros(e.gastado)} de ${formatearEuros(e.limite.limite)} este mes.")
            } else {
                Aviso(clave, "Llevas el 80 %: $nombre", "Te quedan ${formatearEuros(e.queda)} de ${formatearEuros(e.limite.limite)} este mes.")
            }
        }
    }
    return res.filter { it.clave !in hechos }
}
