package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCabecera
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FechasTest {
    private val hoy = LocalDate.of(2026, 11, 22)

    @Test fun hoyYAyer() {
        assertEquals("Hoy", fechaCabecera(hoy, hoy))
        assertEquals("Ayer", fechaCabecera(hoy.minusDays(1), hoy))
    }

    @Test fun esteAnoSinAnoYOtroAnoConAno() {
        assertEquals("Domingo 1 mar", fechaCabecera(LocalDate.of(2026, 3, 1), hoy))
        assertEquals("Viernes 12 dic 2025", fechaCabecera(LocalDate.of(2025, 12, 12), hoy))
    }

    @Test fun alBuscarEnTodosLosMesesSiempreConAno() {
        assertEquals("Domingo 1 mar 2026", fechaCabecera(LocalDate.of(2026, 3, 1), hoy, conAnio = true))
    }
}
