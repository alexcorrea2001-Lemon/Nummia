package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** «Repetir hoy»: apuntar otra vez un gasto que ya existe. Datos inventados. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class RepetirGastoTest {
    private lateinit var db: NummiaDb
    private lateinit var repo: MovimientosRepo
    private val hoy = LocalDate.of(2030, 3, 15)

    @Before fun preparar() = runBlocking {
        db = dbPrueba()
        repo = MovimientosRepo(db, RelojFijo(hoy))
        repo.prepararDatos()
    }

    @After fun cerrar() = db.close()

    @Test fun copiaLoImportanteConLaFechaNuevaYSinDevolucionesNiNotas() = runBlocking {
        val id = repo.guardarGasto(
            GastoInput(fecha = LocalDate.of(2030, 3, 1), concepto = "Menú", tienda = "Bar Ejemplo", categoriaId = "comida", importe = 1250, notas = "con postre"),
        )
        repo.anadirReembolso(id, 500, LocalDate.of(2030, 3, 2), null, "")
        val nuevo = repo.repetirGasto(id, hoy)
        assertNotEquals(id, nuevo)
        val g = db.gastos().obtener(nuevo)!!
        assertEquals(hoy, g.fecha)
        assertEquals("Menú", g.concepto)
        assertEquals("Bar Ejemplo", g.tienda)
        assertEquals("comida", g.categoriaId)
        assertEquals(1250L, g.importe)
        assertEquals("", g.notas)
        assertNull(g.pagoMensualId)
        assertEquals(0, db.reembolsos().volcar().count { it.gastoId == nuevo })
    }
}
