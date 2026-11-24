package io.github.alexcorrea2001lemon.nummia.ui.inicio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

/**
 * Avance 0→1 la primera vez que se ven estos datos. Al volver a la tarjeta tras desplazarse
 * (la lista la descarta al salir de pantalla) no se repite: se guarda qué datos ya se animaron.
 * Con «Quitar animaciones» llega al instante.
 */
@Composable
private fun avance(clave: Any?): Float {
    var animada by rememberSaveable { mutableIntStateOf(0) }
    val huella = clave.hashCode() or 1
    val p = remember { Animatable(if (animada == huella) 1f else 0f) }
    LaunchedEffect(huella) {
        if (animada == huella) { p.snapTo(1f); return@LaunchedEffect }
        p.snapTo(0f)
        p.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        animada = huella
    }
    return p.value
}

/** Anillo con la parte gastada de lo que entró (0–1) y el porcentaje en el centro. */
@Composable
fun AnilloGasto(parte: Float?, porcentaje: Int?, lado: Dp = 104.dp, modifier: Modifier = Modifier) {
    val c = LocalColoresNummia.current
    val fondo = MaterialTheme.colorScheme.surfaceVariant
    val color = when {
        porcentaje == null -> fondo
        porcentaje >= 100 -> c.gasto
        porcentaje >= 80 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val barrido by animateFloatAsState(parte ?: 0f, spring(dampingRatio = 0.85f, stiffness = 120f), label = "anillo")
    Box(modifier.size(lado), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(lado)) {
            val grosor = size.minDimension * 0.12f
            val inset = grosor / 2
            val area = Size(size.width - grosor, size.height - grosor)
            drawArc(fondo, 0f, 360f, false, Offset(inset, inset), area, style = Stroke(grosor))
            if (barrido > 0f) drawArc(color, -90f, 360f * barrido, false, Offset(inset, inset), area, style = Stroke(grosor, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                porcentaje?.let { "$it %" } ?: "—",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Text("gastado", style = MaterialTheme.typography.labelSmall, color = c.textoSuave, textAlign = TextAlign.Center)
        }
    }
}

/** Barra partida en tramos de colores (uno por categoría), que se abre de izquierda a derecha. */
@Composable
fun BarraReparto(partes: List<Pair<String, Long>>, colores: List<Color>, descripcion: String, modifier: Modifier = Modifier) {
    val p = avance(partes)
    val total = partes.sumOf { it.second }.toFloat()
    Row(
        modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).semantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total <= 0f) return@Row
        partes.forEachIndexed { i, (_, v) ->
            val peso = (v / total * p).coerceAtLeast(0.001f)
            Box(Modifier.weight(peso).height(14.dp).background(colores[i % colores.size]))
        }
        // Lo que falta por abrirse en la animación queda vacío.
        if (p < 1f) Box(Modifier.weight(1f - p))
    }
}

/** Punto de color de una leyenda. */
@Composable
fun PuntoLeyenda(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(color))
}
