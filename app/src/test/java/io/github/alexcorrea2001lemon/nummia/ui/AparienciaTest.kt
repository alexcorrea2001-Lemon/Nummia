// Pruebas de los ajustes de apariencia y de seguridad nuevos (datos inventados).
package io.github.alexcorrea2001lemon.nummia.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.ajustes.BloqueoTras
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.textoUltimaCopia
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.Cerrojo
import io.github.alexcorrea2001lemon.nummia.ui.theme.TipoLetra
import io.github.alexcorrea2001lemon.nummia.ui.theme.coloresMovimiento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AparienciaTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo() = AjustesRepo(PreferenceDataStoreFactory.create { tmp.newFile("a.preferences_pb") })

    private fun contraste(a: Color, b: Color): Double {
        val (c, d) = if (a.luminance() > b.luminance()) a to b else b to a
        return (c.luminance() + 0.05) / (d.luminance() + 0.05)
    }

    @Test fun losAjustesNuevosTienenValoresPorDefectoSensatos() = runBlocking {
        val a = repo().ajustes.first()
        assertEquals(TipoLetra.SISTEMA, a.letra)
        assertFalse(a.coloresAccesibles)
        assertFalse(a.tinteFondo)
        assertFalse(a.ocultarEnRecientes)
        assertEquals(BloqueoTras.UN_MINUTO, a.bloqueoTras)
    }

    @Test fun losAjustesNuevosSeGuardan() = runBlocking {
        val r = repo()
        r.letra(TipoLetra.SERIFA); r.coloresAccesibles(true); r.tinteFondo(true)
        r.bloqueoTras(BloqueoTras.QUINCE_MINUTOS); r.ocultarEnRecientes(true)
        val a = r.ajustes.first()
        assertEquals(TipoLetra.SERIFA, a.letra)
        assertTrue(a.coloresAccesibles && a.tinteFondo && a.ocultarEnRecientes)
        assertEquals(BloqueoTras.QUINCE_MINUTOS, a.bloqueoTras)
    }

    @Test fun losColoresDeMovimientoCumplenAAEnTodasLasCombinaciones() {
        val fondos = mapOf(false to Color(0xFFF6F6F9), true to Color(0xFF0E0F13))
        for (oscuro in listOf(false, true)) for (acc in listOf(false, true)) {
            val (ingreso, gasto) = coloresMovimiento(oscuro, acc)
            val f = fondos.getValue(oscuro)
            assertTrue("ingreso oscuro=$oscuro acc=$acc", contraste(ingreso, f) >= 4.5)
            assertTrue("gasto oscuro=$oscuro acc=$acc", contraste(gasto, f) >= 4.5)
        }
    }

    @Test fun losColoresAccesiblesNoUsanRojoNiVerde() {
        for (oscuro in listOf(false, true)) {
            val (ingreso, gasto) = coloresMovimiento(oscuro, true)
            assertTrue(ingreso.blue > ingreso.green && ingreso.blue > ingreso.red)
            assertTrue(gasto.red > gasto.blue && gasto.green > gasto.blue)
        }
    }

    @Test fun elCerrojoSoloBloqueaPasadaLaEspera() {
        assertFalse(Cerrojo.debeBloquear(salida = 0L, ahora = 1_000_000L, esperaMs = 60_000L))
        assertFalse(Cerrojo.debeBloquear(1_000L, 30_000L, 60_000L))
        assertTrue(Cerrojo.debeBloquear(1_000L, 61_000L, 60_000L))
        assertTrue(Cerrojo.debeBloquear(1_000L, 1_000L, BloqueoTras.AL_INSTANTE.ms))
        assertFalse(Cerrojo.debeBloquear(1_000L, 800_000L, BloqueoTras.QUINCE_MINUTOS.ms))
    }
}

class TextoCopiaTest {
    @Test fun diceCuandoFueLaUltimaCopia() {
        assertEquals("Todavía no has guardado ninguna copia en un archivo", textoUltimaCopia(null))
        assertEquals("Última copia en un archivo: hoy", textoUltimaCopia(0))
        assertEquals("Última copia en un archivo: ayer", textoUltimaCopia(1))
        assertEquals("Última copia en un archivo: hace 12 días", textoUltimaCopia(12))
    }
}
