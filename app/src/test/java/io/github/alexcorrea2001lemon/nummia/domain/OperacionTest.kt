package io.github.alexcorrea2001lemon.nummia.domain

import io.github.alexcorrea2001lemon.nummia.domain.dinero.esOperacion
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperacionTest {
    @Test fun sumas() {
        assertEquals(1550L, leerImporteOperacion("12+3,50").getOrThrow())
        assertEquals(600L, leerImporteOperacion("1+2+3").getOrThrow())
        assertEquals(152075L, leerImporteOperacion("1.500+20,75").getOrThrow())
    }

    @Test fun restasConLosDosSignos() {
        assertEquals(1525L, leerImporteOperacion("20−4,75").getOrThrow())
        assertEquals(1525L, leerImporteOperacion("20-4,75").getOrThrow())
    }

    @Test fun unSoloNumeroEsComoSiempre() {
        assertEquals(275L, leerImporteOperacion("2,75").getOrThrow())
        assertFalse(esOperacion("2,75"))
        assertTrue(esOperacion("2+1"))
    }

    @Test fun operadorAlFinalSeIgnora() {
        assertEquals(1200L, leerImporteOperacion("12+").getOrThrow())
    }

    @Test fun errores() {
        assertTrue(leerImporteOperacion("5−10").isFailure) // total negativo
        assertTrue(leerImporteOperacion("5−5").isFailure) // total cero
        assertTrue(leerImporteOperacion("2,555+1").isFailure) // 3 decimales
        assertTrue(leerImporteOperacion("2++1").isFailure)
    }
}
