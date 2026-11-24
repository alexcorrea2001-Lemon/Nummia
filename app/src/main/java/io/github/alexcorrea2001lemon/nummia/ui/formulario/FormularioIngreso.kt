package io.github.alexcorrea2001lemon.nummia.ui.formulario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.repo.IngresoInput
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreTipo
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@Immutable
data class FormIngreso(
    val id: String? = null,
    val tipo: TipoIngreso = TipoIngreso.SUELDO,
    val importe: String = "",
    val concepto: String = "Sueldo",
    val conceptoTocado: Boolean = false,
    val fecha: LocalDate,
    val notas: String = "",
    val previsto: Boolean = false,
    val cadaMes: Boolean = false,
    val errorImporte: String? = null,
    val guardando: Boolean = false,
)

@HiltViewModel
class FormularioIngresoViewModel @Inject constructor(
    estadoGuardado: SavedStateHandle,
    private val repo: MovimientosRepo,
    val reloj: Reloj,
) : ViewModel() {
    private val idEdicion = estadoGuardado.get<String>("id")?.takeIf { it.isNotBlank() }
    private val _form = MutableStateFlow(FormIngreso(fecha = reloj.hoy()))
    val form = _form.asStateFlow()
    private val _hecho = Channel<Unit>(Channel.BUFFERED)
    val hecho = _hecho.receiveAsFlow()

    init {
        if (idEdicion != null) viewModelScope.launch {
            repo.ingreso(idEdicion)?.let { i ->
                _form.value = FormIngreso(
                    id = i.id, tipo = i.tipo, importe = textoImporte(i.importe), concepto = i.concepto, conceptoTocado = true,
                    fecha = i.fecha, notas = i.notas, previsto = i.estado == Estado.PREVISTO, cadaMes = repo.esIngresoFijo(i),
                )
            }
        }
    }

    fun tipo(t: TipoIngreso) = _form.update { if (it.conceptoTocado) it.copy(tipo = t) else it.copy(tipo = t, concepto = nombreTipo(t)) }
    fun importe(v: String) = _form.update { it.copy(importe = v, errorImporte = null) }
    fun concepto(v: String) = _form.update { it.copy(concepto = v, conceptoTocado = true) }
    fun fecha(v: LocalDate) = _form.update { it.copy(fecha = v) }
    fun notas(v: String) = _form.update { it.copy(notas = v) }
    fun cadaMes(v: Boolean) = _form.update { it.copy(cadaMes = v) }

    fun guardar() {
        val f = _form.value
        val r = leerImporteOperacion(f.importe)
        if (r.isFailure) { _form.update { it.copy(errorImporte = (r.exceptionOrNull() as? ImporteInvalido)?.motivo) }; return }
        _form.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repo.guardarIngreso(IngresoInput(f.id, f.fecha, f.concepto.ifBlank { nombreTipo(f.tipo) }, f.tipo, r.getOrThrow(), f.notas, cadaMes = f.cadaMes))
            io.github.alexcorrea2001lemon.nummia.ui.comun.Hecho.avisar(if (f.previsto) "Cobro confirmado" else "Ingreso guardado")
            _hecho.send(Unit)
        }
    }

    fun borrar() = viewModelScope.launch { idEdicion?.let { repo.borrarIngreso(it) }; _hecho.send(Unit) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioIngresoScreen(vm: FormularioIngresoViewModel, alCerrar: () -> Unit) {
    val f by vm.form.collectAsStateWithLifecycle()
    val foco = remember { FocusRequester() }
    val haptica = rememberHaptica()
    var confirmarBorrado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (f.id == null) runCatching { foco.requestFocus() } }
    LaunchedEffect(Unit) { vm.hecho.collect { haptica(); alCerrar() } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (f.id != null) "Editar ingreso" else "Nuevo ingreso") },
                navigationIcon = { IconButton(onClick = alCerrar) { Icon(Iconos.Cerrar, "Cerrar") } },
                actions = { if (f.id != null) IconButton(onClick = { confirmarBorrado = true }) { Icon(Iconos.Papelera, "Borrar ingreso") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, LocalColoresNummia.current.borde)) {
                Button(onClick = vm::guardar, enabled = !f.guardando, modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(Espacio.l)) {
                    Text(if (f.previsto) "Confirmar cobro" else "Guardar", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal = Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            CampoImporteGrande(f.importe, vm::importe, f.errorImporte, foco)
            Text("Tipo", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                TipoIngreso.entries.forEach { t -> FilterChip(selected = f.tipo == t, onClick = { vm.tipo(t) }, label = { Text(nombreTipo(t)) }) }
            }
            OutlinedTextField(
                f.concepto, vm::concepto, label = { Text("Concepto") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            SelectorFecha(f.fecha, vm.reloj.hoy(), vm::fecha)
            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, if (f.cadaMes) MaterialTheme.colorScheme.primary else LocalColoresNummia.current.borde),
            ) {
                Row(Modifier.padding(Espacio.l), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Se cobra cada mes", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Cada mes aparecerá en Inicio el día ${f.fecha.dayOfMonth} para confirmarlo o cambiar el importe. Lo verás en Ajustes → Pagos y cobros fijos",
                            style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave,
                        )
                    }
                    androidx.compose.material3.Switch(checked = f.cadaMes, onCheckedChange = { haptica(); vm.cadaMes(it) })
                }
            }
            OutlinedTextField(f.notas, vm::notas, label = { Text("Notas (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }

    if (confirmarBorrado) {
        DialogoNummia(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar este ingreso?") },
            text = { Text("Dejará de contar en tus cuentas.") },
            confirmButton = { TextButton(onClick = { confirmarBorrado = false; vm.borrar() }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}
