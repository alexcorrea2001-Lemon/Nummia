package io.github.alexcorrea2001lemon.nummia.data.drive

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReconciliadorTest {
    private fun c(vararg v: String): Canon = v.toList()
    private val a = c("G-0001", "1", "Cafetería", "Café", "275")
    private val a2 = c("G-0001", "1", "Cafetería", "Café", "325")
    private val a3 = c("G-0001", "1", "Cafetería", "Café", "400")
    private val b = c("G-0002", "2", "Librería", "Libro", "5180")

    @Test fun todoIgualNoHaceNada() {
        val p = reconciliar(mapOf("a" to a), listOf(FilaRemota(2, "a", a)), mapOf("a" to firma(a)))
        assertTrue(!p.hayCambiosRemotos && !p.hayCambiosLocales && p.conflictos.isEmpty())
        assertEquals(mapOf("a" to firma(a)), p.instantanea)
    }

    @Test fun altaEnElMovilSeAnadeALaHoja() {
        val p = reconciliar(mapOf("a" to a, "b" to b), listOf(FilaRemota(2, "a", a)), mapOf("a" to firma(a)))
        assertEquals(listOf("b" to b), p.anadirRemoto)
        assertEquals(firma(b), p.instantanea["b"])
    }

    @Test fun cambioEnElMovilActualizaSuFila() {
        val p = reconciliar(mapOf("a" to a2), listOf(FilaRemota(5, "a", a)), mapOf("a" to firma(a)))
        assertEquals(listOf(Triple(5, "a", a2)), p.actualizarRemoto)
    }

    @Test fun borradoEnElMovilBorraLaFila() {
        val p = reconciliar(emptyMap(), listOf(FilaRemota(3, "a", a), FilaRemota(7, "b", b)), mapOf("a" to firma(a), "b" to firma(b)))
        assertEquals(listOf(7, 3), p.borrarRemoto)
        assertTrue(p.instantanea.isEmpty())
    }

    @Test fun cambioEnLaHojaSeAplicaEnElMovil() {
        val p = reconciliar(mapOf("a" to a), listOf(FilaRemota(2, "a", a2)), mapOf("a" to firma(a)))
        assertEquals(listOf("a" to a2), p.aplicarLocal)
        assertEquals(firma(a2), p.instantanea["a"])
    }

    @Test fun filaBorradaEnLaHojaSeBorraEnElMovil() {
        val p = reconciliar(mapOf("a" to a), emptyList(), mapOf("a" to firma(a)))
        assertEquals(listOf("a"), p.borrarLocal)
        assertTrue(p.instantanea.isEmpty())
    }

    @Test fun altaDesdeElOrdenadorSinId() {
        val p = reconciliar(emptyMap(), listOf(FilaRemota(4, null, b)), emptyMap())
        assertEquals(listOf(4 to b), p.nuevasDesdeRemoto)
    }

    @Test fun filaCopiadaConIdRepetidoEsNueva() {
        val p = reconciliar(mapOf("a" to a), listOf(FilaRemota(2, "a", a), FilaRemota(3, "a", a)), mapOf("a" to firma(a)))
        assertEquals(listOf(3 to a), p.nuevasDesdeRemoto)
    }

    @Test fun cambiosEnLosDosSitiosEsConflicto() {
        val p = reconciliar(mapOf("a" to a2), listOf(FilaRemota(2, "a", a3)), mapOf("a" to firma(a)))
        assertEquals(listOf(Conflicto("a", a2, a3, 2)), p.conflictos)
        assertTrue(!p.hayCambiosRemotos && !p.hayCambiosLocales)
        assertEquals(firma(a), p.instantanea["a"])
    }

    @Test fun mismoCambioEnLosDosNoEsConflicto() {
        val p = reconciliar(mapOf("a" to a2), listOf(FilaRemota(2, "a", a2)), mapOf("a" to firma(a)))
        assertTrue(p.conflictos.isEmpty())
        assertEquals(firma(a2), p.instantanea["a"])
    }

    @Test fun filaInvalidaNoBorraNada() {
        val p = reconciliar(mapOf("a" to a), listOf(FilaRemota(2, "a", null, "Importe no válido")), mapOf("a" to firma(a)))
        assertTrue(!p.hayCambiosRemotos && !p.hayCambiosLocales)
        assertEquals(listOf(2 to "Importe no válido"), p.invalidas)
    }

    @Test fun conflictoAbiertoNoSeToca() {
        val p = reconciliar(mapOf("a" to a2), listOf(FilaRemota(2, "a", a3)), mapOf("a" to firma(a)), setOf("a"))
        assertTrue(p.conflictos.isEmpty() && !p.hayCambiosRemotos && !p.hayCambiosLocales)
    }

    @Test fun celdasDeLaHoja() {
        val gastos = Pestana.GASTOS.columnas
        val f = LocalDate.of(2030, 3, 15)
        assertEquals(f.toEpochDay().toString(), leerCelda(JsonPrimitive(f.toEpochDay() + 25569), gastos[1]))
        assertEquals(f.toEpochDay().toString(), leerCelda(JsonPrimitive("15/3/2030"), gastos[1]))
        assertEquals("275", leerCelda(JsonPrimitive(2.75), gastos[4]))
        assertEquals("152075", leerCelda(JsonPrimitive("1.520,75 €"), gastos[4]))
        assertEquals("Sí", leerCelda(JsonPrimitive("si"), gastos[6]))
        assertEquals("", leerCelda(null, gastos[8]))
        assertEquals(JsonPrimitive(java.math.BigDecimal("2.75")), escribirCelda("275", gastos[4]))
        assertEquals(JsonPrimitive(f.toEpochDay() + 25569), escribirCelda(f.toEpochDay().toString(), gastos[1]))
    }

    @Test fun celdasMalas() {
        val gastos = Pestana.GASTOS.columnas
        val filas = leerFilas(
            Pestana.GASTOS,
            listOf(
                gastos.map { JsonPrimitive(it.titulo) },
                listOf(JsonPrimitive(""), JsonPrimitive(47000), JsonPrimitive("X"), JsonPrimitive("Café"), JsonPrimitive("abc")),
                listOf(JsonPrimitive("")),
                listOf(JsonPrimitive(""), JsonPrimitive(47000), JsonPrimitive("X"), JsonPrimitive("Café"), JsonPrimitive(1.5)),
            ),
        )
        assertEquals(2, filas.size)
        assertEquals("Importe no válido: «abc»", filas[0].error)
        assertEquals(4, filas[1].fila)
        assertEquals("150", filas[1].canon!![4])
    }
}

class ForzarLocalTest {
    private fun c(vararg v: String): Canon = v.toList()
    private val a = c("G-0001", "1", "Cafetería", "Café", "275")
    private val a2 = c("G-0001", "1", "Cafetería", "Café", "325")
    private val b = c("G-0002", "2", "Librería", "Libro", "5180")

    @Test fun laHojaQuedaIgualQueElMovil() {
        val remoto = listOf(
            FilaRemota(2, "a", a2), // distinta → se reescribe
            FilaRemota(3, "x", b), // no existe en el móvil → se borra
            FilaRemota(4, null, b), // fila sin id → se borra
            FilaRemota(5, "a", a), // repetida → se borra
        )
        val p = reconciliarForzandoLocal(mapOf("a" to a, "b" to b), remoto)
        assertEquals(listOf(Triple(2, "a", a)), p.actualizarRemoto)
        assertEquals(listOf("b" to b), p.anadirRemoto)
        assertEquals(listOf(5, 4, 3), p.borrarRemoto)
        assertEquals(mapOf("a" to firma(a), "b" to firma(b)), p.instantanea)
        assertTrue(p.aplicarLocal.isEmpty() && p.borrarLocal.isEmpty() && p.conflictos.isEmpty())
    }
}
