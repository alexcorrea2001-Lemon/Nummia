package io.github.alexcorrea2001lemon.nummia.data.informes

import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.data.importar.LectorXlsx
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class InformeTest {
    private fun g(id: String, dia: LocalDate, importe: Long, concepto: String, devuelto: Long = 0, persona: String? = null, estado: Estado = Estado.CONFIRMADO) =
        GastoConReembolsos(
            GastoEntity(id, "G-$id", dia, concepto, concepto, "alimentacion", importe, persona != null, persona, null, "", estado, 1, 1),
            if (devuelto > 0) listOf(ReembolsoEntity("r$id", "R-$id", id, null, devuelto, persona, "", 1, 1)) else emptyList(),
        )

    private val gastos = listOf(
        g("1", LocalDate.of(2030, 1, 5), 2750, "Supermercado"),
        g("2", LocalDate.of(2030, 1, 20), 34950, "Móvil", devuelto = 5000, persona = "ana"),
        g("3", LocalDate.of(2030, 2, 3), 1200, "Panadería"),
        g("4", LocalDate.of(2030, 2, 9), 999, "Previsto", estado = Estado.PREVISTO),
        g("5", LocalDate.of(2029, 12, 30), 1000, "Regalo", persona = "luis"),
    )
    private val ingresos = listOf(IngresoEntity("i", "I-0001", LocalDate.of(2030, 1, 31), "Sueldo", TipoIngreso.SUELDO, 150000, "", Estado.CONFIRMADO, 1, 1))
    private val personas = mapOf("ana" to "Ana", "luis" to "Luis")

    private fun informe(desde: LocalDate, hasta: LocalDate, persona: String? = null) =
        construirInforme("Informe", desde, hasta, Contenido.entries.toSet(), gastos, ingresos, personas, mapOf("alimentacion" to "Alimentación"), persona)

    @Test fun resumenDelMes() {
        val i = informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))
        assertEquals("Enero 2030", i.periodo)
        assertEquals(37700L, i.resumen.pagado)
        assertEquals(32700L, i.resumen.gastoReal)
        assertEquals(150000L - 32700L, i.resumen.balance)
        assertEquals(3, i.movimientos.size)
    }

    @Test fun anioConMesesYSinLoPrevisto() {
        val i = informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31))
        assertEquals("Año 2030", i.periodo)
        assertEquals(12, i.meses.size)
        assertEquals(1200L, i.meses[1].second.pagado)
        assertTrue(i.movimientos.none { it.concepto == "Previsto" })
        assertEquals("Móvil", i.tiendas.first().nombre)
    }

    @Test fun loQueMeDebenEsDeCualquierFechaYSePuedeFiltrar() {
        val i = informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))
        assertEquals(listOf("Ana" to 29950L, "Luis" to 1000L), i.deben.map { it.persona to it.falta })
        assertEquals(30950L, i.totalDeben)
        assertEquals(listOf("Ana"), informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), "ana").deben.map { it.persona })
    }

    @Test fun informeDeUnaPersonaSoloTraeLoSuyo() {
        val i = informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), "ana")
        assertEquals(listOf("Móvil"), i.movimientos.map { it.concepto }) // ni el súper ni el sueldo
        assertEquals(34950L, i.resumen.pagado)
        assertEquals(5000L, i.resumen.reembolsos)
        assertEquals(0L, i.resumen.ingresos)
    }

    @Test fun elExcelSeLeeConLosMismosNumeros() {
        val bytes = EscritorXlsx.escribir(hojasDeInforme(informe(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))))
        val celdas = LectorXlsx.leer(bytes) // primera hoja: Resumen
        assertEquals("Informe", celdas["A1"]?.texto)
        assertEquals("Concepto", celdas["A4"]?.texto) // cabecera de la tabla
        assertEquals(BigDecimal("1500.00"), celdas["B5"]?.numero)
        assertEquals(BigDecimal("327.00"), celdas["B8"]?.numero)
        assertEquals(EscritorXlsx.EUROS, celdas["B5"]?.estilo)
    }

    @Test fun columnasDeExcel() {
        assertEquals("A", EscritorXlsx.columna(0))
        assertEquals("Z", EscritorXlsx.columna(25))
        assertEquals("AA", EscritorXlsx.columna(26))
    }
}
