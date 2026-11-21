package io.github.alexcorrea2001lemon.nummia.ui.papelera

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
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
import io.github.alexcorrea2001lemon.nummia.data.repo.DIAS_PAPELERA
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class Borrado(val id: String, val esGasto: Boolean, val titulo: String, val importe: Long, val fecha: LocalDate, val diasRestantes: Int)

@HiltViewModel
class PapeleraViewModel @Inject constructor(private val repo: MovimientosRepo, private val reloj: Reloj) : ViewModel() {
    val borrados: StateFlow<List<Borrado>?> = combine(repo.papeleraGastos(), repo.papeleraIngresos()) { g, i ->
        fun quedan(ts: Long) = (DIAS_PAPELERA - ((reloj.ahoraMs() - ts) / 86_400_000L).toInt()).coerceAtLeast(0)
        val lista = g.map { Borrado(it.gasto.id, true, it.gasto.concepto, it.gasto.importe, it.gasto.fecha, quedan(it.gasto.deletedAt!!)) } +
            i.map { Borrado(it.id, false, it.concepto, it.importe, it.fecha, quedan(it.deletedAt!!)) }
        lista.sortedBy { it.diasRestantes }.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restaurar(b: Borrado) = viewModelScope.launch { if (b.esGasto) repo.deshacerBorradoGasto(b.id) else repo.deshacerBorradoIngreso(b.id) }
    fun eliminar(b: Borrado) = viewModelScope.launch { repo.eliminarDefinitivo(if (b.esGasto) listOf(b.id) else emptyList(), if (b.esGasto) emptyList() else listOf(b.id)) }
    fun vaciar() = viewModelScope.launch {
        val l = borrados.value.orEmpty()
        repo.eliminarDefinitivo(l.filter { it.esGasto }.map { it.id }, l.filter { !it.esGasto }.map { it.id })
    }
}

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun PapeleraScreen(vm: PapeleraViewModel, snackbar: SnackbarHostState, alVolver: () -> Unit) {
    val lista by vm.borrados.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val alcance = rememberCoroutineScope()
    var vaciando by remember { mutableStateOf(false) }
    var eliminando by remember { mutableStateOf<Borrado?>(null) }

    PantallaAjustes("Papelera", alVolver) {
        val l = lista.orEmpty()
        if (lista == null) {
            Unit
        } else if (l.isEmpty()) {
            Tarjeta { EstadoVacio(Iconos.Papelera, "La papelera está vacía", "Lo que borres se queda aquí $DIAS_PAPELERA días por si te arrepientes.") }
        } else {
            Text("Lo borrado se guarda $DIAS_PAPELERA días. Después desaparece solo.", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
            Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                l.forEachIndexed { n, b ->
                    if (n > 0) HorizontalDivider(color = c.borde)
                    Column(Modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(formatearEuros(if (b.esGasto) -b.importe else b.importe, conSigno = true), style = MaterialTheme.typography.bodyLarge, color = if (b.esGasto) c.gasto else c.ingreso)
                        }
                        Text(
                            "${if (b.esGasto) "Gasto" else "Ingreso"} del ${FECHA.format(b.fecha)} · ${if (b.diasRestantes == 1) "queda 1 día" else "quedan ${b.diasRestantes} días"}",
                            style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            OutlinedButton(onClick = {
                                vm.restaurar(b)
                                alcance.launch { snackbar.showSnackbar("«${b.titulo}» recuperado") }
                            }) { Text("Recuperar") }
                            TextButton(onClick = { eliminando = b }) { Text("Borrar para siempre", color = c.gasto) }
                        }
                    }
                }
            }
            TextButton(onClick = { vaciando = true }) { Text("Vaciar papelera", color = c.gasto) }
        }
    }

    if (vaciando) {
        AlertDialog(
            onDismissRequest = { vaciando = false },
            title = { Text("¿Vaciar la papelera?") },
            text = { Text("Se borrará para siempre todo lo que hay en ella. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { vaciando = false; vm.vaciar() }) { Text("Vaciar", color = c.gasto) } },
            dismissButton = { TextButton(onClick = { vaciando = false }) { Text("Cancelar") } },
        )
    }
    eliminando?.let { b ->
        AlertDialog(
            onDismissRequest = { eliminando = null },
            title = { Text("¿Borrar para siempre?") },
            text = { Text("«${b.titulo}» desaparecerá y no se podrá recuperar.") },
            confirmButton = { TextButton(onClick = { eliminando = null; vm.eliminar(b) }) { Text("Borrar", color = c.gasto) } },
            dismissButton = { TextButton(onClick = { eliminando = null }) { Text("Cancelar") } },
        )
    }
}
