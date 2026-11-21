package io.github.alexcorrea2001lemon.nummia.ui.comun

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlin.math.roundToLong

/** Vibración corta de confirmación. */
@Composable
fun rememberHaptica(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            view.performHapticFeedback(tipo)
        }
    }
}

/** Importe que «cuenta» hasta el valor nuevo. Si Android tiene las animaciones quitadas, cambia al instante. */
@Composable
fun ImporteAnimado(
    centimos: Long,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    conSigno: Boolean = false,
    discreto: Boolean = false,
) {
    var desde by remember { mutableLongStateOf(centimos) }
    var hasta by remember { mutableLongStateOf(centimos) }
    val progreso = remember { Animatable(1f) }
    val mostrado = desde + ((hasta - desde) * progreso.value).roundToLong()
    LaunchedEffect(centimos) {
        if (centimos != hasta) {
            desde = mostrado
            hasta = centimos
            progreso.snapTo(0f)
            progreso.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
        }
    }
    val final = formatearEuros(centimos, conSigno, discreto)
    Text(
        text = formatearEuros(mostrado, conSigno, discreto),
        style = estilo,
        color = color,
        modifier = modifier.semantics { contentDescription = if (discreto) "Importe oculto" else final },
    )
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.surfaceContainer,
    relleno: Boolean = true,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = fondo,
        border = BorderStroke(1.dp, LocalColoresNummia.current.borde),
    ) {
        Column(
            modifier = if (relleno) Modifier.padding(Espacio.l) else Modifier,
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
            content = contenido,
        )
    }
}

@Composable
fun Avatar(texto: String, fondo: Color = MaterialTheme.colorScheme.surfaceVariant, color: Color = MaterialTheme.colorScheme.onSurface, icono: ImageVector? = null) {
    Box(Modifier.size(40.dp).background(fondo, CircleShape), contentAlignment = Alignment.Center) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        } else {
            Text(texto.take(1).uppercase(), color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EncabezadoSeccion(titulo: String, modifier: Modifier = Modifier, accion: String? = null, alPulsar: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (accion != null) TextButton(onClick = alPulsar) { Text(accion) }
    }
}

/** Estado vacío: una invitación, no un error. */
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier, accion: String? = null, alPulsar: () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = Espacio.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave, textAlign = TextAlign.Center)
        if (accion != null) {
            Spacer(Modifier.height(Espacio.xs))
            androidx.compose.material3.Button(onClick = alPulsar) { Text(accion) }
        }
    }
}
