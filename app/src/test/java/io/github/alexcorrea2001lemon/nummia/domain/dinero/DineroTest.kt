package io.github.alexcorrea2001lemon.nummia.domain.dinero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DineroTest {
    private val e = " €"

    @Test fun formateaConMilesYDecimales() {
        assertEquals("1.520,75$e", formatearEuros(152075))
        assertEquals("24.573,19$e", formatearEuros(2457319))
        assertEquals("0,00$e", formatearEuros(0))
        assertEquals("0,05$e", formatearEuros(5))
        assertEquals("1.000.000,00$e", formatearEuros(100_000_000))
    }

    @Test fun formateaSignos() {
        assertEquals("−1,20$e", formatearEuros(-120, conSigno = true))
        assertEquals("−1,20$e", formatearEuros(-120))
        assertEquals("+1,20$e", formatearEuros(120, conSigno = true))
        assertEquals("0,00$e", formatearEuros(0, conSigno = true))
    }

    @Test fun leeImportesValidos() {
        assertEquals(270L, leerImporte("2,7").getOrThrow())
        assertEquals(270L, leerImporte("2,70").getOrThrow())
        assertEquals(152075L, leerImporte("1.520,75").getOrThrow())
        assertEquals(152075L, leerImporte("1520.75").getOrThrow())
        assertEquals(152000L, leerImporte("1.520").getOrThrow())
        assertEquals(1200L, leerImporte(" 12 ").getOrThrow())
        assertEquals(50L, leerImporte(",5").getOrThrow())
        assertEquals(34950L, leerImporte("349,50 €").getOrThrow())
    }

    @Test fun rechazaImportesInvalidos() {
        for (t in listOf("0,005", "", "   ", "abc", "-5", "0", "0,00", "1,2,3", "12a", "100000000,00")) {
            val r = leerImporte(t)
            assertTrue("\"$t\" debería fallar", r.isFailure)
            assertTrue(r.exceptionOrNull() is ImporteInvalido)
        }
    }

    @Test fun mensajesClaros() {
        assertEquals("Escribe un importe", (leerImporte("").exceptionOrNull() as ImporteInvalido).motivo)
        assertEquals("Como máximo 2 decimales", (leerImporte("0,005").exceptionOrNull() as ImporteInvalido).motivo)
        assertEquals("El importe no puede ser negativo", (leerImporte("-5").exceptionOrNull() as ImporteInvalido).motivo)
        assertEquals("El importe debe ser mayor que 0", (leerImporte("0").exceptionOrNull() as ImporteInvalido).motivo)
    }

    @Test fun formateaPorcentajes() {
        assertEquals("90,7 %", formatearPorMil(907))
        assertEquals("−1,4 %", formatearPorMil(-14))
        assertEquals("1,4 %", formatearPorMil(14))
        assertEquals("—", formatearPorMil(null))
    }

    @Test fun ocultaImportesEnModoDiscreto() {
        assertEquals("••••$e", formatearEuros(152075, discreto = true))
    }
}
