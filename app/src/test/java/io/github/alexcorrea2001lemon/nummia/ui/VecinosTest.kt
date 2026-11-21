package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.ui.detalle.Vecinos
import io.github.alexcorrea2001lemon.nummia.ui.detalle.vecinos
import org.junit.Assert.assertEquals
import org.junit.Test

class VecinosTest {
    private val ids = listOf("a", "b", "c")

    @Test fun enMedioTieneLosDos() = assertEquals(Vecinos("a", "c", 2, 3), vecinos(ids, "b"))
    @Test fun elPrimeroNoTieneAnterior() = assertEquals(Vecinos(null, "b", 1, 3), vecinos(ids, "a"))
    @Test fun elUltimoNoTieneSiguiente() = assertEquals(Vecinos("b", null, 3, 3), vecinos(ids, "c"))
    @Test fun siNoEstaNoHayNada() = assertEquals(Vecinos(), vecinos(ids, "x"))
}
