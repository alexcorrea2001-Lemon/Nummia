package io.github.alexcorrea2001lemon.nummia.data.avisos

import io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.umbralesCruzados
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import java.time.LocalDate
import java.time.YearMonth

/** Una notificación. [clave] sirve para no repetirla nunca. [abrir] es la acción de MainActivity al tocarla. */
data class Aviso(val clave: String, val titulo: String, val texto: String, val accion: AccionAviso? = null, val abrir: String? = null)

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
            val clave = "presupuesto-${e.limite.id}-${io.github.alexcorrea2001lemon.nummia.domain.calculo.clavePeriodo(e.limite.periodo, ym, hoy)}-$umbral"
            // Si ya se ha pasado del 100 %, no se avisa además del 80 %.
            res += if (umbral >= 1000) {
                Aviso(clave, "Te has pasado: $nombre", "Llevas ${formatearEuros(e.gastado)} de ${formatearEuros(e.limite.limite)} ${e.limite.periodo.este}.")
            } else {
                Aviso(clave, "Llevas el 80 %: $nombre", "Te quedan ${formatearEuros(e.queda)} de ${formatearEuros(e.limite.limite)} ${e.limite.periodo.este}.")
            }
        }
    }
    return res.filter { it.clave !in hechos }
}

/**
 * Sin Drive conectado, una vez al mes se recuerda guardar una copia en archivo si la última tiene 30 días o más
 * (o no hay ninguna). A quien acaba de empezar (menos de una semana con datos) no se le molesta.
 */
fun avisoCopia(hoy: LocalDate, driveConectado: Boolean, ultimaCopia: LocalDate?, primerMovimiento: LocalDate?): Aviso? {
    if (driveConectado || primerMovimiento == null || primerMovimiento.isAfter(hoy.minusDays(7))) return null
    if (ultimaCopia != null && ultimaCopia.isAfter(hoy.minusDays(30))) return null
    return Aviso(
        "copia-${YearMonth.from(hoy)}", "Guarda una copia de tus datos",
        "No tienes Drive conectado. Guarda una copia en un archivo (o conecta Drive) para no perder nada si cambias de móvil.",
        abrir = io.github.alexcorrea2001lemon.nummia.MainActivity.ACCION_DATOS,
    )
}
