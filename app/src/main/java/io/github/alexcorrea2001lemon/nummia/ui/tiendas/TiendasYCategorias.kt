package io.github.alexcorrea2001lemon.nummia.ui.tiendas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import io.github.alexcorrea2001lemon.nummia.ui.comun.AccionPanel
import io.github.alexcorrea2001lemon.nummia.ui.comun.AvatarIniciales
import io.github.alexcorrea2001lemon.nummia.ui.comun.CabeceraPanel
import io.github.alexcorrea2001lemon.nummia.ui.comun.DatosPanel
import io.github.alexcorrea2001lemon.nummia.ui.comun.PanelNummia
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
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
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
    fun crearCategoria(nombre: String, color: Long, icono: String) = viewModelScope.launch { repo.crearCategoria(nombre, color, icono) }
    fun editarCategoria(c: CategoriaEntity) = viewModelScope.launch { repo.editarCategoria(c) }
    fun borrarCategoria(c: CategoriaEntity) = viewModelScope.launch { repo.borrarCategoria(c) }
    fun ordenarCategorias(ids: List<String>) = viewModelScope.launch { repo.ordenarCategorias(ids) }
}

/** Grupos de nombres que parecen la misma tienda (mayúsculas, tildes, espacios o una errata). */
fun posiblesDuplicados(tiendas: List<TiendaResumen>): List<List<TiendaResumen>> {
    val unificado = ExcelAnual.unificarNombres(tiendas.map { it.nombre })
    return tiendas.groupBy { unificado.getValue(it.nombre) }.values.filter { it.size > 1 }.map { g -> g.sortedByDescending { it.veces } }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TiendasScreen(vm: TiendasViewModel, snackbar: SnackbarHostState, alTienda: (String) -> Unit = {}, alVolver: () -> Unit) {
    val tiendas by vm.tiendas.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val alcance = rememberCoroutineScope()
    val haptica = androidx.compose.ui.platform.LocalHapticFeedback.current
    var busqueda by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var orden by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(OrdenTiendas.VECES) }
    var marcadas by remember { mutableStateOf(setOf<String>()) }
    var abierta by remember { mutableStateOf<TiendaResumen?>(null) }
    var uniendo by remember { mutableStateOf<Set<String>?>(null) }
    val seleccionando = marcadas.isNotEmpty()

    fun unir(origen: Set<String>, destino: String) = alcance.launch {
        val n = vm.unir(origen, destino)
        marcadas = emptySet()
        snackbar.showSnackbar(if (n == 1) "1 gasto pasa a «$destino»" else "$n gastos pasan a «$destino»")
    }
    fun alternar(t: TiendaResumen) { marcadas = if (t.nombre in marcadas) marcadas - t.nombre else marcadas + t.nombre }
    androidx.activity.compose.BackHandler(seleccionando) { marcadas = emptySet() }

    Box(Modifier.fillMaxSize()) {
        PantallaAjustes("Tiendas", alVolver) {
            val lista = tiendas.orEmpty()
            if (tiendas != null && lista.isEmpty()) {
                Tarjeta { EstadoVacio(Iconos.Bolsa, "Aún no hay tiendas", "Aparecerán aquí cuando apuntes gastos.") }
            } else if (lista.isNotEmpty()) {
                Text(
                    "${lista.size} tiendas · ${formatearEuros(lista.sumOf { it.total })} en total",
                    style = MaterialTheme.typography.bodyMedium.merge(androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")), color = c.textoSuave,
                    modifier = Modifier.padding(start = Espacio.xs),
                )
                OutlinedTextField(
                    busqueda, { busqueda = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar tienda") }, leadingIcon = { Icon(Iconos.Buscar, null) },
                    trailingIcon = { if (busqueda.isNotEmpty()) IconButton(onClick = { busqueda = "" }) { Icon(Iconos.Cerrar, "Borrar la búsqueda") } },
                    shape = RoundedCornerShape(50),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    OrdenTiendas.entries.forEach { o ->
                        FilterChip(selected = orden == o, onClick = { orden = o }, label = { Text(o.texto) })
                    }
                }
                posiblesDuplicados(lista).forEach { g ->
                    Tarjeta(fondo = c.heroe) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Icon(Iconos.Duplicar, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                g.joinToString(" y ") { "«${it.nombre}»" } + " parecen la misma",
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            Button(onClick = { unir(g.map { it.nombre }.toSet(), g.first().nombre) }) { Text("Unir en «${g.first().nombre}»", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                            TextButton(onClick = { uniendo = g.map { it.nombre }.toSet() }) { Text("Otro nombre") }
                        }
                    }
                }
                val vista = filtrarTiendas(lista, busqueda, orden)
                if (vista.isEmpty()) {
                    Text("Ninguna tienda coincide con «${busqueda.trim()}».", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.padding(Espacio.s))
                } else {
                    Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                        vista.forEachIndexed { n, t ->
                            if (n > 0) HorizontalDivider(color = c.borde)
                            val marcada = t.nombre in marcadas
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (marcada) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                                    .combinedClickable(
                                        onClickLabel = if (seleccionando) "Marcar o desmarcar" else "Abrir",
                                        onLongClickLabel = "Marcar para unir",
                                        onClick = { if (seleccionando) alternar(t) else abierta = t },
                                        onLongClick = { haptica.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); alternar(t) },
                                    )
                                    .semantics { selected = marcada }
                                    .padding(horizontal = Espacio.l, vertical = Espacio.m),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l),
                            ) {
                                AvatarIniciales(iniciales(t.nombre).take(1), marcado = marcada)
                                Column(Modifier.weight(1f)) {
                                    Text(t.nombre, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(
                                        (if (t.veces == 1) "1 vez" else "${t.veces} veces") + (t.ultima?.let { " · última ${io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta(it)}" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                                    )
                                }
                                Text(formatearEuros(t.total), style = MaterialTheme.typography.bodyLarge.merge(androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Text(
                        "Toca una para ver sus opciones · Mantén pulsado para unir varias",
                        style = MaterialTheme.typography.bodySmall, color = c.textoSuave, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = if (seleccionando) 96.dp else 0.dp),
                    )
                }
            }
        }

        // Barra de selección: cuántas hay marcadas y «Unir».
        AnimatedVisibility(
            seleccionando, modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
        ) {
            val elegidas = tiendas.orEmpty().filter { it.nombre in marcadas }
            Surface(
                shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 6.dp,
                modifier = Modifier.navigationBarsPadding().padding(Espacio.l).fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = Espacio.s, end = Espacio.l, top = Espacio.s, bottom = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { marcadas = emptySet() }) { Icon(Iconos.Cerrar, "Salir de la selección") }
                    Column(Modifier.weight(1f)) {
                        Text(if (marcadas.size == 1) "1 marcada" else "${marcadas.size} marcadas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${elegidas.sumOf { it.veces }} gastos · ${formatearEuros(elegidas.sumOf { it.total })}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { uniendo = marcadas }) { Text(if (marcadas.size == 1) "Cambiar nombre" else "Unir…") }
                }
            }
        }
    }

    abierta?.let { t ->
        val cerrar = { abierta = null }
        PanelNummia(alCerrar = cerrar) {
            CabeceraPanel({ AvatarIniciales(iniciales(t.nombre).take(1), tam = 52.dp) }, t.nombre, t.ultima?.let { "Último gasto: ${io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta(it)}" })
            DatosPanel("Total" to formatearEuros(t.total), "Veces" to t.veces.toString(), "Media" to formatearEuros(if (t.veces > 0) t.total / t.veces else 0))
            HorizontalDivider(color = c.borde)
            AccionPanel(Iconos.Movimientos, "Ver sus gastos", conFlecha = true) { cerrar(); alTienda(t.nombre) }
            AccionPanel(Iconos.Lapiz, "Cambiar nombre") { cerrar(); uniendo = setOf(t.nombre) }
            AccionPanel(Iconos.Duplicar, "Unir con otras tiendas…") {
                cerrar(); marcadas = setOf(t.nombre)
                alcance.launch { snackbar.showSnackbar("Marca las tiendas que quieras unir con «${t.nombre}»") }
            }
            Text("Si cambias el nombre, todos sus gastos pasan a llamarse así (también en tu hoja de Drive).", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        }
    }

    uniendo?.let { origen ->
        val sugerido = tiendas.orEmpty().filter { it.nombre in origen }.maxByOrNull { it.veces }?.nombre.orEmpty()
        var nombre by remember(origen) { mutableStateOf(sugerido) }
        DialogoNummia(
            onDismissRequest = { uniendo = null },
            title = { Text(if (origen.size == 1) "Cambiar nombre" else "Unir ${origen.size} tiendas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Text("Todos sus gastos pasarán a llamarse así (también en tu hoja de Drive).", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                }
            },
            confirmButton = { Button(enabled = nombre.isNotBlank(), onClick = { unir(origen, nombre.trim()); uniendo = null }) { Text("Guardar") } },
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
        // Ordenar arrastrando (desde el asa o manteniendo pulsado). El orden es el de los chips del formulario.
        var orden by remember(categorias) { mutableStateOf(categorias.orEmpty()) }
        var arrastrando by remember { mutableStateOf<String?>(null) }
        var dy by remember { mutableFloatStateOf(0f) }
        var alto by remember { mutableFloatStateOf(1f) }
        val haptica = io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica()
        fun mover(id: String, pasos: Int): Boolean {
            val i = orden.indexOfFirst { it.id == id }
            val j = (i + pasos).coerceIn(0, orden.lastIndex)
            if (i < 0 || i == j) return false
            orden = orden.toMutableList().apply { add(j, removeAt(i)) }
            return true
        }
        fun terminar() { arrastrando = null; dy = 0f; vm.ordenarCategorias(orden.map { it.id }) }
        Tarjeta(relleno = false) {
            orden.forEachIndexed { n, cat -> androidx.compose.runtime.key(cat.id) {
                if (n > 0) HorizontalDivider(color = c.borde)
                val moviendo = arrastrando == cat.id
                val gesto: suspend androidx.compose.ui.input.pointer.PointerInputScope.(Boolean) -> Unit = { tras ->
                    val alEmpezar: (androidx.compose.ui.geometry.Offset) -> Unit = { arrastrando = cat.id; dy = 0f; haptica() }
                    val alMover: (androidx.compose.ui.input.pointer.PointerInputChange, androidx.compose.ui.geometry.Offset) -> Unit = { cambio, d ->
                        cambio.consume()
                        dy += d.y
                        val pasos = kotlin.math.round(dy / alto).toInt()
                        if (pasos != 0 && mover(cat.id, pasos)) { dy -= pasos * alto; haptica() }
                    }
                    if (tras) detectDragGesturesAfterLongPress(alEmpezar, { terminar() }, { terminar() }, alMover)
                    else detectDragGestures(alEmpezar, { terminar() }, { terminar() }, alMover)
                }
                Row(
                    Modifier.fillMaxWidth()
                        .onSizeChanged { alto = it.height.toFloat().coerceAtLeast(1f) }
                        .zIndex(if (moviendo) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (moviendo) dy else 0f
                            shadowElevation = if (moviendo) 8.dp.toPx() else 0f
                        }
                        .background(if (moviendo) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                        .pointerInput(cat.id) { gesto(true) }
                        .clickable { editando = cat }
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Subir") { mover(cat.id, -1).also { if (it) vm.ordenarCategorias(orden.map { o -> o.id }) } },
                                CustomAccessibilityAction("Bajar") { mover(cat.id, 1).also { if (it) vm.ordenarCategorias(orden.map { o -> o.id }) } },
                            )
                        }
                        .padding(start = Espacio.l, top = Espacio.m, bottom = Espacio.m),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar(
                        cat.nombre, Color(cat.color).copy(alpha = 0.2f), Color(cat.color),
                        io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria.de(cat.icono),
                    )
                    Text(cat.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (!cat.predefinida) Text("tuya", style = MaterialTheme.typography.labelSmall, color = c.textoSuave)
                    androidx.compose.material3.Icon(
                        Iconos.Arrastrar, "Arrastra para ordenar", tint = c.textoSuave,
                        modifier = Modifier.pointerInput(cat.id) { gesto(false) }.padding(horizontal = Espacio.l, vertical = Espacio.s).size(20.dp),
                    )
                }
            } }
        }
        Text("Arrastra ☰ para cambiar el orden.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        TextButton(onClick = { creando = true }) { Text("+ Nueva categoría") }
    }

    if (creando) DialogoCategoria(null, alCerrar = { creando = false }, alBorrar = {}) { nombre, color, icono -> vm.crearCategoria(nombre, color, icono); creando = false }
    editando?.let { cat ->
        DialogoCategoria(cat, alCerrar = { editando = null }, alBorrar = { vm.borrarCategoria(cat); editando = null }) { nombre, color, icono ->
            vm.editarCategoria(cat.copy(nombre = nombre, color = color, icono = icono)); editando = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoCategoria(cat: CategoriaEntity?, alCerrar: () -> Unit, alBorrar: () -> Unit, alGuardar: (String, Long, String) -> Unit) {
    var nombre by remember { mutableStateOf(cat?.nombre.orEmpty()) }
    var color by remember { mutableStateOf(cat?.color ?: Acento.POR_DEFECTO.argb) }
    var icono by remember { mutableStateOf(cat?.icono ?: "more") }
    var confirmarBorrado by remember { mutableStateOf(false) }
    DialogoNummia(
        onDismissRequest = alCerrar,
        title = { Text(if (cat == null) "Nueva categoría" else "Editar categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) { // la hoja ya se desplaza: aquí un scroll propio cerraría la app
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                Text("Icono", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.xs), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria.claves.forEach { k ->
                        val elegido = icono == k
                        Box(
                            Modifier.size(40.dp)
                                .background(if (elegido) Color(color).copy(alpha = 0.25f) else Color.Transparent, CircleShape)
                                .clickable { icono = k }
                                .semantics { contentDescription = "Icono $k"; selected = elegido },
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria.de(k), null,
                                tint = if (elegido) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelMedium)
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
        confirmButton = { Button(enabled = nombre.isNotBlank(), onClick = { alGuardar(nombre.trim(), color, icono) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
