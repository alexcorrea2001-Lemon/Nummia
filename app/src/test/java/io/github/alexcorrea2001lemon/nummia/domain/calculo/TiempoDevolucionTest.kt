package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Cuánto suele tardar alguien en devolverte el dinero (mediana de días). Datos inventados. */
class TiempoDevolucionTest {
    private fun d(dia: Int) = LocalDate.of(2030, 3, dia)

    @Test fun medianaDeLosDias() {
        // 2, 5 y 30 días → 5 (un retraso raro no lo estropea)
        assertEquals(5, diasMediosDevolucion(listOf(d(1) to d(3), d(1) to d(6), d(1) to d(31))))
        // pares: media de los dos del medio, redondeada
        assertEquals(4, diasMediosDevolucion(listOf(d(1) to d(3), d(1) to d(6))))
    }

    @Test fun mismoDiaEsCeroYFechasAlReves() {
        assertEquals(0, diasMediosDevolucion(listOf(d(10) to d(10))))
        assertEquals(0, diasMediosDevolucion(listOf(d(10) to d(8)))) // devolución antes del gasto: 0, nunca negativo
    }

    @Test fun sinDevolucionesNada() = assertNull(diasMediosDevolucion(emptyList()))

    @Test fun textoAmable() {
        assertEquals("Suele devolverte el mismo día", textoDevolucion(0))
        assertEquals("Suele devolverte en 1 día", textoDevolucion(1))
        assertEquals("Suele devolverte en unos 5 días", textoDevolucion(5))
        assertEquals("Suele devolverte en unas 3 semanas", textoDevolucion(20))
        assertEquals("Suele devolverte en unos 2 meses", textoDevolucion(65))
    }
}
