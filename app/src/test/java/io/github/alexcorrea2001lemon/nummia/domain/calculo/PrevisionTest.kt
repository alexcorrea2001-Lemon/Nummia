package io.github.alexcorrea2001lemon.nummia.domain.calculo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** «A fin de mes te quedarán…» en Inicio. Datos inventados. */
class PrevisionTest {
    private val nov = YearMonth.of(2030, 11)
    private val hoy = LocalDate.of(2030, 11, 10)
    private fun g(id: String, dia: Int, c: Long, e: Estado = Estado.CONFIRMADO) = MovGasto(id, nov.atDay(dia), c, estado = e)
    private fun i(dia: Int, c: Long, e: Estado = Estado.CONFIRMADO) = MovIngreso(nov.atDay(dia), c, estado = e)

    @Test fun sumaLoQueVieneYRestaLoPendienteYElRitmo() {
        val gastos = listOf(
            g("super", 2, 6000), g("cine", 8, 4000), // 100 € de gasto normal en 10 días → 10 €/día
            g("gym", 1, 3000), // pago fijo ya pagado: no cuenta para el ritmo
            g("luz", 20, 5000, Estado.PREVISTO), // pago fijo pendiente
        )
        val ingresos = listOf(i(1, 20000), i(28, 150000, Estado.PREVISTO))
        val p = previsionFinDeMes(nov, hoy, gastos, listOf(MovReembolso("cine", 1000)), ingresos, fijos = setOf("gym", "luz"))!!
        // Ahora: 200 − (60 + 30 + 30) = 80 €
        assertEquals(8_000L, p.ahora)
        assertEquals(150_000L, p.porCobrar)
        assertEquals(5_000L, p.porPagar)
        // Gasto normal real 60 + 30 = 90 € en 10 días → 9 €/día × 20 días que quedan
        assertEquals(18_000L, p.ritmo)
        assertEquals(8_000L + 150_000 - 5_000 - 18_000, p.quedara)
    }

    @Test fun soloEnElMesEnCurso() {
        assertNull(previsionFinDeMes(nov.minusMonths(1), hoy, emptyList(), emptyList(), emptyList(), emptySet()))
        assertNull(previsionFinDeMes(nov.plusMonths(1), hoy, emptyList(), emptyList(), emptyList(), emptySet()))
    }

    @Test fun losPrimerosDiasNoSeCalculaRitmo() {
        val p = previsionFinDeMes(nov, LocalDate.of(2030, 11, 2), listOf(g("a", 1, 5000)), emptyList(), emptyList(), emptySet())!!
        assertEquals(0L, p.ritmo)
        assertEquals(-5_000L, p.quedara)
    }

    @Test fun elUltimoDiaNoQuedaRitmo() {
        val p = previsionFinDeMes(nov, LocalDate.of(2030, 11, 30), listOf(g("a", 3, 5000)), emptyList(), emptyList(), emptySet())!!
        assertEquals(0L, p.ritmo)
    }
}
