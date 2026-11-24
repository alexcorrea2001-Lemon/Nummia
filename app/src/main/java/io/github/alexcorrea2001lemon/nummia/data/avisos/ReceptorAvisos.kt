package io.github.alexcorrea2001lemon.nummia.data.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Botón «Confirmar» de las notificaciones de pago mensual y de sueldo: se confirma sin abrir la app. */
@AndroidEntryPoint
class ReceptorAvisos : BroadcastReceiver() {
    @Inject lateinit var repo: MovimientosRepo

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CONFIRMAR) return
        val id = intent.getStringExtra(ID) ?: return
        val importe = intent.getLongExtra(IMPORTE, 0)
        val tipo = intent.getStringExtra(TIPO)
        val notificacion = intent.getIntExtra(NOTIFICACION, 0)
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Si ya lo resolviste en la app (o ya no existe), el botón no cambia nada; nunca cierra la app.
                runCatching {
                    when (tipo) {
                        AccionAviso.Tipo.PAGO.name -> repo.confirmarPagoDesdeAviso(id, importe)
                        AccionAviso.Tipo.SUELDO.name -> repo.confirmarSueldoDesdeAviso(id, importe)
                    }
                }
                NotificationManagerCompat.from(context).cancel(notificacion)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val CONFIRMAR = "io.github.alexcorrea2001lemon.nummia.CONFIRMAR_AVISO"
        const val TIPO = "tipo"
        const val ID = "id"
        const val IMPORTE = "importe"
        const val NOTIFICACION = "notificacion"
    }
}
