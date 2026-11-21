package io.github.alexcorrea2001lemon.nummia.ui.inicio

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.DatosPeriodo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.aMov
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.data.repo.reembolsosMov
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.teDeben
import io.github.alexcorrea2001lemon.nummia.domain.calculo.variacionPorMil
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
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
data class TopTienda(val nombre: String, val importe: Long, val fraccion: Float)

@Immutable
data class SueldoPendiente(val id: String, val importe: Long, val fecha: LocalDate)

@Immutable
data class PagoPendiente(val id: String, val concepto: String, val importe: Long, val fecha: LocalDate)

@Immutable
data class InicioEstado(
    val mes: YearMonth,
    val resumen: Resumen = Resumen.VACIO,
    /** Variación del gasto real frente al mes anterior, ×10. */
    val variacion: Int? = null,
    val sueldosPendientes: List<SueldoPendiente> = emptyList(),
    val pagosPendientes: List<PagoPendiente> = emptyList(),
    val teDeben: List<Pair<String, Long>> = emptyList(),
    val top: List<TopTienda> = emptyList(),
    val ahorradoAnio: Long = 0,
    val ultimos: List<FilaMov> = emptyList(),
    val discreto: Boolean = false,
    val vacio: Boolean = true,
    val esMesActual: Boolean = true,
    val cargando: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val repo: MovimientosRepo,
    private val ajustesRepo: AjustesRepo,
    private val reloj: Reloj,
) : ViewModel() {
    private val mes = MutableStateFlow(YearMonth.from(reloj.hoy()))

    private val datos = mes.flatMapLatest { ym ->
        combine(
            repo.mes(ym),
            repo.mes(ym.minusMonths(1)),
            repo.periodo(LocalDate.of(ym.year, 1, 1), LocalDate.of(ym.year, 12, 31)),
        ) { actual, anterior, anio -> Triple(ym, actual, anterior) to anio }
    }

    val estado: StateFlow<InicioEstado> = combine(
        datos, repo.conDevolucion(), repo.personas(), repo.categorias(), ajustesRepo.ajustes,
    ) { (t, anio), conDev, personas, categorias, ajustes ->
        val (ym, actual, anterior) = t
        calcular(ym, actual, anterior, anio, conDev, personas.associate { it.id to it.nombre }, categorias.associate { it.id to it.nombre }, ajustes.modoDiscreto)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioEstado(mes.value))

    private fun calcular(
        ym: YearMonth,
        actual: DatosPeriodo,
        anterior: DatosPeriodo,
        anio: DatosPeriodo,
        conDev: List<io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos>,
        personas: Map<String, String>,
        categorias: Map<String, String>,
        discreto: Boolean,
    ): InicioEstado {
        val r = resumen(ym.atDay(1), ym.atEndOfMonth(), actual.movGastos, actual.movReembolsos, actual.movIngresos)
        val prev = YearMonth.from(ym.minusMonths(1))
        val rAnt = resumen(prev.atDay(1), prev.atEndOfMonth(), anterior.movGastos, anterior.movReembolsos, anterior.movIngresos)
        val hoy = reloj.hoy()
        val ahorrado = resumen(LocalDate.of(ym.year, 1, 1), LocalDate.of(ym.year, 12, 31), anio.movGastos, anio.movReembolsos, anio.movIngresos).balance

        val sueldos = anio.ingresos
            .filter { it.tipo == TipoIngreso.SUELDO && it.estado == Estado.PREVISTO }
            .filter { val m = YearMonth.from(it.fecha); m < YearMonth.from(hoy) || (m == YearMonth.from(hoy) && hoy.dayOfMonth >= 27) }
            .map { SueldoPendiente(it.id, it.importe, it.fecha) }

        // Pagos mensuales previstos: desde 3 días antes de su fecha.
        val pagos = anio.gastos.map { it.gasto }
            .filter { it.estado == Estado.PREVISTO && it.pagoMensualId != null && !it.fecha.isAfter(hoy.plusDays(3)) }
            .sortedBy { it.fecha }
            .map { PagoPendiente(it.id, it.concepto, it.importe, it.fecha) }

        val deben = teDeben(conDev.map { it.aMov() }, conDev.flatMap { it.reembolsosMov() }) { id ->
            conDev.firstOrNull { it.gasto.id == id }?.gasto?.personaId
        }.map { (pid, importe) -> (pid?.let(personas::get) ?: "Sin persona") to importe }.sortedByDescending { it.second }

        val confirmados = actual.gastos.filter { it.gasto.estado == Estado.CONFIRMADO }
        val porTienda = confirmados.groupBy { it.gasto.tienda.ifBlank { it.gasto.concepto } }
            .mapValues { (_, gs) -> gs.sumOf { it.real } }
            .filterValues { it > 0 }
            .entries.sortedByDescending { it.value }.take(3)
        val maximo = porTienda.firstOrNull()?.value ?: 1L
        val top = porTienda.map { TopTienda(it.key, it.value, (it.value.toFloat() / maximo).coerceIn(0.04f, 1f)) }

        val filas = (actual.gastos.map { it.aFila(personas, categorias) } + actual.ingresos.map { it.aFila() })
            .sortedWith(compareByDescending<FilaMov> { it.fecha }.thenByDescending { it.creado })
            .take(5)

        return InicioEstado(
            mes = ym,
            resumen = r,
            variacion = variacionPorMil(r.gastoReal, rAnt.gastoReal),
            sueldosPendientes = sueldos,
            pagosPendientes = pagos,
            teDeben = deben,
            top = top,
            ahorradoAnio = ahorrado,
            ultimos = filas,
            discreto = discreto,
            vacio = actual.gastos.isEmpty() && actual.ingresos.isEmpty(),
            esMesActual = ym == YearMonth.from(hoy),
            cargando = false,
        )
    }

    /** Presupuestos del mes elegido, con su nombre (vacío si no hay ninguno). */
    val presupuestos: StateFlow<List<Pair<String, io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto>>> =
        combine(mes.flatMapLatest { repo.estadoPresupuestos(it) }, repo.categorias()) { ps, cats ->
            val nombres = cats.associate { it.id to it.nombre }
            ps.map { io.github.alexcorrea2001lemon.nummia.ui.presupuestos.nombrePresupuesto(it, nombres) to it }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun mesAnterior() = mes.update { it.minusMonths(1) }
    fun mesSiguiente() = mes.update { it.plusMonths(1) }
    fun irAHoy() { mes.value = YearMonth.from(reloj.hoy()) }

    fun alternarDiscreto() = viewModelScope.launch { ajustesRepo.modoDiscreto(!estado.value.discreto) }

    fun confirmarPago(id: String, importe: Long) = viewModelScope.launch { repo.confirmarPago(id, importe) }
    fun omitirPago(id: String) = viewModelScope.launch { repo.omitirPago(id) }

    fun confirmarSueldo(id: String, fecha: LocalDate, importe: Long) = viewModelScope.launch {
        repo.confirmarIngreso(id, fecha, importe)
    }
}
