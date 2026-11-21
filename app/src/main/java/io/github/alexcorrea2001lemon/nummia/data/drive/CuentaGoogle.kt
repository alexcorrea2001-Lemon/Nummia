package io.github.alexcorrea2001lemon.nummia.data.drive

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class NecesitaReconectar : Exception("Vuelve a conectar tu Drive en Ajustes.")

sealed interface ResultadoConexion {
    data class Conectado(val token: String, val correo: String?) : ResultadoConexion
    data class PideConsentimiento(val pendiente: PendingIntent) : ResultadoConexion
    data class Error(val mensaje: String) : ResultadoConexion
}

/**
 * Permiso de Google con `drive.file`: Nummia solo ve los archivos que ella misma crea.
 * No hace falta ningún identificador en el código: Google reconoce la app por su paquete y su firma.
 */
@Singleton
class CuentaGoogle @Inject constructor(@ApplicationContext private val context: Context) {
    private val alcance = Scope("https://www.googleapis.com/auth/drive.file")
    @Volatile private var ultimoToken: String? = null

    private fun peticion(correo: String?) = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(alcance))
        .apply { if (correo != null) setAccount(Account(correo, "com.google")) }
        .build()

    private fun leer(r: AuthorizationResult): ResultadoConexion {
        val pendiente = r.pendingIntent
        if (r.hasResolution() && pendiente != null) return ResultadoConexion.PideConsentimiento(pendiente)
        val token = r.accessToken ?: return ResultadoConexion.Error("Google no ha dado acceso.")
        ultimoToken = token
        @Suppress("DEPRECATION")
        return ResultadoConexion.Conectado(token, r.toGoogleSignInAccount()?.email)
    }

    /** Pide acceso. Si hace falta que la persona acepte, devuelve el diálogo que hay que mostrar. */
    suspend fun conectar(correo: String? = null): ResultadoConexion = try {
        leer(Identity.getAuthorizationClient(context).authorize(peticion(correo)).await())
    } catch (e: Exception) {
        ResultadoConexion.Error(mensajeDe(e))
    }

    /** Resultado del diálogo de Google. */
    fun resultado(datos: Intent?): ResultadoConexion = try {
        leer(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(datos))
    } catch (e: Exception) {
        ResultadoConexion.Error(mensajeDe(e))
    }

    /** Token para usar en segundo plano. Si Google pide confirmar algo, hay que reconectar desde Ajustes. */
    suspend fun token(correo: String?, renovar: Boolean): String {
        if (!renovar) ultimoToken?.let { return it }
        if (renovar) ultimoToken?.let { t -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, t) } } }
        ultimoToken = null
        return when (val r = conectar(correo)) {
            is ResultadoConexion.Conectado -> r.token
            is ResultadoConexion.PideConsentimiento -> throw NecesitaReconectar()
            is ResultadoConexion.Error -> throw java.io.IOException(r.mensaje)
        }
    }

    fun olvidar() { ultimoToken = null }

    private fun mensajeDe(e: Exception): String {
        val codigo = (e as? com.google.android.gms.common.api.ApiException)?.statusCode
        val t = e.message.orEmpty()
        val texto = when {
            codigo == 10 || "DEVELOPER_ERROR" in t -> "Google no reconoce la app. Revisa en Google Cloud el cliente Android (paquete y huella SHA-1) o espera unos minutos."
            codigo == 7 || "NETWORK" in t -> "No hay conexión."
            codigo == 16 || "CANCELED" in t -> "Google ha cerrado la ventana sin dar acceso."
            else -> "No se pudo conectar con Google."
        }
        return "$texto (código ${codigo ?: "?"}${if (t.isNotBlank()) ": $t" else ""})"
    }
}
