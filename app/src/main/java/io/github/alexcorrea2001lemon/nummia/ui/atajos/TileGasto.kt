package io.github.alexcorrea2001lemon.nummia.ui.atajos

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.alexcorrea2001lemon.nummia.MainActivity

/**
 * Botón de los Ajustes rápidos: «Nuevo gasto» abre el formulario directamente.
 * No hace nada en segundo plano: solo existe mientras el panel está abierto.
 */
class TileGasto : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; updateTile() }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACCION_GASTO)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val abrir = {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
        // Con el móvil bloqueado, primero se pide desbloquearlo.
        if (isLocked) unlockAndRun { abrir() } else abrir()
    }
}
