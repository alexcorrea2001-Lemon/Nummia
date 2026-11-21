package io.github.alexcorrea2001lemon.nummia.ui.estadisticas

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.DatosPeriodo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.aMov
import io.github.alexcorrea2001lemon.nummia.data.repo.reembolsoTardio
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Comparacion
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GastoEtiquetado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.ResumenAnual
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.acumuladoDiario
import io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.diaMasGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.porCategoria
import io.github.alexcorrea2001lemon.nummia.domain.calculo.porTienda
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumenAnual
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.aFila
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class PestanaEst(val nombre: String) { MES("Mes"), ANIO("Año"), COMPARAR("Comparar"), DONDE("Dónde gasto") }
enum class Periodo(val nombre: String) { MES("Este mes"), ANIO("Este año"), SIEMPRE("Desde siempre") }

data class Seleccion(
    val pestana: PestanaEst = PestanaEst.MES,
    val mes: YearMonth,
    val anio: Int,
    val anioA: Int,
    val anioB: Int,
    val periodo: Periodo = Periodo.MES,
    val porCategoria: Boolean = false,
    val tiendaAbierta: String? = null,
)

@Immutable
data class EstadoMes(
    val resumen: Resumen = Resumen.VACIO,
    val sueldo: Long = 0,
    val otros: Long = 0,
    val acumulado: List<Long> = emptyList(),
    val acumuladoAnterior: List<Long> = emptyList(),
    val reparto: List<GrupoGasto> = emptyList(),
    val diaMas: Pair<LocalDate, Long>? = null,
    val tardio: LocalDate? = null,
    val vacio: Boolean = true,
)

@Immutable
data class EstadoEst(
    val sel: Seleccion,
    val mes: EstadoMes = EstadoMes(),
    val anual: ResumenAnual? = null,
    val ahorroAcumulado: List<Long> = emptyList(),
    val comparacion: Comparacion? = null,
    val ranking: List<GrupoGasto> = emptyList(),
    val historialTienda: List<FilaMov> = emptyList(),
    val anios: List<Int> = emptyList(),
    val discreto: Boolean = false,
    val cargando: Boolean = true,
)

@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    repo: MovimientosRepo,
    ajustesRepo: AjustesRepo,
    private val reloj: Reloj,
) : ViewModel() {
    private val hoy get() = reloj.hoy()
    private val sel = MutableStateFlow(
        Seleccion(mes = YearMonth.from(hoy), anio = hoy.year, anioA = hoy.year - 1, anioB = hoy.year),
    )

    val estado: StateFlow<EstadoEst> = combine(
        sel, repo.todo(), repo.personas(), repo.categorias(), ajustesRepo.ajustes,
    ) { s, datos, personas, categorias, ajustes ->
        calcular(s, datos, personas.associate { it.id to it.nombre }, categorias.associate { it.id to it.nombre }).copy(discreto = ajustes.modoDiscreto)
    }.flowOn(Dispatchers.Default)
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoEst(sel.value))

    private fun calcular(s: Seleccion, d: DatosPeriodo, personas: Map<String, String>, categorias: Map<String, String>): EstadoEst {
        val gastos = d.movGastos
        val reembolsos = d.movReembolsos
        val ingresos = d.movIngresos
        val nombreCategoria: (String?) -> String = { it?.let(categorias::get) ?: "Sin categoría" }

        // --- Mes
        val ym = s.mes
        val r = resumen(ym.atDay(1), ym.atEndOfMonth(), gastos, reembolsos, ingresos)
        val delMes = d.gastos.filter { YearMonth.from(it.gasto.fecha) == ym }
        val etiquetadosMes = delMes.map { GastoEtiquetado(it.aMov(), it.gasto.tienda.ifBlank { it.gasto.concepto }, it.gasto.categoriaId) }
        val ingresosMes = d.ingresos.filter { YearMonth.from(it.fecha) == ym && it.estado == Estado.CONFIRMADO }
        val mes = EstadoMes(
            resumen = r,
            sueldo = ingresosMes.filter { it.tipo == TipoIngreso.SUELDO }.sumOf { it.importe },
            otros = ingresosMes.filter { it.tipo != TipoIngreso.SUELDO }.sumOf { it.importe },
            acumulado = acumuladoDiario(ym, gastos, reembolsos),
            acumuladoAnterior = acumuladoDiario(ym.minusMonths(1), gastos, reembolsos),
            reparto = if (s.porCategoria) porCategoria(etiquetadosMes, reembolsos, nombreCategoria) else porTienda(etiquetadosMes, reembolsos),
            diaMas = diaMasGasto(delMes.map { it.aMov() }, reembolsos),
            tardio = delMes.mapNotNull { it.reembolsoTardio }.maxOrNull(),
            vacio = delMes.none { it.gasto.estado == Estado.CONFIRMADO } && ingresosMes.isEmpty(),
        )

        // --- Año
        val anual = resumenAnual(s.anio, gastos, reembolsos, ingresos)
        var suma = 0L
        val ahorro = anual.meses.map { suma += it.balance; suma }

        // --- Comparar
        val comparacion = Comparacion(resumenAnual(s.anioA, gastos, reembolsos, ingresos), resumenAnual(s.anioB, gastos, reembolsos, ingresos))

        // --- Dónde gasto
        val hoyYm = YearMonth.from(hoy)
        val enPeriodo = d.gastos.filter {
            when (s.periodo) {
                Periodo.MES -> YearMonth.from(it.gasto.fecha) == hoyYm
                Periodo.ANIO -> it.gasto.fecha.year == hoy.year
                Periodo.SIEMPRE -> true
            }
        }
        val ranking = porTienda(enPeriodo.map { GastoEtiquetado(it.aMov(), it.gasto.tienda.ifBlank { it.gasto.concepto }, it.gasto.categoriaId) }, reembolsos)
        val historial = s.tiendaAbierta?.let { t ->
            val clave = claveTexto(t)
            enPeriodo.filter { claveTexto(it.gasto.tienda.ifBlank { it.gasto.concepto }) == clave }
                .map { it.aFila(personas, categorias) }
                .sortedWith(compareByDescending<FilaMov> { it.fecha }.thenByDescending { it.creado })
        }.orEmpty()

        val anios = (d.gastos.map { it.gasto.fecha.year } + d.ingresos.map { it.fecha.year } + hoy.year + s.anioA + s.anioB).distinct().sorted()

        return EstadoEst(s, mes, anual, ahorro, comparacion, ranking, historial, anios, cargando = false)
    }

    fun pestana(p: PestanaEst) = sel.update { it.copy(pestana = p) }

    /** Llega desde Inicio: pestaña y mes ya elegidos. */
    fun ir(p: PestanaEst, ym: YearMonth) = sel.update {
        it.copy(pestana = p, mes = ym, anio = ym.year, periodo = if (p == PestanaEst.DONDE) Periodo.MES else it.periodo, tiendaAbierta = null)
    }
    fun mesAnterior() = sel.update { it.copy(mes = it.mes.minusMonths(1)) }
    fun mesSiguiente() = sel.update { it.copy(mes = it.mes.plusMonths(1)) }
    fun anioAnterior() = sel.update { it.copy(anio = it.anio - 1) }
    fun anioSiguiente() = sel.update { it.copy(anio = it.anio + 1) }
    /** Al tocar un mes en la pestaña Año se abre ese mes en la pestaña Mes. */
    fun verMes(numero: Int) = sel.update { it.copy(mes = YearMonth.of(it.anio, numero), pestana = PestanaEst.MES) }
    fun anioA(a: Int) = sel.update { it.copy(anioA = a) }
    fun anioB(b: Int) = sel.update { it.copy(anioB = b) }
    fun periodo(p: Periodo) = sel.update { it.copy(periodo = p, tiendaAbierta = null) }
    fun agruparPorCategoria(v: Boolean) = sel.update { it.copy(porCategoria = v) }
    fun abrirTienda(nombre: String?) = sel.update { it.copy(tiendaAbierta = nombre) }
}
