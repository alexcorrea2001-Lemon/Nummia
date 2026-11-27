package io.github.alexcorrea2001lemon.nummia.domain.ticket

import org.junit.Assert.assertEquals
import org.junit.Test

class FilasOcrTest {
    @Test fun unePorFilaElConceptoYElPrecio() {
        val l = listOf(
            LineaOcr("LECHE", 10, 100, 120), LineaOcr("0,95", 300, 102, 122),
            LineaOcr("PAN", 10, 130, 150), LineaOcr("1,20", 300, 131, 151),
        )
        assertEquals("LECHE 0,95\nPAN 1,20", juntarFilas(l))
    }

    @Test fun noImportaElOrdenEnQueLlegan() {
        val l = listOf(
            LineaOcr("1,20", 300, 131, 151), LineaOcr("LECHE", 10, 100, 120),
            LineaOcr("PAN", 10, 130, 150), LineaOcr("0,95", 300, 102, 122),
        )
        assertEquals("LECHE 0,95\nPAN 1,20", juntarFilas(l))
    }

    @Test fun dosLineasSeguidasSiguenSiendoDos() {
        assertEquals("A\nB", juntarFilas(listOf(LineaOcr("A", 10, 100, 120), LineaOcr("B", 10, 125, 145))))
    }

    @Test fun elTotalYSuImporteLejosEnHorizontalSeUnen() {
        val l = listOf(LineaOcr("TOTAL (€)", 10, 400, 424), LineaOcr("5,10", 520, 398, 422))
        assertEquals("TOTAL (€) 5,10", juntarFilas(l))
    }

    @Test fun sinLineasNoHayTexto() {
        assertEquals("", juntarFilas(emptyList()))
    }
}
