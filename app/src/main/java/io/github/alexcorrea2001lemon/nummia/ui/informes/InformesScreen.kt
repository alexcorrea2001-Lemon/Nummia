package io.github.alexcorrea2001lemon.nummia.ui.informes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.informes.Contenido
import io.github.alexcorrea2001lemon.nummia.data.informes.Formato
import io.github.alexcorrea2001lemon.nummia.ui.comun.EncabezadoSeccion
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private enum class Periodo(val nombre: String) { MES("Este mes"), MES_ANTERIOR("Mes anterior"), ANIO("Este año"), ANIO_ANTERIOR("Año anterior"), FECHAS("Elegir fechas") }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InformesScreen(vm: InformesViewModel, snackbar: SnackbarHostState, alAjustes: () -> Unit, relleno: PaddingValues) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val personas by vm.personas.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val ctx = LocalContext.current
    val hoy = remember { vm.hoy() }

    var periodo by rememberSaveable { mutableStateOf(Periodo.MES) }
    var desde by rememberSaveable { mutableStateOf(hoy.withDayOfMonth(1).toString()) }
    var hasta by rememberSaveable { mutableStateOf(hoy.toString()) }
    var contenidos by rememberSaveable { mutableStateOf(setOf(Contenido.RESUMEN, Contenido.TIENDAS, Contenido.CATEGORIAS, Contenido.MOVIMIENTOS)) }
    var persona by rememberSaveable { mutableStateOf<String?>(null) }
    var eligiendoFechas by remember { mutableStateOf(false) }

    // Un selector por formato: así el móvil sabe qué tipo de archivo se guarda.
    val alGuardar: (android.net.Uri?) -> Unit = { uri ->
        val a = e.ultimo
        if (uri != null && a != null) vm.guardarEn(uri, a)
    }
    val guardarPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Formato.PDF.mime), alGuardar)
    val guardarExcel = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Formato.EXCEL.mime), alGuardar)
    LaunchedEffect(e.mensaje) { e.mensaje?.let { vm.mensajeVisto(); snackbar.showSnackbar(it) } }

    fun rango(): Pair<LocalDate, LocalDate> = when (periodo) {
        Periodo.MES -> YearMonth.from(hoy).let { it.atDay(1) to it.atEndOfMonth() }
        Periodo.MES_ANTERIOR -> YearMonth.from(hoy).minusMonths(1).let { it.atDay(1) to it.atEndOfMonth() }
        Periodo.ANIO -> LocalDate.of(hoy.year, 1, 1) to LocalDate.of(hoy.year, 12, 31)
        Periodo.ANIO_ANTERIOR -> LocalDate.of(hoy.year - 1, 1, 1) to LocalDate.of(hoy.year - 1, 12, 31)
        Periodo.FECHAS -> LocalDate.parse(desde) to LocalDate.parse(hasta)
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, bottom = relleno.calculateBottomPadding() + Espacio.xxl),
        verticalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(start = Espacio.s, top = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
                Text("Informes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = alAjustes) { Icon(Iconos.Ajustes, "Ajustes") }
            }
        }
        item {
            Text(
                "Crea un resumen bonito para guardarlo, imprimirlo o enviarlo.",
                style = MaterialTheme.typography.bodyMedium, color = c.textoSuave, modifier = Modifier.padding(horizontal = Espacio.xs),
            )
        }
        // Formato: dos tarjetas grandes en vez de un interruptor pequeño
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Formato.entries.forEach { f ->
                    val sel = e.formato == f
                    val color = if (f == Formato.PDF) androidx.compose.ui.graphics.Color(0xFFF87171) else androidx.compose.ui.graphics.Color(0xFF4ADE80)
                    androidx.compose.material3.Surface(
                        onClick = { vm.formato(f) }, selected = sel,
                        shape = MaterialTheme.shapes.medium,
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else c.borde),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(Modifier.padding(Espacio.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar("", color.copy(alpha = 0.18f), color, if (f == Formato.PDF) Iconos.Informes else Iconos.Estadisticas)
                            Column {
                                Text(if (f == Formato.PDF) "PDF" else "Excel", fontWeight = FontWeight.SemiBold)
                                Text(if (f == Formato.PDF) "Para leer o imprimir" else "Para hacer cuentas", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                        }
                    }
                }
            }
        }
        // Atajos: tarjetas con icono, título y lo que incluyen
        item {
            val ym = YearMonth.from(hoy)
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                EncabezadoSeccion("En un toque")
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    AtajoInforme(
                        Iconos.Calendario, io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes(ym), "Resumen, categorías, tiendas y movimientos",
                        !e.generando, Modifier.weight(1f),
                    ) { vm.generar("Informe", ym.atDay(1), ym.atEndOfMonth(), setOf(Contenido.RESUMEN, Contenido.CATEGORIAS, Contenido.TIENDAS, Contenido.MOVIMIENTOS)) }
                    AtajoInforme(
                        Iconos.Estadisticas, "Año ${hoy.year}", "Resumen, mes a mes, categorías y tiendas",
                        !e.generando, Modifier.weight(1f),
                    ) { vm.generar("Informe", LocalDate.of(hoy.year, 1, 1), LocalDate.of(hoy.year, 12, 31), setOf(Contenido.RESUMEN, Contenido.MESES, Contenido.CATEGORIAS, Contenido.TIENDAS)) }
                }
                AtajoInforme(
                    Iconos.Persona, "Lo que me deben", "Quién te debe, cuánto y desde cuándo",
                    !e.generando, Modifier.fillMaxWidth(),
                ) { vm.generar("Lo que me deben", LocalDate.of(2000, 1, 1), hoy, setOf(Contenido.DEBEN)) }
            }
        }
        item {
            Tarjeta(modifier = Modifier.animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar("", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Iconos.Filtro)
                    Column {
                        Text("A tu medida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Elige el periodo y qué incluir", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    }
                }
                Text("Periodo", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Periodo.entries.forEach { p ->
                        FilterChip(selected = periodo == p, onClick = { periodo = p; if (p == Periodo.FECHAS) eligiendoFechas = true }, label = { Text(p.nombre) })
                    }
                }
                if (periodo == Periodo.FECHAS) {
                    TextButton(onClick = { eligiendoFechas = true }) {
                        Text("Del ${LocalDate.parse(desde).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }} al ${LocalDate.parse(hasta).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) }}")
                    }
                }
                Text("Qué incluir", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Contenido.entries.forEach { k ->
                        FilterChip(selected = k in contenidos, onClick = { contenidos = if (k in contenidos) contenidos - k else contenidos + k }, label = { Text(k.nombre) })
                    }
                }
                AnimatedVisibility(Contenido.DEBEN in contenidos && personas.isNotEmpty()) {
                    Column {
                        Text("Lo que me deben de", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            FilterChip(selected = persona == null, onClick = { persona = null }, label = { Text("Todos") })
                            personas.forEach { p -> FilterChip(selected = persona == p.id, onClick = { persona = p.id }, label = { Text(p.nombre) }) }
                        }
                    }
                }
                Button(
                    enabled = !e.generando && contenidos.isNotEmpty(),
                    onClick = { val (a, b) = rango(); vm.generar("Informe", a, b, contenidos, persona) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Crear informe") }
            }
        }
        if (e.generando) {
            item {
                Row(Modifier.fillMaxWidth().padding(Espacio.l), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("  Creando el informe…")
                }
            }
        }
        e.ultimo?.let { a ->
            item("ultimo") {
                Tarjeta(modifier = Modifier.animateItem()) {
                    Text(a.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    e.vistaPrevia?.let { img ->
                        Image(
                            img.asImageBitmap(), "Vista previa de la primera página",
                            Modifier.fillMaxWidth().heightIn(max = 520.dp).border(1.dp, c.borde), contentScale = ContentScale.Fit,
                        )
                    }
                    if (a.formato == Formato.EXCEL) {
                        e.vistaExcel?.let { VistaExcel(it) } ?: Text("Hoja de Excel lista. Se abre en Excel, Google Sheets o LibreOffice.", color = c.textoSuave)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Button(onClick = { ctx.startActivity(vm.intentCompartir(a)) }) { Text("Compartir") }
                        OutlinedButton(onClick = { (if (a.formato == Formato.PDF) guardarPdf else guardarExcel).launch(a.nombre) }) { Text("Guardar en el móvil") }
                        if (e.driveConectado) {
                            val subiendo = e.subiendo == a.nombre
                            val hecho = a.nombre in e.enDrive
                            OutlinedButton(enabled = !subiendo && !hecho, onClick = { vm.guardarEnDrive(a) }) {
                                if (subiendo) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("  Guardando…")
                                } else if (hecho) {
                                    Icon(Iconos.Check, null, Modifier.size(18.dp))
                                    Text("  En tu Drive")
                                } else {
                                    Text("Guardar en Drive")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (e.historial.isNotEmpty()) {
            item { EncabezadoSeccion("Tus informes") }
            item {
                Tarjeta(relleno = false) {
                    e.historial.forEachIndexed { n, a ->
                        if (n > 0) HorizontalDivider(color = c.borde)
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.abrir(a) }.padding(start = Espacio.l, top = Espacio.s, bottom = Espacio.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val pdf = a.formato == Formato.PDF
                            val colorTipo = if (pdf) androidx.compose.ui.graphics.Color(0xFFF87171) else androidx.compose.ui.graphics.Color(0xFF4ADE80)
                            io.github.alexcorrea2001lemon.nummia.ui.comun.Etiqueta(if (pdf) "PDF" else "XLSX", colorTipo.copy(alpha = 0.18f), colorTipo)
                            androidx.compose.foundation.layout.Spacer(Modifier.size(Espacio.m))
                            Column(Modifier.weight(1f)) {
                                Text(a.nombre.substringBeforeLast('_').replace('_', ' '), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(
                                    "${if (a.formato == Formato.PDF) "PDF" else "Excel"} · ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(Date(a.archivo.lastModified()))} · ${(a.archivo.length() + 1023) / 1024} KB",
                                    style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                                )
                            }
                            IconButton(onClick = { ctx.startActivity(vm.intentCompartir(a)) }) { Icon(Iconos.Subir, "Compartir ${a.nombre}") }
                            IconButton(onClick = { vm.borrar(a) }) { Icon(Iconos.Papelera, "Borrar ${a.nombre}", tint = c.textoSuave) }
                        }
                    }
                }
            }
        }
    }

    if (eligiendoFechas) {
        val estado = rememberDateRangePickerState(
            initialSelectedStartDateMillis = LocalDate.parse(desde).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            initialSelectedEndDateMillis = LocalDate.parse(hasta).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFechas = false },
            confirmButton = {
                TextButton(enabled = estado.selectedStartDateMillis != null && estado.selectedEndDateMillis != null, onClick = {
                    desde = Instant.ofEpochMilli(estado.selectedStartDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    hasta = Instant.ofEpochMilli(estado.selectedEndDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    eligiendoFechas = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendoFechas = false }) { Text("Cancelar") } },
        ) {
            DateRangePicker(estado, Modifier.heightIn(max = 520.dp), title = { Text("Elige las fechas", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

@Composable
private fun AtajoInforme(
    icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, texto: String, activo: Boolean,
    modifier: Modifier, alPulsar: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = alPulsar, enabled = activo, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalColoresNummia.current.borde),
        modifier = modifier,
    ) {
        Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar("", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, icono)
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave, maxLines = 2)
        }
    }
}
