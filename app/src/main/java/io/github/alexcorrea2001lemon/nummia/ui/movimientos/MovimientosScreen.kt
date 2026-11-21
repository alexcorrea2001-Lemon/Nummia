package io.github.alexcorrea2001lemon.nummia.ui.movimientos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
    val alcance = rememberCoroutineScope()
    val haptica = rememberHaptica()
    var verFiltros by rememberSaveable { mutableStateOf(false) }
    val c = LocalColoresNummia.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = relleno.calculateBottomPadding() + 150.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(horizontal = Espacio.l, vertical = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Movimientos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = alAjustes) { Icon(Iconos.Ajustes, "Ajustes") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = e.filtros.texto, onValueChange = vm::texto, singleLine = true,
                            placeholder = { Text("Buscar por nombre, tienda, importe o fecha") },
                            leadingIcon = { Icon(Iconos.Buscar, null) },
                            trailingIcon = { if (e.filtros.texto.isNotEmpty()) IconButton(onClick = { vm.texto("") }) { Icon(Iconos.Cerrar, "Borrar búsqueda") } },
                            shape = RoundedCornerShape(28.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        BadgedBox(badge = { if (e.filtros.avanzados > 0) Badge { Text("${e.filtros.avanzados}") } }) {
                            IconButton(onClick = { verFiltros = true }) { Icon(Iconos.Filtro, "Filtros y orden") }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Pestana.entries.forEach { p ->
                            FilterChip(selected = e.filtros.pestana == p, onClick = { vm.pestana(p) }, label = { Text(p.nombre) })
                        }
                    }
                    if (e.buscandoEnTodo) {
                        Text("Mostrando todos los meses", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = vm::mesAnterior) { Icon(Iconos.ChevronIzq, "Mes anterior") }
                            Text(nombreMes(e.mes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = vm::mesSiguiente) { Icon(Iconos.ChevronDer, "Mes siguiente") }
                            Spacer(Modifier.weight(1f))
                            Text("Orden: ${e.filtros.orden.nombre.lowercase()}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                    }
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
                        Row(Modifier.padding(horizontal = Espacio.l + Espacio.s, vertical = Espacio.xs)) {
                            Text(fechaCabecera(g.fecha, e.hoy), style = MaterialTheme.typography.labelLarge, color = c.textoSuave, modifier = Modifier.weight(1f))
                            Text(formatearEuros(g.total, conSigno = true, discreto = e.discreto), style = MaterialTheme.typography.labelLarge, color = c.textoSuave)
                        }
                    }
                }
                items(g.filas, key = { (if (it.esGasto) "g" else "i") + it.id }) { f ->
                    FilaDeslizable(
                        f, e.discreto,
                        modifier = Modifier.animateItem().padding(horizontal = Espacio.l),
                        alPulsar = { if (f.esGasto) alAbrirGasto(f.id) else alAbrirIngreso(f.id) },
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

        AnimatedVisibility(e.cuenta > 0, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = relleno.calculateBottomPadding() + 84.dp)) {
            Surface(
                shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 3.dp, shadowElevation = 6.dp, modifier = Modifier.padding(horizontal = Espacio.l),
            ) {
                Row(Modifier.padding(horizontal = Espacio.xl, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(Espacio.xl)) {
                    Text("${e.cuenta} movimientos", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    Text("Gasto real ${formatearEuros(e.totalGasto, discreto = e.discreto)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (verFiltros) {
        ModalBottomSheet(onDismissRequest = { verFiltros = false }) {
            HojaFiltros(e.filtros, vm::orden, alAplicar = { cr, mi, ma -> vm.avanzados(cr, mi, ma); verFiltros = false }, alLimpiar = { vm.limpiarAvanzados(); verFiltros = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaDeslizable(
    f: FilaMov,
    discreto: Boolean,
    modifier: Modifier,
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
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
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
            FilaMovimiento(f, discreto, alPulsar)
        }
    }
}

@Composable
private fun HojaFiltros(f: Filtros, alOrden: (Orden) -> Unit, alAplicar: (Boolean?, Long?, Long?) -> Unit, alLimpiar: () -> Unit) {
    var conReembolso by remember { mutableStateOf(f.conReembolso) }
    var minimo by remember { mutableStateOf(f.minimo?.let { formatearEuros(it).removeSuffix(" €") }.orEmpty()) }
    var maximo by remember { mutableStateOf(f.maximo?.let { formatearEuros(it).removeSuffix(" €") }.orEmpty()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = Espacio.xl).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        Text("Ordenar por", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Orden.entries.forEach { o -> FilterChip(selected = f.orden == o, onClick = { alOrden(o) }, label = { Text(o.nombre) }) }
        }
        Text("Reembolsos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
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
            Button(onClick = { alAplicar(conReembolso, leerImporte(minimo).getOrNull(), leerImporte(maximo).getOrNull()) }) { Text("Aplicar") }
        }
        Spacer(Modifier.height(Espacio.s))
    }
}
