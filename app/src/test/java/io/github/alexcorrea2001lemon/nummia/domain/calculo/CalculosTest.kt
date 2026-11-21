package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculosTest {
    private fun d(m: Int, dia: Int, a: Int = 2030) = LocalDate.of(a, m, dia)
    private val ene1 = d(1, 1)
    private val ene31 = d(1, 31)

    @Test fun gastoRealConReembolsoParcial() {
        val g = MovGasto("g", d(1, 15), 24000)
        assertEquals(12000L, gastoReal(g, listOf(MovReembolso("g", 12000))))
    }

    @Test fun variosReembolsosLlegandoACero() {
        val g = MovGasto("g", d(1, 15), 30000)
        assertEquals(0L, gastoReal(g, listOf(MovReembolso("g", 10000), MovReembolso("g", 20000))))
    }

    @Test fun reembolsoMayorQueElPrecioDaNegativo() {
        val g = MovGasto("g", d(7, 25), 47320)
        assertEquals(-180L, gastoReal(g, listOf(MovReembolso("g", 47500))))
    }

    @Test fun reembolsosDeOtrosGastosNoCuentan() {
        val g = MovGasto("g", d(1, 15), 1000)
        assertEquals(1000L, gastoReal(g, listOf(MovReembolso("otro", 500))))
    }

    @Test fun faltaPorRecuperarNuncaNegativa() {
        val g = MovGasto("g", d(1, 1), 34950, meLoDevuelven = true, esperado = 35000)
        assertEquals(35000L, faltaPorRecuperar(g, emptyList()))
        assertEquals(0L, faltaPorRecuperar(g, listOf(MovReembolso("g", 40000))))
        assertEquals(5000L, faltaPorRecuperar(g, listOf(MovReembolso("g", 30000))))
    }

    @Test fun faltaSinMeLoDevuelvenEsCero() {
        assertEquals(0L, faltaPorRecuperar(MovGasto("g", d(1, 1), 1000), emptyList()))
    }

    @Test fun faltaSinEsperadoUsaElImporte() {
        val g = MovGasto("g", d(1, 1), 18000, meLoDevuelven = true)
        assertEquals(18000L, faltaPorRecuperar(g, emptyList()))
    }

    @Test fun reembolsoCuentaEnElMesDelGasto() {
        val g = MovGasto("g", d(1, 28), 10000)
        val r = listOf(MovReembolso("g", 4000)) // recibido en febrero
        val enero = resumen(ene1, ene31, listOf(g), r, emptyList())
        val febrero = resumen(d(2, 1), d(2, 28), listOf(g), r, emptyList())
        assertEquals(4000L, enero.reembolsos)
        assertEquals(6000L, enero.gastoReal)
        assertEquals(0L, febrero.reembolsos)
        assertEquals(0L, febrero.pagado)
    }

    @Test fun previstosNoCuentan() {
        val gs = listOf(MovGasto("a", d(1, 1), 1249, estado = Estado.PREVISTO), MovGasto("b", d(1, 2), 275))
        val ins = listOf(
            MovIngreso(d(1, 31), 152075, TipoIngreso.SUELDO, Estado.PREVISTO),
            MovIngreso(d(1, 6), 4500, TipoIngreso.EXTRA),
        )
        val r = resumen(ene1, ene31, gs, emptyList(), ins)
        assertEquals(275L, r.pagado)
        assertEquals(4500L, r.ingresos)
    }

    @Test fun mesSinIngresosNoDivideEntreCero() {
        val r = resumen(ene1, ene31, listOf(MovGasto("a", d(1, 3), 500)), emptyList(), emptyList())
        assertNull(r.ahorroPorMil)
        assertEquals(-500L, r.balance)
    }

    @Test fun mesVacioEsTodoCero() {
        assertEquals(Resumen.VACIO, resumen(ene1, ene31, emptyList(), emptyList(), emptyList()))
    }

    @Test fun balanceYAhorro() {
        val r = resumen(
            d(10, 1), d(10, 31),
            listOf(MovGasto("a", d(10, 8), 22300)),
            emptyList(),
            listOf(MovIngreso(d(10, 31), 152075, TipoIngreso.SUELDO)),
        )
        assertEquals(129775L, r.balance)
        assertEquals(853, r.ahorroPorMil)
    }

    @Test fun limitesDelRangoIncluidos() {
        val gs = listOf(MovGasto("a", ene1, 100), MovGasto("b", ene31, 200), MovGasto("c", d(2, 1), 400))
        assertEquals(300L, resumen(ene1, ene31, gs, emptyList(), emptyList()).pagado)
    }

    @Test fun variacion() {
        assertEquals(115, variacionPorMil(22300, 20000))
        assertEquals(-500, variacionPorMil(5000, 10000))
        assertNull(variacionPorMil(100, 0))
    }

    @Test fun ahorradoEnAnioSumaLosMeses() {
        val gs = listOf(MovGasto("a", d(1, 5), 1000), MovGasto("b", d(6, 5), 2000), MovGasto("c", d(1, 5, 2031), 9999))
        val ins = listOf(MovIngreso(d(1, 31), 5000), MovIngreso(d(6, 30), 5000))
        val porMeses = (1..12).sumOf { m ->
            val ini = LocalDate.of(2030, m, 1)
            resumen(ini, ini.withDayOfMonth(ini.lengthOfMonth()), gs, emptyList(), ins).balance
        }
        assertEquals(7000L, ahorradoEnAnio(2030, gs, emptyList(), ins))
        assertEquals(porMeses, ahorradoEnAnio(2030, gs, emptyList(), ins))
    }

    @Test fun teDebenAgrupaPorPersona() {
        val gs = listOf(
            MovGasto("t1", d(5, 24), 30000, meLoDevuelven = true),
            MovGasto("t2", d(6, 25), 18000, meLoDevuelven = true),
            MovGasto("m", d(6, 1), 34950, meLoDevuelven = true, esperado = 35000),
            MovGasto("x", d(6, 2), 275),
        )
        val rs = listOf(MovReembolso("t1", 30000))
        val personas = mapOf("t1" to "Luis", "t2" to "Luis", "m" to "Ana")
        assertEquals(mapOf("Luis" to 18000L, "Ana" to 35000L), teDeben(gs, rs) { personas[it] })
    }
}
