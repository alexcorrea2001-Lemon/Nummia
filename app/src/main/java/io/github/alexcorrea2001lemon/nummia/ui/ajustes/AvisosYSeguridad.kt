package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.avisos.TrabajoAvisos
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.Bloqueo
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

@Composable
fun NotificacionesScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var permitido by remember { mutableStateOf(TrabajoAvisos.puedeNotificar(ctx)) }
    var denegado by remember { mutableStateOf(false) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permitido = TrabajoAvisos.puedeNotificar(ctx)
        if (ok && permitido) vm.notificaciones(true) else denegado = true
    }
    LaunchedEffect(Unit) { permitido = TrabajoAvisos.puedeNotificar(ctx) }

    PantallaAjustes("Notificaciones", alVolver) {
        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Info, "Avisarme", "Nummia no usa internet para esto ni gasta batería: revisa una vez al día y tras cada cambio", a.notificaciones && permitido) { v ->
                when {
                    !v -> vm.notificaciones(false)
                    permitido -> vm.notificaciones(true)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> denegado = true
                }
            }
        }
        AnimatedVisibility(denegado && !permitido) {
            Tarjeta(fondo = LocalColoresNummia.current.avisoFondo) {
                Text("Las notificaciones están desactivadas para Nummia en tu móvil.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text("Abrir ajustes del móvil") }
            }
        }
        AnimatedVisibility(a.notificaciones && permitido) {
            Tarjeta(relleno = false) {
                OpcionInterruptor(Iconos.Euro, "Sueldo", "Desde el día 27, para confirmarlo", a.avisoSueldo, vm::avisoSueldo)
                HorizontalDivider(color = LocalColoresNummia.current.borde)
                OpcionInterruptor(Iconos.Repetir, "Pagos del día", "Cuando toca un pago mensual", a.avisoPagos, vm::avisoPagos)
                HorizontalDivider(color = LocalColoresNummia.current.borde)
                OpcionInterruptor(Iconos.Estadisticas, "Presupuestos", "Al llegar al 80 % y al pasarte", a.avisoPresupuestos, vm::avisoPresupuestos)
            }
        }
    }
}

@Composable
fun SeguridadScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val actividad = ctx as? FragmentActivity
    val disponible = remember { BiometricManager.from(ctx).canAuthenticate(Bloqueo.AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaAjustes("Seguridad", alVolver) {
        Tarjeta(relleno = false) {
            OpcionInterruptor(
                Iconos.Escudo, "Bloquear Nummia",
                "Pide tu huella, cara o el PIN del móvil al abrir la app y tras 1 minuto fuera de ella",
                a.bloqueo,
            ) { v ->
                error = null
                when {
                    !v -> vm.bloqueo(false)
                    !disponible -> error = "Primero pon un bloqueo de pantalla (PIN, patrón o huella) en los ajustes del móvil."
                    actividad != null -> Bloqueo.pedir(actividad, "Activar el bloqueo", alConseguir = { io.github.alexcorrea2001lemon.nummia.ui.bloqueo.Cerrojo.desbloquear(); vm.bloqueo(true) }, alFallar = { error = it })
                }
            }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionInterruptor(Iconos.OjoTachado, "Modo discreto", "Oculta los importes con ••••", a.modoDiscreto, vm::discreto)
        }
        error?.let { Column { Text(it, color = LocalColoresNummia.current.aviso, style = MaterialTheme.typography.bodyMedium) } }
        Text(
            "Nummia usa el bloqueo de tu propio móvil: no hay otra contraseña que recordar. Tus datos ya están cifrados en el móvil aunque no actives esto.",
            style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave,
        )
    }
}
