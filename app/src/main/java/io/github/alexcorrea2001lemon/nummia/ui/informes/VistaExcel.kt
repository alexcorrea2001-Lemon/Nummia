package io.github.alexcorrea2001lemon.nummia.ui.informes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexcorrea2001lemon.nummia.data.importar.Celda
import io.github.alexcorrea2001lemon.nummia.data.importar.LectorXlsx
import io.github.alexcorrea2001lemon.nummia.data.informes.EscritorXlsx
import io.github.alexcorrea2001lemon.nummia.data.informes.Estilo
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import java.io.File
import java.time.LocalDate

enum class TipoCeldaVista { NORMAL, NEGRITA, CABECERA, TITULO, SUBTITULO, TOTAL, FRANJA }

data class CeldaVista(val texto: String, val tipo: TipoCeldaVista, val derecha: Boolean)
data class HojaVista(val nombre: String, val filas: List<List<CeldaVista>>, val columnas: Int)

private const val MAX_FILAS = 150

/** Lee un Excel creado por Nummia para verlo dentro de la app (las 150 primeras filas de cada hoja). */
fun vistaExcel(f: File): List<HojaVista> = LectorXlsx.leerHojas(f.readBytes()).map { (nombre, celdas) ->
    val refs = celdas.keys.mapNotNull { Regex("""^([A-Z]+)(\d+)$""").find(it)?.destructured?.let { (c, r) -> c to r.toInt() } }
    val numCol = { c: String -> c.fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) } - 1 }
    val columnas = (refs.maxOfOrNull { numCol(it.first) } ?: 0) + 1
    val filas = (refs.maxOfOrNull { it.second } ?: 0).coerceAtMost(MAX_FILAS)
    HojaVista(nombre, (1..filas).map { r -> (0 until columnas).map { c -> celdaVista(celdas[EscritorXlsx.columna(c) + r]) } }, columnas)
}

private fun celdaVista(c: Celda?): CeldaVista {
    if (c == null) return CeldaVista("", TipoCeldaVista.NORMAL, false)
    val tipo = when (c.estilo) {
        Estilo.NEGRITA.indice, EscritorXlsx.EUROS_NEGRITA -> TipoCeldaVista.NEGRITA
        Estilo.CABECERA.indice -> TipoCeldaVista.CABECERA
        Estilo.TITULO.indice -> TipoCeldaVista.TITULO
        Estilo.SUBTITULO.indice -> TipoCeldaVista.SUBTITULO
        Estilo.TOTAL.indice, EscritorXlsx.EUROS_TOTAL, EscritorXlsx.NUMERO_TOTAL -> TipoCeldaVista.TOTAL
        EscritorXlsx.EUROS_FRANJA, EscritorXlsx.FECHA_FRANJA, EscritorXlsx.TEXTO_FRANJA, EscritorXlsx.NUMERO_FRANJA -> TipoCeldaVista.FRANJA
        else -> TipoCeldaVista.NORMAL
    }
    val n = c.numero
    return when {
        n != null && c.estilo in EscritorXlsx.ESTILOS_EUROS -> CeldaVista(formatearEuros(n.movePointRight(2).toLong()), tipo, true)
        n != null && c.estilo in EscritorXlsx.ESTILOS_FECHA -> CeldaVista(LocalDate.ofEpochDay(n.toLong() - 25569).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }, tipo, false)
        n != null -> CeldaVista(n.stripTrailingZeros().toPlainString(), tipo, true)
        else -> CeldaVista(c.texto.orEmpty(), tipo, false)
    }
}

/** Vista previa con pestañas por hoja y la tabla con los mismos colores que el Excel. */
@Composable
fun VistaExcel(hojas: List<HojaVista>) {
    var sel by remember(hojas) { mutableIntStateOf(0) }
    val hoja = hojas.getOrNull(sel) ?: return
    val violeta = Color(0xFF7C5CD6)
    val franja = MaterialTheme.colorScheme.surfaceContainerHighest
    Column {
        if (hojas.size > 1) {
            PrimaryScrollableTabRow(selectedTabIndex = sel, edgePadding = 0.dp, containerColor = Color.Transparent) {
                hojas.forEachIndexed { i, h -> Tab(selected = sel == i, onClick = { sel = i }, text = { Text(h.nombre, maxLines = 1) }) }
            }
        }
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
            hoja.filas.forEach { fila ->
                val titulo = fila.any { it.tipo == TipoCeldaVista.TITULO || it.tipo == TipoCeldaVista.SUBTITULO }
                Row {
                    fila.forEachIndexed { i, c ->
                        val ancho = if (titulo) 320.dp else if (i == 0) 130.dp else 104.dp
                        if (titulo && i > 0) return@forEachIndexed
                        val fondo = when (c.tipo) {
                            TipoCeldaVista.CABECERA -> violeta
                            TipoCeldaVista.TOTAL, TipoCeldaVista.FRANJA -> franja
                            else -> Color.Transparent
                        }
                        Text(
                            c.texto,
                            modifier = Modifier.width(ancho).background(fondo)
                                .then(if (titulo) Modifier else Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant))
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = if (c.derecha) TextAlign.End else TextAlign.Start,
                            color = when (c.tipo) {
                                TipoCeldaVista.CABECERA -> Color.White
                                TipoCeldaVista.TITULO -> violeta
                                TipoCeldaVista.SUBTITULO -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (c.tipo in setOf(TipoCeldaVista.CABECERA, TipoCeldaVista.TOTAL, TipoCeldaVista.NEGRITA, TipoCeldaVista.TITULO)) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = if (c.tipo == TipoCeldaVista.TITULO) 18.sp else 13.sp,
                        )
                    }
                }
            }
        }
    }
}
