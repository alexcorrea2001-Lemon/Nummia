package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Pagos que se repiten cada mes, cada 3 meses o cada año. Datos inventados. */
class SuscripcionesTest {
    private val marzo = YearMonth.of(2030, 3)

    @Test fun mensualTocaSiempre() {
        assertTrue(tocaEnMes(1, null, marzo))
        assertTrue(tocaEnMes(1, YearMonth.of(2029, 7), marzo))
    }

    @Test fun trimestralCadaTresMesesDesdeElPrimero() {
        val desde = YearMonth.of(2030, 1)
        assertTrue(tocaEnMes(3, desde, YearMonth.of(2030, 1)))
        assertFalse(tocaEnMes(3, desde, YearMonth.of(2030, 2)))
        assertFalse(tocaEnMes(3, desde, marzo))
        assertTrue(tocaEnMes(3, desde, YearMonth.of(2030, 4)))
        assertTrue(tocaEnMes(3, desde, YearMonth.of(2031, 1)))
        assertFalse(tocaEnMes(3, desde, YearMonth.of(2029, 10))) // antes de empezar, no
    }

    @Test fun anualUnaVezAlAno() {
        val desde = YearMonth.of(2030, 3)
        assertTrue(tocaEnMes(12, desde, marzo))
        assertFalse(tocaEnMes(12, desde, YearMonth.of(2030, 4)))
        assertTrue(tocaEnMes(12, desde, YearMonth.of(2032, 3)))
    }

    @Test fun proximoCobro() {
        val hoy = LocalDate.of(2030, 3, 15)
        assertEquals(LocalDate.of(2030, 3, 20), proximoCobro(20, 1, null, hoy))
        assertEquals(LocalDate.of(2030, 4, 10), proximoCobro(10, 1, null, hoy)) // el 10 ya pasó
        assertEquals(LocalDate.of(2030, 4, 30), proximoCobro(31, 3, YearMonth.of(2030, 1), hoy)) // abril tiene 30 días
        assertEquals(LocalDate.of(2031, 3, 1), proximoCobro(1, 12, YearMonth.of(2030, 3), hoy))
        assertEquals(LocalDate.of(2030, 6, 5), proximoCobro(5, 12, YearMonth.of(2030, 6), hoy)) // aún no ha empezado
    }

    @Test fun costeAlAno() {
        assertEquals(12_000L, costeAnual(1_000, 1))
        assertEquals(4_000L, costeAnual(1_000, 3))
        assertEquals(32_000L, costeAnual(32_000, 12))
    }

    @Test fun avisoTresDiasAntesSoloDeLosQueNoSonMensuales() {
        val hoy = LocalDate.of(2030, 3, 15)
        val pagos = listOf(
            PagoProximo("seguro", "Seguro Ejemplo", 32_000, LocalDate.of(2030, 3, 18), cadaMeses = 12),
            PagoProximo("musica", "Música Ejemplo", 999, LocalDate.of(2030, 3, 17), cadaMeses = 1),
            PagoProximo("agua", "Agua Ejemplo", 4_500, LocalDate.of(2030, 3, 25), cadaMeses = 3),
        )
        val a = avisosAntesDelCobro(hoy, pagos)
        assertEquals(listOf("antes-seguro-2030-03"), a.map { it.clave })
        assertTrue(a.single().texto.contains("320,00"))
    }
}
