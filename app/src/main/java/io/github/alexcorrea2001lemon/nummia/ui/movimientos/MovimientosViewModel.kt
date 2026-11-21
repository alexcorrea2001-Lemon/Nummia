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
import kotlinx.coroutines.flow.map
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

/** Buscar texto, mirar pendientes o filtrar por tienda o persona busca en todos los meses. */
private fun enTodo(f: Filtros) = f.texto.isNotBlank() || f.pestana == Pestana.PENDIENTES || f.tienda != null || f.persona != null

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
        if (enTodo(f)) repo.todo() else repo.mes(m)
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
            buscandoEnTodo = enTodo(f),
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
    fun avanzados(n: Filtros) = filtros.update {
        it.copy(conReembolso = n.conReembolso, minimo = n.minimo, maximo = n.maximo, categoria = n.categoria, persona = n.persona, tienda = n.tienda)
    }
    fun limpiarAvanzados() = avanzados(Filtros())
    fun quitar(categoria: Boolean = false, tienda: Boolean = false, persona: Boolean = false) = filtros.update {
        it.copy(
            categoria = if (categoria) null else it.categoria,
            tienda = if (tienda) null else it.tienda,
            persona = if (persona) null else it.persona,
        )
    }

    /** Opciones para los filtros: personas y las tiendas más usadas. */
    val personas = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tiendas = repo.tiendas().map { ts -> ts.sortedByDescending { it.veces }.take(20).map { it.nombre }.filter { it.isNotBlank() } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun borrar(id: String, esGasto: Boolean) = viewModelScope.launch {
        if (esGasto) repo.borrarGasto(id) else repo.borrarIngreso(id)
    }

    fun deshacer(id: String, esGasto: Boolean) = viewModelScope.launch {
        if (esGasto) repo.deshacerBorradoGasto(id) else repo.deshacerBorradoIngreso(id)
    }

    fun repetirHoy(id: String) = viewModelScope.launch { repo.repetirHoy(id) }

    // ---------- Selección múltiple (mantener pulsado) ----------

    private val _seleccion = MutableStateFlow<Set<String>>(emptySet())
    /** Claves «g:id» (gastos) e «i:id» (ingresos). */
    val seleccion: StateFlow<Set<String>> = _seleccion

    fun alternar(f: io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov) {
        val k = (if (f.esGasto) "g:" else "i:") + f.id
        _seleccion.update { if (k in it) it - k else it + k }
    }

    fun limpiarSeleccion() { _seleccion.value = emptySet() }

    private fun gastosSel() = _seleccion.value.filter { it.startsWith("g:") }.map { it.drop(2) }
    private fun ingresosSel() = _seleccion.value.filter { it.startsWith("i:") }.map { it.drop(2) }
    val categorias = repo.categorias().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Borra lo elegido y devuelve con qué deshacerlo. */
    fun borrarSeleccion(): Pair<List<String>, List<String>> {
        val g = gastosSel(); val i = ingresosSel()
        viewModelScope.launch { repo.borrarVarios(g, i) }
        limpiarSeleccion()
        return g to i
    }

    fun deshacerVarios(g: List<String>, i: List<String>) = viewModelScope.launch { repo.deshacerVarios(g, i) }

    fun categoriaSeleccion(categoria: String?) {
        val g = gastosSel()
        viewModelScope.launch { repo.cambiarCategoria(g, categoria) }
        limpiarSeleccion()
    }

    fun tiendaSeleccion(tienda: String) {
        val g = gastosSel()
        if (tienda.isBlank()) return
        viewModelScope.launch { repo.cambiarTienda(g, tienda) }
        limpiarSeleccion()
    }
}
