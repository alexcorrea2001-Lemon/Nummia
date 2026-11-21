package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.PresupuestoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.AvisoCambios
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Ambito
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Nivel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class TiendasCategoriasTest {
    private lateinit var db: NummiaDb
    private lateinit var repo: MovimientosRepo
    private var avisos = 0

    @Before fun preparar() = runBlocking {
        db = dbPrueba()
        repo = MovimientosRepo(db, RelojFijo(), AvisoCambios { avisos++ })
        repo.prepararDatos()
    }

    @After fun cerrar() = db.close()

    private suspend fun gasto(tienda: String, importe: Long, cat: String? = null) =
        repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 10), concepto = "Compra", tienda = tienda, importe = importe, categoriaId = cat))

    @Test fun unirTiendasCambiaGastosYPresupuestos() = runBlocking {
        gasto("Supermercado", 1000); gasto("Super Sol", 500); gasto("Hiper", 200); gasto("Panadería", 100)
        repo.guardarPresupuesto(PresupuestoEntity("p", Ambito.TIENDA.name, "Super Sol", 5000, 1, 1))
        avisos = 0
        assertEquals(2, repo.unirTiendas(listOf("Super Sol", "Hiper", "Supermercado"), "Supermercado"))
        val t = repo.tiendas().first()
        assertEquals(listOf("Supermercado" to 3, "Panadería" to 1), t.map { it.nombre to it.veces })
        assertEquals(1700L, t.first().total)
        assertEquals("Supermercado", db.presupuestos().volcar().single().ref)
        assertEquals(1, avisos)
    }

    @Test fun categoriasCrearRenombrarYBorrar() = runBlocking {
        val id = repo.crearCategoria("Mascotas", 0xFF34D399)
        assertEquals(id, repo.crearCategoria("mascotas", 0xFF000000)) // no se duplica
        val g = gasto("Tienda de animales", 2000, id)
        repo.guardarPresupuesto(PresupuestoEntity("p", Ambito.CATEGORIA.name, id, 5000, 1, 1))
        val c = db.categorias().volcar().single { it.id == id }
        repo.editarCategoria(c.copy(nombre = " Animales "))
        assertEquals("Animales", db.categorias().volcar().single { it.id == id }.nombre)
        repo.borrarCategoria(db.categorias().volcar().single { it.id == id })
        assertTrue(db.categorias().volcar().none { it.id == id })
        assertNull(db.gastos().obtener(g)!!.categoriaId)
        assertTrue(db.presupuestos().volcar().isEmpty())
    }

    @Test fun lasCategoriasDeSerieNoSeBorran() = runBlocking {
        val c = db.categorias().volcar().first { it.predefinida }
        repo.borrarCategoria(c)
        assertTrue(db.categorias().volcar().any { it.id == c.id })
    }

    @Test fun estadoDePresupuestosDelMes() = runBlocking {
        gasto("Supermercado", 9000, "alimentacion")
        repo.guardarPresupuesto(PresupuestoEntity("p", Ambito.CATEGORIA.name, "alimentacion", 10000, 1, 1))
        val e = repo.estadoPresupuestosAhora(YearMonth.of(2030, 3)).single()
        assertEquals(9000L, e.gastado)
        assertEquals(Nivel.CERCA, e.nivel)
    }

    @Test fun sinCategoriaSeUsaLaDeLaMismaTienda() = runBlocking {
        val a = gasto("Cine", 800, "ocio")
        val b = gasto("cine", 900) // sin categoría: hereda «ocio»
        assertEquals("ocio", db.gastos().obtener(b)!!.categoriaId)
        assertEquals("ocio", db.gastos().obtener(a)!!.categoriaId)
    }

    @Test fun ponerCategoriaLaPasaALosQueNoTenian() = runBlocking {
        val a = gasto("Cine", 800)
        val b = gasto("Cine", 900)
        assertNull(db.gastos().obtener(a)!!.categoriaId)
        val g = db.gastos().obtener(b)!!
        repo.guardarGasto(GastoInput(id = b, fecha = g.fecha, concepto = g.concepto, tienda = g.tienda, importe = g.importe, categoriaId = "ocio"))
        assertEquals("ocio", db.gastos().obtener(a)!!.categoriaId)
    }

    @Test fun completarCategoriasTrasImportar() = runBlocking {
        val a = gasto("Cine", 800, "ocio")
        // Gastos sin categoría metidos directamente (como al importar)
        val g = db.gastos().obtener(a)!!
        db.gastos().guardar(g.copy(id = "imp", codigo = "G-0099", categoriaId = null, tienda = "CINE"))
        db.gastos().guardar(g.copy(id = "otro", codigo = "G-0098", categoriaId = null, tienda = "Teatro"))
        assertEquals(1, repo.completarCategorias())
        assertEquals("ocio", db.gastos().obtener("imp")!!.categoriaId)
        assertNull(db.gastos().obtener("otro")!!.categoriaId)
    }

    @Test fun laMismaTiendaConTildesOMayusculasSeGuardaIgual() = runBlocking {
        gasto("BonoBus", 1000); gasto("BonoBus", 1000)
        val c = gasto("Bonobús", 1000)
        assertEquals("BonoBus", db.gastos().obtener(c)!!.tienda)
    }

    @Test fun unificarTiendasIgualesAlAbrir() = runBlocking {
        gasto("Bus", 100)
        val g = db.gastos().obtener(gasto("Bus", 100))!!
        db.gastos().guardar(g.copy(id = "x1", codigo = "G-0090", tienda = "Bonobús"))
        db.gastos().guardar(g.copy(id = "x2", codigo = "G-0091", tienda = "BonoBus"))
        db.gastos().guardar(g.copy(id = "x3", codigo = "G-0092", tienda = "BonoBus"))
        assertEquals(1, repo.unificarTiendasIguales())
        assertEquals(setOf("BonoBus"), db.gastos().volcar().filter { it.id.startsWith("x") }.map { it.tienda }.toSet())
    }
}
