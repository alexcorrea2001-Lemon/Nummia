package io.github.alexcorrea2001lemon.nummia.ui.formulario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.aFila
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@Immutable
data class FormReembolso(
    val gasto: GastoConReembolsos? = null,
    val importe: String = "",
    val fecha: LocalDate,
    val personaId: String? = null,
    val nota: String = "",
    val error: String? = null,
    val guardando: Boolean = false,
)

@HiltViewModel
class FormularioReembolsoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repo: MovimientosRepo,
    val reloj: Reloj,
) : ViewModel() {
    private val _form = MutableStateFlow(FormReembolso(fecha = reloj.hoy()))
    val form = _form.asStateFlow()
    private val _hecho = Channel<Unit>(Channel.BUFFERED)
    val hecho = _hecho.receiveAsFlow()

    val personas: StateFlow<List<PersonaEntity>> = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Candidatos: primero los que tienen dinero pendiente; luego los de este mes y el anterior. */
    val candidatos: StateFlow<List<GastoConReembolsos>> = run {
        val hoy = YearMonth.from(reloj.hoy())
        combine(repo.conDevolucion(), repo.periodo(hoy.minusMonths(1).atDay(1), hoy.atEndOfMonth())) { dev, recientes ->
            (dev.filter { it.falta > 0 } + recientes.gastos).distinctBy { it.gasto.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    init {
        estadoGuardado.get<String>("gastoId")?.takeIf { it.isNotBlank() }?.let { id ->
            viewModelScope.launch { repo.gastoUnaVez(id)?.let(::elegir) }
        }
    }

    fun elegir(g: GastoConReembolsos) {
        val sugerido = if (g.gasto.meLoDevuelven) g.falta else g.real.coerceAtLeast(0)
        _form.update { it.copy(gasto = g, importe = if (sugerido > 0) textoImporte(sugerido) else "", personaId = g.gasto.personaId, error = null) }
    }

    fun cambiarGasto() = _form.update { it.copy(gasto = null) }
    fun importe(v: String) = _form.update { it.copy(importe = v, error = null) }
    fun fecha(v: LocalDate) = _form.update { it.copy(fecha = v) }
    fun persona(v: String?) = _form.update { it.copy(personaId = v) }
    fun nota(v: String) = _form.update { it.copy(nota = v) }

    fun guardar() {
        val f = _form.value
        val g = f.gasto ?: return
        val r = leerImporteOperacion(f.importe)
        if (r.isFailure) { _form.update { it.copy(error = (r.exceptionOrNull() as? ImporteInvalido)?.motivo) }; return }
        _form.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repo.anadirReembolso(g.gasto.id, r.getOrThrow(), f.fecha, f.personaId, f.nota)
            io.github.alexcorrea2001lemon.nummia.ui.comun.Hecho.avisar("Devolución guardada")
            _hecho.send(Unit)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioReembolsoScreen(vm: FormularioReembolsoViewModel, alCerrar: () -> Unit) {
    val f by vm.form.collectAsStateWithLifecycle()
    val candidatos by vm.candidatos.collectAsStateWithLifecycle()
    val personas by vm.personas.collectAsStateWithLifecycle()
    val foco = remember { FocusRequester() }
    val haptica = rememberHaptica()
    val c = LocalColoresNummia.current
    LaunchedEffect(Unit) { vm.hecho.collect { haptica(); alCerrar() } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (f.gasto == null) "¿Qué te han devuelto?" else "Nueva devolución") },
                navigationIcon = { IconButton(onClick = alCerrar) { Icon(Iconos.Cerrar, "Cerrar") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (f.gasto != null) {
                Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, c.borde)) {
                    Button(onClick = vm::guardar, enabled = !f.guardando, modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(Espacio.l)) {
                        Text("Guardar devolución", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { p ->
        val g = f.gasto
        if (g == null) {
            LazyColumn(Modifier.fillMaxSize().padding(p)) {
                item { Text("Elige el gasto", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.padding(Espacio.l)) }
                items(candidatos, key = { it.gasto.id }) { cg -> FilaMovimiento(cg.aFila(personas.associate { it.id to it.nombre }, emptyMap()), false, { vm.elegir(cg) }) }
            }
            return@Scaffold
        }
        LaunchedEffect(g.gasto.id) { runCatching { foco.requestFocus() } }
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal = Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, c.borde), modifier = Modifier.clickable(onClick = vm::cambiarGasto)) {
                FilaMovimiento(g.aFila(personas.associate { it.id to it.nombre }, emptyMap()), false, vm::cambiarGasto)
            }
            CampoImporteGrande(f.importe, vm::importe, f.error, foco, etiqueta = "Te han devuelto")
            SelectorFecha(f.fecha, vm.reloj.hoy(), vm::fecha)
            Text("¿Quién te lo ha dado? (opcional)", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                FilterChip(selected = f.personaId == null, onClick = { vm.persona(null) }, label = { Text("Nadie en concreto") })
                personas.forEach { pe -> FilterChip(selected = f.personaId == pe.id, onClick = { vm.persona(pe.id) }, label = { Text(pe.nombre) }) }
            }
            OutlinedTextField(f.nota, vm::nota, label = { Text("Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
