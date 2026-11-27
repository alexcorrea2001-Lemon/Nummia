package io.github.alexcorrea2001lemon.nummia.ui.bloqueo

import android.os.SystemClock
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImagenLogo
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** La actividad que hay detrás de un contexto de Compose (puede venir envuelto). */
tailrec fun android.content.Context.actividad(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.actividad()
    else -> null
}

/** Huella, cara o el PIN/patrón del propio móvil. */
object Bloqueo {
    const val AUTENTICADORES = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /** Si el móvil tiene huella, PIN o patrón con los que pedir la identidad. */
    fun disponible(ctx: android.content.Context) =
        androidx.biometric.BiometricManager.from(ctx).canAuthenticate(AUTENTICADORES) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

    fun pedir(actividad: FragmentActivity, titulo: String, alConseguir: () -> Unit, alFallar: (String) -> Unit) {
        val prompt = BiometricPrompt(
            actividad, ContextCompat.getMainExecutor(actividad),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = alConseguir()
                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    val cancelado = codigo == BiometricPrompt.ERROR_USER_CANCELED || codigo == BiometricPrompt.ERROR_CANCELED ||
                        codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    alFallar(if (cancelado) "" else mensaje.toString())
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder().setTitle(titulo).setSubtitle("Nummia").setAllowedAuthenticators(AUTENTICADORES).build(),
        )
    }
}

/**
 * Si la app está bloqueada. Se bloquea al arrancar y al volver tras estar fuera más del tiempo elegido en
 * Ajustes → Seguridad (solo tiene efecto si el bloqueo está activado).
 */
object Cerrojo {
    private val _bloqueada = MutableStateFlow(true)
    val bloqueada: StateFlow<Boolean> = _bloqueada
    private var salida = 0L

    /** Tiempo fuera permitido sin volver a pedir la identidad (lo fija MainActivity desde los ajustes). */
    @Volatile var esperaMs: Long = 60_000L

    /** ¿Hay que bloquear al volver? [salida] = 0 significa que aún no se había salido. */
    fun debeBloquear(salida: Long, ahora: Long, esperaMs: Long): Boolean = salida != 0L && ahora - salida >= esperaMs

    fun alSalir() { salida = SystemClock.elapsedRealtime() }
    fun alVolver() { if (debeBloquear(salida, SystemClock.elapsedRealtime(), esperaMs)) _bloqueada.value = true }
    fun desbloquear() { _bloqueada.value = false }
}

@Composable
fun PantallaBloqueo(logo: Logo, color: Acento) {
    val actividad = LocalContext.current.actividad()
    var error by remember { mutableStateOf("") }
    fun pedir() = actividad?.let { Bloqueo.pedir(it, "Desbloquear Nummia", alConseguir = { Cerrojo.desbloquear() }, alFallar = { m -> error = m }) }
    LaunchedEffect(Unit) { pedir() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(Espacio.xxl),
            verticalArrangement = Arrangement.spacedBy(Espacio.l, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImagenLogo(logo, color, Modifier.size(88.dp), "Nummia")
            Text("Nummia está bloqueada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (error.isNotEmpty()) Text(error, color = LocalColoresNummia.current.aviso, textAlign = TextAlign.Center)
            Button(onClick = { error = ""; pedir() }) { Text("Desbloquear") }
        }
    }
}
