package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Aviso de «guardado»: los formularios lo lanzan y la app dibuja el ✓ encima de todo. */
object Hecho {
    private val _eventos = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val eventos = _eventos.asSharedFlow()
    fun avisar(texto: String) { _eventos.tryEmit(texto) }
}

/**
 * Un ✓ que aparece con efecto muelle, se dibuja y se va (≈0,8 s). No tapa nada que se pueda tocar.
 * Con «Quitar animaciones», Compose hace las transiciones al instante.
 */
@Composable
fun CapaHecho() {
    var texto by remember { mutableStateOf<String?>(null) }
    val escala = remember { Animatable(0.6f) }
    val trazo = remember { Animatable(0f) }
    val opacidad = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        Hecho.eventos.collect { t ->
            texto = t
            escala.snapTo(0.6f); trazo.snapTo(0f); opacidad.snapTo(1f)
            coroutineScope {
                listOf(
                    async { escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) },
                    async { delay(80); trazo.animateTo(1f, tween(320)) },
                ).awaitAll()
            }
            delay(260)
            opacidad.animateTo(0f, tween(200))
            texto = null
        }
    }
    val t = texto ?: return
    val fondo = MaterialTheme.colorScheme.primary
    val tinta = MaterialTheme.colorScheme.onPrimary
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(88.dp)
                .graphicsLayer { scaleX = escala.value; scaleY = escala.value; alpha = opacidad.value }
                .background(fondo, CircleShape)
                .semantics { contentDescription = t; liveRegion = LiveRegionMode.Polite },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(44.dp)) {
                val w = size.width
                val camino = Path().apply {
                    moveTo(w * 0.18f, w * 0.52f)
                    lineTo(w * 0.42f, w * 0.75f)
                    lineTo(w * 0.84f, w * 0.28f)
                }
                val medida = PathMeasure().apply { setPath(camino, false) }
                val parcial = Path()
                medida.getSegment(0f, medida.length * trazo.value, parcial, true)
                drawPath(parcial, tinta, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
