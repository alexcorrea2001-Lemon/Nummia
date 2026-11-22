package io.github.alexcorrea2001lemon.nummia.ui.movimientos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCabecera
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMesMinuscula
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.nav.compartido
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    vm: MovimientosViewModel,
    snackbar: SnackbarHostState,
    alAbrirGasto: (String) -> Unit,
    alAbrirIngreso: (String) -> Unit,
    alAjustes: () -> Unit,
    alAnadirGasto: () -> Unit,
    relleno: PaddingValues,
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val catsLista by vm.categorias.collectAsStateWithLifecycle()
    val persLista by vm.personas.collectAsStateWithLifecycle()
    val nombresCat = remember(catsLista) { catsLista.associate { it.id to it.nombre } }
    val nombresPer = remember(persLista) { persLista.associate { it.id to it.nombre } }
    val alcance = rememberCoroutineScope()
    val haptica = rememberHaptica()
    var verFiltros by rememberSaveable { mutableStateOf(false) }
    // El texto del buscador vive aquí: si dependiera del estado (que se calcula en segundo plano),
    // cada letra llegaría tarde y el campo «se comería» lo escrito.
    var busqueda by rememberSaveable { mutableStateOf("") }
    fun buscar(t: String) { busqueda = t; vm.texto(t) }
    LaunchedEffect(Unit) { if (busqueda.isNotEmpty()) vm.texto(busqueda) } // tras cerrar y volver a abrir la app
    var eligiendoMes by rememberSaveable { mutableStateOf(false) }
    val seleccion by vm.seleccion.collectAsStateWithLifecycle()
    val seleccionando = seleccion.isNotEmpty()
    var eligiendoCategoria by remember { mutableStateOf(false) }
    var eligiendoTienda by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(seleccionando) { vm.limpiarSeleccion() }
    val c = LocalColoresNummia.current
    LaunchedEffect(Unit) {
        io.github.alexcorrea2001lemon.nummia.ui.nav.Saltos.movimientos.collect { p ->
            if (p != null) { vm.pestana(p); io.github.alexcorrea2001lemon.nummia.ui.nav.Saltos.movimientos.value = null }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = relleno.calculateBottomPadding() + 96.dp),
            // Separación mínima: los movimientos de un mismo día forman un solo bloque.
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(horizontal = Espacio.l, vertical = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    if (seleccionando) {
                        // Barra de selección: mantén pulsado un movimiento para elegir varios
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(28.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = vm::limpiarSeleccion) { Icon(Iconos.Cerrar, "Dejar de seleccionar") }
                                Text(if (seleccion.size == 1) "1 elegido" else "${seleccion.size} elegidos", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                val hayGastos = seleccion.any { it.startsWith("g:") }
                                IconButton(onClick = { eligiendoCategoria = true }, enabled = hayGastos) { Icon(Iconos.Paleta, "Cambiar la categoría") }
                                IconButton(onClick = { eligiendoTienda = true }, enabled = hayGastos) { Icon(Iconos.Bolsa, "Cambiar la tienda") }
                                IconButton(onClick = {
                                    haptica()
                                    val n = seleccion.size
                                    val (g, i) = vm.borrarSeleccion()
                                    alcance.launch {
                                        val r = snackbar.showSnackbar("$n borrados", actionLabel = "Deshacer", duration = SnackbarDuration.Long)
                                        if (r == SnackbarResult.ActionPerformed) vm.deshacerVarios(g, i)
                                    }
                                }) { Icon(Iconos.Papelera, "Borrar los elegidos") }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Movimientos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = alAjustes) { Icon(Iconos.Ajustes, "Ajustes") }
                        }
                    }
                    // 1) Qué mes miras (como en Inicio). Al buscar, se busca en todos.
                    if (e.buscandoEnTodo) {
                        Text(
                            if (e.filtros.texto.isNotBlank()) "Buscando en todos los meses" else "Pendientes de todos los meses",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = Espacio.s),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = vm::mesAnterior) { Icon(Iconos.ChevronIzq, "Mes anterior") }
                            TextButton(onClick = { eligiendoMes = true }) {
                                io.github.alexcorrea2001lemon.nummia.ui.comun.TextoMes(e.mes, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = vm::mesSiguiente) { Icon(Iconos.ChevronDer, "Mes siguiente") }
                        }
                    }
                    // 2) Buscar y filtrar
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = busqueda, onValueChange = ::buscar, singleLine = true,
                            placeholder = { Text("Buscar", maxLines = 1) },
                            leadingIcon = { Icon(Iconos.Buscar, null) },
                            trailingIcon = { if (busqueda.isNotEmpty()) IconButton(onClick = { buscar("") }) { Icon(Iconos.Cerrar, "Borrar búsqueda") } },
                            supportingText = null,
                            shape = RoundedCornerShape(28.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            modifier = Modifier.weight(1f).semantics { contentDescription = "Buscar por nombre, tienda, importe o fecha" },
                        )
                        BadgedBox(badge = { if (e.filtros.avanzados > 0) Badge { Text("${e.filtros.avanzados}") } }) {
                            IconButton(onClick = { verFiltros = true }) { Icon(Iconos.Filtro, "Filtros y orden") }
                        }
                    }
                    // Filtros activos a la vista, y se quitan con un toque
                    val activos = listOfNotNull(
                        e.filtros.categoria?.let { id -> (nombresCat[id] ?: "Categoría") to { vm.quitar(categoria = true) } },
                        e.filtros.tienda?.let { t -> t to { vm.quitar(tienda = true) } },
                        e.filtros.persona?.let { id -> (nombresPer[id] ?: "Persona") to { vm.quitar(persona = true) } },
                    )
                    if (activos.isNotEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = Espacio.s), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            activos.forEach { (nombre, quitar) ->
                                androidx.compose.material3.InputChip(
                                    selected = true, onClick = { haptica(); quitar() }, label = { Text(nombre, maxLines = 1) },
                                    trailingIcon = { Icon(Iconos.Cerrar, "Quitar filtro $nombre", Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                }
            }
            // 3) Qué tipo ver: pestañas de ancho fijo, siempre visibles enteras
            item {
                SecondaryTabRow(
                    selectedTabIndex = e.filtros.pestana.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.padding(horizontal = Espacio.s),
                ) {
                    Pestana.entries.forEach { p ->
                        Tab(
                            selected = e.filtros.pestana == p, onClick = { haptica(); vm.pestana(p) },
                            text = { Text(p.nombre, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)) },
                        )
                    }
                }
                if (e.filtros.orden != Orden.FECHA) {
                    Text(
                        "Ordenado por ${e.filtros.orden.nombre.lowercase()}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                        modifier = Modifier.padding(horizontal = Espacio.xl, vertical = Espacio.s),
                    )
                }
            }

            if (e.grupos.all { it.filas.isEmpty() } && !e.cargando) {
                item {
                    if (e.filtros.texto.isNotBlank() || e.filtros.avanzados > 0) {
                        EstadoVacio(Iconos.Buscar, "Sin resultados", "Prueba con otra palabra, otro importe o quita algún filtro.")
                    } else {
                        EstadoVacio(Iconos.Bolsa, "Nada apuntado en ${nombreMesMinuscula(e.mes)}", "Cuando añadas gastos o ingresos aparecerán aquí, agrupados por día.", accion = "Añadir gasto", alPulsar = alAnadirGasto)
                    }
                }
            }

            e.grupos.forEach { g ->
                if (g.fecha != LocalDate.MIN) {
                    item(key = "d" + g.fecha) {
                        Row(Modifier.padding(start = Espacio.l + Espacio.s, end = Espacio.l + Espacio.s, top = Espacio.l, bottom = Espacio.xs)) {
                            Text(fechaCabecera(g.fecha, e.hoy, conAnio = e.buscandoEnTodo || g.fecha.year != e.hoy.year), style = MaterialTheme.typography.labelLarge, color = c.textoSuave, modifier = Modifier.weight(1f))
                            Text("Total del día ${formatearEuros(g.total, conSigno = true, discreto = e.discreto)}", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                        }
                    }
                }
                itemsIndexed(g.filas, key = { _, it -> (if (it.esGasto) "g" else "i") + it.id }) { i, f ->
                    val grande = io.github.alexcorrea2001lemon.nummia.ui.theme.LocalPreferenciasUi.current.radio.dp
                    val chica = 4.dp
                    val forma = RoundedCornerShape(
                        topStart = if (i == 0) grande else chica, topEnd = if (i == 0) grande else chica,
                        bottomStart = if (i == g.filas.lastIndex) grande else chica, bottomEnd = if (i == g.filas.lastIndex) grande else chica,
                    )
                    val clave = (if (f.esGasto) "g:" else "i:") + f.id
                    FilaDeslizable(
                        f, e.discreto, forma,
                        modifier = Modifier.animateItem().padding(horizontal = Espacio.l),
                        seleccionando = seleccionando, seleccionado = clave in seleccion,
                        alMantener = { haptica(); vm.alternar(f) },
                        alPulsar = { if (seleccionando) vm.alternar(f) else if (f.esGasto) alAbrirGasto(f.id) else alAbrirIngreso(f.id) },
                        alBorrar = {
                            haptica()
                            vm.borrar(f.id, f.esGasto)
                            alcance.launch {
                                val r = snackbar.showSnackbar(
                                    if (f.esGasto) "Gasto borrado" else "Ingreso borrado", actionLabel = "Deshacer",
                                    duration = SnackbarDuration.Long,
                                )
                                if (r == SnackbarResult.ActionPerformed) vm.deshacer(f.id, f.esGasto)
                            }
                        },
                        alRepetir = {
                            haptica()
                            vm.repetirHoy(f.id)
                            alcance.launch { snackbar.showSnackbar("«${f.titulo}» apuntado hoy") }
                        },
                    )
                }
            }
        }

    }

    if (eligiendoCategoria) {
        val categorias by vm.categorias.collectAsStateWithLifecycle()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { eligiendoCategoria = false },
            title = { Text("Categoría para los elegidos") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Ninguna", Modifier.fillMaxWidth().clickable { vm.categoriaSeleccion(null); eligiendoCategoria = false }.padding(vertical = Espacio.m))
                    categorias.forEach { cat ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.categoriaSeleccion(cat.id); eligiendoCategoria = false }.padding(vertical = Espacio.s),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                        ) {
                            io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar(cat.nombre, Color(cat.color).copy(alpha = 0.2f), Color(cat.color), io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria.de(cat.icono))
                            Text(cat.nombre)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { eligiendoCategoria = false }) { Text("Cancelar") } },
        )
    }
    if (eligiendoTienda) {
        io.github.alexcorrea2001lemon.nummia.ui.formulario.DialogoNombre("Tienda para los elegidos", alCerrar = { eligiendoTienda = false }) {
            vm.tiendaSeleccion(it); eligiendoTienda = false
        }
    }
    if (eligiendoMes) {
        io.github.alexcorrea2001lemon.nummia.ui.comun.SelectorMes(e.mes, vm.hoyMes(), alCerrar = { eligiendoMes = false }) { vm.irAMes(it); eligiendoMes = false }
    }
    if (verFiltros) {
        ModalBottomSheet(onDismissRequest = { verFiltros = false }) {
            val cats by vm.categorias.collectAsStateWithLifecycle()
            val pers by vm.personas.collectAsStateWithLifecycle()
            val tnds by vm.tiendas.collectAsStateWithLifecycle()
            HojaFiltros(
                e.filtros, vm::orden,
                categorias = cats.map { it.id to it.nombre }, personas = pers.map { it.id to it.nombre }, tiendas = tnds,
                alAplicar = { vm.avanzados(it); verFiltros = false }, alLimpiar = { vm.limpiarAvanzados(); verFiltros = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaDeslizable(
    f: FilaMov,
    discreto: Boolean,
    forma: androidx.compose.ui.graphics.Shape,
    modifier: Modifier,
    seleccionando: Boolean,
    seleccionado: Boolean,
    alMantener: () -> Unit,
    alPulsar: () -> Unit,
    alBorrar: () -> Unit,
    alRepetir: () -> Unit,
) {
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.EndToStart -> { alBorrar(); true }
                SwipeToDismissBoxValue.StartToEnd -> { alRepetir(); false }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    val c = LocalColoresNummia.current
    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = f.esGasto,
        gesturesEnabled = !seleccionando,
        modifier = modifier.clip(forma),
        backgroundContent = {
            val direccion = estado.dismissDirection
            val fondo by animateColorAsState(
                when (direccion) {
                    SwipeToDismissBoxValue.EndToStart -> c.gasto
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                },
                label = "fondo",
            )
            Row(
                Modifier.fillMaxSize().background(fondo).padding(horizontal = Espacio.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (direccion == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
            ) {
                if (direccion == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Iconos.Repetir, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    Text("  Repetir hoy", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                } else if (direccion == SwipeToDismissBoxValue.EndToStart) {
                    Text("Borrar  ", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Icon(Iconos.Papelera, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = if (f.esGasto) Modifier.compartido("gasto-${f.id}") else Modifier) {
            FilaMovimiento(f, discreto, alPulsar, alMantener = alMantener, seleccionado = seleccionado)
        }
    }
}

@Composable
private fun HojaFiltros(
    f: Filtros, alOrden: (Orden) -> Unit,
    categorias: List<Pair<String, String>>, personas: List<Pair<String, String>>, tiendas: List<String>,
    alAplicar: (Filtros) -> Unit, alLimpiar: () -> Unit,
) {
    var conReembolso by remember { mutableStateOf(f.conReembolso) }
    var minimo by remember { mutableStateOf(f.minimo?.let { formatearEuros(it).removeSuffix("€").trim() }.orEmpty()) }
    var maximo by remember { mutableStateOf(f.maximo?.let { formatearEuros(it).removeSuffix("€").trim() }.orEmpty()) }
    var categoria by remember { mutableStateOf(f.categoria) }
    var persona by remember { mutableStateOf(f.persona) }
    var tienda by remember { mutableStateOf(f.tienda) }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Text("Ordenar por", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Orden.entries.forEach { o -> FilterChip(selected = f.orden == o, onClick = { alOrden(o) }, label = { Text(o.nombre) }) }
        }
        if (categorias.isNotEmpty()) FilaOpciones("Categoría", categorias, categoria) { categoria = it }
        if (tiendas.isNotEmpty()) FilaOpciones("Tienda", tiendas.map { it to it }, tienda) { tienda = it }
        if (personas.isNotEmpty()) FilaOpciones("Persona", personas, persona) { persona = it }
        Text("Reembolsos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            FilterChip(selected = conReembolso == null, onClick = { conReembolso = null }, label = { Text("Todos") })
            FilterChip(selected = conReembolso == true, onClick = { conReembolso = true }, label = { Text("Con reembolso") })
            FilterChip(selected = conReembolso == false, onClick = { conReembolso = false }, label = { Text("Sin reembolso") })
        }
        Text("Importe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            OutlinedTextField(minimo, { minimo = it }, label = { Text("Mínimo") }, suffix = { Text("€") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(maximo, { maximo = it }, label = { Text("Máximo") }, suffix = { Text("€") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }
        Row(Modifier.fillMaxWidth().padding(bottom = Espacio.l), horizontalArrangement = Arrangement.spacedBy(Espacio.s, Alignment.End)) {
            TextButton(onClick = alLimpiar) { Text("Quitar filtros") }
            Button(onClick = {
                alAplicar(Filtros(
                    conReembolso = conReembolso, minimo = leerImporte(minimo).getOrNull(), maximo = leerImporte(maximo).getOrNull(),
                    categoria = categoria, persona = persona, tienda = tienda,
                ))
            }) { Text("Aplicar") }
        }
        Spacer(Modifier.height(Espacio.s))
    }
}

/** Una fila de chips deslizable: «Todas» + cada opción (id a nombre visible). */
@Composable
private fun FilaOpciones(titulo: String, opciones: List<Pair<String, String>>, elegida: String?, alElegir: (String?) -> Unit) {
    Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        FilterChip(selected = elegida == null, onClick = { alElegir(null) }, label = { Text("Todas") })
        opciones.forEach { (id, nombre) ->
            FilterChip(selected = elegida == id, onClick = { alElegir(if (elegida == id) null else id) }, label = { Text(nombre) })
        }
    }
}
