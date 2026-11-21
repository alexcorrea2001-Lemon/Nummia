package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.PagoMensualEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class PagosMensualesTest {
    private lateinit var db: NummiaDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo

    @Before fun preparar() {
        db = dbPrueba()
        reloj = RelojFijo(LocalDate.of(2030, 3, 15))
        repo = MovimientosRepo(db, reloj)
    }

    @After fun cerrar() = db.close()

    private fun previstos() = runBlocking { db.gastos().volcar().filter { it.estado == Estado.PREVISTO && it.deletedAt == null } }

    private suspend fun gimnasioDesdeGasto(): String {
        val id = repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 5), concepto = "Gimnasio", tienda = "Gimnasio", importe = 2999, cadaMes = true))
        return db.gastos().obtener(id)!!.pagoMensualId!!
    }

    @Test fun elGastoCreaElPagoYEsteMesNoSeDuplica() = runBlocking {
        val pago = gimnasioDesdeGasto()
        val p = db.pagos().obtener(pago)!!
        assertEquals(5, p.dia)
        assertEquals(2999L, p.importe)
        repo.asegurarPagosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
    }

    @Test fun elMesSiguienteApareceComoPrevistoYNoCuenta() = runBlocking {
        gimnasioDesdeGasto()
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarPagosPrevistos(reloj.hoy())
        repo.asegurarPagosPrevistos(reloj.hoy()) // dos veces: no se duplica
        val p = previstos().single()
        assertEquals(LocalDate.of(2030, 4, 5), p.fecha)
        assertEquals(0L, repo.resumenMes(YearMonth.of(2030, 4)).first().pagado)
    }

    @Test fun confirmarConOtroPrecioCambiaElPrecioDelPago() = runBlocking {
        val pago = gimnasioDesdeGasto()
        reloj.fecha = LocalDate.of(2030, 4, 6)
        repo.asegurarPagosPrevistos(reloj.hoy())
        repo.confirmarPago(previstos().single().id, 3299)
        assertEquals(3299L, repo.resumenMes(YearMonth.of(2030, 4)).first().pagado)
        assertEquals(3299L, db.pagos().obtener(pago)!!.importe)
        assertEquals(listOf(3299L, 2999L), repo.historialPago(pago).first().map { it.importe })
    }

    @Test fun esteMesNoNoVuelveAAparecer() = runBlocking {
        gimnasioDesdeGasto()
        reloj.fecha = LocalDate.of(2030, 4, 6)
        repo.asegurarPagosPrevistos(reloj.hoy())
        repo.omitirPago(previstos().single().id)
        repo.asegurarPagosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
        assertTrue(repo.papeleraGastos().first().isEmpty()) // los omitidos no van a la papelera
        reloj.fecha = LocalDate.of(2030, 5, 1)
        repo.asegurarPagosPrevistos(reloj.hoy())
        assertEquals(1, previstos().size)
    }

    @Test fun diaQueNoExisteEnElMesVaAlUltimoDia() = runBlocking {
        repo.guardarPago(PagoMensualEntity("p", "Alquiler", "Casero", null, 50000, 31, createdAt = 1, updatedAt = 1))
        reloj.fecha = LocalDate.of(2030, 2, 1)
        repo.asegurarPagosPrevistos(reloj.hoy())
        assertEquals(LocalDate.of(2030, 2, 28), previstos().single().fecha)
    }

    @Test fun pausarQuitaElPrevistoYBorrarConservaLosConfirmados() = runBlocking {
        val pago = gimnasioDesdeGasto()
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarPagosPrevistos(reloj.hoy())
        repo.guardarPago(db.pagos().obtener(pago)!!.copy(activo = false))
        assertTrue(previstos().isEmpty())
        repo.asegurarPagosPrevistos(reloj.hoy())
        assertTrue(previstos().isEmpty())
        repo.borrarPago(pago)
        assertNotNull(db.pagos().obtener(pago)!!.deletedAt)
        assertEquals(1, db.gastos().volcar().count { it.estado == Estado.CONFIRMADO && it.deletedAt == null })
    }

    @Test fun cambiarElPrecioActualizaElPrevistoPendiente() = runBlocking {
        val pago = gimnasioDesdeGasto()
        reloj.fecha = LocalDate.of(2030, 4, 1)
        repo.asegurarPagosPrevistos(reloj.hoy())
        repo.guardarPago(db.pagos().obtener(pago)!!.copy(importe = 3500, dia = 10))
        val p = previstos().single()
        assertEquals(3500L, p.importe)
        assertEquals(LocalDate.of(2030, 4, 10), p.fecha)
    }
}
