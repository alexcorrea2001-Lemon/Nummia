package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class EstadisticasTest {
    private fun d(m: Int, dia: Int, a: Int = 2030) = LocalDate.of(a, m, dia)
    private fun g(id: String, f: LocalDate, imp: Long, est: Estado = Estado.CONFIRMADO) = MovGasto(id, f, imp, estado = est)

    @Test fun anioSinDatos() {
        val r = resumenAnual(2030, emptyList(), emptyList(), emptyList())
        assertEquals(12, r.meses.size)
        assertEquals(0L, r.mediaGasto)
        assertEquals(0L, r.mediaIngreso)
        assertNull(r.mesMasGasto)
        assertNull(r.mesMenosGasto)
        assertNull(r.total.ahorroPorMil)
    }

    @Test fun mediasSobreMesesConDatos() {
        val gs = listOf(g("a", d(1, 5), 1000), g("b", d(3, 5), 3000))
        val ins = listOf(MovIngreso(d(1, 31), 50000, TipoIngreso.SUELDO), MovIngreso(d(3, 31), 50000, TipoIngreso.SUELDO), MovIngreso(d(3, 6), 2000, TipoIngreso.EXTRA))
        val r = resumenAnual(2030, gs, emptyList(), ins)
        assertEquals(2000L, r.mediaGasto)
        assertEquals(51000L, r.mediaIngreso)
        assertEquals(100000L, r.sueldos)
        assertEquals(2000L, r.otrosIngresos)
        assertEquals(3, r.mesMasGasto)
        assertEquals(1, r.mesMenosGasto)
        assertEquals(4000L, r.total.gastoReal)
    }

    @Test fun empateEnMesDeMasGastoEligeElPrimero() {
        val r = resumenAnual(2030, listOf(g("a", d(2, 1), 500), g("b", d(5, 1), 500)), emptyList(), emptyList())
        assertEquals(2, r.mesMasGasto)
        assertEquals(2, r.mesMenosGasto)
    }

    @Test fun previstosNoCuentanEnElAnio() {
        val r = resumenAnual(2030, listOf(g("a", d(2, 1), 500, Estado.PREVISTO)), emptyList(), emptyList())
        assertNull(r.mesMasGasto)
    }

    @Test fun acumuladoDeFebrero() {
        val gs = listOf(g("a", d(2, 1), 100), g("b", d(2, 10), 250), g("c", d(2, 28), 50), g("x", d(3, 1), 999))
        val acc = acumuladoDiario(YearMonth.of(2030, 2), gs, listOf(MovReembolso("b", 50)))
        assertEquals(28, acc.size)
        assertEquals(100L, acc[0])
        assertEquals(100L, acc[8])
        assertEquals(300L, acc[9])
        assertEquals(350L, acc.last())
        assertTrue(acc.zipWithNext().all { (x, y) -> y >= x })
    }

    @Test fun acumuladoDeMesesDe30y31() {
        assertEquals(30, acumuladoDiario(YearMonth.of(2030, 4), emptyList(), emptyList()).size)
        assertEquals(31, acumuladoDiario(YearMonth.of(2030, 5), emptyList(), emptyList()).size)
    }

    @Test fun agrupaTiendasEscritasDeVariasFormas() {
        val gs = listOf(
            GastoEtiquetado(g("1", d(1, 1), 300), "Cafetería", "alimentacion"),
            GastoEtiquetado(g("2", d(1, 2), 200), "cafeteria", "alimentacion"),
            GastoEtiquetado(g("3", d(1, 3), 100), "Cafetería", null),
            GastoEtiquetado(g("4", d(1, 4), 5000), "Librería", "ocio"),
        )
        val r = porTienda(gs, listOf(MovReembolso("4", 1000)))
        assertEquals(listOf("Librería", "Cafetería"), r.map { it.nombre })
        assertEquals(GrupoGasto("Cafetería", 600, 3), r[1])
        assertEquals(200L, r[1].media)
        assertEquals(4000L, r[0].total)
        val c = porCategoria(gs, emptyList()) { it ?: "Sin categoría" }
        assertEquals(listOf("ocio", "alimentacion", "Sin categoría"), c.map { it.nombre })
    }

    @Test fun diaDeMasGasto() {
        val gs = listOf(g("1", d(1, 3), 300), g("2", d(1, 3), 200), g("3", d(1, 9), 400))
        assertEquals(d(1, 3) to 500L, diaMasGasto(gs, emptyList()))
        assertNull(diaMasGasto(emptyList(), emptyList()))
    }

    @Test fun calendarioConGastoNegativoYPrevistos() {
        val gs = listOf(g("1", d(1, 3), 1000), g("2", d(1, 4), 500, Estado.PREVISTO))
        val ins = listOf(MovIngreso(d(1, 31), 9000, estado = Estado.PREVISTO), MovIngreso(d(1, 3), 700))
        val r = porDia(gs, listOf(MovReembolso("1", 1200)), ins)
        assertEquals(DiaCalendario(-200, 700, false), r[d(1, 3)])
        assertEquals(DiaCalendario(0, 0, true), r[d(1, 4)])
        assertEquals(DiaCalendario(0, 0, true), r[d(1, 31)])
    }

    @Test fun compararConReferenciaVacia() {
        val a = resumenAnual(2030, emptyList(), emptyList(), emptyList())
        val b = resumenAnual(2031, listOf(g("1", d(1, 1, 2031), 23000)), emptyList(), emptyList())
        val c = Comparacion(a, b)
        assertEquals(23000L, c.diferenciaGasto)
        assertNull(c.porMilGasto)
        val c2 = Comparacion(b, b)
        assertEquals(0L, c2.diferenciaGasto)
        assertEquals(0, c2.porMilGasto)
    }
}
