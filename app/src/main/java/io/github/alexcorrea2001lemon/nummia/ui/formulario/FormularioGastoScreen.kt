package io.github.alexcorrea2001lemon.nummia.ui.formulario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.reloj.RelojSistema
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioGastoScreen(
    vm: FormularioGastoViewModel, snackbar: SnackbarHostState,
    alEditado: (io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity) -> Unit = {}, alCerrar: () -> Unit,
) {
    val f by vm.form.collectAsStateWithLifecycle()
    val personas by vm.personas.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val foco = remember { FocusRequester() }
    val focoConcepto = remember { FocusRequester() }
    val haptica = rememberHaptica()
    var nuevaPersona by rememberSaveable { mutableStateOf(false) }
    val c = LocalColoresNummia.current
    val hoy = remember { RelojSistema().hoy() }
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val escaner = rememberEscanerTicket { r ->
        when (r) {
            is ResultadoEscaneo.Texto -> alcance.launch {
                val t = vm.aplicarTicket(r.texto)
                snackbar.showSnackbar(
                    when {
                        t.vacio -> "No he podido leer el ticket. Prueba con más luz o apúntalo a mano."
                        t.importe == null -> "Ticket leído, pero no he encontrado el importe: escríbelo tú."
                        else -> "Ticket leído. Revisa el importe, la tienda y la fecha antes de guardar."
                    },
                )
            }
            is ResultadoEscaneo.Error -> alcance.launch { snackbar.showSnackbar(r.motivo) }
            ResultadoEscaneo.Cancelado -> Unit
        }
    }

    LaunchedEffect(Unit) { if (!f.editando) runCatching { foco.requestFocus() } }
    LaunchedEffect(Unit) {
        vm.eventos.collect { ev ->
            if (ev is EventoForm.Guardado) {
                haptica()
                if (ev.otro) { snackbar.showSnackbar("Gasto guardado"); runCatching { foco.requestFocus() } } else { ev.antes?.let(alEditado); alCerrar() }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (f.editando) "Editar gasto" else if (f.meLoDevuelven && f.importe.isEmpty()) "Pago para otra persona" else "Nuevo gasto") },
                navigationIcon = { IconButton(onClick = alCerrar) { Icon(Iconos.Cerrar, "Cerrar") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, c.borde)) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(Espacio.l), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    if (!f.editando) {
                        OutlinedButton(onClick = { vm.guardar(otro = true) }, enabled = !f.guardando, modifier = Modifier.weight(1f)) { Text("Guardar y otro") }
                    }
                    Button(onClick = { vm.guardar(otro = false) }, enabled = !f.guardando, modifier = Modifier.weight(1f)) { Text("Guardar", fontWeight = FontWeight.Bold) }
                }
            }
        },
    ) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal = Espacio.l, vertical = Espacio.s),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            if (!f.editando) {
                OutlinedButton(onClick = escaner.abrir, enabled = !escaner.ocupado, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (escaner.ocupado) {
                        androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Leyendo el ticket…")
                    } else {
                        Icon(Iconos.Camara, null, Modifier.size(20.dp))
                        Text("  Escanear ticket")
                    }
                }
            }
            CampoImporteGrande(f.importe, vm::importe, f.errorImporte, foco) { runCatching { focoConcepto.requestFocus() } }

            OutlinedTextField(
                f.concepto, vm::concepto, label = { Text("Concepto") }, placeholder = { Text("Comida, ropa, regalo…") },
                singleLine = true, isError = f.errorConcepto != null, supportingText = f.errorConcepto?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focoConcepto),
            )
            OutlinedTextField(
                f.tienda, vm::tienda, label = { Text("Tienda o lugar") }, placeholder = { Text("Supermercado, cafetería…") },
                singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            SelectorFecha(f.fecha, hoy, vm::fecha)

            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Text("Categoría (opcional)", style = MaterialTheme.typography.bodySmall, color = c.textoSuave, modifier = Modifier.padding(start = Espacio.xs))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    FilterChip(selected = f.categoriaId == null, onClick = { vm.categoria(null) }, label = { Text("Ninguna") })
                    categorias.forEach { cat ->
                        FilterChip(
                            selected = f.categoriaId == cat.id, onClick = { haptica(); vm.categoria(cat.id) }, label = { Text(cat.nombre) },
                            leadingIcon = {
                                Icon(
                                    io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria.de(cat.icono), null,
                                    tint = androidx.compose.ui.graphics.Color(cat.color), modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, if (f.meLoDevuelven) MaterialTheme.colorScheme.primary else c.borde),
            ) {
                Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lo pago yo, pero me lo devuelven", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Aparecerá en «Te deben» hasta que te paguen", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                        Switch(checked = f.meLoDevuelven, onCheckedChange = vm::meLoDevuelven)
                    }
                    AnimatedVisibility(f.meLoDevuelven) {
                        Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            Text("¿Quién?", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                                personas.forEach { pe -> FilterChip(selected = f.personaId == pe.id, onClick = { vm.persona(pe.id) }, label = { Text(pe.nombre) }) }
                                FilterChip(selected = false, onClick = { nuevaPersona = true }, label = { Text("+ Nueva persona") })
                            }
                            OutlinedTextField(
                                f.esperado, vm::esperado, label = { Text("¿Cuánto te van a dar?") },
                                placeholder = { Text(f.importe.ifEmpty { "Lo mismo que has pagado" }) }, suffix = { Text("€") },
                                singleLine = true, isError = f.errorEsperado != null, supportingText = f.errorEsperado?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                            )
                            val pagado = io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion(f.importe).getOrNull()
                            val esperado = leerImporte(f.esperado).getOrNull()
                            if (pagado != null && esperado != null && esperado > pagado) {
                                Text("+${formatearEuros(esperado - pagado)} a tu favor cuando te paguen", color = c.ingreso, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!f.editando) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("¿Ya te lo han dado?", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Switch(checked = f.yaDevuelto, onCheckedChange = vm::yaDevuelto)
                                }
                            }
                        }
                    }
                }
            }

            run {
                Surface(
                    shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (f.cadaMes) MaterialTheme.colorScheme.primary else c.borde),
                ) {
                    Row(Modifier.padding(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Se paga cada mes", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Cada mes aparecerá en Inicio el día ${f.fecha.dayOfMonth} para confirmarlo o cambiar el importe. Lo verás en Ajustes → Pagos y cobros fijos",
                                style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                            )
                        }
                        Switch(checked = f.cadaMes, onCheckedChange = vm::cadaMes)
                    }
                }
            }

            OutlinedTextField(
                f.notas, vm::notas, label = { Text("Notas (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }

    if (nuevaPersona) DialogoNombre("Nueva persona", alCerrar = { nuevaPersona = false }) { vm.nuevaPersona(it); nuevaPersona = false }
}
