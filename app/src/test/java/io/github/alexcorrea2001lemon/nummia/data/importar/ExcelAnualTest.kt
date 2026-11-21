package io.github.alexcorrea2001lemon.nummia.data.importar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExcelAnualTest {
    private val analisis = ExcelAnual.analizar(LectorXlsx.leer(XlsxDePrueba.crear(XlsxDePrueba.ejemplo())), 2030)

    @Test fun leeLosGastosConFechaPrecioYReembolso() {
        val g = analisis.gastos
        assertEquals(5, g.size)
        val libro = g.single { it.nombre == "Librería" }
        assertEquals(LocalDate.of(2030, 1, 12), libro.fecha)
        assertEquals(5180L, libro.importe)
        assertEquals(1000L, libro.reembolso)
        assertEquals(LocalDate.of(2030, 7, 5), g.single { it.nombre == "Cine" }.fecha)
        assertEquals(LocalDate.of(2030, 2, 3), g.single { it.nombre == "Panadería" }.fecha)
    }

    @Test fun unificaVariantesDelMismoNombre() {
        assertEquals(2, analisis.gastos.count { it.nombre == "Cafetería" })
    }

    @Test fun sueldosAlFinalDeMesYGananciasExtra() {
        val sueldos = analisis.ingresos.filter { it.esSueldo }
        assertEquals(listOf(LocalDate.of(2030, 1, 31), LocalDate.of(2030, 2, 28)), sueldos.map { it.fecha })
        assertEquals(150000L, sueldos.first().importe)
        val extra = analisis.ingresos.single { !it.esSueldo }
        assertEquals("Regalo", extra.concepto)
        assertEquals(5000L, extra.importe)
        assertTrue(extra.fechaAproximada)
    }

    @Test fun compruebaElTotalDelMesConElExcel() {
        val enero = analisis.meses.first()
        assertEquals(275L + 5180 - 1000 + 300, enero.gastoRealCalculado)
        assertEquals(4755L, enero.gastoRealEnExcel)
        assertTrue(enero.coincide)
        assertNull(analisis.meses[1].gastoRealEnExcel)
    }

    @Test fun ignoraLaZonaH56I60() {
        assertTrue(analisis.ingresos.none { it.importe == 9900L })
        assertTrue(analisis.gastos.none { it.importe == 9900L })
    }

    @Test fun avisaDeLoQueNoSePuedeLeer() {
        val raro = ExcelAnual.analizar(
            mapOf("C9" to Celda("Sin precio(01/01)"), "D10" to Celda(numero = 5.toBigDecimal()), "C11" to Celda("Sin fecha"), "D11" to Celda(numero = 2.toBigDecimal())),
            2030,
        )
        assertEquals(1, raro.gastos.size)
        assertEquals(LocalDate.of(2030, 1, 1), raro.gastos.single().fecha)
        assertEquals(3, raro.avisos.size)
    }

    @Test fun anioDelNombreDelArchivo() {
        assertEquals(2026, ExcelAnual.anioDelNombre("2026.xlsx"))
        assertEquals(2031, ExcelAnual.anioDelNombre("Cuentas 2031 final.xlsx"))
        assertNull(ExcelAnual.anioDelNombre("cuentas.xlsx"))
    }

    @Test fun unificarNombresSeQuedaConLaGrafiaMasFrecuente() {
        val m = ExcelAnual.unificarNombres(listOf("Mercadona", "mercadona", "Mercadona", "Mercdona", "Zara"))
        assertEquals(setOf("Mercadona", "Zara"), m.values.toSet())
        assertEquals("Mercadona", m["Mercdona"])
        // Nombres cortos distintos no se mezclan
        assertEquals(setOf("Bar", "Bus"), ExcelAnual.unificarNombres(listOf("Bar", "Bus")).values.toSet())
    }

    @Test(expected = ExcelNoValido::class) fun archivoQueNoEsExcel() {
        LectorXlsx.leer("hola".toByteArray())
    }
}
