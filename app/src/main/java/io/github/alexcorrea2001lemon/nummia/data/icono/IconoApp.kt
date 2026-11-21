package io.github.alexcorrea2001lemon.nummia.data.icono

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo

/**
 * Cambia el icono de la pantalla de inicio activando uno de los 36 `activity-alias` del manifiesto
 * (3 logos × 12 colores) y desactivando el resto.
 */
object IconoApp {
    private const val PAQUETE_CODIGO = "io.github.alexcorrea2001lemon.nummia"

    fun alias(logo: Logo, color: Acento) = "$PAQUETE_CODIGO.Icono_${logo.name}_${color.name.lowercase()}"

    private val POR_DEFECTO = alias(Logo.A, Acento.VIOLETA)

    private fun activo(pm: PackageManager, context: Context, nombre: String): Boolean =
        when (pm.getComponentEnabledSetting(ComponentName(context.packageName, nombre))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> nombre == POR_DEFECTO
            else -> false
        }

    /** Cambia el icono solo si el que está activo no es el elegido. Devuelve si hubo cambio. */
    fun aplicarSiCambia(context: Context, logo: Logo, color: Acento): Boolean {
        val pm = context.packageManager
        val elegido = alias(logo, color)
        val otrosActivos = Logo.entries.flatMap { l -> Acento.entries.map { alias(l, it) } }.filter { it != elegido && activo(pm, context, it) }
        if (activo(pm, context, elegido) && otrosActivos.isEmpty()) return false
        aplicar(context, logo, color)
        return true
    }

    fun aplicar(context: Context, logo: Logo, color: Acento) {
        val pm = context.packageManager
        val elegido = alias(logo, color)
        // Primero se activa el nuevo para que nunca quede la app sin icono.
        pm.setComponentEnabledSetting(ComponentName(context.packageName, elegido), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        Logo.entries.forEach { l ->
            Acento.entries.forEach { a ->
                val nombre = alias(l, a)
                if (nombre != elegido) {
                    pm.setComponentEnabledSetting(ComponentName(context.packageName, nombre), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
