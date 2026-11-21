package io.github.alexcorrea2001lemon.nummia.data.importar

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.repo.AvisoCambios
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ImportadorExcelTest {
    private lateinit var db: NummiaDb
    private lateinit var repo: MovimientosRepo
    private var avisos = 0
    private lateinit var imp: ImportadorExcel
    private val analisis = ExcelAnual.analizar(LectorXlsx.leer(XlsxDePrueba.crear(XlsxDePrueba.ejemplo())), 2030)

    @Before fun preparar() {
        db = dbPrueba()
        val reloj = RelojFijo()
        repo = MovimientosRepo(db, reloj)
        imp = ImportadorExcel(db, reloj, AvisoCambios { avisos++ })
    }

    @After fun cerrar() = db.close()

    @Test fun importaTodoYCuadraConElExcel() = runBlocking {
        val r = imp.importar(analisis, emptyMap())
        assertEquals(ResultadoImportacion(5, 1, 3), r)
        val enero = repo.resumenMes(YearMonth.of(2030, 1)).first()
        assertEquals(4755L, enero.gastoReal)
        assertEquals(150000L, enero.ingresos)
        assertEquals(listOf("G-0001", "G-0002", "G-0003", "G-0004", "G-0005"), db.gastos().volcar().map { it.codigo }.sorted())
        assertEquals(2, db.ingresos().volcar().count { it.tipo == TipoIngreso.SUELDO })
        assertEquals(1, avisos)
        assertEquals(5, imp.gastosEnElAnio(2030))
    }

    @Test fun losPagadosPorOtrosQuedanComoMeLoDevuelven() = runBlocking {
        val ana = repo.crearPersona("Ana")
        val libro = analisis.gastos.indexOfFirst { it.nombre == "Librería" }
        imp.importar(analisis, mapOf(libro to ana))
        val g = db.gastos().volcar().single { it.concepto == "Librería" }
        assertTrue(g.meLoDevuelven)
        assertEquals(ana, g.personaId)
        assertEquals(5180L, g.importeEsperado)
        assertEquals(ana, db.reembolsos().volcar().single().personaId)
    }

    @Test fun losCodigosSiguenALosQueYaHabia() = runBlocking {
        repo.guardarGasto(io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput(fecha = java.time.LocalDate.of(2030, 3, 1), concepto = "Bus", tienda = "Bus", importe = 150))
        imp.importar(analisis, emptyMap())
        assertEquals("G-0006", db.gastos().volcar().maxOf { it.codigo })
    }
}
