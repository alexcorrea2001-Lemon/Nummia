package io.github.alexcorrea2001lemon.nummia.data.actualizar

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.security.MessageDigest

/** Instala la actualización con PackageInstaller tras comprobar que está firmada igual que la app instalada. */
object Instalador {
    const val ACCION = "io.github.alexcorrea2001lemon.nummia.INSTALACION"
    const val EXTRA_ARCHIVO = "archivo"

    fun puedeInstalar(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun abrirPermiso(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    @Suppress("DEPRECATION")
    private fun huellas(info: PackageInfo?): Set<String> {
        if (info == null) return emptySet()
        val firmas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory }
        } else {
            info.signatures
        } ?: return emptySet()
        return firmas.map { s -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    @Suppress("DEPRECATION")
    fun mismaFirma(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val nueva = pm.getPackageArchiveInfo(apk.absolutePath, flag)
        if (nueva?.packageName != context.packageName) return false
        val instalada = huellas(pm.getPackageInfo(context.packageName, flag))
        val archivo = huellas(nueva)
        return instalada.isNotEmpty() && archivo.isNotEmpty() && instalada.intersect(archivo).isNotEmpty()
    }

    /** Lanza la instalación. El resultado llega a [ReceptorInstalacion], que borra el archivo. */
    fun instalar(context: Context, apk: File) {
        val instalador = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = instalador.createSession(params)
        instalador.openSession(id).use { sesion ->
            sesion.openWrite("nummia.apk", 0, apk.length()).use { salida ->
                apk.inputStream().use { it.copyTo(salida) }
                sesion.fsync(salida)
            }
            val intent = Intent(context, ReceptorInstalacion::class.java).setAction(ACCION).putExtra(EXTRA_ARCHIVO, apk.absolutePath)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getBroadcast(context, id, intent, flags)
            sesion.commit(pi.intentSender)
        }
    }
}

class ReceptorInstalacion : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ruta = intent.getStringExtra(Instalador.EXTRA_ARCHIVO)
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmar?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity)
            }
            else -> ruta?.let { File(it).delete() }
        }
    }
}
