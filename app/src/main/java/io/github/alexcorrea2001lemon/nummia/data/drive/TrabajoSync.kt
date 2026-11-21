package io.github.alexcorrea2001lemon.nummia.data.drive

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Sincronización en segundo plano, siempre con WorkManager (nada de servicios permanentes):
 * cada 12 h, al abrir la app y 30 s después de un cambio (los cambios seguidos se agrupan en una sola subida).
 */
@HiltWorker
class TrabajoSync @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sincronizador: Sincronizador,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val r = sincronizador.sincronizar()) {
        is ResultadoSync.Fallo -> if (r.reintentar && runAttemptCount < 5) Result.retry() else Result.success()
        else -> Result.success()
    }

    companion object {
        private const val PERIODICO = "drive_periodico"
        private const val PRONTO = "drive_pronto"

        private fun condiciones(soloWifi: Boolean) = Constraints.Builder()
            .setRequiredNetworkType(if (soloWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun programar(context: Context, conectado: Boolean, soloWifi: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!conectado) { wm.cancelUniqueWork(PERIODICO); wm.cancelUniqueWork(PRONTO); return }
            val p = PeriodicWorkRequestBuilder<TrabajoSync>(12, TimeUnit.HOURS)
                .setConstraints(condiciones(soloWifi))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(PERIODICO, ExistingPeriodicWorkPolicy.UPDATE, p)
        }

        /** Sincroniza dentro de [segundos]; si llega otro cambio antes, se vuelve a esperar (se agrupan). */
        fun pronto(context: Context, soloWifi: Boolean, segundos: Long = 30) {
            val p = OneTimeWorkRequestBuilder<TrabajoSync>()
                .setInitialDelay(segundos, TimeUnit.SECONDS)
                .setConstraints(condiciones(soloWifi))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(PRONTO, ExistingWorkPolicy.REPLACE, p)
        }
    }
}
