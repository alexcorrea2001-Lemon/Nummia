package io.github.alexcorrea2001lemon.nummia.ui.detalle

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DetalleIngresoEstado(
    val ingreso: IngresoEntity? = null,
    val historial: List<HistorialEntity> = emptyList(),
    val discreto: Boolean = false,
    val cargando: Boolean = true,
)

@HiltViewModel
class DetalleIngresoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repo: MovimientosRepo,
    ajustesRepo: AjustesRepo,
) : ViewModel() {
    val id: String = checkNotNull(estadoGuardado["id"])

    val estado: StateFlow<DetalleIngresoEstado> = combine(
        repo.ingresoObservado(id), repo.historial(id), ajustesRepo.ajustes,
    ) { i, h, a -> DetalleIngresoEstado(i?.takeIf { it.deletedAt == null }, h, a.modoDiscreto, false) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleIngresoEstado())

    fun borrar(alTerminar: () -> Unit) = viewModelScope.launch { repo.borrarIngreso(id); alTerminar() }
    fun deshacerBorrado() = viewModelScope.launch { repo.deshacerBorradoIngreso(id) }
}
