package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

/** Evolución del precio de cada compra en una tienda. Datos inventados. */
class PrecioMedioTest {
    private val meses = (0L until 4L).map { YearMonth.of(2030, 1).plusMonths(it) } // ene–abr

    @Test fun mediaPorCompraYLosMesesSinComprasSiguenConLaAnterior() {
        val compras = listOf(
            YearMonth.of(2030, 1) to 1000L, YearMonth.of(2030, 1) to 2000L, // 15 € de media
            YearMonth.of(2030, 3) to 1800L,
        )
        // febrero sin compras: se queda en 15 €; abril igual que marzo
        assertEquals(listOf(1500L, 1500L, 1800L, 1800L), precioMedioPorMes(meses, compras))
    }

    @Test fun antesDeLaPrimeraCompraTomaLaPrimera() {
        assertEquals(listOf(900L, 900L, 900L, 1200L), precioMedioPorMes(meses, listOf(YearMonth.of(2030, 3) to 900L, YearMonth.of(2030, 4) to 1200L)))
    }

    @Test fun sinComprasNada() = assertEquals(emptyList<Long>(), precioMedioPorMes(meses, emptyList()))
}
