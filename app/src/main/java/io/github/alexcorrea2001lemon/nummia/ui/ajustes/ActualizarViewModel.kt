package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.BuildConfig
import io.github.alexcorrea2001lemon.nummia.data.actualizar.Actualizador
import io.github.alexcorrea2001lemon.nummia.data.actualizar.EstadoActualizacion
import io.github.alexcorrea2001lemon.nummia.data.actualizar.Instalador
import io.github.alexcorrea2001lemon.nummia.data.actualizar.Lanzamiento
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface EstadoPantallaActualizar {
    data object Inicial : EstadoPantallaActualizar
    data object Comprobando : EstadoPantallaActualizar
    data object AlDia : EstadoPantallaActualizar
    data class Disponible(val etiqueta: String, val notas: String) : EstadoPantallaActualizar
    data class Descargando(val progreso: Float) : EstadoPantallaActualizar
    data object NecesitaPermiso : EstadoPantallaActualizar
    data object Instalando : EstadoPantallaActualizar
    data class Error(val mensaje: String) : EstadoPantallaActualizar
}

@HiltViewModel
class ActualizarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val actualizador: Actualizador,
    private val ajustes: AjustesRepo,
) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoPantallaActualizar>(EstadoPantallaActualizar.Inicial)
    val estado = _estado.asStateFlow()
    private var lanzamiento: Lanzamiento? = null

    fun comprobar() = viewModelScope.launch {
        _estado.value = EstadoPantallaActualizar.Comprobando
        _estado.value = when (val e = actualizador.comprobar()) {
            EstadoActualizacion.AlDia -> { ajustes.versionDisponible(null); EstadoPantallaActualizar.AlDia }
            is EstadoActualizacion.NoSePudoComprobar -> EstadoPantallaActualizar.Error(e.motivo)
            is EstadoActualizacion.Disponible -> {
                lanzamiento = e.lanzamiento
                ajustes.versionDisponible(e.lanzamiento.etiqueta)
                EstadoPantallaActualizar.Disponible(e.lanzamiento.etiqueta, e.lanzamiento.notas)
            }
        }
    }

    fun abrirPermiso() = Instalador.abrirPermiso(context)

    fun actualizar() = viewModelScope.launch {
        val l = lanzamiento ?: return@launch
        if (BuildConfig.DEBUG) {
            _estado.value = EstadoPantallaActualizar.Error("La versión de pruebas no se puede actualizar sola. Instala la versión publicada.")
            return@launch
        }
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (ajustes.ajustes.first().actualizacionesSoloWifi && cm.isActiveNetworkMetered) {
            _estado.value = EstadoPantallaActualizar.Error("Conéctate a una red Wi-Fi o desactiva «Descargar solo con Wi-Fi».")
            return@launch
        }
        if (!Instalador.puedeInstalar(context)) {
            _estado.value = EstadoPantallaActualizar.NecesitaPermiso
            Instalador.abrirPermiso(context)
            return@launch
        }
        _estado.value = EstadoPantallaActualizar.Descargando(0f)
        actualizador.descargar(l) { p -> _estado.value = EstadoPantallaActualizar.Descargando(p) }.fold(
            onSuccess = { apk ->
                val firmaOk = withContext(Dispatchers.IO) { Instalador.mismaFirma(context, apk) }
                if (!firmaOk) {
                    apk.delete()
                    _estado.value = EstadoPantallaActualizar.Error("La actualización no está firmada por Nummia. No se instalará.")
                } else {
                    _estado.value = EstadoPantallaActualizar.Instalando
                    runCatching { withContext(Dispatchers.IO) { Instalador.instalar(context, apk) } }.onFailure {
                        apk.delete()
                        _estado.value = EstadoPantallaActualizar.Error("No se pudo instalar la actualización.")
                    }
                }
            },
            onFailure = { _estado.value = EstadoPantallaActualizar.Error(it.message ?: "No se pudo descargar.") },
        )
    }
}
