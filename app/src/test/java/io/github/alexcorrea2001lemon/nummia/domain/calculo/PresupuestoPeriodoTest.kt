package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Presupuestos por semana, mes o año. Datos inventados. */
class PresupuestoPeriodoTest {
    private val hoy = LocalDate.of(2030, 11, 13) // miércoles
    private val nov = YearMonth.of(2030, 11)

    @Test fun rangos() {
        assertEquals(LocalDate.of(2030, 11, 11)..LocalDate.of(2030, 11, 17), rangoPresupuesto(Periodo.SEMANA, nov, hoy))
        assertEquals(LocalDate.of(2030, 11, 1)..LocalDate.of(2030, 11, 30), rangoPresupuesto(Periodo.MES, nov, hoy))
        assertEquals(LocalDate.of(2030, 1, 1)..LocalDate.of(2030, 12, 31), rangoPresupuesto(Periodo.ANIO, nov, hoy))
        // Mirando un mes pasado, la semana es la última de ese mes.
        assertEquals(LocalDate.of(2030, 9, 30)..LocalDate.of(2030, 10, 6), rangoPresupuesto(Periodo.SEMANA, YearMonth.of(2030, 9), hoy))
    }

    @Test fun cadaPresupuestoCuentaSoloSuPeriodo() {
        fun g(id: String, f: LocalDate, c: Long) = GastoDePresupuesto(MovGasto(id, f, c), "Súper", null)
        val gastos = listOf(
            g("a", LocalDate.of(2030, 11, 12), 3000), // esta semana
            g("b", LocalDate.of(2030, 11, 3), 2000), // este mes, otra semana
            g("c", LocalDate.of(2030, 2, 10), 5000), // este año, otro mes
        )
        val limites = listOf(
            Limite("s", Ambito.TOTAL, null, 10000, Periodo.SEMANA),
            Limite("m", Ambito.TOTAL, null, 10000, Periodo.MES),
            Limite("y", Ambito.TOTAL, null, 10000, Periodo.ANIO),
        )
        val e = estadoPresupuestos(limites, gastos, emptyList(), nov, hoy).associate { it.limite.id to it.gastado }
        assertEquals(mapOf("s" to 3000L, "m" to 5000L, "y" to 10000L), e)
    }

    @Test fun claveDelPeriodoParaNoAvisarDosVeces() {
        assertEquals("2030-11", clavePeriodo(Periodo.MES, nov, hoy))
        assertEquals("2030", clavePeriodo(Periodo.ANIO, nov, hoy))
        assertEquals("s2030-11-11", clavePeriodo(Periodo.SEMANA, nov, hoy))
    }
}
