package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.BuildConfig
import io.github.alexcorrea2001lemon.nummia.data.actualizar.esMasNueva
import io.github.alexcorrea2001lemon.nummia.data.icono.IconoApp
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImagenLogo
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.formulario.DialogoNombre
import io.github.alexcorrea2001lemon.nummia.ui.formulario.textoImporte
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import io.github.alexcorrea2001lemon.nummia.ui.theme.ModoAcento
import io.github.alexcorrea2001lemon.nummia.ui.theme.TamanoTexto
import io.github.alexcorrea2001lemon.nummia.ui.theme.Tema
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------ Menú

@Composable
fun AjustesScreen(vm: AjustesViewModel, alVolver: () -> Unit, ir: (String) -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    PantallaAjustes("Ajustes", alVolver) {
        a.versionDisponible?.takeIf { esMasNueva(it, BuildConfig.VERSION_NAME) }?.let { v ->
            Tarjeta(fondo = MaterialTheme.colorScheme.primaryContainer) {
                Text("Nummia $v está disponible", fontWeight = FontWeight.SemiBold)
                Button(onClick = { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.ACERCA) }) { Text("Ver la actualización") }
            }
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Nube, "Tu Drive", "Sincroniza con tu Google Drive y guarda copias de seguridad") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.DRIVE) }
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Paleta, "Apariencia", "Tema, color de la app, icono y tamaño del texto") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.APARIENCIA) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Escudo, "Seguridad", if (a.bloqueo) "Bloqueo activado · modo discreto" else "Bloqueo con huella o PIN · modo discreto") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.SEGURIDAD) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Info, "Notificaciones", if (a.notificaciones) "Activadas" else "Sueldo, pagos del día y presupuestos") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.NOTIFICACIONES) }
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Euro, "Sueldo", a.sueldoHabitual?.let { "Habitual: ${formatearEuros(it)} · se cobra a final de mes" } ?: "Sin configurar") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.SUELDO) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Repetir, "Pagos mensuales", "Gimnasio, móvil, suscripciones…") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.PAGOS) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Estadisticas, "Presupuestos", "Límites de gasto al mes") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.PRESUPUESTOS) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Persona, "Personas", "Quién te devuelve el dinero") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.PERSONAS) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Bolsa, "Tiendas", "Unir nombres repetidos o cambiarlos") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.TIENDAS) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Filtro, "Categorías", "Crear, renombrar y cambiar colores") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.CATEGORIAS) }
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Descargar, "Datos", "Importar Excel, papelera y copia en archivo") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.DATOS) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Info, "Acerca de Nummia", "Versión ${BuildConfig.VERSION_NAME} · actualizaciones") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.ACERCA) }
        }
    }
}

// ------------------------------------------------------------------ Apariencia

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AparienciaScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    var personalizando by rememberSaveable { mutableStateOf(false) }
    val c = LocalColoresNummia.current
    PantallaAjustes("Apariencia", alVolver) {
        Text("Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Tema.entries.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    fila.forEach { t ->
                        val (fondo, texto) = when (t) {
                            Tema.AUTOMATICO -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
                            Tema.CLARO -> Color.White to Color(0xFF14151A)
                            Tema.OSCURO -> Color(0xFF15171C) to Color(0xFFECEDF1)
                            Tema.AMOLED -> Color.Black to Color(0xFFF2F2F2)
                        }
                        val elegido = a.tema == t
                        Surface(
                            onClick = { vm.tema(t) }, color = fondo, shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.primary else c.borde),
                            modifier = Modifier.weight(1f).height(76.dp).semantics { selected = elegido; role = Role.RadioButton },
                        ) {
                            Column(Modifier.padding(horizontal = Espacio.l), verticalArrangement = Arrangement.Center) {
                                Text(t.nombre, color = texto, fontWeight = FontWeight.SemiBold)
                                Text(t.descripcion, color = texto.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Text("Color de la app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Tarjeta {
            SelectorColores(seleccionado = a.acento.takeIf { a.modoAcento == ModoAcento.PALETA }, alElegir = vm::acento)
            Text(
                when (a.modoAcento) {
                    ModoAcento.PALETA -> "Seleccionado: ${a.acento.nombre}"
                    ModoAcento.PERSONALIZADO -> "Seleccionado: personalizado"
                    ModoAcento.FONDO_PANTALLA -> "Seleccionado: colores del fondo de pantalla"
                },
                style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
            )
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Paleta, "Personalizado", "Elige cualquier color", alPulsar = { personalizando = true })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                HorizontalDivider(color = c.borde)
                OpcionAjuste(Iconos.Paleta, "Del fondo de pantalla", "Usa los colores de tu móvil (Material You)", alPulsar = vm::acentoFondo)
            }
        }

        Text("Icono de la app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Tarjeta {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Logo.entries.forEach { l ->
                    val elegido = a.iconoLogo == l
                    Surface(
                        onClick = { vm.icono(l, a.iconoColor) }, shape = MaterialTheme.shapes.medium, color = Color.Transparent,
                        border = BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.onSurface else c.borde),
                        modifier = Modifier.weight(1f).height(96.dp).semantics { selected = elegido; role = Role.RadioButton; contentDescription = "Logo ${l.nombre}" },
                    ) {
                        Box(contentAlignment = Alignment.Center) { ImagenLogo(l, a.iconoColor, Modifier.size(64.dp)) }
                    }
                }
            }
            Text("Color del icono (independiente del de la app): ${a.iconoColor.nombre}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            SelectorColores(seleccionado = a.iconoColor, alElegir = { vm.icono(a.iconoLogo, it) })
            Text("El icono cambiará en tu pantalla de inicio al salir de Nummia.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            val ctx = LocalContext.current
            if (IconoApp.puedeAnclar(ctx)) {
                Text(
                    "¿Tu móvil sigue mostrando el icono anterior? Algunos móviles guardan el icono y no lo actualizan. Pon un acceso directo con el icono elegido y, si quieres, quita el antiguo de la pantalla de inicio.",
                    style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                )
                OutlinedButton(onClick = { IconoApp.anclarAccesoDirecto(ctx, a.iconoLogo, a.iconoColor) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Poner este icono en la pantalla de inicio")
                }
            }
        }

        Text("Tamaño del texto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            TamanoTexto.entries.forEach { t -> FilterChip(selected = a.tamanoTexto == t, onClick = { vm.tamano(t) }, label = { Text(t.nombre) }) }
        }
    }

    if (personalizando) {
        DialogoColorPersonalizado(Color(a.acentoPersonalizado), alCerrar = { personalizando = false }) { vm.acentoPersonalizado(it.toArgb().toLong() and 0xFFFFFFFFL); personalizando = false }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorColores(seleccionado: Acento?, alElegir: (Acento) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s), maxItemsInEachRow = 6) {
        Acento.entries.forEach { ac ->
            val elegido = ac == seleccionado
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .border(2.dp, if (elegido) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable { alElegir(ac) }.padding(4.dp)
                    .semantics { selected = elegido; role = Role.RadioButton; contentDescription = ac.nombre },
            ) { Box(Modifier.fillMaxWidth().aspectRatio(1f).background(ac.color, CircleShape)) }
        }
    }
}

@Composable
private fun DialogoColorPersonalizado(inicial: Color, alCerrar: () -> Unit, alAceptar: (Color) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(inicial.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1].coerceIn(0.3f, 1f)) }
    var v by remember { mutableFloatStateOf(hsv[2].coerceIn(0.5f, 1f)) }
    val color = Color.hsv(h, s, v)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Color personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Box(Modifier.fillMaxWidth().height(56.dp).background(color, RoundedCornerShape(16.dp)))
                Text("Tono", style = MaterialTheme.typography.bodySmall)
                Slider(h, { h = it }, valueRange = 0f..360f)
                Text("Intensidad", style = MaterialTheme.typography.bodySmall)
                Slider(s, { s = it }, valueRange = 0.3f..1f)
                Text("Brillo", style = MaterialTheme.typography.bodySmall)
                Slider(v, { v = it }, valueRange = 0.5f..1f)
            }
        },
        confirmButton = { Button(onClick = { alAceptar(color) }) { Text("Usar este color") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

// ------------------------------------------------------------------ Sueldo

@Composable
fun SueldoScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    var texto by rememberSaveable(a.sueldoHabitual) { mutableStateOf(a.sueldoHabitual?.let(::textoImporte).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    PantallaAjustes("Sueldo", alVolver) {
        Text(
            "Cada mes Nummia apuntará tu sueldo como «previsto» a final de mes. No contará hasta que lo confirmes con el día y el importe reales.",
            style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
        )
        OutlinedTextField(
            texto, { texto = it; error = null }, label = { Text("Sueldo habitual") }, suffix = { Text("€") }, singleLine = true,
            isError = error != null, supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Button(onClick = {
                leerImporte(texto).fold({ vm.sueldo(it); alVolver() }, { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo })
            }) { Text("Guardar") }
            if (a.sueldoHabitual != null) OutlinedButton(onClick = { vm.sueldo(null); alVolver() }) { Text("Quitar sueldo") }
        }
    }
}

// ------------------------------------------------------------------ Personas

@Composable
fun PersonasScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val personas by vm.personas.collectAsStateWithLifecycle()
    var creando by rememberSaveable { mutableStateOf(false) }
    var editando by remember { mutableStateOf<io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity?>(null) }
    PantallaAjustes("Personas", alVolver) {
        Text("Las personas que te devuelven dinero: padres, hermanos, amigos…", style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
        Tarjeta(relleno = false) {
            if (personas.isEmpty()) Text("Aún no has añadido a nadie.", modifier = Modifier.padding(Espacio.l), color = LocalColoresNummia.current.textoSuave)
            personas.forEach { p ->
                OpcionAjuste(
                    Iconos.Persona, p.nombre,
                    final = { IconButton(onClick = { vm.borrarPersona(p) }) { Icon(Iconos.Papelera, "Quitar a ${p.nombre}", tint = LocalColoresNummia.current.textoSuave) } },
                ) { editando = p }
            }
        }
        Button(onClick = { creando = true }) { Text("Añadir persona") }
    }
    if (creando) DialogoNombre("Nueva persona", alCerrar = { creando = false }) { vm.nuevaPersona(it); creando = false }
    editando?.let { p -> DialogoNombre("Cambiar nombre", p.nombre, alCerrar = { editando = null }) { vm.renombrar(p, it); editando = null } }
}

// ------------------------------------------------------------------ Copia de seguridad

@Composable
fun DatosScreen(vm: AjustesViewModel, snackbar: SnackbarHostState, ir: (String) -> Unit, alVolver: () -> Unit) {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    var pendiente by remember { mutableStateOf<ByteArray?>(null) }
    val nombre = remember { "Nummia_copia_${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.nummia" }

    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) alcance.launch {
            val ok = runCatching {
                val datos = vm.exportar()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(datos) } }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "Copia guardada" else "No se pudo guardar la copia")
        }
    }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) alcance.launch {
            pendiente = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } }.getOrNull()
            }
        }
    }

    PantallaAjustes("Datos", alVolver) {
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Informes, "Importar Excel", "Trae los gastos e ingresos de tu Excel de cuentas") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.IMPORTAR) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Papelera, "Papelera", "Lo borrado en los últimos 30 días") { ir(io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.PAPELERA) }
        }
        Text(
            "Copia en un archivo .nummia que guardas donde quieras. Las copias automáticas están en Ajustes → Tu Drive.",
            style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
        )
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Descargar, "Guardar copia", "Crea un archivo con todo") { guardar.launch(nombre) }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Subir, "Restaurar copia", "Sustituye tus datos por los del archivo") { abrir.launch(arrayOf("*/*")) }
        }
    }

    pendiente?.let { bytes ->
        AlertDialog(
            onDismissRequest = { pendiente = null },
            title = { Text("¿Restaurar esta copia?") },
            text = { Text("Tus datos actuales se sustituirán por los del archivo. Antes, Nummia guardará una copia de lo que tienes ahora en el móvil.") },
            confirmButton = {
                TextButton(onClick = {
                    pendiente = null
                    alcance.launch {
                        val r = vm.importar(bytes)
                        snackbar.showSnackbar(r.fold({ "Copia restaurada: $it movimientos" }, { it.message ?: "No se pudo restaurar" }))
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { pendiente = null }) { Text("Cancelar") } },
        )
    }
}

// ------------------------------------------------------------------ Acerca de y actualizaciones

@Composable
fun AcercaScreen(vm: AjustesViewModel, act: ActualizarViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    val e by act.estado.collectAsStateWithLifecycle()
    var leyendo by remember { mutableStateOf<Pair<String, String>?>(null) }
    PantallaAjustes("Acerca de Nummia", alVolver) {
        Column(Modifier.fillMaxWidth().padding(vertical = Espacio.l), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            ImagenLogo(a.iconoLogo, a.iconoColor, Modifier.size(88.dp), "Icono de Nummia")
            Text("Nummia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Versión ${BuildConfig.VERSION_NAME}", color = LocalColoresNummia.current.textoSuave)
        }
        Tarjeta {
            Text("Actualizaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when (val s = e) {
                EstadoPantallaActualizar.Inicial -> Text("Pulsa para comprobar si hay una versión nueva.", color = LocalColoresNummia.current.textoSuave)
                EstadoPantallaActualizar.Comprobando -> LinearProgressIndicator(Modifier.fillMaxWidth())
                EstadoPantallaActualizar.AlDia -> Text("Tienes la última versión.", color = LocalColoresNummia.current.ingreso)
                is EstadoPantallaActualizar.Error -> Text(s.mensaje, color = LocalColoresNummia.current.aviso)
                is EstadoPantallaActualizar.Disponible -> {
                    Text("Nummia ${s.etiqueta} disponible", fontWeight = FontWeight.SemiBold)
                    if (s.notas.isNotBlank()) Text(s.notas.take(1200), style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
                }
                is EstadoPantallaActualizar.Descargando -> {
                    Text("Descargando… ${(s.progreso * 100).toInt()} %")
                    LinearProgressIndicator(progress = { s.progreso }, modifier = Modifier.fillMaxWidth())
                }
                EstadoPantallaActualizar.NecesitaPermiso -> Text("Para instalar, permite a Nummia instalar apps. Solo hay que hacerlo una vez.", color = LocalColoresNummia.current.aviso)
                EstadoPantallaActualizar.Instalando -> Text("Instalando… La app se reiniciará al terminar.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                when (e) {
                    is EstadoPantallaActualizar.Disponible -> Button(onClick = act::actualizar) { Text("Actualizar") }
                    EstadoPantallaActualizar.NecesitaPermiso -> {
                        Button(onClick = act::actualizar) { Text("Continuar") }
                        OutlinedButton(onClick = act::abrirPermiso) { Text("Dar permiso") }
                    }
                    EstadoPantallaActualizar.Comprobando, is EstadoPantallaActualizar.Descargando, EstadoPantallaActualizar.Instalando -> Unit
                    else -> OutlinedButton(onClick = act::comprobar) { Icon(Iconos.Actualizar, null, Modifier.size(18.dp)); Text("  Buscar actualizaciones") }
                }
            }
            if (BuildConfig.DEBUG) Text("En la versión de pruebas las actualizaciones no se instalan.", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        }
        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Actualizar, "Comprobar cada día", "En segundo plano, sin gastar batería", a.buscarActualizaciones, vm::buscarActualizaciones)
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionInterruptor(Iconos.Descargar, "Descargar solo con Wi-Fi", "Para no gastar datos móviles", a.actualizacionesSoloWifi, vm::soloWifi)
        }
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Escudo, "Privacidad", "Sin servidores, sin anuncios, sin rastreo") { leyendo = TEXTO_PRIVACIDAD }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Codigo, "Licencia", "Código abierto · MIT") { leyendo = TEXTO_LICENCIA }
        }
    }

    leyendo?.let { (titulo, texto) ->
        AlertDialog(
            onDismissRequest = { leyendo = null },
            title = { Text(titulo) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(texto, style = MaterialTheme.typography.bodyMedium) } },
            confirmButton = { TextButton(onClick = { leyendo = null }) { Text("Cerrar") } },
        )
    }
}

private val TEXTO_PRIVACIDAD = "Privacidad" to """Nummia guarda solo lo que tú escribes: gastos, ingresos, reembolsos, personas y ajustes.

• Tus datos están en tu móvil, en una base de datos cifrada.
• No hay servidores propios: tus datos nunca pasan por el desarrollador.
• Sin anuncios, sin analítica, sin rastreo y sin ubicación.
• Las copias automáticas de Android están desactivadas para los datos de la app.
• Internet solo se usa para comprobar si hay una versión nueva (no se envía ningún dato personal).
• A partir de la versión 1.2.0, si conectas tu Google Drive, Nummia solo podrá ver los archivos que ella misma cree.

Para borrar todo lo que hay en el móvil, desinstala la app."""

private val TEXTO_LICENCIA = "Licencia" to """Nummia es software libre con licencia MIT.

Copyright (c) 2026 alexcorrea2001-Lemon

Se concede permiso, de forma gratuita, a cualquier persona que obtenga una copia de este software y de los archivos de documentación asociados, para utilizarlo sin restricción, incluyendo sin limitación los derechos a usar, copiar, modificar, fusionar, publicar, distribuir, sublicenciar y/o vender copias del software, con la condición de incluir el aviso de copyright anterior y este aviso de permiso en todas las copias o partes sustanciales del software.

EL SOFTWARE SE PROPORCIONA «TAL CUAL», SIN GARANTÍA DE NINGÚN TIPO. (Traducción orientativa; el texto legal válido es el original en inglés incluido con el código.)"""
