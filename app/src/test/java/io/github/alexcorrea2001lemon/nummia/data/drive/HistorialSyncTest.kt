package io.github.alexcorrea2001lemon.nummia.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Historial de las últimas sincronizaciones (Tu Drive). */
class HistorialSyncTest {
    @Test fun seGuardaYSeLee() {
        val e = EntradaSync(1_000L, subidos = 3, nuevos = 1, editados = 2, borrados = 0, conflictos = 1)
        assertEquals(listOf(e), EntradaSync.lista(anadirHistorial(null, e)))
        val f = EntradaSync(2_000L, error = "Sin conexión; con punto y coma")
        val l = EntradaSync.lista(anadirHistorial(anadirHistorial(null, e), f))
        assertEquals(listOf(f, e), l) // la más reciente primero
    }

    @Test fun soloLasUltimas15() {
        var h: String? = null
        repeat(20) { h = anadirHistorial(h, EntradaSync(it.toLong())) }
        val l = EntradaSync.lista(h)
        assertEquals(15, l.size)
        assertEquals(19L, l.first().fecha)
    }

    @Test fun textoParaLeer() {
        assertEquals("Sin cambios", EntradaSync(1).resumen())
        assertEquals("3 subidos · 1 nuevo y 2 cambiados desde el ordenador · 1 conflicto", EntradaSync(1, subidos = 3, nuevos = 1, editados = 2, conflictos = 1).resumen())
        assertTrue(EntradaSync(1, error = "Sin conexión").resumen().startsWith("No se pudo"))
        assertEquals(emptyList<EntradaSync>(), EntradaSync.lista("basura"))
    }
}
