package io.github.alexcorrea2001lemon.nummia.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Etiqueta
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImporteAnimado
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaLarga
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreTipo
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

/** Ficha de un ingreso: se ve primero; para cambiarlo se toca «Editar». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleIngresoScreen(
    vm: DetalleIngresoViewModel,
    snackbar: SnackbarHostState,
    alVolver: () -> Unit,
    alEditar: (String) -> Unit,
) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()
    val haptica = rememberHaptica()
    val c = LocalColoresNummia.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ingreso") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Iconos.Atras, "Volver") } },
                actions = { IconButton(onClick = { alEditar(vm.id) }, enabled = e.ingreso != null) { Icon(Iconos.Lapiz, "Editar") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { p ->
        val i = e.ingreso
        if (i == null) {
            if (!e.cargando) EstadoVacio(Iconos.Info, "Este ingreso ya no existe", "Puede que lo hayas borrado.", Modifier.padding(p))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(p),
            contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Box(Modifier.size(72.dp).background(c.ingresoFondo, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Iconos.Euro, null, tint = c.ingreso, modifier = Modifier.size(34.dp))
                    }
                    Text(i.concepto, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(fechaLarga(i.fecha), style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Etiqueta(i.codigo, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
                        if (i.estado == Estado.PREVISTO) Etiqueta("Previsto", MaterialTheme.colorScheme.surfaceVariant, c.textoSuave)
                    }
                }
            }

            item {
                Tarjeta(fondo = c.heroe) {
                    Text(if (i.estado == Estado.PREVISTO) "Cobro previsto" else "Cobrado", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    ImporteAnimado(i.importe, MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), discreto = e.discreto)
                    if (i.estado == Estado.PREVISTO) {
                        Button(onClick = { haptica(); alEditar(i.id) }, modifier = Modifier.fillMaxWidth().padding(top = Espacio.s)) {
                            Text("Confirmar cobro", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Tarjeta {
                    Dato("Tipo", nombreTipo(i.tipo))
                    HorizontalDivider(color = c.borde)
                    Dato("Se cobra cada mes", if (i.ingresoFijoId != null) "Sí" else "No")
                    HorizontalDivider(color = c.borde)
                    Dato("Notas", i.notas.ifBlank { "—" })
                }
            }

            item {
                Tarjeta {
                    Text("Línea de tiempo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val formato = remember { SimpleDateFormat("d MMM yyyy · HH:mm", io.github.alexcorrea2001lemon.nummia.ui.comun.ES) }
                    val eventos = e.historial.take(30)
                    if (eventos.isEmpty()) Text("Sin cambios todavía.", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    eventos.forEachIndexed { n, h ->
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                val color = if (h.campo == "borrado") c.gasto else MaterialTheme.colorScheme.primary
                                Box(Modifier.padding(top = 5.dp).size(10.dp).background(color, CircleShape))
                                if (n < eventos.lastIndex) Box(Modifier.width(2.dp).weight(1f).background(c.borde))
                            }
                            Column(Modifier.padding(bottom = Espacio.m)) {
                                Text(textoHistorial(h.campo, h.antes, h.despues), style = MaterialTheme.typography.bodyMedium)
                                Text("${formato.format(Date(h.fecha))} · desde el ${if (h.origen.name == "ORDENADOR") "ordenador" else "móvil"}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    OutlinedButton(onClick = { haptica(); alEditar(i.id) }, modifier = Modifier.weight(1f)) {
                        Icon(Iconos.Lapiz, null, Modifier.size(18.dp)); Text("  Editar")
                    }
                    OutlinedButton(
                        onClick = {
                            vm.borrar {
                                alVolver()
                                alcance.launch {
                                    val r = snackbar.showSnackbar("Ingreso borrado", "Deshacer", duration = SnackbarDuration.Long)
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
}
