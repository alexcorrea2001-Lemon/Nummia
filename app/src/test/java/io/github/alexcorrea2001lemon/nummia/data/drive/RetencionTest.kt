package io.github.alexcorrea2001lemon.nummia.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RetencionTest {
    @Test fun nombreYFechaVanYVuelven() {
        val f = LocalDateTime.of(2030, 3, 15, 21, 30)
        assertEquals("Nummia_copia_2030-03-15_21-30.nummia", nombreCopia(f))
        assertEquals(f, fechaDeCopia(nombreCopia(f)))
        assertNull(fechaDeCopia("otra cosa.nummia"))
    }

    @Test fun pocasCopiasNoSeBorraNada() {
        val copias = (1..5).map { "c$it" to LocalDateTime.of(2030, 3, it, 10, 0) }
        assertTrue(copiasABorrar(copias).isEmpty())
    }

    @Test fun variasElMismoDiaSoloQuedaLaUltima() {
        val copias = listOf("m" to LocalDateTime.of(2030, 3, 15, 9, 0), "t" to LocalDateTime.of(2030, 3, 15, 20, 0))
        assertEquals(listOf("m"), copiasABorrar(copias))
    }

    @Test fun unAnioDiarioQuedan7Dias4SemanasY12Meses() {
        val inicio = LocalDateTime.of(2030, 1, 1, 12, 0)
        val copias = (0 until 400).map { "d$it" to inicio.plusDays(it.toLong()) }
        val quedan = copias.map { it.first }.toSet() - copiasABorrar(copias).toSet()
        // 7 días + hasta 4 semanas y 12 meses (algunas coinciden): nunca más de 23 ni menos de 12.
        assertTrue(quedan.size in 12..23)
        (393 until 400).forEach { assertTrue("d$it" in quedan) } // los últimos 7 días
        assertTrue(quedan.none { it == "d0" }) // la más antigua (hace más de 12 meses) se va
    }

    @Test fun lasCopiasSinFechaNoSeTocan() {
        val copias = (1..30).map { "c$it" to LocalDateTime.of(2030, 3, 1, 0, 0).plusDays(it.toLong()) } + ("rara" to null)
        assertTrue("rara" !in copiasABorrar(copias))
    }
}
