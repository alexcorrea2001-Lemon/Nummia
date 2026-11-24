package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.ui.inicio.TarjetaInicio
import io.github.alexcorrea2001lemon.nummia.ui.inicio.mover
import io.github.alexcorrea2001lemon.nummia.ui.inicio.ordenTarjetas
import org.junit.Assert.assertEquals
import org.junit.Test

/** Orden y tarjetas visibles de Inicio. */
class TarjetasInicioTest {
    @Test fun sinNadaGuardadoElOrdenDeSiempre() = assertEquals(TarjetaInicio.entries.toList(), ordenTarjetas(""))

    @Test fun respetaElOrdenGuardadoYAnadeLasNuevasAlFinal() {
        val o = ordenTarjetas("ultimos,ahorrado,desconocida")
        assertEquals(listOf(TarjetaInicio.ULTIMOS, TarjetaInicio.AHORRADO), o.take(2))
        assertEquals(TarjetaInicio.entries.size, o.size) // las que faltan, después; lo desconocido se ignora
        assertEquals(TarjetaInicio.entries.toSet(), o.toSet())
    }

    @Test fun moverArribaYAbajo() {
        val o = TarjetaInicio.entries.toList()
        assertEquals(o[1], mover(o, o[1], -1).first())
        assertEquals(o, mover(o, o.first(), -1)) // la primera no sube más
        assertEquals(o[o.size - 2], mover(o, o[o.size - 2], 1).last()) // la penúltima baja al final
        assertEquals(o, mover(o, o.last(), 1)) // la última no baja más
    }
}
