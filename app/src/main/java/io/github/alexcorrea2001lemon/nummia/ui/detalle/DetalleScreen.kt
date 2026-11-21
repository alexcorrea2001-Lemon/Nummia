package io.github.alexcorrea2001lemon.nummia.ui.detalle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Etiqueta
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImporteAnimado
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaLarga
import io.github.alexcorrea2001lemon.nummia.ui.nav.compartido
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    vm: DetalleViewModel,
    snackbar: SnackbarHostState,
    alVolver: () -> Unit,
    alEditar: (String) -> Unit,
    alAnadirReembolso: (String) -> Unit,
    alAbrirGasto: (String) -> Unit,
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    var borrandoReembolso by remember { mutableStateOf<ReembolsoEntity?>(null) }
    val c = LocalColoresNummia.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gasto") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Iconos.Atras, "Volver") } },
                actions = { IconButton(onClick = { alEditar(vm.id) }, enabled = e.gasto != null) { Icon(Iconos.Lapiz, "Editar") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { p ->
        val g = e.gasto
        if (g == null) {
            if (!e.cargando) EstadoVacio(Iconos.Info, "Este gasto ya no existe", "Puede que lo hayas borrado.", Modifier.padding(p))
            return@Scaffold
        }
        val gasto = g.gasto
        LazyColumn(
            Modifier.fillMaxSize().padding(p).compartido("gasto-${gasto.id}"),
            contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Text(gasto.concepto.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(gasto.concepto, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(
                        listOfNotNull(fechaLarga(gasto.fecha), gasto.categoriaId?.let(e.categorias::get)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Etiqueta(gasto.codigo, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
                        when {
                            gasto.estado == Estado.PREVISTO -> Etiqueta("Previsto", MaterialTheme.colorScheme.surfaceVariant, c.textoSuave)
                            e.real < 0 -> Etiqueta("+${formatearEuros(-e.real)} a tu favor", c.ingresoFondo, c.ingreso)
                            e.devuelto > 0 && e.real == 0L -> Etiqueta("Devuelto 100 %", c.ingresoFondo, c.ingreso)
                            e.devuelto > 0 -> Etiqueta("Devuelto ${(e.devuelto * 100 / gasto.importe.coerceAtLeast(1))} %", c.avisoFondo, c.aviso)
                        }
                    }
                }
            }

            item {
                Tarjeta(fondo = c.heroe) {
                    Text("Gasto real", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    ImporteAnimado(e.real, MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), discreto = e.discreto)
                    val fraccion by animateFloatAsState((e.devuelto.toFloat() / gasto.importe.coerceAtLeast(1)).coerceIn(0f, 1f), label = "devuelto")
                    Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
                        Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)))
                    }
                    Row(Modifier.padding(top = Espacio.xs)) {
                        Cifra("Pagado", formatearEuros(gasto.importe, discreto = e.discreto), Modifier.weight(1f))
                        Cifra("Devuelto", formatearEuros(e.devuelto, discreto = e.discreto), Modifier.weight(1f), c.ingreso)
                        Cifra("Falta", if (gasto.meLoDevuelven) formatearEuros(e.falta, discreto = e.discreto) else "—", Modifier.weight(1f), if (e.falta > 0) c.aviso else Color.Unspecified)
                    }
                    e.tardio?.let { Text("Actualizado: reembolso recibido el ${fechaCorta(it)}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave) }
                }
            }

            item {
                Tarjeta(relleno = false) {
                    Text("Reembolsos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = Espacio.l, top = Espacio.l))
                    if (g.activos.isEmpty()) {
                        Text("Aún no hay reembolsos.", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.padding(horizontal = Espacio.l))
                    }
                    g.activos.sortedBy { it.createdAt }.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Avatar("", c.ingresoFondo, c.ingreso, Iconos.FlechaAbajo)
                            Column(Modifier.weight(1f)) {
                                Text(listOfNotNull(r.codigo, r.personaId?.let(e.personas::get)).joinToString(" · "), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(r.fecha?.let(::fechaCorta) ?: "Sin fecha", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                            Text(formatearEuros(r.importe, conSigno = true, discreto = e.discreto), color = c.ingreso, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { borrandoReembolso = r }) { Icon(Iconos.Papelera, "Borrar reembolso ${r.codigo}", tint = c.textoSuave) }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(Espacio.l), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Button(onClick = { alAnadirReembolso(gasto.id) }, modifier = Modifier.weight(1f)) { Text("Añadir reembolso") }
                        val hayPendiente = if (gasto.meLoDevuelven) e.falta > 0 else e.real > 0
                        OutlinedButton(onClick = vm::devueltoEntero, enabled = hayPendiente, modifier = Modifier.weight(1f)) { Text("Devuelto entero") }
                    }
                }
            }

            item {
                Tarjeta {
                    Dato("Tienda", gasto.tienda.ifBlank { "—" })
                    HorizontalDivider(color = c.borde)
                    Dato("¿Me lo devuelven?", if (gasto.meLoDevuelven) "Sí" else "No")
                    if (gasto.meLoDevuelven) {
                        HorizontalDivider(color = c.borde)
                        Dato("Quién", gasto.personaId?.let(e.personas::get) ?: "—")
                        HorizontalDivider(color = c.borde)
                        Dato("Esperado", formatearEuros(gasto.importeEsperado ?: gasto.importe, discreto = e.discreto))
                    }
                    HorizontalDivider(color = c.borde)
                    Dato("Notas", gasto.notas.ifBlank { "—" })
                }
            }

            item {
                Tarjeta {
                    Text("Historial de cambios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val formato = remember { SimpleDateFormat("d MMM yyyy · HH:mm", io.github.alexcorrea2001lemon.nummia.ui.comun.ES) }
                    e.historial.take(30).forEach { h ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Box(Modifier.padding(top = 6.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Column {
                                Text(textoHistorial(h.campo, h.antes, h.despues), style = MaterialTheme.typography.bodyMedium)
                                Text("${formato.format(Date(h.fecha))} · desde el ${if (h.origen.name == "ORDENADOR") "ordenador" else "móvil"}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    OutlinedButton(onClick = { vm.duplicar { nuevo -> alAbrirGasto(nuevo) } }, modifier = Modifier.weight(1f)) {
                        Icon(Iconos.Duplicar, null, Modifier.size(18.dp)); Text("  Duplicar")
                    }
                    OutlinedButton(
                        onClick = {
                            vm.borrar {
                                alVolver()
                                alcance.launch {
                                    val r = snackbar.showSnackbar("Gasto borrado", "Deshacer", duration = SnackbarDuration.Long)
                                    if (r == SnackbarResult.ActionPerformed) vm.deshacerBorrado()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Icon(Iconos.Papelera, null, Modifier.size(18.dp), tint = c.gasto); Text("  Borrar", color = c.gasto) }
                }
            }
        }
    }

    borrandoReembolso?.let { r ->
        AlertDialog(
            onDismissRequest = { borrandoReembolso = null },
            title = { Text("¿Borrar el reembolso ${r.codigo}?") },
            text = { Text("El gasto real volverá a subir ${formatearEuros(r.importe)}.") },
            confirmButton = { TextButton(onClick = { vm.borrarReembolso(r.id); borrandoReembolso = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { borrandoReembolso = null }) { Text("Cancelar") } },
        )
    }
}

private fun textoHistorial(campo: String, antes: String?, despues: String?): String = when (campo) {
    "creado" -> "Creado${despues?.let { " ($it)" }.orEmpty()}"
    "borrado" -> "Borrado"
    "restaurado" -> "Restaurado"
    "reembolso" -> "Reembolso añadido: ${despues.orEmpty()}"
    "reembolso borrado" -> "Reembolso borrado: ${antes.orEmpty()}"
    else -> "${campo.replaceFirstChar { it.uppercase() }}: ${antes ?: "—"} → ${despues ?: "—"}"
}

@Composable
private fun Cifra(titulo: String, valor: String, modifier: Modifier, color: Color = Color.Unspecified) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        Text(valor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun Dato(titulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Espacio.xs)) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
