package io.github.alexcorrea2001lemon.nummia.ui.deben

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.devuelto
import io.github.alexcorrea2001lemon.nummia.data.repo.falta
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class Deuda(val gastoId: String, val concepto: String, val fecha: LocalDate, val esperado: Long, val devuelto: Long, val falta: Long)
data class PersonaDeuda(val personaId: String?, val nombre: String, val deudas: List<Deuda>) {
    val total get() = deudas.sumOf { it.falta }
}

@HiltViewModel
class DebenViewModel @Inject constructor(private val repo: MovimientosRepo) : ViewModel() {
    val personas: StateFlow<List<PersonaDeuda>?> = combine(repo.conDevolucion(), repo.personas()) { gastos, ps ->
        val nombres = ps.associate { it.id to it.nombre }
        gastos.filter { it.gasto.estado == Estado.CONFIRMADO && it.falta > 0 }
            .groupBy { it.gasto.personaId }
            .map { (pid, gs) ->
                PersonaDeuda(
                    pid, pid?.let(nombres::get) ?: "Sin persona",
                    gs.sortedBy { it.gasto.fecha }.map { Deuda(it.gasto.id, it.gasto.concepto, it.gasto.fecha, it.gasto.importeEsperado ?: it.gasto.importe, it.devuelto, it.falta) },
                )
            }
            .sortedByDescending { it.total }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** «Me lo ha pagado todo»: un reembolso por lo que falta de cada gasto. Devuelve los reembolsos creados (para deshacer). */
    suspend fun pagarTodo(p: PersonaDeuda): List<String> = p.deudas.mapNotNull { repo.devueltoEntero(it.gastoId) }

    fun deshacer(reembolsos: List<String>) = viewModelScope.launch { reembolsos.forEach { repo.borrarReembolso(it) } }
}

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun DebenScreen(vm: DebenViewModel, snackbar: SnackbarHostState, alAbrirGasto: (String) -> Unit, alVolver: () -> Unit) {
    val lista by vm.personas.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val alcance = rememberCoroutineScope()
    var abierta by remember { mutableStateOf<String?>(null) }
    var pagando by remember { mutableStateOf<PersonaDeuda?>(null) }

    PantallaAjustes("Te deben", alVolver) {
        val l = lista.orEmpty()
        if (lista != null && l.isEmpty()) {
            Tarjeta { EstadoVacio(Iconos.Check, "Nadie te debe nada", "Cuando pagues algo por otra persona aparecerá aquí hasta que te lo devuelva.") }
        } else if (l.isNotEmpty()) {
            Tarjeta(fondo = c.heroe) {
                Text("En total te deben", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                Text(formatearEuros(l.sumOf { it.total }), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = c.aviso)
            }
            l.forEach { p ->
                val clave = p.personaId ?: "-"
                Tarjeta(Modifier.animateContentSize()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { abierta = if (abierta == clave) null else clave },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                    ) {
                        Avatar(p.nombre, c.avisoFondo, c.aviso)
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (p.deudas.size == 1) "1 cosa pendiente" else "${p.deudas.size} cosas pendientes", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                        Text(formatearEuros(p.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.aviso)
                    }
                    AnimatedVisibility(abierta == clave) {
                        Column {
                            p.deudas.forEach { d ->
                                HorizontalDivider(color = c.borde, modifier = Modifier.padding(vertical = Espacio.xs))
                                Row(Modifier.fillMaxWidth().clickable { alAbrirGasto(d.gastoId) }.padding(vertical = Espacio.xs)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(d.concepto, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${FECHA.format(d.fecha)} · ${formatearEuros(d.esperado)}" + if (d.devuelto > 0) " · devuelto ${formatearEuros(d.devuelto)}" else "",
                                            style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                                        )
                                    }
                                    Text(formatearEuros(d.falta), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Button(onClick = { pagando = p }, modifier = Modifier.fillMaxWidth()) { Text("Me lo ha pagado todo") }
                }
            }
        }
    }

    pagando?.let { p ->
        DialogoNummia(
            onDismissRequest = { pagando = null },
            title = { Text("¿${p.nombre} te lo ha pagado todo?") },
            text = { Text("Se apunta que te ha devuelto ${formatearEuros(p.total)} hoy, repartido en sus ${p.deudas.size} gastos.") },
            confirmButton = {
                Button(onClick = {
                    pagando = null
                    alcance.launch {
                        val creados = vm.pagarTodo(p)
                        val r = snackbar.showSnackbar("${p.nombre} está al día", actionLabel = "Deshacer", duration = SnackbarDuration.Long)
                        if (r == SnackbarResult.ActionPerformed) vm.deshacer(creados)
                    }
                }) { Text("Sí, todo") }
            },
            dismissButton = { TextButton(onClick = { pagando = null }) { Text("Cancelar") } },
        )
    }
}
