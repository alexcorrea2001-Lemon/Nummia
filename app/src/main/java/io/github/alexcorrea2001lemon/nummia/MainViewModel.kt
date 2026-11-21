package io.github.alexcorrea2001lemon.nummia

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.data.actualizar.Actualizador
import io.github.alexcorrea2001lemon.nummia.data.actualizar.TrabajoActualizaciones
import io.github.alexcorrea2001lemon.nummia.data.ajustes.Ajustes
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import io.github.alexcorrea2001lemon.nummia.data.actualizar.esMasNueva
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val ajustesRepo: AjustesRepo,
    private val repo: MovimientosRepo,
    private val reloj: Reloj,
    actualizador: Actualizador,
) : ViewModel() {
    /** `null` mientras se leen los ajustes (la pantalla de inicio de Android sigue visible). */
    val ajustes: StateFlow<Ajustes?> = ajustesRepo.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.prepararDatos()
            actualizador.limpiar()
            // Si ya está instalada la versión que se anunciaba (o una posterior), se quita el aviso.
            val anunciada = ajustesRepo.ajustes.first().versionDisponible
            if (anunciada != null && !esMasNueva(anunciada, BuildConfig.VERSION_NAME)) ajustesRepo.versionDisponible(null)
        }
        viewModelScope.launch {
            ajustesRepo.ajustes.map { it.sueldoHabitual }.distinctUntilChanged().collect { repo.asegurarSueldoPrevisto(reloj.hoy(), it) }
        }
        viewModelScope.launch {
            ajustesRepo.ajustes.map { it.buscarActualizaciones }.distinctUntilChanged().collect {
                TrabajoActualizaciones.programar(context, it && !BuildConfig.DEBUG)
            }
        }
    }

    fun terminarBienvenida(sueldo: Long?) = viewModelScope.launch {
        if (sueldo != null) ajustesRepo.sueldoHabitual(sueldo)
        ajustesRepo.bienvenidaVista()
    }
}
