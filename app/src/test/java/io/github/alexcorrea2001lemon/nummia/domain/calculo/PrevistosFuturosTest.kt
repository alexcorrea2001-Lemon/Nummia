package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Lo que viene en los próximos meses (pagos y cobros fijos), para el calendario. Datos inventados. */
class PrevistosFuturosTest {
    private val hoy = LocalDate.of(2030, 11, 10)
    private val reps = listOf(
        Repeticion("gym", "Gimnasio Ejemplo", 2999, dia = 5, cadaMeses = 1, desde = null, ingreso = false),
        Repeticion("seguro", "Seguro Ejemplo", 32000, dia = 31, cadaMeses = 12, desde = YearMonth.of(2030, 2), ingreso = false),
        Repeticion("plaza", "Alquiler plaza", 8000, dia = 25, cadaMeses = 1, desde = null, ingreso = true),
    )

    @Test fun soloDesdeElMesQueViene() {
        // Este mes ya tiene sus previstos de verdad: aquí no se repiten.
        val f = previstosFuturos(hoy, LocalDate.of(2030, 11, 1), LocalDate.of(2030, 12, 31), reps)
        assertEquals(
            listOf(LocalDate.of(2030, 12, 5) to "gym", LocalDate.of(2030, 12, 25) to "plaza"),
            f.map { it.fecha to it.id },
        )
    }

    @Test fun anualEnSuMesYDia31EnFebrero() {
        val f = previstosFuturos(hoy, LocalDate.of(2031, 2, 1), LocalDate.of(2031, 2, 28), reps)
        assertEquals(LocalDate.of(2031, 2, 28), f.single { it.id == "seguro" }.fecha)
    }

    @Test fun nadaEnElPasado() = assertEquals(0, previstosFuturos(hoy, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 10, 31), reps).size)
}
