package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.ui.graficos.ejeBonito
import io.github.alexcorrea2001lemon.nummia.ui.graficos.textoEje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Números del lateral de las gráficas: redondos y que cubran todos los datos. */
class EjeGraficoTest {
    @Test fun marcasRedondasDesdeCero() {
        assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), ejeBonito(0, 69_749)) // hasta 697,49 € → 0, 250, 500, 750 €
        assertEquals(listOf(0L, 50_000L, 100_000L, 150_000L), ejeBonito(0, 140_000))
    }

    @Test fun conNegativosEmpiezaPorDebajo() {
        val m = ejeBonito(-3_000, 9_000)
        assertTrue(m.first() <= -3_000 && m.last() >= 9_000)
        assertTrue(0L in m)
    }

    @Test fun sinDatosNoFalla() {
        val m = ejeBonito(0, 0)
        assertTrue(m.size >= 2 && m.first() == 0L)
    }

    @Test fun textoCortoEnEuros() {
        assertEquals("0 €", textoEje(0))
        assertEquals("250 €", textoEje(25_000))
        assertEquals("1.500 €", textoEje(150_000))
        assertEquals("2,5 €", textoEje(250))
        assertEquals("−50 €", textoEje(-5_000))
    }
}
