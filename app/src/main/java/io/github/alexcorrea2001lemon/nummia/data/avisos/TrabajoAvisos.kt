package io.github.alexcorrea2001lemon.nummia.data.avisos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.alexcorrea2001lemon.nummia.MainActivity
import io.github.alexcorrea2001lemon.nummia.R
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.presupuestos.nombrePresupuesto
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/**
 * Revisa una vez al día (hacia las 10:00) y poco después de cada cambio si hay algo que avisar.
 * Sin red ni servicios permanentes: solo lee la base de datos del móvil.
 */
@HiltWorker
class TrabajoAvisos @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val ajustes: AjustesRepo,
    private val repo: MovimientosRepo,
    private val db: NummiaDb,
    private val reloj: Reloj,
    private val sinc: io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val a = ajustes.ajustes.first()
        if (!a.notificaciones || !puedeNotificar(context)) return Result.success()
        val hoy = reloj.hoy()
        val ym = YearMonth.from(hoy)
        val previsto = db.ingresos().volcar().firstOrNull {
            it.deletedAt == null && it.tipo == TipoIngreso.SUELDO && it.estado == Estado.PREVISTO && YearMonth.from(it.fecha) == ym
        }
        val sueldo = previsto != null
        val pagos = db.gastos().volcar()
            .filter { it.deletedAt == null && it.estado == Estado.PREVISTO && it.pagoMensualId != null && YearMonth.from(it.fecha) == ym }
            .map { PagoDelDia(it.id, it.concepto, it.importe, it.fecha) }
        val categorias = db.categorias().volcar().associate { it.id to it.nombre }
        val presupuestos = repo.estadoPresupuestosAhora(ym).map { nombrePresupuesto(it, categorias) to it }
        val hechos = ajustes.avisosHechos()
        val primero = db.gastos().volcar().filter { it.deletedAt == null }.minOfOrNull { it.fecha }
        val copia = avisoCopia(hoy, sinc.correo() != null, ajustes.ultimaCopiaLocal()?.let(java.time.LocalDate::ofEpochDay), primero)
            ?.takeIf { it.clave !in hechos }
        val nuevos = avisosPendientes(hoy, sueldo, pagos, presupuestos, hechos, PreferenciasAvisos(a.avisoSueldo, a.avisoPagos, a.avisoPresupuestos), previsto?.id, previsto?.importe ?: 0) +
            listOfNotNull(copia)
        nuevos.forEach { mostrar(context, it) }
        if (nuevos.isNotEmpty()) ajustes.anotarAvisos(nuevos.map { it.clave })
        return Result.success()
    }

    companion object {
        private const val CANAL = "avisos"
        private const val DIARIO = "avisos_diario"
        private const val PRONTO = "avisos_pronto"

        fun puedeNotificar(context: Context): Boolean =
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()

        private fun crearCanal(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CANAL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CANAL, "Avisos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Sueldo, pagos del día y presupuestos"
                    },
                )
            }
        }

        private fun mostrar(context: Context, a: Aviso) {
            crearCanal(context)
            val abrir = PendingIntent.getActivity(
                context, if (a.abrir != null) a.clave.hashCode() else 0,
                Intent(context, MainActivity::class.java).setAction(a.abrir ?: Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val b = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_app_a_mono)
                .setContentTitle(a.titulo)
                .setContentText(a.texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(a.texto))
                .setContentIntent(abrir)
                .setAutoCancel(true)
            // Botones: confirmar sin abrir la app, o abrirla para poner otro importe.
            a.accion?.let { ac ->
                val confirmar = PendingIntent.getBroadcast(
                    context, a.clave.hashCode(),
                    Intent(context, ReceptorAvisos::class.java).setAction(ReceptorAvisos.CONFIRMAR)
                        .putExtra(ReceptorAvisos.TIPO, ac.tipo.name).putExtra(ReceptorAvisos.ID, ac.id)
                        .putExtra(ReceptorAvisos.IMPORTE, ac.importe).putExtra(ReceptorAvisos.NOTIFICACION, a.clave.hashCode()),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                b.addAction(0, "Confirmar", confirmar)
                b.addAction(0, "Otro importe", abrir)
            }
            val n = b.build()
            try {
                NotificationManagerCompat.from(context).notify(a.clave.hashCode(), n)
            } catch (_: SecurityException) {
                // Permiso retirado justo ahora: no se muestra.
            }
        }

        /** Revisión diaria hacia las 10:00. */
        fun programar(context: Context, activo: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!activo) { wm.cancelUniqueWork(DIARIO); wm.cancelUniqueWork(PRONTO); return }
            val ahora = LocalDateTime.now()
            var siguiente = ahora.toLocalDate().atTime(10, 0)
            if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
            val p = PeriodicWorkRequestBuilder<TrabajoAvisos>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(ahora, siguiente).toMinutes(), TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(DIARIO, ExistingPeriodicWorkPolicy.UPDATE, p)
        }

        /** Tras un cambio (p. ej. un gasto que hace pasar un presupuesto). Los cambios seguidos se agrupan. */
        fun pronto(context: Context) {
            val p = OneTimeWorkRequestBuilder<TrabajoAvisos>().setInitialDelay(15, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(PRONTO, ExistingWorkPolicy.REPLACE, p)
        }
    }
}
