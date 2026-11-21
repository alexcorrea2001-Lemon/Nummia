package io.github.alexcorrea2001lemon.nummia.ui.detalle

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.devuelto
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.data.repo.reembolsoTardio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@Immutable
data class DetalleEstado(
    val gasto: GastoConReembolsos? = null,
    val historial: List<HistorialEntity> = emptyList(),
    val personas: Map<String, String> = emptyMap(),
    val categorias: Map<String, String> = emptyMap(),
    val discreto: Boolean = false,
    val cargando: Boolean = true,
) {
    val real get() = gasto?.real ?: 0
    val devuelto get() = gasto?.devuelto ?: 0
    val falta get() = gasto?.falta ?: 0
    val tardio: LocalDate? get() = gasto?.reembolsoTardio
}

@HiltViewModel
class DetalleViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repo: MovimientosRepo,
    ajustesRepo: AjustesRepo,
) : ViewModel() {
    val id: String = checkNotNull(estadoGuardado["id"])

    val estado: StateFlow<DetalleEstado> = combine(
        repo.gasto(id), repo.historial(id), repo.personas(), repo.categorias(), ajustesRepo.ajustes,
    ) { g, h, p, c, a ->
        DetalleEstado(g, h, p.associate { it.id to it.nombre }, c.associate { it.id to it.nombre }, a.modoDiscreto, false)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleEstado())

    fun devueltoEntero() = viewModelScope.launch { repo.devueltoEntero(id) }
    fun borrarReembolso(rid: String) = viewModelScope.launch { repo.borrarReembolso(rid) }
    fun editarReembolso(rid: String, importe: Long, fecha: LocalDate?, personaId: String?, nota: String) =
        viewModelScope.launch { repo.editarReembolso(rid, importe, fecha, personaId, nota) }
    fun duplicar(alTerminar: (String) -> Unit) = viewModelScope.launch { alTerminar(repo.duplicarGasto(id)) }
    fun borrar(alTerminar: () -> Unit) = viewModelScope.launch { repo.borrarGasto(id); alTerminar() }
    fun deshacerBorrado() = viewModelScope.launch { repo.deshacerBorradoGasto(id) }
}
