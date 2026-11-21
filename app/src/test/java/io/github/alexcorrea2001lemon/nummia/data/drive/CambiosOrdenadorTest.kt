package io.github.alexcorrea2001lemon.nummia.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CambiosOrdenadorTest {
    @Test fun sinCambiosNoHayTexto() = assertNull(CambiosOrdenador().texto())

    @Test fun singularYPlural() {
        assertEquals("Desde el ordenador: 1 nuevo", CambiosOrdenador(nuevos = 1).texto())
        assertEquals("Desde el ordenador: 2 nuevos y 1 cambiado", CambiosOrdenador(2, 1, 0).texto())
        assertEquals("Desde el ordenador: 2 nuevos, 3 cambiados y 1 borrado", CambiosOrdenador(2, 3, 1).texto())
        assertEquals("Desde el ordenador: 4 borrados", CambiosOrdenador(borrados = 4).texto())
    }

    @Test fun seGuardaYSeLee() {
        val c = CambiosOrdenador(2, 3, 1)
        assertEquals(c, CambiosOrdenador.leer(c.guardar()))
        assertNull(CambiosOrdenador.leer(null))
        assertNull(CambiosOrdenador.leer("basura"))
    }
}
