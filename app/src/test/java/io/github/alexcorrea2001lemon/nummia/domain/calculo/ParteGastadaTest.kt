package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Qué parte de lo que entró en el mes se gastó (para la barra de cada mes). */
class ParteGastadaTest {
    @Test fun parteDeLoQueEntro() {
        assertEquals(0.25f, parteGastada(gasto = 25000, ingresos = 100000)!!, 0.0001f)
        assertEquals(22, porcentajeGastado(30435, 141162)) // 21,56 % → 22 %
    }

    @Test fun siSeGastaMasDeLoQueEntroLaBarraSeLlena() {
        assertEquals(1f, parteGastada(150000, 100000)!!, 0f)
        assertEquals(150, porcentajeGastado(150000, 100000))
    }

    @Test fun sinIngresosNoHayParte() {
        assertNull(parteGastada(5000, 0))
        assertNull(porcentajeGastado(5000, 0))
    }

    @Test fun sinGastoEsCero() = assertEquals(0, porcentajeGastado(0, 100000))
}
