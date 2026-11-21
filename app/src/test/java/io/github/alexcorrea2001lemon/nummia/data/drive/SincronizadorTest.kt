package io.github.alexcorrea2001lemon.nummia.data.drive

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaLocal
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class SincronizadorTest {
    private lateinit var db: NummiaDb
    private lateinit var syncDb: SyncDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo
    private lateinit var nube: FakeNube
    private lateinit var s: Sincronizador
    private val hoja = "Nummia_2030"

    @Before fun preparar() = runBlocking {
        db = dbPrueba()
        syncDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SyncDb::class.java).allowMainThreadQueries().build()
        reloj = RelojFijo()
        repo = MovimientosRepo(db, reloj)
        repo.prepararDatos()
        nube = FakeNube()
        s = Sincronizador(db, syncDb.sync(), reloj, CopiaLocal(db)) { nube }
        s.conectar("prueba@example.com")
    }

    @After fun cerrar() { db.close(); syncDb.close() }

    private fun gasto(importe: Long, concepto: String = "Café", dia: Int = 10) =
        GastoInput(fecha = LocalDate.of(2030, 3, dia), concepto = concepto, tienda = "Cafetería", importe = importe)

    private fun gastos() = nube.filas(hoja, "Gastos").drop(1)
    private fun texto(fila: List<*>, col: Int) = (fila.getOrNull(col) as? JsonPrimitive)?.contentOrNull.orEmpty()
    private suspend fun hecho(): ResultadoSync.Hecho = s.sincronizar() as ResultadoSync.Hecho

    @Test fun sinCuentaNoHaceNada() = runBlocking {
        s.desconectar()
        assertEquals(ResultadoSync.NoConectado, s.sincronizar())
        assertTrue(nube.archivos.isEmpty())
    }

    @Test fun primeraVezCreaCarpetaHojaYSubeLosGastos() = runBlocking {
        repo.guardarGasto(gasto(275))
        repo.guardarGasto(gasto(5180, "Libro"))
        hecho()
        assertNotNull(nube.archivos.values.firstOrNull { it.nombre == Sincronizador.CARPETA })
        assertEquals(listOf("Resumen", "Gastos", "Ingresos", "Reembolsos"), nube.hoja(hoja).pestanas.values.toList())
        assertEquals("Código", texto(nube.filas(hoja, "Gastos")[0], 0))
        assertEquals(listOf("G-0001", "G-0002"), gastos().map { texto(it, 0) })
        assertEquals("2.75", texto(gastos()[0], 4))
        // Copia de seguridad del día
        assertEquals(1, nube.archivos.values.count { it.nombre.startsWith("Nummia_copia_") })
    }

    @Test fun sinCambiosNoSeLeeNiSeEscribe() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        val version = nube.hoja(hoja).version
        hecho()
        assertEquals(version, nube.hoja(hoja).version)
    }

    @Test fun cambioDesdeElOrdenadorLlegaAlMovil() = runBlocking {
        val id = repo.guardarGasto(gasto(275))
        hecho()
        nube.editarDesdeOrdenador(hoja, "Gastos", 2, 5, JsonPrimitive(3.25))
        hecho()
        assertEquals(325L, db.gastos().obtener(id)!!.importe)
    }

    @Test fun filaNuevaEscritaEnElOrdenadorSeCreaEnElMovilConIdYCodigo() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        val serial = LocalDate.of(2030, 3, 12).toEpochDay() + 25569
        listOf(JsonPrimitive(""), JsonPrimitive(serial), JsonPrimitive("Panadería"), JsonPrimitive("Pan"), JsonPrimitive(1.2))
            .forEachIndexed { i, v -> nube.editarDesdeOrdenador(hoja, "Gastos", 3, i + 1, v) }
        hecho()
        val nuevo = db.gastos().volcar().single { it.concepto == "Pan" }
        assertEquals(120L, nuevo.importe)
        assertEquals("G-0002", nuevo.codigo)
        val fila = gastos()[1]
        assertEquals(nuevo.id, fila[Pestana.GASTOS.columnaId].jsonPrimitive.content)
        assertEquals("G-0002", texto(fila, 0))
    }

    @Test fun borrarEnElMovilBorraLaFila() = runBlocking {
        val a = repo.guardarGasto(gasto(275))
        repo.guardarGasto(gasto(5180, "Libro"))
        hecho()
        repo.borrarGasto(a)
        hecho()
        assertEquals(listOf("Libro"), gastos().map { texto(it, 3) })
    }

    @Test fun borrarEnElOrdenadorBorraEnElMovil() = runBlocking {
        val a = repo.guardarGasto(gasto(275))
        hecho()
        nube.borrarFilaDesdeOrdenador(hoja, "Gastos", 2)
        hecho()
        assertNotNull(db.gastos().obtener(a)!!.deletedAt)
    }

    @Test fun cambiosEnLosDosLadosCreanConflictoYNoSePierdeNada() = runBlocking {
        val id = repo.guardarGasto(gasto(275))
        hecho()
        repo.guardarGasto(gasto(300).copy(id = id))
        nube.editarDesdeOrdenador(hoja, "Gastos", 2, 5, JsonPrimitive(4))
        assertEquals(1, hecho().conflictos)
        assertEquals(300L, db.gastos().obtener(id)!!.importe) // el móvil no cambia hasta que decides
        val c = syncDb.sync().conflictosDe("GASTOS", 2030).single()
        s.resolver(c, movil = false)
        assertEquals(400L, db.gastos().obtener(id)!!.importe)
        hecho()
        assertEquals("4", texto(gastos()[0], 4))
        assertTrue(syncDb.sync().conflictosDe("GASTOS", 2030).isEmpty())
    }

    @Test fun quedarseConElMovilSubeSuVersion() = runBlocking {
        val id = repo.guardarGasto(gasto(275))
        hecho()
        repo.guardarGasto(gasto(300).copy(id = id))
        nube.editarDesdeOrdenador(hoja, "Gastos", 2, 5, JsonPrimitive(4))
        hecho()
        s.resolver(syncDb.sync().conflictosDe("GASTOS", 2030).single(), movil = true)
        hecho()
        assertEquals(300L, db.gastos().obtener(id)!!.importe)
        assertEquals("3.00", texto(gastos()[0], 4))
    }

    @Test fun filaConErroresSeAvisaYNoSeToca() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        nube.editarDesdeOrdenador(hoja, "Gastos", 3, 4, JsonPrimitive("Sin fecha ni importe"))
        val r = hecho()
        assertTrue(r.avisos.single().startsWith("Gastos, fila 3"))
        assertEquals(1, db.gastos().volcar().size)
        assertEquals(2, gastos().size)
    }

    @Test fun siFallaLaEscrituraNoSeGuardaLaInstantaneaYSeReintenta() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        repo.guardarGasto(gasto(5180, "Libro"))
        nube.fallarEscrituras = true
        val r = s.sincronizar()
        assertTrue(r is ResultadoSync.Fallo && r.reintentar)
        nube.fallarEscrituras = false
        hecho()
        assertEquals(listOf("Café", "Libro"), gastos().map { texto(it, 3) })
        assertNull(syncDb.sync().meta(Meta.ERROR))
    }

    @Test fun trasRestaurarLaHojaQuedaIgualQueElMovil() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        nube.editarDesdeOrdenador(hoja, "Gastos", 2, 5, JsonPrimitive(9))
        nube.editarDesdeOrdenador(hoja, "Gastos", 3, 4, JsonPrimitive("Basura"))
        s.trasRestaurar()
        hecho()
        assertEquals(listOf("2.75"), gastos().map { texto(it, 4) })
        assertEquals(275L, db.gastos().volcar().single().importe)
    }

    @Test fun movilNuevoDescubreLaHojaExistente() = runBlocking {
        repo.guardarGasto(gasto(275))
        hecho()
        // Otro móvil vacío conectado a la misma cuenta
        val db2 = dbPrueba()
        val sync2 = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SyncDb::class.java).allowMainThreadQueries().build()
        val s2 = Sincronizador(db2, sync2.sync(), reloj, CopiaLocal(db2)) { nube }
        s2.conectar("prueba@example.com")
        s2.sincronizar()
        assertEquals(275L, db2.gastos().volcar().single().importe)
        assertEquals(1, nube.archivos.values.count { it.nombre == hoja })
        db2.close(); sync2.close()
    }
}
