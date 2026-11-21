package io.github.alexcorrea2001lemon.nummia.ui.drive

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

/** Icono pequeño en Inicio: sincronizando, todo bien o algo que mirar. No aparece si Drive no está conectado. */
@Composable
fun IconoEstadoDrive(alPulsar: () -> Unit, vm: DriveViewModel = hiltViewModel()) {
    val e by vm.estado.collectAsStateWithLifecycle()
    if (!e.conectado) return
    val contexto = LocalContext.current
    val animar = remember { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val c = LocalColoresNummia.current
    IconButton(onClick = alPulsar) {
        when {
            e.ocupado -> {
                val giro = if (animar) {
                    val t = rememberInfiniteTransition(label = "sync")
                    t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "giro").value
                } else 0f
                Icon(Iconos.Actualizar, "Sincronizando con Drive", Modifier.rotate(giro), tint = c.textoSuave)
            }
            e.conflictos.isNotEmpty() || e.error != null || e.avisos.isNotEmpty() ->
                Icon(Iconos.Aviso, "Drive necesita tu atención", tint = c.aviso)
            else -> Icon(Iconos.Nube, if (e.pendiente) "Cambios pendientes de subir a Drive" else "Drive sincronizado", tint = c.textoSuave)
        }
    }
}
