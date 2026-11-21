package io.github.alexcorrea2001lemon.nummia.ui.formulario

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.GastoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@Immutable
data class FormGasto(
    val id: String? = null,
    val importe: String = "",
    val concepto: String = "",
    val tienda: String = "",
    val fecha: LocalDate,
    val categoriaId: String? = null,
    val meLoDevuelven: Boolean = false,
    val personaId: String? = null,
    val esperado: String = "",
    val yaDevuelto: Boolean = false,
    val cadaMes: Boolean = false,
    val notas: String = "",
    val errorImporte: String? = null,
    val errorConcepto: String? = null,
    val errorEsperado: String? = null,
    val guardando: Boolean = false,
) {
    val editando get() = id != null
}

sealed interface EventoForm {
    /** [antes] = cómo estaba el gasto antes de editarlo (para «Deshacer»); null si es nuevo. */
    data class Guardado(val id: String, val otro: Boolean, val antes: io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity? = null) : EventoForm
}

/** Texto «1.520,75» sin «€», para rellenar un campo. */
fun textoImporte(c: Long): String = formatearEuros(c).removeSuffix(" €").replace("−", "-")

private fun motivo(t: Throwable) = (t as? ImporteInvalido)?.motivo ?: "Importe no válido"

@HiltViewModel
class FormularioGastoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repo: MovimientosRepo,
    private val reloj: Reloj,
) : ViewModel() {
    private val idEdicion: String? = estadoGuardado.get<String>("id")?.takeIf { it.isNotBlank() }
    private val paraOtro: Boolean = estadoGuardado.get<Boolean>("paraOtro") ?: false

    private val fechaInicial: LocalDate = estadoGuardado.get<String>("fecha")?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: reloj.hoy()

    private val _form = MutableStateFlow(FormGasto(fecha = fechaInicial, meLoDevuelven = paraOtro))
    val form: StateFlow<FormGasto> = _form.asStateFlow()

    val personas: StateFlow<List<PersonaEntity>> = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categorias: StateFlow<List<CategoriaEntity>> = repo.categorias().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _eventos = Channel<EventoForm>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        if (idEdicion != null) viewModelScope.launch {
            repo.gastoUnaVez(idEdicion)?.gasto?.let { g ->
                _form.value = FormGasto(
                    id = g.id, importe = textoImporte(g.importe), concepto = g.concepto, tienda = g.tienda,
                    fecha = g.fecha, categoriaId = g.categoriaId, meLoDevuelven = g.meLoDevuelven, personaId = g.personaId,
                    esperado = g.importeEsperado?.let(::textoImporte).orEmpty(), notas = g.notas,
                    cadaMes = repo.esPagoMensual(g),
                )
            }
        }
    }

    fun importe(v: String) = _form.update { it.copy(importe = v, errorImporte = null) }
    fun concepto(v: String) = _form.update { it.copy(concepto = v, errorConcepto = null) }
    fun fecha(v: LocalDate) = _form.update { it.copy(fecha = v) }
    fun categoria(v: String?) = _form.update { it.copy(categoriaId = v) }
    fun meLoDevuelven(v: Boolean) = _form.update { it.copy(meLoDevuelven = v) }
    fun persona(v: String?) = _form.update { it.copy(personaId = v) }
    fun esperado(v: String) = _form.update { it.copy(esperado = v, errorEsperado = null) }
    fun yaDevuelto(v: Boolean) = _form.update { it.copy(yaDevuelto = v) }
    fun cadaMes(v: Boolean) = _form.update { it.copy(cadaMes = v) }
    fun notas(v: String) = _form.update { it.copy(notas = v) }

    /** Al escribir la tienda se sugiere su categoría habitual (si aún no hay una elegida). */
    fun tienda(v: String) {
        _form.update { it.copy(tienda = v) }
        if (_form.value.categoriaId == null && v.isNotBlank()) viewModelScope.launch {
            val sugerida = repo.categoriaHabitual(v)
            if (sugerida != null && _form.value.tienda == v && _form.value.categoriaId == null) _form.update { it.copy(categoriaId = sugerida) }
        }
    }

    fun nuevaPersona(nombre: String) = viewModelScope.launch {
        if (nombre.isNotBlank()) persona(repo.crearPersona(nombre))
    }

    /** Valida, guarda y avisa. Con [otro] el formulario se limpia para el siguiente (conservando la fecha). */
    fun guardar(otro: Boolean) {
        val f = _form.value
        if (f.guardando) return
        val importe = leerImporteOperacion(f.importe)
        val esperado = if (f.meLoDevuelven && f.esperado.isNotBlank()) leerImporte(f.esperado) else null
        val errConcepto = if (f.concepto.isBlank()) "Escribe qué has pagado" else null
        _form.update {
            it.copy(
                errorImporte = importe.exceptionOrNull()?.let(::motivo),
                errorConcepto = errConcepto,
                errorEsperado = esperado?.exceptionOrNull()?.let(::motivo),
            )
        }
        if (importe.isFailure || errConcepto != null || esperado?.isFailure == true) return
        _form.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val antes = f.id?.let { repo.gastoUnaVez(it)?.gasto }
            val id = repo.guardarGasto(
                GastoInput(
                    id = f.id, fecha = f.fecha, concepto = f.concepto, tienda = f.tienda, categoriaId = f.categoriaId,
                    importe = importe.getOrThrow(), meLoDevuelven = f.meLoDevuelven, personaId = f.personaId,
                    esperado = esperado?.getOrNull(), notas = f.notas, yaDevuelto = f.yaDevuelto && f.id == null, cadaMes = f.cadaMes,
                ),
            )
            _form.value = if (otro) FormGasto(fecha = f.fecha) else f.copy(guardando = false)
            io.github.alexcorrea2001lemon.nummia.ui.comun.Hecho.avisar(if (f.id == null) "Gasto guardado" else "Cambios guardados")
            _eventos.send(EventoForm.Guardado(id, otro, antes))
        }
    }
}
