package io.github.alexcorrea2001lemon.nummia.ui.graficos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

/** Progreso 0→1 cada vez que cambian los datos (con «Quitar animaciones», Compose lo deja al instante). */
@Composable
private fun progreso(clave: Any?): Float {
    val p = remember { Animatable(0f) }
    LaunchedEffect(clave) {
        p.snapTo(0f)
        p.animateTo(1f, tween(durationMillis = 750, easing = FastOutSlowInEasing))
    }
    return p.value
}

data class SerieLinea(val valores: List<Long>, val color: Color, val discontinua: Boolean = false)

/** Gráfico de líneas que se traza de izquierda a derecha. */
@Composable
fun GraficoLineas(series: List<SerieLinea>, etiquetas: List<String>, descripcion: String, modifier: Modifier = Modifier, alto: Dp = 180.dp) {
    val p = progreso(series.map { it.valores })
    val rejilla = LocalColoresNummia.current.borde
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(alto).semantics { contentDescription = descripcion }) {
            val todos = series.flatMap { it.valores }
            if (todos.isEmpty()) return@Canvas
            val min = minOf(0L, todos.min()).toFloat()
            val max = maxOf(todos.max().toFloat(), min + 1f)
            val pad = 6.dp.toPx()
            val h = size.height - pad * 2
            fun y(v: Long) = pad + h - (v - min) / (max - min) * h
            for (i in 0..3) {
                val yy = pad + h * i / 3f
                drawLine(rejilla, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1.dp.toPx())
            }
            if (min < 0) drawLine(rejilla.copy(alpha = 1f), Offset(0f, y(0)), Offset(size.width, y(0)), strokeWidth = 1.5.dp.toPx())
            series.forEach { s ->
                if (s.valores.isEmpty()) return@forEach
                val paso = if (s.valores.size > 1) size.width / (s.valores.size - 1) else 0f
                val camino = Path().apply {
                    s.valores.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(i * paso, y(v)) }
                }
                val medida = PathMeasure().apply { setPath(camino, false) }
                val parcial = Path()
                medida.getSegment(0f, medida.length * p, parcial, true)
                drawPath(
                    parcial, s.color,
                    style = Stroke(
                        width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = if (s.discontinua) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null,
                    ),
                )
            }
        }
        if (etiquetas.isNotEmpty()) EtiquetasEje(etiquetas)
    }
}

/** Barras agrupadas (una barra por serie en cada punto). Pulsar una columna avisa con su índice. */
@Composable
fun GraficoBarras(
    grupos: List<List<Long>>,
    colores: List<Color>,
    etiquetas: List<String>,
    descripcion: String,
    modifier: Modifier = Modifier,
    seleccionado: Int? = null,
    alto: Dp = 180.dp,
    alPulsar: ((Int) -> Unit)? = null,
) {
    val p = progreso(grupos)
    val rejilla = LocalColoresNummia.current.borde
    val resalte = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxWidth().height(alto).semantics { contentDescription = descripcion }.let { m ->
                if (alPulsar == null || grupos.isEmpty()) m else m.pointerInput(grupos.size) {
                    detectTapGestures { pos -> alPulsar((pos.x / (size.width.toFloat() / grupos.size)).toInt().coerceIn(0, grupos.size - 1)) }
                }
            },
        ) {
            if (grupos.isEmpty()) return@Canvas
            val todos = grupos.flatten()
            val min = minOf(0L, todos.minOrNull() ?: 0).toFloat()
            val max = maxOf((todos.maxOrNull() ?: 0).toFloat(), 1f)
            val rango = max - min
            val pad = 4.dp.toPx()
            val h = size.height - pad * 2
            fun y(v: Float) = pad + h - (v - min) / rango * h
            val cero = y(0f)
            val ranura = size.width / grupos.size
            val series = grupos.maxOf { it.size }.coerceAtLeast(1)
            val hueco = ranura * 0.22f
            val ancho = (ranura - hueco * 2) / series
            grupos.forEachIndexed { i, valores ->
                if (i == seleccionado) drawRoundRect(resalte, Offset(i * ranura, 0f), Size(ranura, size.height), CornerRadius(8.dp.toPx()))
                valores.forEachIndexed { j, v ->
                    val alturaFinal = y(v.toFloat())
                    val tope = cero + (alturaFinal - cero) * p
                    val x = i * ranura + hueco + j * ancho
                    val arriba = minOf(tope, cero)
                    val alto2 = kotlin.math.abs(cero - tope).coerceAtLeast(if (v != 0L) 1.dp.toPx() else 0f)
                    drawRoundRect(colores[j % colores.size], Offset(x + ancho * 0.08f, arriba), Size(ancho * 0.84f, alto2), CornerRadius(minOf(ancho * 0.3f, 6.dp.toPx())))
                }
            }
            drawLine(rejilla, Offset(0f, cero), Offset(size.width, cero), strokeWidth = 1.dp.toPx())
        }
        if (etiquetas.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                etiquetas.forEach { e ->
                    Text(e, style = MaterialTheme.typography.labelSmall, color = LocalColoresNummia.current.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1)
                }
            }
        }
    }
}

/** Colores para gráficos de sectores: los de la paleta de acentos, empezando por el principal. */
@Composable
fun coloresGrafico(): List<Color> {
    val primero = MaterialTheme.colorScheme.primary
    return listOf(primero) + Acento.entries.map { it.color }.filter { it != primero }
}

/** Donut que se abre en giro. Solo valores positivos. */
@Composable
fun GraficoDonut(partes: List<Pair<String, Long>>, colores: List<Color>, descripcion: String, modifier: Modifier = Modifier, lado: Dp = 160.dp) {
    val p = progreso(partes)
    val fondo = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.size(lado).semantics { contentDescription = descripcion }) {
        val grosor = size.minDimension * 0.16f
        val estilo = Stroke(width = grosor, cap = StrokeCap.Butt)
        val inset = grosor / 2
        val area = Size(size.width - grosor, size.height - grosor)
        drawArc(fondo, 0f, 360f, false, Offset(inset, inset), area, style = estilo)
        val total = partes.sumOf { it.second.coerceAtLeast(0) }.toFloat()
        if (total <= 0f) return@Canvas
        var inicio = -90f
        partes.forEachIndexed { i, (_, v) ->
            val barrido = v.coerceAtLeast(0) / total * 360f * p
            drawArc(colores[i % colores.size], inicio, (barrido - 1.5f).coerceAtLeast(0.5f), false, Offset(inset, inset), area, style = estilo)
            inicio += barrido
        }
    }
}

@Composable
private fun EtiquetasEje(etiquetas: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        etiquetas.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = LocalColoresNummia.current.textoSuave) }
    }
}
