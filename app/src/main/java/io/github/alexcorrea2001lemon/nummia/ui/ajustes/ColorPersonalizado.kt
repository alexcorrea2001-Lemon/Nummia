package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlin.math.roundToInt

/** Texto del color en hexadecimal, p. ej. `#7C5CFF`. */
internal fun hexadecimal(c: Color): String = "#%06X".format(c.toArgb() and 0xFFFFFF)

/**
 * Barra de degradado con un tirador: se ve el color que se va a obtener en cada punto, como en los
 * selectores de color de los móviles. Se arrastra, se toca y funciona con TalkBack (subir/bajar).
 */
@Composable
private fun BarraDegradado(
    titulo: String, valor: Float, rango: ClosedFloatingPointRange<Float>, colores: List<Color>, color: Color,
    alCambiar: (Float) -> Unit,
) {
    var ancho by remember { mutableFloatStateOf(1f) }
    val tirador = 28.dp
    val densidad = LocalDensity.current
    val fraccion = ((valor - rango.start) / (rango.endInclusive - rango.start)).coerceIn(0f, 1f)
    fun desde(x: Float) = alCambiar(rango.start + (x / ancho).coerceIn(0f, 1f) * (rango.endInclusive - rango.start))
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier.fillMaxWidth().height(tirador + 8.dp).onSizeChanged { ancho = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(rango) { detectTapGestures { desde(it.x) } }
                .pointerInput(rango) { detectDragGestures { cambio, _ -> cambio.consume(); desde(cambio.position.x) } }
                .semantics {
                    contentDescription = titulo
                    progressBarRangeInfo = ProgressBarRangeInfo(valor, rango)
                    setProgress { v -> alCambiar(v.coerceIn(rango.start, rango.endInclusive)); true }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(colores)))
            val x = with(densidad) { (fraccion * (ancho - tirador.toPx())).roundToInt() }
            Box(
                Modifier.offset { IntOffset(x, 0) }.size(tirador).shadow(3.dp, CircleShape)
                    .background(color, CircleShape).border(3.dp, Color.White, CircleShape),
            )
        }
    }
}

/**
 * Hoja para elegir cualquier color de la app: vista previa, tres barras con degradado (tono, intensidad
 * y brillo) y el código del color. El brillo mínimo asegura que el texto siga leyéndose bien.
 */
@Composable
fun DialogoColorPersonalizado(inicial: Color, alCerrar: () -> Unit, alAceptar: (Color) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(inicial.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1].coerceIn(0.3f, 1f)) }
    var v by remember { mutableFloatStateOf(hsv[2].coerceIn(0.5f, 1f)) }
    val color = Color.hsv(h, s, v)
    val tonos = remember { (0..6).map { Color.hsv(it * 60f, s.coerceAtLeast(0.3f), v) } }
    DialogoNummia(
        onDismissRequest = alCerrar,
        title = { Text("Color personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
                // Vista previa con los elementos que llevarán este color.
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.16f)).padding(Espacio.l),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    Box(Modifier.size(48.dp).background(color, CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text("Así se verá", fontWeight = FontWeight.SemiBold)
                        Text(hexadecimal(color), style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
                    }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = Espacio.l, vertical = Espacio.s)) {
                        Text("Botón", color = if (color.luminance() > 0.5f) Color.Black else Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                BarraDegradado("Tono", h, 0f..360f, tonos, color) { h = it }
                BarraDegradado("Intensidad", s, 0.3f..1f, listOf(Color.hsv(h, 0.3f, v), Color.hsv(h, 1f, v)), color) { s = it }
                BarraDegradado("Brillo", v, 0.5f..1f, listOf(Color.hsv(h, s, 0.5f), Color.hsv(h, s, 1f)), color) { v = it }
            }
        },
        confirmButton = { Button(onClick = { alAceptar(color) }) { Text("Usar este color") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
