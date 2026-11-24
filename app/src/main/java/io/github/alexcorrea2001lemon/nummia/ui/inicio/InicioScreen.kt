package io.github.alexcorrea2001lemon.nummia.ui.inicio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.actividad
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearPorMil
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar
import io.github.alexcorrea2001lemon.nummia.ui.comun.EncabezadoSeccion
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImporteAnimado
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMesMinuscula
import io.github.alexcorrea2001lemon.nummia.ui.nav.compartido
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InicioScreen(
    vm: InicioViewModel,
    alAbrirGasto: (String) -> Unit,
    alAbrirIngreso: (String) -> Unit,
    alVerTodos: () -> Unit,
    alBuscar: () -> Unit,
    alAjustes: () -> Unit,
    alAnadirGasto: () -> Unit,
    relleno: PaddingValues,
    alDrive: () -> Unit = {},
    alPresupuestos: () -> Unit = {},
    alEstadisticas: (io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst, java.time.YearMonth) -> Unit = { _, _ -> },
    alPendientes: () -> Unit = {},
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val presupuestos by vm.presupuestos.collectAsStateWithLifecycle()
    var confirmando by remember { mutableStateOf<SueldoPendiente?>(null) }
    var cambiandoPago by remember { mutableStateOf<PagoPendiente?>(null) }
    var cambiandoCobro by remember { mutableStateOf<CobroPendiente?>(null) }
    var eligiendoMes by rememberSaveable { mutableStateOf(false) }

    val haptica = io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica()
    val umbral = with(androidx.compose.ui.platform.LocalDensity.current) { 96.dp.toPx() }
    // En pantallas grandes (tablet, plegable abierto), dos columnas.
    val columnas = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= io.github.alexcorrea2001lemon.nummia.ui.nav.ANCHO_DOS_COLUMNAS) 2 else 1
    val todaLaFila = StaggeredGridItemSpan.FullLine
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columnas),
        // Deslizar a los lados cambia de mes (como en el Calendario).
        modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    if (total > umbral) { haptica(); vm.mesAnterior() } else if (total < -umbral) { haptica(); vm.mesSiguiente() }
                },
            ) { _, d -> total += d }
        },
        contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, bottom = relleno.calculateBottomPadding() + 96.dp),
        verticalItemSpacing = Espacio.l,
        horizontalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        item(span = todaLaFila) {
            Cabecera(e, vm::mesAnterior, vm::mesSiguiente, { eligiendoMes = true }, alBuscar, alAjustes, vm::irAHoy) {
                io.github.alexcorrea2001lemon.nummia.ui.drive.IconoEstadoDrive(alDrive)
            }
        }
        item {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            TarjetaBalance(
                e, vm::alternarDiscreto,
                alRevelar = {
                    // Modo discreto: mantener pulsado + huella muestra los importes hasta salir de la app
                    ctx.actividad()?.let { act ->
                        io.github.alexcorrea2001lemon.nummia.ui.bloqueo.Bloqueo.pedir(act, "Mostrar los importes", alConseguir = { haptica(); vm.mostrarImportes() }, alFallar = {})
                    }
                },
            ) { alEstadisticas(io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst.MES, e.mes) }
        }
        item { Indicadores(e) { alEstadisticas(io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst.MES, e.mes) } }
        e.prevision?.takeIf { !e.vacio }?.let { p -> item("prevision") { TarjetaPrevision(p, e.discreto, Modifier.animateItem()) } }

        if (presupuestos.isNotEmpty()) {
            item("presupuestos") {
                Tarjeta(modifier = Modifier.animateItem().clickable(onClick = alPresupuestos)) {
                    Text("Presupuestos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    presupuestos.forEach { (nombre, p) ->
                        io.github.alexcorrea2001lemon.nummia.ui.presupuestos.BarraPresupuesto(p, nombre, e.discreto)
                    }
                }
            }
        }

        if (e.sueldosPendientes.isNotEmpty() || e.teDeben.isNotEmpty() || e.pagosPendientes.isNotEmpty() || e.cobrosPendientes.isNotEmpty()) {
            item(span = todaLaFila) { EncabezadoSeccion("Pendientes") }
            items(e.sueldosPendientes, key = { "s" + it.id }) { s ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                        Avatar("€", LocalColoresNummia.current.ingresoFondo, LocalColoresNummia.current.ingreso, Iconos.Euro)
                        Column(Modifier.weight(1f)) {
                            Text("Sueldo de ${nombreMesMinuscula(java.time.YearMonth.from(s.fecha))} previsto", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(formatearEuros(s.importe, discreto = e.discreto), style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
                        }
                        Button(onClick = { confirmando = s }) { Text("Confirmar") }
                    }
                }
            }
            items(e.cobrosPendientes, key = { "c" + it.id }) { p ->
                Tarjeta(modifier = Modifier.animateItem()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                        Avatar("€", LocalColoresNummia.current.ingresoFondo, LocalColoresNummia.current.ingreso, Iconos.Repetir)
                        Column(Modifier.weight(1f)) {
                            Text(p.concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${formatearEuros(p.importe, discreto = e.discreto)} · se cobra cada mes, día ${p.fecha.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Button(onClick = { haptica(); vm.confirmarCobro(p.id, p.importe) }) { Text("Cobrado") }
                        OutlinedButton(onClick = { cambiandoCobro = p }) { Text("Otro importe") }
                        TextButton(onClick = { vm.omitirCobro(p.id) }) { Text("Este mes no") }
                    }
                }
            }
            items(e.pagosPendientes, key = { "p" + it.id }) { p ->
                Tarjeta(modifier = Modifier.animateItem()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                        Avatar("", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Iconos.Repetir)
                        Column(Modifier.weight(1f)) {
                            Text(p.concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                "${formatearEuros(p.importe, discreto = e.discreto)} · pago mensual, día ${p.fecha.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Button(onClick = { vm.confirmarPago(p.id, p.importe) }) { Text("Confirmar") }
                        OutlinedButton(onClick = { cambiandoPago = p }) { Text("Otro importe") }
                        TextButton(onClick = { vm.omitirPago(p.id) }) { Text("Este mes no") }
                    }
                }
            }
            if (e.teDeben.isNotEmpty()) {
                item("deben") {
                    Tarjeta(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = alPendientes)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Avatar("", LocalColoresNummia.current.avisoFondo, LocalColoresNummia.current.aviso, Iconos.Persona)
                            Column(Modifier.weight(1f)) {
                                Text("Te deben", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(e.teDeben.joinToString(" · ") { "${it.first} ${formatearEuros(it.second, discreto = e.discreto)}" }, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
                            }
                            Text(formatearEuros(e.teDeben.sumOf { it.second }, discreto = e.discreto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LocalColoresNummia.current.aviso)
                        }
                    }
                }
            }
        }

        if (e.vacio && !e.cargando) {
            item(span = todaLaFila) {
                EstadoVacio(
                    Iconos.Bolsa,
                    "Empieza ${if (e.esMesActual) "tu mes" else nombreMesMinuscula(e.mes)}",
                    "Apunta un gasto o un ingreso y Nummia hará las cuentas por ti.",
                    accion = "Añadir gasto",
                    alPulsar = alAnadirGasto,
                )
            }
        }

        if (e.top.isNotEmpty()) item { DondeMas(e) { alEstadisticas(io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst.DONDE, e.mes) } }

        item {
            Tarjeta(Modifier.clip(MaterialTheme.shapes.medium).clickable { alEstadisticas(io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst.ANIO, e.mes) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Ahorrado en ${e.mes.year}", style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
                        ImporteAnimado(
                            e.ahorradoAnio, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (e.ahorradoAnio >= 0) LocalColoresNummia.current.ingreso else LocalColoresNummia.current.gasto,
                            discreto = e.discreto,
                        )
                    }
                }
            }
        }

        if (e.ultimos.isNotEmpty()) {
            item(span = todaLaFila) { EncabezadoSeccion("Últimos movimientos", accion = "Ver todos", alPulsar = alVerTodos) }
            item {
                Tarjeta(relleno = false) {
                    e.ultimos.forEach { f ->
                        FilaMovimiento(
                            f, e.discreto,
                            alPulsar = { if (f.esGasto) alAbrirGasto(f.id) else alAbrirIngreso(f.id) },
                            modifier = if (f.esGasto) Modifier.compartido("gasto-${f.id}") else Modifier,
                        )
                    }
                }
            }
        }
    }

    if (eligiendoMes) {
        io.github.alexcorrea2001lemon.nummia.ui.comun.SelectorMes(e.mes, vm.hoy(), alCerrar = { eligiendoMes = false }) { vm.irAMes(it); eligiendoMes = false }
    }
    cambiandoCobro?.let { p ->
        DialogoImporte("${p.concepto}: ¿cuánto has cobrado este mes?", p.importe, alCerrar = { cambiandoCobro = null }) { vm.confirmarCobro(p.id, it); cambiandoCobro = null }
    }
    cambiandoPago?.let { p ->
        DialogoImporte("${p.concepto}: ¿cuánto has pagado este mes?", p.importe, alCerrar = { cambiandoPago = null }) { vm.confirmarPago(p.id, it); cambiandoPago = null }
    }
    confirmando?.let { s -> DialogoConfirmarSueldo(s, alCerrar = { confirmando = null }) { fecha, importe -> vm.confirmarSueldo(s.id, fecha, importe); confirmando = null } }
}

@Composable
private fun Cabecera(e: InicioEstado, anterior: () -> Unit, siguiente: () -> Unit, hoy: () -> Unit, buscar: () -> Unit, ajustes: () -> Unit, irAHoy: () -> Unit, estadoDrive: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
        // El mes ocupa el espacio que sobra: los iconos de la derecha siempre tienen el mismo tamaño.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = anterior, modifier = Modifier.size(40.dp)) { Icon(Iconos.ChevronIzq, "Mes anterior") }
            TextButton(onClick = hoy, modifier = Modifier.weight(1f, fill = false), contentPadding = PaddingValues(horizontal = Espacio.xs)) {
                io.github.alexcorrea2001lemon.nummia.ui.comun.TextoMes(
                    e.mes, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = siguiente, modifier = Modifier.size(40.dp)) { Icon(Iconos.ChevronDer, "Mes siguiente") }
        }
        io.github.alexcorrea2001lemon.nummia.ui.comun.ChipHoy(e.mes != java.time.YearMonth.now(), irAHoy)
        estadoDrive()
        IconButton(onClick = ajustes) { Icon(Iconos.Ajustes, "Ajustes") }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun TarjetaBalance(e: InicioEstado, alternarDiscreto: () -> Unit, alRevelar: () -> Unit, alPulsar: () -> Unit) {
    val c = LocalColoresNummia.current
    Tarjeta(
        Modifier.clip(MaterialTheme.shapes.medium).combinedClickable(
            onClick = alPulsar,
            onLongClick = if (e.discreto) alRevelar else null,
            onLongClickLabel = if (e.discreto) "Mostrar los importes con tu huella" else null,
        ),
        fondo = c.heroe,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Balance del mes", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.weight(1f))
            IconButton(onClick = alternarDiscreto) {
                Icon(if (e.discreto) Iconos.OjoTachado else Iconos.Ojo, if (e.discreto) "Mostrar importes" else "Ocultar importes", tint = c.textoSuave)
            }
        }
        ImporteAnimado(
            e.resumen.balance,
            MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = if (e.resumen.balance >= 0) c.ingreso else c.gasto,
            conSigno = true,
            discreto = e.discreto,
        )
        val v = e.variacion
        val anterior = nombreMesMinuscula(e.mes.minusMonths(1))
        val texto = when {
            v == null -> null
            v == 0 -> "Has gastado lo mismo que en $anterior"
            v > 0 -> "Has gastado un ${formatearPorMil(v)} más que en $anterior"
            else -> "Has gastado un ${formatearPorMil(-v)} menos que en $anterior"
        }
        AnimatedVisibility(texto != null) {
            Text(texto.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
        }
        AnimatedVisibility(e.discreto) {
            Text("Mantén pulsado para verlos con tu huella", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        }
        // Sin el sueldo confirmado el mes parece en negativo: se enseña cómo quedará.
        AnimatedVisibility(e.sueldoPrevistoMes > 0) {
            Text(
                "Con el sueldo previsto: ${formatearEuros(e.resumen.balance + e.sueldoPrevistoMes, conSigno = true, discreto = e.discreto)}",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                color = if (e.resumen.balance + e.sueldoPrevistoMes >= 0) c.ingreso else c.gasto,
            )
        }
    }
}

/** «A fin de mes te quedarán unos X €», con el desglose (se abre al tocar). */
@Composable
private fun TarjetaPrevision(p: io.github.alexcorrea2001lemon.nummia.domain.calculo.PrevisionMes, discreto: Boolean, modifier: Modifier = Modifier) {
    val c = LocalColoresNummia.current
    var abierta by remember { mutableStateOf(false) }
    Tarjeta(modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = if (abierta) "Ocultar desglose" else "Ver desglose") { abierta = !abierta }) {
        Text("A fin de mes te quedarán unos", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
        Text(
            formatearEuros(p.quedara, conSigno = p.quedara < 0, discreto = discreto),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
            color = if (p.quedara < 0) c.gasto else c.ingreso,
        )
        AnimatedVisibility(abierta) {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs), modifier = Modifier.padding(top = Espacio.s)) {
                @Composable fun linea(t: String, v: Long, signo: String) = Row {
                    Text(t, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.weight(1f))
                    Text(signo + formatearEuros(kotlin.math.abs(v), discreto = discreto), style = MaterialTheme.typography.bodyMedium)
                }
                linea("Lo que tienes este mes", p.ahora, if (p.ahora < 0) "−" else "")
                if (p.porCobrar > 0) linea("Por cobrar (sueldo y cobros fijos)", p.porCobrar, "+")
                if (p.porPagar > 0) linea("Pagos fijos pendientes", p.porPagar, "−")
                if (p.ritmo > 0) linea("Si sigues gastando a este ritmo", p.ritmo, "−")
            }
        }
        if (!abierta) Text("Toca para ver de dónde sale", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
    }
}

@Composable
private fun Indicadores(e: InicioEstado, alPulsar: () -> Unit) {
    val c = LocalColoresNummia.current
    // Las cuatro cifras en una sola tarjeta: menos bordes y se ve antes lo de debajo.
    Tarjeta(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = alPulsar), relleno = false) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Indicador("Ingresos", e.resumen.ingresos, c.ingreso, e.discreto, Modifier.weight(1f))
            VerticalDivider(color = c.borde)
            Indicador("Pagado", e.resumen.pagado, MaterialTheme.colorScheme.onSurface, e.discreto, Modifier.weight(1f))
        }
        HorizontalDivider(color = c.borde)
        Row(Modifier.height(IntrinsicSize.Min)) {
            Indicador("Devoluciones", e.resumen.reembolsos, MaterialTheme.colorScheme.onSurface, e.discreto, Modifier.weight(1f))
            VerticalDivider(color = c.borde)
            Indicador("Gasto real", e.resumen.gastoReal, c.gasto, e.discreto, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Indicador(titulo: String, valor: Long, color: androidx.compose.ui.graphics.Color, discreto: Boolean, modifier: Modifier) {
    Column(modifier.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        ImporteAnimado(valor, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = color, discreto = discreto)
    }
}

@Composable
private fun DondeMas(e: InicioEstado, alPulsar: () -> Unit) {
    Tarjeta(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = alPulsar)) {
        Text("Dónde más has gastado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        e.top.forEach { t ->
            val ancho by animateFloatAsState(t.fraccion, spring(dampingRatio = 0.8f, stiffness = 200f), label = "barra")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = Espacio.xs)) {
                Row {
                    Text(t.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(formatearEuros(t.importe, discreto = e.discreto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                    Box(Modifier.fillMaxWidth(ancho).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

@Composable
private fun DialogoImporte(titulo: String, inicial: Long, alCerrar: () -> Unit, alAceptar: (Long) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(formatearEuros(inicial).removeSuffix("€").trim()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it; error = null },
                    label = { Text("Importe") }, suffix = { Text("€") }, singleLine = true,
                    isError = error != null, supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Text("A partir de ahora será el precio de este pago.", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
            }
        },
        confirmButton = {
            Button(onClick = {
                leerImporte(texto).fold({ alAceptar(it) }, { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo ?: "Importe no válido" })
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoConfirmarSueldo(s: SueldoPendiente, alCerrar: () -> Unit, alConfirmar: (java.time.LocalDate, Long) -> Unit) {
    val ym = java.time.YearMonth.from(s.fecha)
    val dias = (27..ym.lengthOfMonth()).toList()
    var dia by rememberSaveable { mutableStateOf(dias.last()) }
    var texto by rememberSaveable { mutableStateOf(formatearEuros(s.importe).removeSuffix(" €")) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Confirmar sueldo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Text("¿Qué día lo cobraste?", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    dias.forEach { d -> FilterChip(selected = d == dia, onClick = { dia = d }, label = { Text("$d") }) }
                }
                OutlinedTextField(
                    value = texto, onValueChange = { texto = it; error = null },
                    label = { Text("Importe cobrado") }, suffix = { Text("€") }, singleLine = true,
                    isError = error != null, supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                leerImporte(texto).fold({ alConfirmar(ym.atDay(dia), it) }, { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo ?: "Importe no válido" })
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
