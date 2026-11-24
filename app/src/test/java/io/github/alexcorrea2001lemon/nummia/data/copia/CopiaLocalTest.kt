package io.github.alexcorrea2001lemon.nummia.data.copia

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.RelojFijo
import io.github.alexcorrea2001lemon.nummia.data.dbPrueba
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.IngresoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class CopiaLocalTest {
    @Test fun exportarEImportarDaLosMismosDatos() = runTest {
        val origen = dbPrueba()
        val repo = MovimientosRepo(origen, RelojFijo())
        repo.prepararDatos()
        val p = repo.crearPersona("Luis")
        val g = repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 1), concepto = "Entradas concierto", tienda = "", importe = 18000, meLoDevuelven = true, personaId = p))
        repo.anadirReembolso(g, 18000, null, p)
        repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 2), concepto = "Cafetería", tienda = "Cafetería", importe = 275))
        repo.guardarIngreso(IngresoInput(fecha = LocalDate.of(2030, 3, 31), concepto = "Sueldo", tipo = TipoIngreso.SUELDO, importe = 152075))
        val antes = repo.resumenMes(YearMonth.of(2030, 3)).first()
        val bytes = CopiaLocal(origen).exportar(1L)

        val destino = dbPrueba()
        val copia = CopiaLocal(destino)
        copia.restaurar(copia.leer(bytes))
        val repo2 = MovimientosRepo(destino, RelojFijo())
        assertEquals(antes, repo2.resumenMes(YearMonth.of(2030, 3)).first())
        assertEquals(origen.historial().volcar().size, destino.historial().volcar().size)
        assertEquals("Luis", repo2.personas().first().single().nombre)
        assertEquals(origen.categorias().volcar().size, destino.categorias().volcar().size)
        origen.close(); destino.close()
    }

    @Test fun copiaConContrasenaSoloSeAbreConElla() = runTest {
        val origen = dbPrueba()
        val repo = MovimientosRepo(origen, RelojFijo())
        repo.prepararDatos()
        repo.guardarGasto(GastoInput(fecha = LocalDate.of(2030, 3, 2), concepto = "Cafetería secreta", tienda = "", importe = 275))
        val bytes = CopiaLocal(origen).exportar(1L, contrasena = "una frase inventada")

        // No se lee nada sin la contraseña: ni el concepto aparece en el archivo.
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("Cafetería"))
        assertTrue(CifradoCopia.estaCifrada(bytes))
        val destino = dbPrueba()
        val copia = CopiaLocal(destino)
        assertThrows(CopiaCifrada::class.java) { copia.leer(bytes) }
        assertThrows(CopiaInvalida::class.java) { copia.leer(bytes, "otra") }
        copia.restaurar(copia.leer(bytes, "una frase inventada"))
        assertEquals(1, destino.gastos().volcar().count { it.concepto == "Cafetería secreta" })
        origen.close(); destino.close()
    }

    @Test fun unaCopiaCifradaModificadaSeRechaza() {
        val datos = "datos inventados".toByteArray()
        val c = CifradoCopia.cifrar(datos, "clave")
        assertEquals("datos inventados", String(CifradoCopia.descifrar(c, "clave")))
        c[c.size - 5] = (c[c.size - 5].toInt() xor 1).toByte() // un solo bit cambiado
        assertThrows(CopiaInvalida::class.java) { CifradoCopia.descifrar(c, "clave") }
    }

    @Test fun archivoQueNoEsCopiaSeRechaza() {
        val db = dbPrueba()
        assertThrows(CopiaInvalida::class.java) { CopiaLocal(db).leer("hola".toByteArray()) }
        db.close()
    }
}
