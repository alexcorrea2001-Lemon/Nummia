package io.github.alexcorrea2001lemon.nummia.ui

import io.github.alexcorrea2001lemon.nummia.ui.comun.EtiquetaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.Filtros
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.Orden
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.Pestana
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.agrupar
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.aplicar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FiltrosTest {
    private fun g(id: String, titulo: String, importe: Long, dia: Int, pagado: Long? = null, et: EtiquetaMov? = null, tienda: String = titulo) =
        FilaMov(id, true, titulo, tienda, importe, pagado, LocalDate.of(2030, 1, dia), tienda, et, dia.toLong())
    private fun i(id: String, importe: Long, dia: Int) = FilaMov(id, false, "Sueldo", "Ingreso", importe, null, LocalDate.of(2030, 1, dia), "", null, dia.toLong())

    private val filas = listOf(
        g("1", "Café", 275, 3),
        g("2", "Tienda online", 12000, 15, pagado = 24000),
        g("3", "Móvil", 34950, 20, et = EtiquetaMov.TeDeben("Ana")),
        i("4", 152075, 31),
    )

    @Test fun busquedaSinTildesNiMayusculas() {
        assertEquals(listOf("1"), aplicar(filas, Filtros(texto = "cafe")).map { it.id })
        assertEquals(listOf("3"), aplicar(filas, Filtros(texto = "MOVIL")).map { it.id })
    }

    @Test fun buscaPorFecha() {
        assertEquals(listOf("2"), aplicar(filas, Filtros(texto = "15/01")).map { it.id })
        assertEquals(listOf("1"), aplicar(filas, Filtros(texto = "3/1/2030")).map { it.id })
        assertEquals(4, aplicar(filas, Filtros(texto = "Enero")).size)
        assertEquals(4, aplicar(filas, Filtros(texto = "enero de 2030")).size)
        assertEquals(0, aplicar(filas, Filtros(texto = "febrero")).size)
        assertEquals(4, aplicar(filas, Filtros(texto = "2030")).size)
    }

    @Test fun buscaPorImporte() {
        assertEquals(listOf("1"), aplicar(filas, Filtros(texto = "2,75")).map { it.id })
        assertEquals(listOf("2"), aplicar(filas, Filtros(texto = "240")).map { it.id })
    }

    @Test fun pestanas() {
        assertEquals(3, aplicar(filas, Filtros(pestana = Pestana.GASTOS)).size)
        assertEquals(listOf("4"), aplicar(filas, Filtros(pestana = Pestana.INGRESOS)).map { it.id })
        assertEquals(listOf("3"), aplicar(filas, Filtros(pestana = Pestana.PENDIENTES)).map { it.id })
    }

    @Test fun filtrosSeCombinan() {
        val r = aplicar(filas, Filtros(pestana = Pestana.GASTOS, conReembolso = false, minimo = 1000))
        assertEquals(listOf("3"), r.map { it.id })
    }

    @Test fun filtraPorCategoriaPersonaYTienda() {
        val conDatos = listOf(
            g("a", "Cine", 900, 2).copy(categoriaId = "ocio"),
            g("b", "Cena", 3000, 4).copy(categoriaId = "comida", personaId = "p1"),
            g("c", "Súper Ejemplo", 4500, 6, tienda = "Súper Ejemplo").copy(categoriaId = "comida"),
            i("d", 100000, 7),
        )
        assertEquals(listOf("b", "c"), aplicar(conDatos, Filtros(categoria = "comida")).map { it.id })
        assertEquals(listOf("b"), aplicar(conDatos, Filtros(persona = "p1")).map { it.id })
        assertEquals(listOf("c"), aplicar(conDatos, Filtros(tienda = "super ejemplo")).map { it.id })
        assertEquals(listOf("b"), aplicar(conDatos, Filtros(categoria = "comida", persona = "p1")).map { it.id })
        assertEquals(3, Filtros(categoria = "x", persona = "y", tienda = "z").avanzados)
    }

    @Test fun agrupaPorDiaConTotal() {
        val dias = agrupar(filas + g("5", "Panadería", 120, 3), Orden.FECHA)
        assertEquals(LocalDate.of(2030, 1, 31), dias.first().fecha)
        val dia3 = dias.first { it.fecha.dayOfMonth == 3 }
        assertEquals(-395L, dia3.total)
        assertEquals(2, dia3.filas.size)
    }

    @Test fun ordenPorImporte() {
        assertEquals(listOf("4", "3", "2", "1"), agrupar(filas, Orden.IMPORTE).single().filas.map { it.id })
    }
}
