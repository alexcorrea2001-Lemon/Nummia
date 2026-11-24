package io.github.alexcorrea2001lemon.nummia.ui.calendario

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.DiaCalendario
import io.github.alexcorrea2001lemon.nummia.domain.calculo.porDia
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToLong

/** Semanas (de lunes a domingo) que cubren el mes. */
fun semanasDelMes(ym: YearMonth): List<List<LocalDate>> {
    val inicio = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val fin = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return generateSequence(inicio) { it.plusDays(1) }.takeWhile { !it.isAfter(fin) }.toList().chunked(7)
}

/** Rejilla fija de 6 semanas desde el lunes de la semana del día 1 (todos los meses miden lo mismo). */
fun seisSemanas(ym: YearMonth): List<List<LocalDate>> {
    val inicio = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 42).map { inicio.plusDays(it.toLong()) }.chunked(7)
}

/** Importe corto para una celda: «12», «1.520», «12k». Sin signo. */
fun compacto(centimos: Long): String {
    val euros = (abs(centimos) / 100.0).roundToLong()
    return when {
        euros >= 10_000 -> "${euros / 1000}k"
        euros >= 1_000 -> "${euros / 1000}.${(euros % 1000).toString().padStart(3, '0')}"
        else -> "$euros"
    }
}

@Immutable
data class Celda(val fecha: LocalDate, val delMes: Boolean, val dia: DiaCalendario?, val esHoy: Boolean, val elegido: Boolean)

@Immutable
data class EstadoCalendario(
    val mes: YearMonth,
    val elegido: LocalDate,
    val semana: Boolean = false,
    val filas: List<List<Celda>> = emptyList(),
    /** Rejillas de 6 semanas del mes anterior, este y el siguiente (para deslizar entre meses). */
    val rejillas: Map<YearMonth, List<List<Celda>>> = emptyMap(),
    val movimientosDia: List<FilaMov> = emptyList(),
    val totalDia: DiaCalendario? = null,
    /** Pagos y cobros fijos que tocarán el día elegido (meses que vienen). */
    val previstosDia: List<io.github.alexcorrea2001lemon.nummia.domain.calculo.PrevistoFuturo> = emptyList(),
    /** En un mes que aún no ha llegado: lo previsto de pagos y de cobros fijos. */
    val previstoPagos: Long = 0,
    val previstoCobros: Long = 0,
    val gastoMes: Long = 0,
    val ingresoMes: Long = 0,
    val discreto: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarioViewModel @Inject constructor(
    repo: MovimientosRepo,
    ajustesRepo: AjustesRepo,
    private val reloj: Reloj,
) : ViewModel() {
    private val mes = MutableStateFlow(YearMonth.from(reloj.hoy()))
    private val elegido = MutableStateFlow(reloj.hoy())
    private val semana = MutableStateFlow(false)

    // Se cargan también el mes anterior y el siguiente: al deslizar ya están dibujados.
    private val datos = mes.flatMapLatest { ym ->
        repo.periodo(seisSemanas(ym.minusMonths(1)).first().first(), seisSemanas(ym.plusMonths(1)).last().last())
    }

    // Pagos y cobros fijos activos, para dibujar lo que viene en los meses siguientes.
    private val repeticiones = combine(repo.pagos(), repo.ingresosFijos()) { pagos, cobros ->
        pagos.filter { it.activo }.map { io.github.alexcorrea2001lemon.nummia.domain.calculo.Repeticion(it.id, it.concepto, it.importe, it.dia, it.cadaMeses, it.mesDesde, false) } +
            cobros.filter { it.activo }.map { io.github.alexcorrea2001lemon.nummia.domain.calculo.Repeticion(it.id, it.concepto, it.importe, it.dia, 1, null, true) }
    }

    val estado: StateFlow<EstadoCalendario> = combine(
        combine(mes, elegido, semana) { m, e, s -> Triple(m, e, s) }, combine(datos, repeticiones) { d, r -> d to r }, repo.personas(), repo.categorias(), ajustesRepo.ajustes,
    ) { (ym, sel, vistaSemana), (d, reps), personas, categorias, ajustes ->
        val hoy = reloj.hoy()
        val futuros = io.github.alexcorrea2001lemon.nummia.domain.calculo.previstosFuturos(
            hoy, seisSemanas(ym.minusMonths(1)).first().first(), seisSemanas(ym.plusMonths(1)).last().last(), reps,
        )
        // Los días con algo previsto llevan su marca, como los previstos de este mes.
        val dias = porDia(d.movGastos, d.movReembolsos, d.movIngresos).toMutableMap()
        futuros.forEach { f -> dias[f.fecha] = dias[f.fecha]?.copy(previsto = true) ?: DiaCalendario(0, 0, true) }
        val semanas = semanasDelMes(ym).map { sem -> sem.map { f -> Celda(f, YearMonth.from(f) == ym, dias[f], f == hoy, f == sel) } }
        fun rejilla(m: YearMonth) = seisSemanas(m).map { sem -> sem.map { f -> Celda(f, YearMonth.from(f) == m, dias[f], f == hoy, m == ym && f == sel) } }
        val rejillas = listOf(ym.minusMonths(1), ym, ym.plusMonths(1)).associateWith(::rejilla)
        val filas = if (vistaSemana) semanas.filter { s -> s.any { it.fecha == sel } }.ifEmpty { listOf(semanas.first()) } else semanas
        val pm = personas.associate { it.id to it.nombre }
        val cm = categorias.associate { it.id to it.nombre }
        val r = resumen(ym.atDay(1), ym.atEndOfMonth(), d.movGastos, d.movReembolsos, d.movIngresos)
        EstadoCalendario(
            mes = ym, elegido = sel, semana = vistaSemana, filas = filas, rejillas = rejillas,
            movimientosDia = (d.gastos.filter { it.gasto.fecha == sel }.map { it.aFila(pm, cm) } + d.ingresos.filter { it.fecha == sel }.map { it.aFila() })
                .sortedByDescending { it.creado },
            totalDia = dias[sel], gastoMes = r.gastoReal, ingresoMes = r.ingresos, discreto = ajustes.modoDiscreto,
            previstosDia = futuros.filter { it.fecha == sel },
            previstoPagos = futuros.filter { !it.ingreso && YearMonth.from(it.fecha) == ym }.sumOf { it.importe },
            previstoCobros = futuros.filter { it.ingreso && YearMonth.from(it.fecha) == ym }.sumOf { it.importe },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCalendario(mes.value, elegido.value))

    fun elegir(f: LocalDate) {
        elegido.value = f
        if (YearMonth.from(f) != mes.value) mes.value = YearMonth.from(f)
    }

    /** Mes anterior, o semana anterior en la vista de semana. */
    fun anterior() = if (semana.value) elegir(elegido.value.minusWeeks(1)) else cambiarMes(-1)
    fun siguiente() = if (semana.value) elegir(elegido.value.plusWeeks(1)) else cambiarMes(1)

    private fun cambiarMes(n: Long) {
        mes.update { it.plusMonths(n) }
        val ym = mes.value
        elegido.value = if (YearMonth.from(reloj.hoy()) == ym) reloj.hoy() else ym.atDay(1)
    }

    fun irAHoy() = elegir(reloj.hoy())
    fun irAMes(ym: YearMonth) = elegir(if (YearMonth.from(reloj.hoy()) == ym) reloj.hoy() else ym.atDay(1))
    fun hoy(): YearMonth = YearMonth.from(reloj.hoy())
    fun hoyDia(): LocalDate = reloj.hoy()
    /** El calendario se ha quedado en [ym] tras deslizar. */
    fun alPasarA(ym: YearMonth) { if (ym != mes.value) irAMes(ym) }
    fun alternarVista() = semana.update { !it }
}
