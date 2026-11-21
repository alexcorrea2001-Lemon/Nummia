package io.github.alexcorrea2001lemon.nummia.domain

import io.github.alexcorrea2001lemon.nummia.data.avisos.avisoCopia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Recordatorio mensual de copia si no hay Drive conectado. */
class AvisoCopiaTest {
    private val hoy = LocalDate.of(2030, 3, 10)
    private val antiguo = LocalDate.of(2030, 1, 1)

    @Test fun sinDriveNiCopiaSeAvisaUnaVezAlMes() {
        val a = avisoCopia(hoy, driveConectado = false, ultimaCopia = null, primerMovimiento = antiguo)!!
        assertEquals("copia-2030-03", a.clave)
        assertEquals(io.github.alexcorrea2001lemon.nummia.MainActivity.ACCION_DATOS, a.abrir)
    }

    @Test fun conDriveNoSeAvisa() = assertNull(avisoCopia(hoy, true, null, antiguo))

    @Test fun conUnaCopiaRecienteNoSeAvisa() = assertNull(avisoCopia(hoy, false, hoy.minusDays(29), antiguo))

    @Test fun conUnaCopiaDeHaceUnMesSeAvisa() = assertEquals("copia-2030-03", avisoCopia(hoy, false, hoy.minusDays(30), antiguo)?.clave)

    @Test fun sinDatosOConDatosDeEstaSemanaNoSeAvisa() {
        assertNull(avisoCopia(hoy, false, null, null))
        assertNull(avisoCopia(hoy, false, null, hoy.minusDays(3)))
    }
}
