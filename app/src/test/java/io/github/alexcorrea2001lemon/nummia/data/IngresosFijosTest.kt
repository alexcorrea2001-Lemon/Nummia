package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.repo.IngresoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.runBlocking
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

/** Ingresos que se cobran cada mes (alquiler de una plaza, una pensión…). Datos inventados. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class IngresosFijosTest {
    private lateinit var db: NummiaDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo

    @Before fun preparar() {
        db = dbPrueba()
        reloj = RelojFijo(LocalDate.of(2030, 3, 15))
        repo = MovimientosRepo(db, reloj)
    }

    @After fun cerrar() = db.close()

    private fun previstos() = runBlocking { db.ingresos().volcar().filter { it.estado == Estado.PREVISTO && it.deletedAt == null } }

    private suspend fun alquilerDesdeIngreso(): String {
        val id = repo.guardarIngreso(
            IngresoInput(fecha = LocalDate.of(2030, 3, 10), concepto = "Alquiler plaza", tipo = TipoIngreso.OTRO, importe = 8000, cadaMes = true),
        )
        return db.ingresos().obtener(id)!!.ingresoFijoId!!
    }

    @Test fun elIngresoCreaElFijoYEsteMesNoSeDuplica() = runBlocking {
        val fijo = db.ingresosFijos().obtener(alquilerDesdeIngreso())!!
        assertEquals(10, fijo.dia)
        assertEquals(8000L, fijo.importe)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
    }

    @Test fun elMesSiguienteApareceComoPrevisto() = runBlocking {
        alquilerDesdeIngreso()
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        repo.asegurarIngresosPrevistos(reloj.hoy()) // dos veces: no se duplica
        val p = previstos().single()
        assertEquals(LocalDate.of(2030, 4, 10), p.fecha)
        assertEquals(8000L, p.importe)
        assertEquals(TipoIngreso.OTRO, p.tipo)
    }

    @Test fun diaQueNoExisteVaAlUltimoDelMes() = runBlocking {
        repo.guardarIngreso(IngresoInput(fecha = LocalDate.of(2030, 1, 31), concepto = "Pensión", tipo = TipoIngreso.OTRO, importe = 50000, cadaMes = true))
        reloj.fecha = LocalDate.of(2030, 2, 3)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        assertEquals(LocalDate.of(2030, 2, 28), previstos().single().fecha)
    }

    @Test fun confirmarConOtroImporteCambiaElFijo() = runBlocking {
        val fijo = alquilerDesdeIngreso()
        reloj.fecha = LocalDate.of(2030, 4, 12)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        val p = previstos().single()
        repo.confirmarIngresoFijo(p.id, 8500)
        val i = db.ingresos().obtener(p.id)!!
        assertEquals(Estado.CONFIRMADO, i.estado)
        assertEquals(8500L, i.importe)
        assertEquals(8500L, db.ingresosFijos().obtener(fijo)!!.importe)
    }

    @Test fun esteMesNoNoVuelveAAparecer() = runBlocking {
        alquilerDesdeIngreso()
        reloj.fecha = LocalDate.of(2030, 4, 12)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        repo.omitirIngreso(previstos().single().id)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
    }

    @Test fun pausarOBorrarQuitaLosPrevistosYGuardaLosCobrados() = runBlocking {
        val fijo = alquilerDesdeIngreso()
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        repo.guardarIngresoFijo(db.ingresosFijos().obtener(fijo)!!.copy(activo = false))
        assertTrue(previstos().isEmpty())
        repo.borrarIngresoFijo(fijo)
        assertNotNull(db.ingresosFijos().obtener(fijo)!!.deletedAt)
        assertEquals(1, db.ingresos().volcar().count { it.estado == Estado.CONFIRMADO && it.deletedAt == null })
    }

    @Test fun quitarCadaMesAlEditarBorraElFijo() = runBlocking {
        val fijo = alquilerDesdeIngreso()
        val i = db.ingresos().volcar().single()
        repo.guardarIngreso(IngresoInput(id = i.id, fecha = i.fecha, concepto = i.concepto, tipo = i.tipo, importe = i.importe, cadaMes = false))
        assertNotNull(db.ingresosFijos().obtener(fijo)!!.deletedAt)
        assertNull(db.ingresos().obtener(i.id)!!.ingresoFijoId)
    }

    @Test fun editarElCobroDesdeElOrdenadorNoLoDesenlazaNiLoDuplica() = runBlocking {
        val fijo = alquilerDesdeIngreso()
        val i = db.ingresos().volcar().single()
        val datos = io.github.alexcorrea2001lemon.nummia.data.drive.DatosLocales(db, reloj)
        val canon = datos.canon(io.github.alexcorrea2001lemon.nummia.data.drive.Pestana.INGRESOS, 2030).getValue(i.id).toMutableList()
        canon[4] = "8200" // en el ordenador cambian el importe
        datos.aplicar(io.github.alexcorrea2001lemon.nummia.data.drive.Pestana.INGRESOS, i.id, canon)
        assertEquals(fijo, db.ingresos().obtener(i.id)!!.ingresoFijoId)
        repo.asegurarIngresosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
    }

    @Test fun unSueldoFijoNoDuplicaElSueldoHabitual() = runBlocking {
        repo.guardarIngreso(IngresoInput(fecha = LocalDate.of(2030, 3, 28), concepto = "Sueldo", tipo = TipoIngreso.SUELDO, importe = 150000, cadaMes = true))
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarSueldoPrevisto(reloj.hoy(), 150000) // el habitual llega antes
        repo.asegurarIngresosPrevistos(reloj.hoy())
        assertEquals(1, previstos().size)
    }
}
