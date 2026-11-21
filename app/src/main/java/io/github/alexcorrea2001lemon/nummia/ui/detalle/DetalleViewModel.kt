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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

/** Gasto anterior (más reciente) y siguiente (más antiguo) del mismo mes, en el orden de Movimientos. */
@Immutable
data class Vecinos(val anterior: String? = null, val siguiente: String? = null, val posicion: Int = 0, val total: Int = 0)

fun vecinos(ordenados: List<String>, id: String): Vecinos {
    val i = ordenados.indexOf(id)
    if (i < 0) return Vecinos()
    return Vecinos(ordenados.getOrNull(i - 1), ordenados.getOrNull(i + 1), i + 1, ordenados.size)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    val vecinos: StateFlow<Vecinos> = repo.gasto(id)
        .map { it?.gasto?.fecha?.let(java.time.YearMonth::from) }
        .distinctUntilChanged()
        .flatMapLatest { ym -> if (ym == null) flowOf(null) else repo.mes(ym) }
        .map { datos ->
            val ids = datos?.gastos.orEmpty().map { it.gasto }
                .sortedWith(compareByDescending<io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity> { it.fecha }.thenByDescending { it.createdAt })
                .map { it.id }
            vecinos(ids, id)
        }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Vecinos())

    fun devueltoEntero() = viewModelScope.launch { repo.devueltoEntero(id) }
    fun borrarReembolso(rid: String) = viewModelScope.launch { repo.borrarReembolso(rid) }
    fun editarReembolso(rid: String, importe: Long, fecha: LocalDate?, personaId: String?, nota: String) =
        viewModelScope.launch { repo.editarReembolso(rid, importe, fecha, personaId, nota) }
    fun duplicar(alTerminar: (String) -> Unit) = viewModelScope.launch { alTerminar(repo.duplicarGasto(id)) }
    fun borrar(alTerminar: () -> Unit) = viewModelScope.launch { repo.borrarGasto(id); alTerminar() }
    fun deshacerBorrado() = viewModelScope.launch { repo.deshacerBorradoGasto(id) }
}
