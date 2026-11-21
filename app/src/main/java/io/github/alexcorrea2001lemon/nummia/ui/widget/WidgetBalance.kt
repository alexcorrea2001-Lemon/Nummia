package io.github.alexcorrea2001lemon.nummia.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexcorrea2001lemon.nummia.MainActivity
import io.github.alexcorrea2001lemon.nummia.R
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Widget con el balance del mes y «+ Gasto». Se actualiza al cambiar los datos, nunca por su cuenta. */
@AndroidEntryPoint
class WidgetBalance : AppWidgetProvider() {
    @Inject lateinit var repo: MovimientosRepo
    @Inject lateinit var ajustes: AjustesRepo
    @Inject lateinit var reloj: Reloj

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // Hilt inyecta aquí
        if (intent.action == ACTUALIZAR) actualizarTodos(context)
    }

    override fun onUpdate(context: Context, gestor: AppWidgetManager, ids: IntArray) = actualizarTodos(context)

    private fun actualizarTodos(context: Context) {
        val gestor = AppWidgetManager.getInstance(context)
        val ids = gestor.getAppWidgetIds(ComponentName(context, WidgetBalance::class.java))
        if (ids.isEmpty()) return
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ym = YearMonth.from(reloj.hoy())
                val r = repo.resumenMes(ym).first()
                val discreto = ajustes.ajustes.first().modoDiscreto
                val vista = RemoteViews(context.packageName, R.layout.widget_balance).apply {
                    setTextViewText(R.id.widget_mes, "Balance de ${nombreMes(ym).substringBefore(' ').lowercase()}")
                    setTextViewText(R.id.widget_balance, formatearEuros(r.balance, conSigno = true, discreto = discreto))
                    setTextColor(R.id.widget_balance, if (r.balance >= 0) 0xFF4ADE80.toInt() else 0xFFF87171.toInt())
                    setTextViewText(
                        R.id.widget_detalle,
                        "Gastado ${formatearEuros(r.gastoReal, discreto = discreto)} · Ingresos ${formatearEuros(r.ingresos, discreto = discreto)}",
                    )
                    setOnClickPendingIntent(R.id.widget_raiz, abrir(context, null, 0))
                    setOnClickPendingIntent(R.id.widget_anadir, abrir(context, MainActivity.ACCION_GASTO, 1))
                }
                gestor.updateAppWidget(ids, vista)
            } finally {
                pendiente.finish()
            }
        }
    }

    private fun abrir(context: Context, accion: String?, codigo: Int): PendingIntent = PendingIntent.getActivity(
        context, codigo,
        Intent(context, MainActivity::class.java).setAction(accion ?: Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val ACTUALIZAR = "io.github.alexcorrea2001lemon.nummia.ACTUALIZAR_WIDGET"

        /** Pide al widget (si lo tienes puesto) que se redibuje. Barato: si no hay widgets, no hace nada. */
        fun actualizar(context: Context) {
            context.sendBroadcast(Intent(context, WidgetBalance::class.java).setAction(ACTUALIZAR))
        }
    }
}
