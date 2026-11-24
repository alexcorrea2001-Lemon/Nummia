package io.github.alexcorrea2001lemon.nummia.data.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Al abrir la app solo se sincroniza si hace falta: ahorra batería y datos. */
class SincronizarAlAbrirTest {
    private val ahora = 1_000_000_000_000L
    private val min = 60_000L

    @Test fun nuncaSincronizadoSi() = assertTrue(convieneAlAbrir(ultima = null, pendiente = false, error = false, ahora = ahora))
    @Test fun conCambiosSinSubirSi() = assertTrue(convieneAlAbrir(ahora - 1 * min, pendiente = true, error = false, ahora = ahora))
    @Test fun trasUnErrorSi() = assertTrue(convieneAlAbrir(ahora - 1 * min, pendiente = false, error = true, ahora = ahora))
    @Test fun haceMenosDe15MinutosNo() = assertFalse(convieneAlAbrir(ahora - 14 * min, pendiente = false, error = false, ahora = ahora))
    @Test fun haceMasDe15MinutosSi() = assertTrue(convieneAlAbrir(ahora - 15 * min, pendiente = false, error = false, ahora = ahora))
    @Test fun relojAtrasadoSi() = assertTrue(convieneAlAbrir(ahora + 60 * min, pendiente = false, error = false, ahora = ahora))
}
