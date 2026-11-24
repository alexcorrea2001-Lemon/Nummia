package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Test

/** Reparto del gasto por categorías para la barra de Inicio: las más grandes y el resto en «Otras». */
class RepartoTest {
    @Test fun ordenaDeMayorAMenorYJuntaElRestoEnOtras() {
        val r = repartoConOtras(
            listOf("Ocio" to 1000L, "Comida" to 5000L, "Casa" to 3000L, "Ropa" to 500L, "Coche" to 200L),
            maximo = 3,
        )
        assertEquals(listOf("Comida" to 5000L, "Casa" to 3000L, "Ocio" to 1000L, "Otras" to 700L), r)
    }

    @Test fun siCabenTodasNoHayOtras() {
        assertEquals(listOf("Casa" to 300L, "Ocio" to 100L), repartoConOtras(listOf("Ocio" to 100L, "Casa" to 300L), maximo = 3))
    }

    @Test fun juntaLasRepetidasYDescartaCerosYNegativos() {
        val r = repartoConOtras(listOf("Ocio" to 100L, "Ocio" to 250L, "Casa" to 0L, "Ropa" to -40L), maximo = 3)
        assertEquals(listOf("Ocio" to 350L), r)
    }

    @Test fun unaSolaSobranteNoSeLlamaOtras() {
        // Si solo sobra una, se enseña con su nombre en vez de «Otras».
        val r = repartoConOtras(listOf("A" to 400L, "B" to 300L, "C" to 200L, "D" to 100L), maximo = 3)
        assertEquals(listOf("A" to 400L, "B" to 300L, "C" to 200L, "D" to 100L), r)
    }

    @Test fun sinGastoNoHayReparto() {
        assertEquals(emptyList<Pair<String, Long>>(), repartoConOtras(emptyList(), maximo = 3))
    }
}
