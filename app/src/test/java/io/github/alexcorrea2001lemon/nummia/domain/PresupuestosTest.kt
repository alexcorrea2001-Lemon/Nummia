package io.github.alexcorrea2001lemon.nummia.domain

import io.github.alexcorrea2001lemon.nummia.domain.calculo.Ambito
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GastoDePresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Limite
import io.github.alexcorrea2001lemon.nummia.domain.calculo.MovGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.MovReembolso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Nivel
import io.github.alexcorrea2001lemon.nummia.domain.calculo.estadoPresupuestos
import io.github.alexcorrea2001lemon.nummia.domain.calculo.umbralesCruzados
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PresupuestosTest {
    private val d = LocalDate.of(2030, 3, 10)
    private fun g(id: String, importe: Long, tienda: String, cat: String?, estado: Estado = Estado.CONFIRMADO) =
        GastoDePresupuesto(MovGasto(id, d, importe, estado = estado), tienda, cat)

    private val gastos = listOf(
        g("1", 4000, "Supermercado", "alimentacion"),
        g("2", 2000, "supermercádo", "alimentacion"),
        g("3", 10000, "Tienda online", "compras"),
        g("4", 9999, "Gimnasio", "ocio", Estado.PREVISTO),
    )
    private val reembolsos = listOf(MovReembolso("3", 5000))

    @Test fun totalRestaReembolsosYNoCuentaLoPrevisto() {
        val e = estadoPresupuestos(listOf(Limite("t", Ambito.TOTAL, null, 20000)), gastos, reembolsos).single()
        assertEquals(11000L, e.gastado)
        assertEquals(550, e.porMil)
        assertEquals(Nivel.BIEN, e.nivel)
        assertEquals(9000L, e.queda)
    }

    @Test fun porCategoriaYPorTiendaSinTildes() {
        val r = estadoPresupuestos(
            listOf(Limite("c", Ambito.CATEGORIA, "alimentacion", 7000), Limite("s", Ambito.TIENDA, "SUPERMERCADO", 5000)),
            gastos, reembolsos,
        )
        assertEquals(6000L, r[0].gastado)
        assertEquals(Nivel.CERCA, r[0].nivel) // 85,7 %
        assertEquals(6000L, r[1].gastado)
        assertEquals(Nivel.PASADO, r[1].nivel)
    }

    @Test fun umbrales() {
        val r = estadoPresupuestos(listOf(Limite("c", Ambito.CATEGORIA, "alimentacion", 7000)), gastos, reembolsos).single()
        assertEquals(listOf(800), umbralesCruzados(r))
        val pasado = estadoPresupuestos(listOf(Limite("c", Ambito.CATEGORIA, "alimentacion", 6000)), gastos, reembolsos).single()
        assertEquals(listOf(800, 1000), umbralesCruzados(pasado))
    }

    @Test fun sinGastosEsCero() {
        val r = estadoPresupuestos(listOf(Limite("t", Ambito.TOTAL, null, 1000)), emptyList(), emptyList()).single()
        assertEquals(0, r.porMil)
    }
}
