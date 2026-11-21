package io.github.alexcorrea2001lemon.nummia.data.actualizar

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import java.util.concurrent.TimeUnit

/** Comprobación diaria y silenciosa: solo guarda si hay versión nueva; la app lo muestra al abrirse. */
@HiltWorker
class TrabajoActualizaciones @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val actualizador: Actualizador,
    private val ajustes: AjustesRepo,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        actualizador.limpiar()
        when (val e = actualizador.comprobar()) {
            is EstadoActualizacion.Disponible -> ajustes.versionDisponible(e.lanzamiento.etiqueta)
            EstadoActualizacion.AlDia -> ajustes.versionDisponible(null)
            is EstadoActualizacion.NoSePudoComprobar -> Unit
        }
        return Result.success()
    }

    companion object {
        private const val NOMBRE = "comprobar_actualizaciones"

        fun programar(context: Context, activo: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!activo) { wm.cancelUniqueWork(NOMBRE); return }
            val peticion = PeriodicWorkRequestBuilder<TrabajoActualizaciones>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .build()
            wm.enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, peticion)
        }
    }
}
