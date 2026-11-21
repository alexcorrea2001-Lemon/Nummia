package io.github.alexcorrea2001lemon.nummia

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.icono.IconoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NummiaApp : Application(), Configuration.Provider {
    @Inject lateinit var fabricaTrabajos: HiltWorkerFactory
    @Inject lateinit var ajustes: AjustesRepo

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaTrabajos).build()

    override fun onCreate() {
        super.onCreate()
        // El icono elegido se aplica cuando la app pasa a segundo plano: así el lanzador lo recoge
        // en cuanto vuelves a la pantalla de inicio y la app no se cierra en mitad de un ajuste.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                alcance.launch {
                    val a = ajustes.ajustes.first()
                    runCatching { IconoApp.aplicarSiCambia(this@NummiaApp, a.iconoLogo, a.iconoColor) }
                }
            }
        })
    }
}
