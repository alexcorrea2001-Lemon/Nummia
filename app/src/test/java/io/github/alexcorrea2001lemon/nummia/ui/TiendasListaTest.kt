package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.data.repo.TiendaResumen
import io.github.alexcorrea2001lemon.nummia.ui.tiendas.OrdenTiendas
import io.github.alexcorrea2001lemon.nummia.ui.tiendas.filtrarTiendas
import io.github.alexcorrea2001lemon.nummia.ui.tiendas.iniciales
import org.junit.Assert.assertEquals
import org.junit.Test

class TiendasListaTest {
    private val l = listOf(
        TiendaResumen("Panadería Sol", 41, 9835),
        TiendaResumen("Mercadona", 62, 81240),
        TiendaResumen("Ámbar Café", 3, 1200),
        TiendaResumen("cine Lumière", 9, 7200),
    )

    @Test fun ordenaPorVecesGastoYNombre() {
        assertEquals(listOf("Mercadona", "Panadería Sol", "cine Lumière", "Ámbar Café"), filtrarTiendas(l, "", OrdenTiendas.VECES).map { it.nombre })
        assertEquals(listOf("Mercadona", "Panadería Sol", "cine Lumière", "Ámbar Café"), filtrarTiendas(l, "", OrdenTiendas.GASTO).map { it.nombre })
        assertEquals(listOf("Ámbar Café", "cine Lumière", "Mercadona", "Panadería Sol"), filtrarTiendas(l, "", OrdenTiendas.NOMBRE).map { it.nombre })
    }

    @Test fun buscaSinTildesNiMayusculas() {
        assertEquals(listOf("Panadería Sol"), filtrarTiendas(l, "panaderia", OrdenTiendas.VECES).map { it.nombre })
        assertEquals(listOf("cine Lumière"), filtrarTiendas(l, "  LUMIERE ", OrdenTiendas.VECES).map { it.nombre })
        assertEquals(emptyList<String>(), filtrarTiendas(l, "zapatería", OrdenTiendas.VECES).map { it.nombre })
    }

    @Test fun inicialesDeUnNombre() {
        assertEquals("LU", iniciales("Lucía"))
        assertEquals("PS", iniciales("Panadería Sol"))
        assertEquals("M", iniciales("M"))
        assertEquals("?", iniciales("  "))
    }
}
