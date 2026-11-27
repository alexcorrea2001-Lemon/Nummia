package io.github.alexcorrea2001lemon.nummia.domain.ticket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Todos los tickets de esta prueba son inventados. */
class LectorTicketTest {
    private val hoy = LocalDate.of(2026, 11, 27)
    private fun leer(t: String, conocidas: List<String> = emptyList()) = leerTicket(t.trimIndent(), hoy, conocidas)

    @Test fun ticketDeSupermercado() {
        val r = leer(
            """
            MERCADONA, S.A.
            C/ Falsa 123
            28000 Madrid
            NIF: A00000000
            TEL: 900000000
            27/11/2026 18:42
            FACTURA SIMPLIFICADA: 1234-567-890123
            Descripcion  P.Unit  Importe
            1 LECHE ENTERA 1L   0,95   0,95
            2 PAN BARRA        0,60   1,20
            1 QUESO 250G      2,95   2,95
            TOTAL (€)          5,10
            TARJETA BANCARIA   5,10
            IVA   BASE  CUOTA
            4%    4,90  0,20
            """,
        )
        assertEquals(TicketLeido(510L, "Mercadona", LocalDate.of(2026, 11, 27)), r)
    }

    @Test fun ignoraSubtotalDescuentosEntregadoYCambio() {
        val r = leer(
            """
            BAR LA ESQUINA S.L.
            Fecha: 05-11-26
            Cafe con leche 1,60
            Tostada 2,40
            SUBTOTAL 4,00
            TOTAL DESCUENTOS 0,00
            TOTAL 4,00
            ENTREGADO 5,00
            CAMBIO 1,00
            """,
        )
        assertEquals(400L, r.importe)
        assertEquals("Bar La Esquina", r.tienda)
        assertEquals(LocalDate.of(2026, 11, 5), r.fecha)
    }

    @Test fun prefiereTotalAPagarSiHayDescuento() {
        val r = leer("TIENDA UNO\nTOTAL COMPRA 50,00\nDESCUENTO -5,00\nTOTAL A PAGAR 45,00")
        assertEquals(4500L, r.importe)
    }

    @Test fun leeMilesComaYPuntoDeOcr() {
        assertEquals(123456L, leer("TOTAL 1.234,56").importe)
        assertEquals(2345L, leer("TOTAL 23.45").importe)
        assertEquals(2345L, leer("TOTAL: 23,45 €").importe)
        assertEquals(2345L, leer("TOTAL EUR 23,45").importe)
    }

    @Test fun espaciosRarosDelReconocimientoDeTexto() {
        assertEquals(2345L, leer("TIENDA\nTOTAL\u00A0\u00A023,45").importe)
    }

    @Test fun elImporteEnLaLineaSiguiente() {
        assertEquals(2345L, leer("TOTAL\n23,45").importe)
    }

    @Test fun sinPalabraTotalUsaImporteOElMayor() {
        assertEquals(4520L, leer("GASOLINERA NORTE\nGasoil 30,50 L\nIMPORTE 45,20 EUR").importe)
        assertEquals(1500L, leer("KIOSCO\nRevista 3,50\nLibro 15,00\nCAMBIO 20,00\nIVA 1,36").importe)
    }

    @Test fun unaFechaConPuntosYElAnoCorto() {
        assertEquals(LocalDate.of(2026, 11, 12), leer("TIENDA\n12.11.2026 10:00\nTOTAL 1,00").fecha)
        assertEquals(LocalDate.of(2026, 3, 9), leer("TIENDA\n09/03/26\nTOTAL 1,00").fecha)
        assertEquals(LocalDate.of(2026, 11, 27), leer("TIENDA\n2026-11-27\nTOTAL 1,00").fecha)
    }

    @Test fun laFechaDeUnaFechaNoCuentaNiComoImporte() {
        val r = leer("TIENDA\n12.11.2026\nTOTAL 7,00")
        assertEquals(700L, r.importe)
    }

    @Test fun descartaFechasFuturasAntiguasYDeCaducidad() {
        assertNull(leer("TIENDA\n01/01/2030\nTOTAL 1,00").fecha)
        assertNull(leer("TIENDA\n01/01/2020\nTOTAL 1,00").fecha)
        assertEquals(LocalDate.of(2026, 11, 20), leer("TIENDA\nVALIDO HASTA 25/11/2026\n20/11/2026\nTOTAL 1,00").fecha)
    }

    @Test fun usaElNombreDeUnaTiendaYaConocida() {
        assertEquals("Mercadona", leer("MERCADONA SA\nTOTAL 3,00", listOf("Mercadona", "Panadería Sol")).tienda)
        assertEquals("Panadería Sol", leer("PANADERIA SOL\nTOTAL 3,00", listOf("Mercadona", "Panadería Sol")).tienda)
    }

    @Test fun saltaLineasQueNoSonElNombre() {
        assertEquals("Cafetería Luna", leer("TICKET DE VENTA\nwww.ejemplo.com\n28001 MADRID\nCafetería Luna\nTOTAL 3,00").tienda)
    }

    @Test fun textoVacioOIlegible() {
        assertTrue(leer("").vacio)
        assertTrue(leer("@@ ## !!\n12 34").vacio)
    }
}
