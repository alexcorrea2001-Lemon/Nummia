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
    val movimientosDia: List<FilaMov> = emptyList(),
    val totalDia: DiaCalendario? = null,
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

    private val datos = mes.flatMapLatest { ym ->
        val s = semanasDelMes(ym)
        repo.periodo(s.first().first(), s.last().last())
    }

    val estado: StateFlow<EstadoCalendario> = combine(
        combine(mes, elegido, semana) { m, e, s -> Triple(m, e, s) }, datos, repo.personas(), repo.categorias(), ajustesRepo.ajustes,
    ) { (ym, sel, vistaSemana), d, personas, categorias, ajustes ->
        val dias = porDia(d.movGastos, d.movReembolsos, d.movIngresos)
        val hoy = reloj.hoy()
        val semanas = semanasDelMes(ym).map { sem -> sem.map { f -> Celda(f, YearMonth.from(f) == ym, dias[f], f == hoy, f == sel) } }
        val filas = if (vistaSemana) semanas.filter { s -> s.any { it.fecha == sel } }.ifEmpty { listOf(semanas.first()) } else semanas
        val pm = personas.associate { it.id to it.nombre }
        val cm = categorias.associate { it.id to it.nombre }
        val r = resumen(ym.atDay(1), ym.atEndOfMonth(), d.movGastos, d.movReembolsos, d.movIngresos)
        EstadoCalendario(
            mes = ym, elegido = sel, semana = vistaSemana, filas = filas,
            movimientosDia = (d.gastos.filter { it.gasto.fecha == sel }.map { it.aFila(pm, cm) } + d.ingresos.filter { it.fecha == sel }.map { it.aFila() })
                .sortedByDescending { it.creado },
            totalDia = dias[sel], gastoMes = r.gastoReal, ingresoMes = r.ingresos, discreto = ajustes.modoDiscreto,
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
    fun alternarVista() = semana.update { !it }
}
