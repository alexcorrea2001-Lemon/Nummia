// Las pruebas de ViewModel usan tiempo real (runBlocking) porque Room responde desde hilos propios.
package io.github.alexcorrea2001lemon.nummia.ui

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.ui.formulario.EventoForm
import io.github.alexcorrea2001lemon.nummia.ui.formulario.FormularioGastoViewModel
import io.github.alexcorrea2001lemon.nummia.ui.inicio.InicioViewModel
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import io.github.alexcorrea2001lemon.nummia.ui.theme.Tema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class PantallasViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: NummiaDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo
    private lateinit var ajustes: AjustesRepo

    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = dbPrueba()
        reloj = RelojFijo(LocalDate.of(2030, 3, 28))
        repo = MovimientosRepo(db, reloj)
        ajustes = AjustesRepo(PreferenceDataStoreFactory.create { tmp.newFile("ajustes.preferences_pb") })
    }

    @After fun cerrar() { db.close(); Dispatchers.resetMain() }

    private fun gasto(importe: Long, tienda: String, dia: Int = 10) =
        GastoInput(fecha = LocalDate.of(2030, 3, dia), concepto = tienda, tienda = tienda, importe = importe)

    @Test fun ajustesPorDefecto() = runBlocking {
        val a = ajustes.ajustes.first()
        assertEquals(Acento.VIOLETA, a.acento)
        assertEquals(Acento.VIOLETA, a.iconoColor)
        assertEquals(Logo.A, a.iconoLogo)
        assertEquals(Tema.AUTOMATICO, a.tema)
        assertNull(a.sueldoHabitual)
        ajustes.sueldoHabitual(152075)
        ajustes.icono(Logo.C, Acento.AZUL)
        val b = ajustes.ajustes.first()
        assertEquals(152075L, b.sueldoHabitual)
        assertEquals(Logo.C, b.iconoLogo)
        assertEquals(Acento.AZUL, b.iconoColor)
        assertEquals(Acento.VIOLETA, b.acento)
    }

    @Test fun inicioOrdenaTiendasYMuestraSueldoDesdeEl27() = runBlocking {
        repo.guardarGasto(gasto(275, "Cafetería"))
        repo.guardarGasto(gasto(5180, "Librería"))
        repo.guardarGasto(gasto(2640, "Cine"))
        repo.guardarGasto(gasto(1000, "Cafetería"))
        repo.guardarGasto(gasto(100, "Panadería"))
        repo.asegurarSueldoPrevisto(reloj.hoy(), 152075)
        val vm = InicioViewModel(repo, ajustes, reloj)
        val e = withTimeout(5_000) { vm.estado.first { !it.cargando } }
        assertEquals(listOf("Librería", "Cine", "Cafetería"), e.top.map { it.nombre })
        assertEquals(1, e.sueldosPendientes.size)
        assertEquals(false, e.vacio)
    }

    @Test fun inicioMesVacio() = runBlocking {
        val vm = InicioViewModel(repo, ajustes, reloj)
        val e = withTimeout(5_000) { vm.estado.first { !it.cargando } }
        assertTrue(e.vacio)
        assertNull(e.resumen.ahorroPorMil)
    }

    @Test fun losImportesVistosConLaHuellaSoloDuranHastaSalirDeLaApp() = runBlocking {
        ajustes.modoDiscreto(true)
        val vm = InicioViewModel(repo, ajustes, reloj)
        assertTrue(withTimeout(5_000) { vm.estado.first { !it.cargando } }.discreto)

        // Verlos (tras la huella) no apaga el modo discreto: solo los enseña mientras la app sigue abierta.
        vm.mostrarImportes()
        assertEquals(false, withTimeout(5_000) { vm.estado.first { !it.discreto } }.discreto)
        assertTrue(ajustes.ajustes.first().discretoTemporal)

        // Al salir de la app vuelven a ocultarse.
        ajustes.volverADiscretoSiTemporal()
        assertTrue(withTimeout(5_000) { vm.estado.first { it.discreto } }.discreto)
        assertTrue(ajustes.ajustes.first().modoDiscreto)
    }

    @Test fun elOjoOcultaLosImportesDeFormaFija() = runBlocking {
        val vm = InicioViewModel(repo, ajustes, reloj)
        assertEquals(false, withTimeout(5_000) { vm.estado.first { !it.cargando } }.discreto)
        vm.ocultarImportes()
        assertTrue(withTimeout(5_000) { vm.estado.first { it.discreto } }.discreto)
        val a = ajustes.ajustes.first()
        assertTrue(a.modoDiscreto)
        assertEquals(false, a.discretoTemporal)
    }

    @Test fun formularioRechazaImporteInvalido() = runBlocking {
        val vm = FormularioGastoViewModel(SavedStateHandle(), repo, reloj)
        vm.importe("abc"); vm.concepto("Cafetería")
        vm.guardar(otro = false)
        assertNotNull(vm.form.value.errorImporte)
        assertTrue(repo.mes(java.time.YearMonth.of(2030, 3)).first().gastos.isEmpty())
    }

    @Test fun formularioYaDevueltoCreaElReembolso() = runBlocking {
        val vm = FormularioGastoViewModel(SavedStateHandle(mapOf("paraOtro" to true)), repo, reloj)
        assertTrue(vm.form.value.meLoDevuelven)
        vm.importe("349,50"); vm.concepto("Móvil"); vm.esperado("350"); vm.yaDevuelto(true)
        vm.guardar(otro = false)
        val ev = withTimeout(5_000) { vm.eventos.firstOrNull() } as EventoForm.Guardado
        val g = repo.gasto(ev.id).first()!!
        assertEquals(35000L, g.activos.single().importe)
        assertEquals(0L, g.falta)
    }

    @Test fun guardarYOtroLimpiaYConservaLaFecha() = runBlocking {
        val vm = FormularioGastoViewModel(SavedStateHandle(), repo, reloj)
        vm.importe("2,75"); vm.concepto("Cafetería"); vm.fecha(LocalDate.of(2030, 3, 2))
        vm.guardar(otro = true)
        withTimeout(5_000) { vm.eventos.firstOrNull() }
        assertEquals("", vm.form.value.importe)
        assertEquals("", vm.form.value.concepto)
        assertEquals(LocalDate.of(2030, 3, 2), vm.form.value.fecha)
    }
}
