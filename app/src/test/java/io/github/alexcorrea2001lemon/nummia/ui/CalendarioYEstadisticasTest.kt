// Pruebas de las pantallas de la v1.1.0. Tiempo real (runBlocking) porque Room responde desde sus hilos.
package io.github.alexcorrea2001lemon.nummia.ui

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.ui.calendario.CalendarioViewModel
import io.github.alexcorrea2001lemon.nummia.ui.calendario.compacto
import io.github.alexcorrea2001lemon.nummia.ui.calendario.semanasDelMes
import io.github.alexcorrea2001lemon.nummia.ui.calendario.seisSemanas
import io.github.alexcorrea2001lemon.nummia.ui.estadisticas.EstadisticasViewModel
import io.github.alexcorrea2001lemon.nummia.ui.estadisticas.Periodo
import io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class CalendarioYEstadisticasTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: NummiaDb
    private lateinit var reloj: RelojFijo
    private lateinit var repo: MovimientosRepo
    private lateinit var ajustes: AjustesRepo

    @Before fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = dbPrueba()
        reloj = RelojFijo(LocalDate.of(2030, 3, 15))
        repo = MovimientosRepo(db, reloj)
        ajustes = AjustesRepo(PreferenceDataStoreFactory.create { tmp.newFile("a.preferences_pb") })
    }

    @After fun cerrar() { db.close(); Dispatchers.resetMain() }

    private fun gasto(importe: Long, tienda: String, f: LocalDate) = GastoInput(fecha = f, concepto = tienda, tienda = tienda, importe = importe)

    @Test fun semanasEmpiezanEnLunes() {
        val s = semanasDelMes(YearMonth.of(2030, 1))
        assertEquals(LocalDate.of(2029, 12, 31), s.first().first())
        assertTrue(s.all { it.size == 7 && it.first().dayOfWeek == DayOfWeek.MONDAY })
        assertTrue(s.flatten().containsAll((1..31).map { LocalDate.of(2030, 1, it) }))
        assertEquals(4, semanasDelMes(YearMonth.of(2027, 2)).size)
    }

    @Test fun importeCompacto() {
        assertEquals("12", compacto(1240))
        assertEquals("13", compacto(-1260))
        assertEquals("1.520", compacto(152045))
        assertEquals("1.521", compacto(152075))
        assertEquals("24k", compacto(2457319))
    }

    @Test fun calendarioMuestraLosMovimientosDelDiaElegido() = runBlocking {
        repo.guardarGasto(gasto(275, "Cafetería", LocalDate.of(2030, 3, 10)))
        repo.guardarGasto(gasto(5180, "Librería", LocalDate.of(2030, 3, 12)))
        val vm = CalendarioViewModel(repo, ajustes, reloj)
        vm.elegir(LocalDate.of(2030, 3, 12))
        val e = withTimeout(5_000) { vm.estado.first { it.elegido == LocalDate.of(2030, 3, 12) && it.movimientosDia.isNotEmpty() } }
        assertEquals(listOf("Librería"), e.movimientosDia.map { it.titulo })
        assertEquals(5455L, e.gastoMes)
    }

    @Test fun tocarUnMesDelAnioAbreLaPestanaMes() = runBlocking {
        val vm = EstadisticasViewModel(repo, ajustes, reloj)
        vm.pestana(PestanaEst.ANIO)
        vm.verMes(7)
        val e = withTimeout(5_000) { vm.estado.first { !it.cargando && it.sel.pestana == PestanaEst.MES } }
        assertEquals(YearMonth.of(2030, 7), e.sel.mes)
    }

    @Test fun rankingSegunElPeriodo() = runBlocking {
        repo.guardarGasto(gasto(1000, "Cafetería", LocalDate.of(2030, 3, 2)))
        repo.guardarGasto(gasto(9000, "Librería", LocalDate.of(2030, 1, 2)))
        repo.guardarGasto(gasto(500, "cafeteria", LocalDate.of(2029, 5, 2)))
        val vm = EstadisticasViewModel(repo, ajustes, reloj)
        val mes = withTimeout(5_000) { vm.estado.first { !it.cargando && it.ranking.isNotEmpty() } }
        assertEquals(listOf("Cafetería"), mes.ranking.map { it.nombre })
        vm.periodo(Periodo.ANIO)
        val anio = withTimeout(5_000) { vm.estado.first { it.sel.periodo == Periodo.ANIO && !it.cargando } }
        assertEquals(listOf("Librería", "Cafetería"), anio.ranking.map { it.nombre })
        vm.periodo(Periodo.SIEMPRE)
        val siempre = withTimeout(5_000) { vm.estado.first { it.sel.periodo == Periodo.SIEMPRE } }
        assertEquals(1500L, siempre.ranking.first { it.nombre == "Cafetería" }.total)
    }

    @Test fun laRejillaDelMesSiempreTieneSeisSemanas() {
        // Así, al deslizar de un mes a otro, nada salta de sitio.
        for (ym in listOf(YearMonth.of(2027, 2), YearMonth.of(2030, 1), YearMonth.of(2030, 3), YearMonth.of(2026, 8))) {
            val s = seisSemanas(ym)
            assertEquals(6, s.size)
            assertTrue(s.all { it.size == 7 })
            assertEquals(java.time.DayOfWeek.MONDAY, s.first().first().dayOfWeek)
            assertEquals(semanasDelMes(ym).first().first(), s.first().first())
            assertTrue(s.flatten().containsAll((1..ym.lengthOfMonth()).map { ym.atDay(it) }))
        }
    }
}
