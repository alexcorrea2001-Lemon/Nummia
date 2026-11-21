package io.github.alexcorrea2001lemon.nummia.domain

import io.github.alexcorrea2001lemon.nummia.data.avisos.PagoDelDia
import io.github.alexcorrea2001lemon.nummia.data.avisos.PreferenciasAvisos
import io.github.alexcorrea2001lemon.nummia.data.avisos.avisosPendientes
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Ambito
import io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Limite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AvisosTest {
    private val hoy = LocalDate.of(2030, 3, 28)
    private val pref = PreferenciasAvisos()
    private fun pres(gastado: Long) = "Comida" to EstadoPresupuesto(Limite("p", Ambito.TOTAL, null, 10000), gastado)

    @Test fun sueldoDesdeElDia27() {
        assertEquals(listOf("sueldo-2030-03"), avisosPendientes(hoy, true, emptyList(), emptyList(), emptySet(), pref).map { it.clave })
        assertTrue(avisosPendientes(hoy.withDayOfMonth(26), true, emptyList(), emptyList(), emptySet(), pref).isEmpty())
        assertTrue(avisosPendientes(hoy, false, emptyList(), emptyList(), emptySet(), pref).isEmpty())
    }

    @Test fun pagosDeHoyYAtrasadosPeroNoLosFuturos() {
        val pagos = listOf(PagoDelDia("a", "Gimnasio", 2999, hoy), PagoDelDia("b", "Móvil", 1000, hoy.minusDays(2)), PagoDelDia("c", "Luz", 5000, hoy.plusDays(1)))
        assertEquals(listOf("pago-a", "pago-b"), avisosPendientes(hoy, false, pagos, emptyList(), emptySet(), pref).map { it.clave })
    }

    @Test fun presupuestoUnaSolaVezPorUmbral() {
        assertTrue(avisosPendientes(hoy, false, emptyList(), listOf(pres(7900)), emptySet(), pref).isEmpty())
        val a80 = avisosPendientes(hoy, false, emptyList(), listOf(pres(8000)), emptySet(), pref).single()
        assertEquals("presupuesto-p-2030-03-800", a80.clave)
        assertTrue(avisosPendientes(hoy, false, emptyList(), listOf(pres(9000)), setOf(a80.clave), pref).isEmpty())
        val a100 = avisosPendientes(hoy, false, emptyList(), listOf(pres(10500)), setOf(a80.clave), pref).single()
        assertEquals("presupuesto-p-2030-03-1000", a100.clave)
        assertTrue(a100.titulo.startsWith("Te has pasado"))
    }

    @Test fun respetaLasPreferencias() {
        val r = avisosPendientes(hoy, true, listOf(PagoDelDia("a", "Gimnasio", 2999, hoy)), listOf(pres(10500)), emptySet(), PreferenciasAvisos(false, false, false))
        assertTrue(r.isEmpty())
    }
}
