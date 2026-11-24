package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** «El mes en cifras» de Estadísticas. Datos inventados. */
class CifrasMesTest {
    private val nov = YearMonth.of(2030, 11)
    private fun g(id: String, dia: Int, c: Long, estado: Estado = Estado.CONFIRMADO) = MovGasto(id, nov.atDay(dia), c, estado = estado)

    private val gastos = listOf(
        g("a", 1, 1000), g("b", 1, 500), g("c", 4, 3000), g("d", 9, 2000),
        g("p", 20, 9999, Estado.PREVISTO), // lo previsto no cuenta
    )
    private val devoluciones = listOf(MovReembolso("c", 1000)) // c cuesta de verdad 20 €

    @Test fun mesEnCurso() {
        val c = cifrasMes(nov, LocalDate.of(2030, 11, 10), gastos, devoluciones)
        // Gasto real 10 + 5 + 20 + 20 = 55 € en 10 días
        assertEquals(10, c.dias)
        assertEquals(550L, c.mediaDia)
        assertEquals(4, c.gastos)
        assertEquals(1375L, c.gastoMedio)
        assertEquals(7, c.diasSinGastar) // gastó los días 1, 4 y 9
        assertEquals(16500L, c.prevision) // 55 € en 10 días → 165 € en 30
    }

    @Test fun mesPasadoCuentaTodosLosDiasYSinPrevision() {
        val c = cifrasMes(nov, LocalDate.of(2030, 12, 5), gastos, devoluciones)
        assertEquals(30, c.dias)
        assertEquals(183L, c.mediaDia) // 55 € / 30 = 1,833…
        assertEquals(27, c.diasSinGastar)
        assertNull(c.prevision)
    }

    @Test fun losPrimerosDiasNoHayPrevision() = assertNull(cifrasMes(nov, LocalDate.of(2030, 11, 2), gastos, devoluciones).prevision)

    @Test fun sinGastos() {
        val c = cifrasMes(nov, LocalDate.of(2030, 11, 10), emptyList(), emptyList())
        assertEquals(0, c.gastos)
        assertNull(c.gastoMedio)
        assertEquals(10, c.diasSinGastar)
    }

    @Test fun mesFuturoTodoACero() {
        val c = cifrasMes(nov, LocalDate.of(2030, 10, 1), gastos, devoluciones)
        assertEquals(0, c.dias)
        assertEquals(0L, c.mediaDia)
        assertNull(c.prevision)
    }
}
