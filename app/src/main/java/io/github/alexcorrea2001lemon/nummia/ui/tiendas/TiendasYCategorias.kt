package io.github.alexcorrea2001lemon.nummia.ui.tiendas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.importar.ExcelAnual
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.TiendaResumen
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TiendasViewModel @Inject constructor(private val repo: MovimientosRepo) : ViewModel() {
    val tiendas: StateFlow<List<TiendaResumen>?> = repo.tiendas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val categorias: StateFlow<List<CategoriaEntity>?> = repo.categorias().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun unir(origen: Collection<String>, destino: String): Int = repo.unirTiendas(origen, destino)
    fun crearCategoria(nombre: String, color: Long) = viewModelScope.launch { repo.crearCategoria(nombre, color) }
    fun editarCategoria(c: CategoriaEntity) = viewModelScope.launch { repo.editarCategoria(c) }
    fun borrarCategoria(c: CategoriaEntity) = viewModelScope.launch { repo.borrarCategoria(c) }
}

/** Grupos de nombres que parecen la misma tienda (mayúsculas, tildes, espacios o una errata). */
fun posiblesDuplicados(tiendas: List<TiendaResumen>): List<List<TiendaResumen>> {
    val unificado = ExcelAnual.unificarNombres(tiendas.map { it.nombre })
    return tiendas.groupBy { unificado.getValue(it.nombre) }.values.filter { it.size > 1 }.map { g -> g.sortedByDescending { it.veces } }
}

@Composable
fun TiendasScreen(vm: TiendasViewModel, snackbar: SnackbarHostState, alVolver: () -> Unit) {
    val tiendas by vm.tiendas.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val alcance = rememberCoroutineScope()
    var marcadas by remember { mutableStateOf(setOf<String>()) }
    var uniendo by remember { mutableStateOf<Set<String>?>(null) }

    fun unir(origen: Set<String>, destino: String) = alcance.launch {
        val n = vm.unir(origen, destino)
        marcadas = emptySet()
        snackbar.showSnackbar(if (n == 1) "1 gasto pasa a «$destino»" else "$n gastos pasan a «$destino»")
    }

    PantallaAjustes("Tiendas", alVolver) {
        val lista = tiendas.orEmpty()
        if (tiendas != null && lista.isEmpty()) {
            Tarjeta { EstadoVacio(Iconos.Bolsa, "Aún no hay tiendas", "Aparecerán aquí cuando apuntes gastos.") }
        } else if (lista.isNotEmpty()) {
            val dup = posiblesDuplicados(lista)
            if (dup.isNotEmpty()) {
                Text("Parecen la misma tienda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                dup.forEach { g ->
                    Tarjeta {
                        Text(g.joinToString(" · ") { "${it.nombre} (${it.veces})" }, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            Button(onClick = { unir(g.map { it.nombre }.toSet(), g.first().nombre) }) { Text("Unir en «${g.first().nombre}»") }
                            TextButton(onClick = { uniendo = g.map { it.nombre }.toSet() }) { Text("Otro nombre") }
                        }
                    }
                }
            }
            Text("Todas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Marca varias para unirlas, o una para cambiarle el nombre.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            AnimatedVisibility(marcadas.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Button(onClick = { uniendo = marcadas }) { Text(if (marcadas.size == 1) "Cambiar nombre" else "Unir ${marcadas.size} tiendas") }
                    OutlinedButton(onClick = { marcadas = emptySet() }) { Text("Cancelar") }
                }
            }
            Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                lista.forEachIndexed { n, t ->
                    if (n > 0) HorizontalDivider(color = c.borde)
                    val marcada = t.nombre in marcadas
                    Row(
                        Modifier.fillMaxWidth().clickable { marcadas = if (marcada) marcadas - t.nombre else marcadas + t.nombre }
                            .padding(horizontal = Espacio.s, vertical = Espacio.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = marcada, onCheckedChange = { marcadas = if (it) marcadas + t.nombre else marcadas - t.nombre })
                        Column(Modifier.weight(1f)) {
                            Text(t.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text(if (t.veces == 1) "1 vez" else "${t.veces} veces", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                        Text(formatearEuros(t.total), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = Espacio.m))
                    }
                }
            }
        }
    }

    uniendo?.let { origen ->
        val sugerido = tiendas.orEmpty().filter { it.nombre in origen }.maxByOrNull { it.veces }?.nombre.orEmpty()
        var nombre by remember(origen) { mutableStateOf(sugerido) }
        AlertDialog(
            onDismissRequest = { uniendo = null },
            title = { Text(if (origen.size == 1) "Cambiar nombre" else "Unir ${origen.size} tiendas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Text("Todos sus gastos pasarán a llamarse así (también en tu hoja de Drive).", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                }
            },
            confirmButton = { Button(enabled = nombre.isNotBlank(), onClick = { unir(origen, nombre); uniendo = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { uniendo = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun CategoriasScreen(vm: TiendasViewModel, alVolver: () -> Unit) {
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    var editando by remember { mutableStateOf<CategoriaEntity?>(null) }
    var creando by remember { mutableStateOf(false) }

    PantallaAjustes("Categorías", alVolver) {
        Text("Las categorías son opcionales. Las de serie se pueden renombrar y cambiar de color.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        Tarjeta(relleno = false) {
            categorias.orEmpty().forEachIndexed { n, cat ->
                if (n > 0) HorizontalDivider(color = c.borde)
                Row(
                    Modifier.fillMaxWidth().clickable { editando = cat }.padding(horizontal = Espacio.l, vertical = Espacio.m),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    Box(Modifier.size(14.dp).background(Color(cat.color), CircleShape))
                    Text(cat.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (!cat.predefinida) Text("tuya", style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
                }
            }
        }
        TextButton(onClick = { creando = true }) { Text("+ Nueva categoría") }
    }

    if (creando) DialogoCategoria(null, alCerrar = { creando = false }, alBorrar = {}) { nombre, color -> vm.crearCategoria(nombre, color); creando = false }
    editando?.let { cat ->
        DialogoCategoria(cat, alCerrar = { editando = null }, alBorrar = { vm.borrarCategoria(cat); editando = null }) { nombre, color ->
            vm.editarCategoria(cat.copy(nombre = nombre, color = color)); editando = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoCategoria(cat: CategoriaEntity?, alCerrar: () -> Unit, alBorrar: () -> Unit, alGuardar: (String, Long) -> Unit) {
    var nombre by remember { mutableStateOf(cat?.nombre.orEmpty()) }
    var color by remember { mutableStateOf(cat?.color ?: Acento.POR_DEFECTO.argb) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (cat == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Acento.entries.forEach { a ->
                        val elegido = color == a.argb
                        Box(
                            Modifier.size(36.dp).background(a.color, CircleShape)
                                .let { if (elegido) it.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else it }
                                .clickable { color = a.argb }
                                .semantics { contentDescription = a.nombre; selected = elegido },
                        )
                    }
                }
                if (cat != null && !cat.predefinida) {
                    TextButton(onClick = { if (confirmarBorrado) alBorrar() else confirmarBorrado = true }) {
                        Text(if (confirmarBorrado) "Pulsa otra vez: sus gastos se quedan sin categoría" else "Borrar categoría", color = LocalColoresNummia.current.gasto)
                    }
                }
            }
        },
        confirmButton = { Button(enabled = nombre.isNotBlank(), onClick = { alGuardar(nombre.trim(), color) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
