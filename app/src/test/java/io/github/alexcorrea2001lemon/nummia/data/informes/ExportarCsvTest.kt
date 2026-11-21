package io.github.alexcorrea2001lemon.nummia.data.informes

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.IngresoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

/** Exportar todo a CSV (para abrirlo en cualquier hoja de cálculo). Datos inventados. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ExportarCsvTest {
    private lateinit var db: NummiaDb
    private lateinit var repo: MovimientosRepo

    @Before fun preparar() = runBlocking {
        db = dbPrueba()
        repo = MovimientosRepo(db, RelojFijo(LocalDate.of(2030, 3, 15)))
        repo.prepararDatos()
    }

    @After fun cerrar() = db.close()

    @Test fun celdasConSeparadorComillasOSaltoVanEntreComillas() {
        assertEquals("Café", Csv.celda("Café"))
        assertEquals("\"Pan; leche\"", Csv.celda("Pan; leche"))
        assertEquals("\"Dijo \"\"hola\"\"\"", Csv.celda("Dijo \"hola\""))
        assertEquals("\"dos\nlíneas\"", Csv.celda("dos\nlíneas"))
    }

    @Test fun importesConComaYSinMiles() {
        assertEquals("1520,75", Csv.importe(152075))
        assertEquals("-3,05", Csv.importe(-305))
        assertEquals("0,00", Csv.importe(0))
    }

    @Test fun elTextoLlevaBomParaQueExcelLeaLasTildes() {
        val t = Csv.texto(listOf(listOf("a", "b"), listOf("1", "2")))
        assertTrue(t.startsWith("﻿"))
        assertEquals("﻿a;b\r\n1;2\r\n", t)
    }

    private fun archivos(zip: ByteArray): Map<String, String> {
        val r = HashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(zip)).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                r[e.name] = z.readBytes().toString(Charsets.UTF_8)
            }
        }
        return r
    }

    @Test fun elZipTraeGastosIngresosYDevolucionesSinLoBorrado() = runBlocking {
        val cafe = repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 2), concepto = "Café", tienda = "Cafetería Ejemplo", importe = 275))
        val cena = repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 4), concepto = "Cena", tienda = "Restaurante Ejemplo", importe = 6000, meLoDevuelven = true))
        repo.anadirReembolso(cena, 3000, LocalDate.of(2030, 3, 6), null, "mitad")
        val borrado = repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 5), concepto = "Borrado", tienda = "X", importe = 100))
        repo.borrarGasto(borrado)
        repo.guardarIngreso(IngresoInput(fecha = LocalDate.of(2030, 3, 1), concepto = "Sueldo", tipo = TipoIngreso.SUELDO, importe = 152075))

        val f = archivos(ExportadorCsv(db).zip())
        assertEquals(setOf("gastos.csv", "ingresos.csv", "devoluciones.csv", "pagos_y_cobros_fijos.csv"), f.keys)
        val gastos = f.getValue("gastos.csv").removePrefix("﻿").trim().lines()
        assertEquals(3, gastos.size) // cabecera + 2
        assertTrue(gastos[0].startsWith("Código;Fecha;Concepto;Tienda;Categoría;Importe;Devuelto;Gasto real"))
        assertTrue(gastos.any { it.contains("02/03/2030;Café;Cafetería Ejemplo") && it.contains(";2,75;0,00;2,75;") })
        assertTrue(gastos.any { it.contains("Cena") && it.contains(";60,00;30,00;30,00;") })
        assertFalse(f.getValue("gastos.csv").contains("Borrado"))
        assertTrue(f.getValue("ingresos.csv").contains("01/03/2030;Sueldo;Sueldo;1520,75"))
        assertTrue(f.getValue("devoluciones.csv").contains("06/03/2030;30,00"))
        assertTrue(cafe.isNotBlank())
    }
}
