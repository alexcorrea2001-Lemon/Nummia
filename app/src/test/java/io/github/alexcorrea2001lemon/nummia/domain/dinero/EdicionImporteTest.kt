package io.github.alexcorrea2001lemon.nummia.domain.dinero

import org.junit.Assert.assertEquals
import org.junit.Test

class EdicionImporteTest {
    private fun t(texto: String, cursor: Int, tecla: String) = teclearImporte(texto, cursor, tecla)
    private fun alFinal(texto: String, tecla: String) = teclearImporte(texto, texto.length, tecla).texto

    @Test fun alFinalFuncionaComoAntes() {
        assertEquals("12,50", alFinal("12,5", "0"))
        assertEquals("12,50", alFinal("12,50", "1"))
        assertEquals("0,", alFinal("", ","))
        assertEquals("12,", alFinal("12", ","))
        assertEquals("12,5", alFinal("12,5", ","))
        assertEquals("12+", alFinal("12", "+"))
        assertEquals("12−", alFinal("12+", "−"))
        assertEquals("12+", alFinal("12,", "+"))
        assertEquals("", alFinal("", "+"))
        assertEquals("12+0,", alFinal("12+", ","))
        assertEquals("1", alFinal("12", "⌫"))
        assertEquals("", alFinal("", "⌫"))
    }

    @Test fun elCursorVaDetrasDeLoEscrito() {
        assertEquals(EdicionImporte("125", 3), t("12", 2, "5"))
        assertEquals(EdicionImporte("0,", 2), t("", 0, ","))
    }

    @Test fun escribeEnMedio() {
        assertEquals(EdicionImporte("19381,64", 2), t("1381,64", 1, "9"))
        assertEquals(EdicionImporte("2381,64", 1), t("381,64", 0, "2"))
    }

    @Test fun borraLaCifraDeAntesDelCursor() {
        assertEquals(EdicionImporte("181,64", 1), t("1381,64", 2, "⌫"))
        assertEquals(EdicionImporte("1381,64", 0), t("1381,64", 0, "⌫"))
    }

    @Test fun quitaLosPuntosDeMilesYCorrigeElCursor() {
        // «1.381,64» con el cursor detrás del 3 (posición 3 contando el punto).
        assertEquals(EdicionImporte("13981,64", 3), t("1.381,64", 3, "9"))
        assertEquals(EdicionImporte("181,64", 1), t("1.381,64", 3, "⌫"))
    }

    @Test fun respetaLosDosDecimales() {
        assertEquals(EdicionImporte("12,50", 4), t("12,50", 4, "7"))
        assertEquals(EdicionImporte("912,50", 1), t("12,50", 0, "9"))
    }

    @Test fun rechazaUnaComaQueNoCabe() {
        assertEquals(EdicionImporte("12345", 2), t("12345", 2, ","))
        assertEquals(EdicionImporte("12,5", 1), t("12,5", 1, ","))
        assertEquals(EdicionImporte("123,45", 4), t("12345", 3, ","))
        assertEquals(EdicionImporte("0,12", 2), t("12", 0, ","))
    }

    @Test fun operadoresEnMedio() {
        assertEquals(EdicionImporte("12+20", 3), t("1220", 2, "+"))
        assertEquals(EdicionImporte("1220", 0), t("1220", 0, "+"))
        assertEquals(EdicionImporte("12+20", 3), t("12+20", 3, "−"))
        assertEquals(EdicionImporte("12+20", 2), t("12+20", 2, "−"))
        assertEquals(EdicionImporte("12-5", 3), t("12-5", 3, "+"))
    }

    @Test fun elNumeroDespuesDeUnOperadorEsOtroNumero() {
        assertEquals(EdicionImporte("12,5+3,", 7), t("12,5+3", 6, ","))
        assertEquals(EdicionImporte("12,5+3,25", 9), t("12,5+3,2", 8, "5"))
    }

    @Test fun cursorFueraDeRangoSeAjusta() {
        assertEquals(EdicionImporte("125", 3), t("12", 99, "5"))
        assertEquals(EdicionImporte("512", 1), t("12", -4, "5"))
    }

    @Test fun limiteDeLongitud() {
        val largo = "1".repeat(24)
        assertEquals(EdicionImporte(largo, 24), t(largo, 24, "1"))
    }
}
