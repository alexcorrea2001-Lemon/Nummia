package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MovimientosRepoTest {
    private lateinit var db: NummiaDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo
    private val marzo = YearMonth.of(2030, 3)

    @Before fun preparar() {
        db = dbPrueba()
        reloj = RelojFijo()
        repo = MovimientosRepo(db, reloj)
    }

    @After fun cerrar() = db.close()

    private fun gasto(importe: Long, dia: Int = 10, concepto: String = "Cafetería") =
        GastoInput(fecha = LocalDate.of(2030, 3, dia), concepto = concepto, tienda = concepto, importe = importe)

    @Test fun codigosConsecutivos() = runTest {
        val a = repo.guardarGasto(gasto(275))
        val b = repo.guardarGasto(gasto(120))
        assertEquals("G-0001", db.gastos().obtener(a)!!.codigo)
        assertEquals("G-0002", db.gastos().obtener(b)!!.codigo)
    }

    @Test fun losCodigosNoSeReutilizanTrasBorrar() = runTest {
        val a = repo.guardarGasto(gasto(275))
        repo.borrarGasto(a)
        val b = repo.guardarGasto(gasto(120))
        assertEquals("G-0002", db.gastos().obtener(b)!!.codigo)
    }

    @Test fun borrarYDeshacerDejaTodoIgual() = runTest {
        val id = repo.guardarGasto(gasto(24000))
        repo.anadirReembolso(id, 12000, null, null)
        val antes = repo.resumenMes(marzo).first()
        repo.borrarGasto(id)
        assertEquals(0L, repo.resumenMes(marzo).first().pagado)
        repo.deshacerBorradoGasto(id)
        assertEquals(antes, repo.resumenMes(marzo).first())
        val g = repo.gasto(id).first()!!
        assertEquals(12000L, g.real)
        assertEquals(1, g.activos.size)
    }

    @Test fun repetirHoyCopiaConFechaYCodigoNuevos() = runTest {
        val id = repo.guardarGasto(gasto(275, dia = 2).copy(categoriaId = "alimentacion"))
        val copia = repo.repetirHoy(id)
        val o = db.gastos().obtener(id)!!
        val c = db.gastos().obtener(copia)!!
        assertNotEquals(o.id, c.id)
        assertEquals("G-0002", c.codigo)
        assertEquals(LocalDate.of(2030, 3, 15), c.fecha)
        assertEquals(o.importe, c.importe)
        assertEquals(o.tienda, c.tienda)
        assertEquals("alimentacion", c.categoriaId)
    }

    @Test fun devueltoEnteroDejaLaFaltaACero() = runTest {
        val p = repo.crearPersona("Ana")
        val id = repo.guardarGasto(gasto(34950).copy(meLoDevuelven = true, personaId = p, esperado = 35000))
        repo.anadirReembolso(id, 5000, null, p)
        repo.devueltoEntero(id)
        val g = repo.gasto(id).first()!!
        assertEquals(0L, g.falta)
        assertEquals(-50L, g.real)
        assertNull(repo.devueltoEntero(id))
    }

    @Test fun yaDevueltoCreaElReembolsoEsperado() = runTest {
        val id = repo.guardarGasto(gasto(34950).copy(meLoDevuelven = true, esperado = 35000, yaDevuelto = true))
        val g = repo.gasto(id).first()!!
        assertEquals(35000L, g.activos.single().importe)
        assertEquals(0L, g.falta)
    }

    @Test fun sueldoPrevistoNoSeDuplica() = runTest {
        repo.asegurarSueldoPrevisto(LocalDate.of(2030, 3, 27), 152075)
        repo.asegurarSueldoPrevisto(LocalDate.of(2030, 3, 28), 152075)
        val ingresos = repo.mes(marzo).first().ingresos
        assertEquals(1, ingresos.size)
        assertEquals(Estado.PREVISTO, ingresos.single().estado)
        assertEquals(LocalDate.of(2030, 3, 31), ingresos.single().fecha)
        assertEquals(0L, repo.resumenMes(marzo).first().ingresos)
    }

    @Test fun confirmarSueldoLoCuenta() = runTest {
        repo.asegurarSueldoPrevisto(LocalDate.of(2030, 3, 27), 152075)
        val id = repo.mes(marzo).first().ingresos.single().id
        repo.confirmarIngreso(id, LocalDate.of(2030, 3, 28), 151390)
        assertEquals(151390L, repo.resumenMes(marzo).first().ingresos)
    }

    @Test fun sinSueldoHabitualNoSeCreaNada() = runTest {
        repo.asegurarSueldoPrevisto(LocalDate.of(2030, 3, 27), null)
        assertTrue(repo.mes(marzo).first().ingresos.isEmpty())
    }

    @Test fun editarImporteQuedaEnElHistorial() = runTest {
        val id = repo.guardarGasto(gasto(275))
        repo.guardarGasto(gasto(325).copy(id = id))
        val h = repo.historial(id).first()
        val cambio = h.first { it.campo == "importe" }
        assertEquals("2,75 €", cambio.antes)
        assertEquals("3,25 €", cambio.despues)
        assertTrue(h.any { it.campo == "creado" })
    }

    @Test fun ingresoExtraCuenta() = runTest {
        repo.guardarIngreso(
            io.github.alexcorrea2001lemon.nummia.data.repo.IngresoInput(
                fecha = LocalDate.of(2030, 3, 6), concepto = "Regalo", tipo = TipoIngreso.EXTRA, importe = 4500,
            ),
        )
        repo.guardarGasto(gasto(1000))
        val r = repo.resumenMes(marzo).first()
        assertEquals(4500L, r.ingresos)
        assertEquals(3500L, r.balance)
    }

    @Test fun categoriasDeSerie() = runTest {
        repo.prepararDatos()
        repo.prepararDatos()
        assertEquals(NummiaDb.CATEGORIAS_INICIALES.size, repo.categorias().first().size)
    }
}
