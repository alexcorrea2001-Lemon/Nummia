package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Iconos de trazo propios (24×24), los mismos de la maqueta. Pocos bytes y coherentes entre sí. */
object Iconos {
    private fun trazo(nombre: String, vararg caminos: String, grosor: Float = 1.8f): ImageVector =
        ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
            caminos.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = grosor,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Inicio by lazy { trazo("inicio", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z") }
    val Movimientos by lazy { trazo("movimientos", "M7 4v16M7 4 3 8M7 4l4 4M17 20V4M17 20l-4-4M17 20l4-4") }
    val Calendario by lazy { trazo("calendario", "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zM3 10h18M8 3v4M16 3v4") }
    val Estadisticas by lazy { trazo("estadisticas", "M5 20V11M11 20V5M17 20v-7M3 20h18") }
    val Informes by lazy { trazo("informes", "M14 3H6a1 1 0 0 0-1 1v16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8zM14 3v5h5M9 13h6M9 17h6") }
    val Buscar by lazy { trazo("buscar", "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14zM20 20l-3.5-3.5") }
    val Ajustes by lazy { trazo("ajustes", "M4 7h9M17 7h3M15 5a2 2 0 1 1 0 4a2 2 0 1 1 0-4zM4 17h3M11 17h9M9 15a2 2 0 1 1 0 4a2 2 0 1 1 0-4z") }
    val Ojo by lazy { trazo("ojo", "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z") }
    val OjoTachado by lazy { trazo("ojo_tachado", "M3 3l18 18M10.6 6.1A10 10 0 0 1 12 6c5 0 9 6 9 6a17 17 0 0 1-2.6 3.2M6.6 6.6C4.3 8.1 3 12 3 12s4 6 9 6a9 9 0 0 0 4.4-1.1") }
    val Filtro by lazy { trazo("filtro", "M3 5h18l-7 8v6l-4 2v-8z") }
    val Repetir by lazy { trazo("repetir", "M4 12a8 8 0 1 0 3-6.2M4 4v4h4", grosor = 2f) }
    val Movil by lazy { trazo("movil", "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM11 18h2") }
    val Persona by lazy { trazo("persona", "M12 11a4 4 0 1 0 0-8a4 4 0 1 0 0 8zM4 21a8 8 0 0 1 16 0") }
    val FlechaAbajo by lazy { trazo("flecha_abajo", "M12 5v14M5 12l7 7 7-7", grosor = 2f) }
    val Lapiz by lazy { trazo("lapiz", "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4") }
    val Papelera by lazy { trazo("papelera", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val Mas by lazy { trazo("mas", "M12 5v14M5 12h14", grosor = 2.4f) }
    val Cerrar by lazy { trazo("cerrar", "M6 6l12 12M18 6 6 18", grosor = 2f) }
    val Atras by lazy { trazo("atras", "M19 12H5M11 18l-6-6 6-6", grosor = 2f) }
    val ChevronIzq by lazy { trazo("chevron_izq", "M15 6l-6 6 6 6", grosor = 2f) }
    val ChevronDer by lazy { trazo("chevron_der", "M9 6l6 6-6 6", grosor = 2f) }
    val Duplicar by lazy { trazo("duplicar", "M8 8h12v12H8zM4 16V4h12") }
    val Check by lazy { trazo("check", "M5 12l5 5L20 7", grosor = 2.2f) }
    val Descargar by lazy { trazo("descargar", "M12 4v12M6 10l6 6 6-6M4 20h16") }
    val Subir by lazy { trazo("subir", "M12 16V4M6 10l6-6 6 6M4 20h16") }
    val Info by lazy { trazo("info", "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0-18zM12 11v6M12 7.5v.5") }
    val Euro by lazy { trazo("euro", "M18 6.5A7 7 0 1 0 18 17.5M4 10h10M4 14h10") }
    val Bolsa by lazy { trazo("bolsa", "M5 8h14l-1 12H6zM9 8V6a3 3 0 0 1 6 0v2") }
    val Paleta by lazy { trazo("paleta", "M12 3a9 9 0 1 0 0 18c1 0 1.5-.7 1.5-1.5 0-.5-.2-.8-.5-1.2-.3-.4-.5-.7-.5-1.2 0-.8.7-1.5 1.5-1.5H16a5 5 0 0 0 5-5c0-4-4-7.6-9-7.6zM7.5 11.5h.01M10.5 7.5h.01M15 7.5h.01") }
    val Actualizar by lazy { trazo("actualizar", "M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5") }
    val Escudo by lazy { trazo("escudo", "M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6z") }
    val Codigo by lazy { trazo("codigo", "M8 8l-4 4 4 4M16 8l4 4-4 4M14 5l-4 14") }
}
