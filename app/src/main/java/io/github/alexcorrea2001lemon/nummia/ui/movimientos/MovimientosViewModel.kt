package io.github.alexcorrea2001lemon.nummia.ui.movimientos

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.EtiquetaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.aFila
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@Immutable
data class MovimientosEstado(
    val mes: YearMonth,
    val filtros: Filtros = Filtros(),
    val grupos: List<GrupoDia> = emptyList(),
    val cuenta: Int = 0,
    /** Gasto real de lo filtrado (los ingresos no suman aquí). */
    val totalGasto: Long = 0,
    val buscandoEnTodo: Boolean = false,
    val discreto: Boolean = false,
    val hoy: LocalDate,
    val cargando: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovimientosViewModel @Inject constructor(
    private val repo: MovimientosRepo,
    ajustesRepo: AjustesRepo,
    private val reloj: Reloj,
) : ViewModel() {
    private val mes = MutableStateFlow(YearMonth.from(reloj.hoy()))
    private val filtros = MutableStateFlow(Filtros())

    private val fuente = combine(mes, filtros) { m, f -> m to f }.flatMapLatest { (m, f) ->
        // Al buscar texto o mirar pendientes, se busca en todos los meses.
        if (f.texto.isNotBlank() || f.pestana == Pestana.PENDIENTES) repo.todo() else repo.mes(m)
    }

    val estado: StateFlow<MovimientosEstado> = combine(
        fuente, filtros, mes, repo.personas(), repo.categorias(),
    ) { datos, f, m, personas, categorias ->
        val pm = personas.associate { it.id to it.nombre }
        val cm = categorias.associate { it.id to it.nombre }
        val filas = datos.gastos.map { it.aFila(pm, cm) } + datos.ingresos.map { it.aFila() }
        val filtradas = aplicar(filas, f)
        MovimientosEstado(
            mes = m, filtros = f, grupos = agrupar(filtradas, f.orden), cuenta = filtradas.size,
            totalGasto = filtradas.filter { it.esGasto && it.etiqueta != EtiquetaMov.Previsto }.sumOf { it.importe },
            buscandoEnTodo = f.texto.isNotBlank() || f.pestana == Pestana.PENDIENTES,
            hoy = reloj.hoy(), cargando = false,
        )
    }.combine(ajustesRepo.ajustes) { e, a -> e.copy(discreto = a.modoDiscreto) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimientosEstado(mes.value, hoy = reloj.hoy()))

    fun mesAnterior() = mes.update { it.minusMonths(1) }
    fun mesSiguiente() = mes.update { it.plusMonths(1) }
    fun irAMes(ym: YearMonth) { mes.value = ym }
    fun hoyMes(): YearMonth = YearMonth.from(reloj.hoy())
    fun pestana(p: Pestana) = filtros.update { it.copy(pestana = p) }
    fun texto(t: String) = filtros.update { it.copy(texto = t) }
    fun orden(o: Orden) = filtros.update { it.copy(orden = o) }
    fun avanzados(conReembolso: Boolean?, minimo: Long?, maximo: Long?) =
        filtros.update { it.copy(conReembolso = conReembolso, minimo = minimo, maximo = maximo) }
    fun limpiarAvanzados() = avanzados(null, null, null)

    fun borrar(id: String, esGasto: Boolean) = viewModelScope.launch {
        if (esGasto) repo.borrarGasto(id) else repo.borrarIngreso(id)
    }

    fun deshacer(id: String, esGasto: Boolean) = viewModelScope.launch {
        if (esGasto) repo.deshacerBorradoGasto(id) else repo.deshacerBorradoIngreso(id)
    }

    fun repetirHoy(id: String) = viewModelScope.launch { repo.repetirHoy(id) }
}
