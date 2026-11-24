package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaLarga
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
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
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
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.rotate
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

private data class EntradaAjuste(
    val seccion: String, val icono: androidx.compose.ui.graphics.vector.ImageVector, val titulo: String, val subtitulo: String,
    val ruta: String, val palabras: String = "",
)

@Composable
fun AjustesScreen(vm: AjustesViewModel, alVolver: () -> Unit, ir: (String) -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    var buscar by rememberSaveable { mutableStateOf("") }
    val R = io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas
    val entradas = listOf(
        EntradaAjuste("Tus datos", Iconos.Nube, "Tu Drive", "Sincroniza con Google Drive y guarda copias de seguridad", R.DRIVE, "google sincronizar copia nube hoja excel"),
        EntradaAjuste("Tus datos", Iconos.Descargar, "Datos", "Importar Excel, exportar a CSV, papelera y copia en archivo", R.DATOS, "importar exportar excel csv papelera borrado restaurar copia archivo"),
        EntradaAjuste("Dinero", Iconos.Euro, "Sueldo", a.sueldoHabitual?.let { "Habitual: ${formatearEuros(it)} · se cobra a final de mes" } ?: "Sin configurar", R.SUELDO, "nomina salario ingreso"),
        EntradaAjuste("Dinero", Iconos.Repetir, "Pagos y cobros fijos", "Suscripciones, gimnasio, lo que cobras cada mes…", R.PAGOS, "suscripcion recibo cada mes fijo pagos mensuales ingreso cobro"),
        EntradaAjuste("Dinero", Iconos.Estadisticas, "Presupuestos", "Límites de gasto al mes", R.PRESUPUESTOS, "limite tope ahorro"),
        EntradaAjuste("Dinero", Iconos.Persona, "Personas", "Quién te devuelve el dinero", R.PERSONAS, "deben deuda devolver amigos familia"),
        EntradaAjuste("Dinero", Iconos.Bolsa, "Tiendas", "Unir nombres repetidos o cambiarlos", R.TIENDAS, "comercio lugar duplicados unir"),
        EntradaAjuste("Dinero", Iconos.Filtro, "Categorías", "Crear, renombrar, iconos y colores", R.CATEGORIAS, "tipo grupo icono color"),
        EntradaAjuste("Aspecto", Iconos.Paleta, "Apariencia", "Tema, color de la app, icono y tamaño del texto", R.APARIENCIA, "oscuro claro amoled color icono letra tamaño tema"),
        EntradaAjuste("Seguridad y avisos", Iconos.Escudo, "Seguridad", if (a.bloqueo) "Bloqueo activado · modo discreto" else "Bloqueo con huella o PIN · modo discreto", R.SEGURIDAD, "huella pin bloqueo discreto ocultar privacidad"),
        EntradaAjuste("Seguridad y avisos", Iconos.Info, "Notificaciones", if (a.notificaciones) "Activadas" else "Sueldo, pagos del día y presupuestos", R.NOTIFICACIONES, "avisos alertas recordatorio"),
        EntradaAjuste("Nummia", Iconos.Info, "Acerca de Nummia", "Versión ${BuildConfig.VERSION_NAME} · actualizaciones", R.ACERCA, "version actualizar actualizacion privacidad licencia codigo"),
    )
    PantallaAjustes("Ajustes", alVolver) {
        a.versionDisponible?.takeIf { esMasNueva(it, BuildConfig.VERSION_NAME) }?.let { v ->
            Tarjeta(fondo = MaterialTheme.colorScheme.primaryContainer) {
                Text("Nummia $v está disponible", fontWeight = FontWeight.SemiBold)
                Button(onClick = { ir(R.ACERCA) }) { Text("Ver la actualización") }
            }
        }
        // Buscador: escribe «drive», «huella», «presupuesto»…
        androidx.compose.material3.TextField(
            value = buscar, onValueChange = { buscar = it }, singleLine = true,
            placeholder = { Text("Buscar en Ajustes") },
            leadingIcon = { Icon(Iconos.Buscar, null) },
            trailingIcon = { if (buscar.isNotEmpty()) IconButton(onClick = { buscar = "" }) { Icon(Iconos.Cerrar, "Borrar búsqueda") } },
            shape = RoundedCornerShape(28.dp),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        val q = io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto(buscar)
        val visibles = if (q.isBlank()) entradas else entradas.filter { en ->
            q.split(" ").filter { it.isNotBlank() }.all { p -> io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto("${en.titulo} ${en.subtitulo} ${en.palabras}").contains(p) }
        }
        if (visibles.isEmpty()) {
            Text("No hay ningún ajuste con «$buscar».", color = LocalColoresNummia.current.textoSuave, modifier = Modifier.padding(Espacio.l))
        }
        visibles.groupBy { it.seccion }.forEach { (seccion, lista) ->
            Text(seccion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = Espacio.s, top = Espacio.xs))
            Tarjeta(relleno = false) {
                lista.forEachIndexed { n, en ->
                    if (n > 0) HorizontalDivider(color = LocalColoresNummia.current.borde)
                    OpcionAjuste(en.icono, en.titulo, en.subtitulo) { ir(en.ruta) }
                }
            }
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

        Text("Forma de las tarjetas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            io.github.alexcorrea2001lemon.nummia.ui.theme.FormaTarjetas.entries.forEach { f ->
                val sel = a.formaTarjetas == f
                Surface(
                    onClick = { vm.formaTarjetas(f) }, selected = sel,
                    shape = RoundedCornerShape(f.radio.dp),
                    color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(if (sel) 2.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else c.borde),
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(f.nombre, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
        }

        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Movimientos, "Listas compactas", "Filas más bajas: caben más movimientos en pantalla", a.compacto, vm::compacto)
            HorizontalDivider(color = c.borde)
            OpcionInterruptor(Iconos.Check, "Vibración", "Una pequeña vibración al tocar y al guardar", a.vibracion, vm::vibracion)
        }

        // Tarjetas de Inicio: qué se ve y en qué orden.
        Text("Tarjetas de Inicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Elige qué ves en Inicio y en qué orden. El balance del mes siempre va arriba.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        val orden = io.github.alexcorrea2001lemon.nummia.ui.inicio.ordenTarjetas(a.tarjetasInicio)
        fun guardar(o: List<io.github.alexcorrea2001lemon.nummia.ui.inicio.TarjetaInicio>, ocultas: Set<String>) = vm.tarjetasInicio(o.map { it.clave }, ocultas)
        Tarjeta(relleno = false) {
            orden.forEachIndexed { n, t ->
                if (n > 0) HorizontalDivider(color = c.borde)
                val visible = t.clave !in a.tarjetasOcultas
                Row(Modifier.fillMaxWidth().padding(start = Espacio.l, end = Espacio.s, top = Espacio.xs, bottom = Espacio.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                        color = if (visible) MaterialTheme.colorScheme.onSurface else c.textoSuave,
                    )
                    IconButton(onClick = { guardar(io.github.alexcorrea2001lemon.nummia.ui.inicio.mover(orden, t, -1), a.tarjetasOcultas) }, enabled = n > 0) {
                        Icon(Iconos.ChevronIzq, "Subir ${t.nombre}", Modifier.rotate(90f))
                    }
                    IconButton(onClick = { guardar(io.github.alexcorrea2001lemon.nummia.ui.inicio.mover(orden, t, 1), a.tarjetasOcultas) }, enabled = n < orden.lastIndex) {
                        Icon(Iconos.ChevronDer, "Bajar ${t.nombre}", Modifier.rotate(90f))
                    }
                    Switch(
                        checked = visible,
                        onCheckedChange = { guardar(orden, if (it) a.tarjetasOcultas - t.clave else a.tarjetasOcultas + t.clave) },
                        modifier = Modifier.semantics { contentDescription = "Mostrar ${t.nombre}" },
                    )
                }
            }
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
    DialogoNummia(
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
    val c = LocalColoresNummia.current
    var texto by rememberSaveable(a.sueldoHabitual) { mutableStateOf(a.sueldoHabitual?.let(::textoImporte).orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val haptica = io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica()
    PantallaAjustes("Sueldo", alVolver) {
        // Tarjeta principal: el importe, grande y claro.
        Tarjeta(fondo = c.heroe) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Surface(shape = CircleShape, color = c.ingresoFondo, contentColor = c.ingreso) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Iconos.Euro, null) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Tu sueldo habitual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Lo que cobras normalmente cada mes", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                }
            }
            OutlinedTextField(
                texto, { texto = it; error = null }, singleLine = true,
                placeholder = { Text("0,00", style = MaterialTheme.typography.headlineMedium) },
                suffix = { Text("€", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                isError = error != null, supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Sueldo habitual en euros" },
            )
            a.sueldoHabitual?.let { s ->
                val fin = remember { java.time.YearMonth.now().atEndOfMonth() }
                Text(
                    "Próximo previsto: ${fechaLarga(fin).replaceFirstChar { it.lowercase() }} · ${formatearEuros(s)}",
                    style = MaterialTheme.typography.bodyMedium, color = c.ingreso, fontWeight = FontWeight.Medium,
                )
            }
        }

        // Cómo funciona, en tres pasos.
        Tarjeta {
            Text("Cómo funciona", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "A final de mes, Nummia lo apunta como «previsto». Aún no cuenta en tus números.",
                "Te avisa (si tienes los avisos activados) y lo verás en Inicio, en «Pendientes».",
                "Lo confirmas con el día y el importe reales. Desde ese momento cuenta.",
            ).forEachIndexed { n, paso ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Text("${n + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                    }
                    Text(paso, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(top = 4.dp))
                }
            }
        }

        Button(
            onClick = {
                leerImporte(texto).fold(
                    { haptica(); vm.sueldo(it); io.github.alexcorrea2001lemon.nummia.ui.comun.Hecho.avisar("Sueldo guardado"); alVolver() },
                    { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo },
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Guardar sueldo", fontWeight = FontWeight.SemiBold) }
        if (a.sueldoHabitual != null) {
            OutlinedButton(onClick = { vm.sueldo(null); alVolver() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Quitar sueldo") }
        }
        Text(
            "¿Cobras otras cosas cada mes, como un alquiler o una paga? Añádelas en Ajustes → Pagos y cobros fijos.",
            style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
        )
    }
}

// ------------------------------------------------------------------ Personas

@Composable
fun PersonasScreen(vm: AjustesViewModel, alVolver: () -> Unit, alDeben: () -> Unit = {}) {
    val personas by vm.personas.collectAsStateWithLifecycle()
    var creando by rememberSaveable { mutableStateOf(false) }
    var editando by remember { mutableStateOf<io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity?>(null) }
    PantallaAjustes("Personas", alVolver) {
        Text("Las personas que te devuelven dinero: padres, hermanos, amigos…", style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
        Tarjeta(relleno = false) { OpcionAjuste(Iconos.Euro, "Lo que me deben", "Cuánto te debe cada persona y «Me lo ha pagado todo»", alPulsar = alDeben) }
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
    var protegiendo by remember { mutableStateOf(false) }
    var claveCopia by remember { mutableStateOf<String?>(null) }
    val nombre = remember { "Nummia_copia_${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.nummia" }

    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) alcance.launch {
            val clave = claveCopia
            claveCopia = null
            val ok = runCatching {
                val datos = vm.exportar(clave)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(datos) } }
                vm.copiaGuardada()
            }.isSuccess
            snackbar.showSnackbar(if (!ok) "No se pudo guardar la copia" else if (clave != null) "Copia guardada con contraseña" else "Copia guardada")
        }
    }
    val nombreCsv = remember { "Nummia_${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}_csv.zip" }
    var exportandoCsv by remember { mutableStateOf(false) }
    val guardarCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) alcance.launch {
            exportandoCsv = true
            val ok = runCatching {
                val datos = vm.exportarCsv()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(datos) } }
            }.isSuccess
            exportandoCsv = false
            snackbar.showSnackbar(if (ok) "Exportado: gastos, ingresos y devoluciones en CSV" else "No se pudo exportar")
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
            OpcionAjuste(Iconos.Descargar, "Guardar copia", "Crea un archivo con todo, con contraseña si quieres") { protegiendo = true }
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            OpcionAjuste(Iconos.Subir, "Restaurar copia", "Sustituye tus datos por los del archivo") { abrir.launch(arrayOf("*/*")) }
        }
        Text(
            "Para abrir tus datos en Excel, Google Sheets o Numbers: un .zip con un CSV de gastos, otro de ingresos y otro de devoluciones.",
            style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
        )
        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Informes, if (exportandoCsv) "Exportando…" else "Exportar todo a CSV", "Todos tus movimientos, menos lo de la papelera") {
                if (!exportandoCsv) guardarCsv.launch(nombreCsv)
            }
        }
    }

    if (protegiendo) {
        DialogoContrasenaCopia(
            alCerrar = { protegiendo = false },
            alElegir = { clave -> protegiendo = false; claveCopia = clave; guardar.launch(nombre) },
        )
    }

    pendiente?.let { bytes ->
        val cifrada = remember(bytes) { io.github.alexcorrea2001lemon.nummia.data.copia.CifradoCopia.estaCifrada(bytes) }
        var clave by remember(bytes) { mutableStateOf("") }
        var error by remember(bytes) { mutableStateOf<String?>(null) }
        var restaurando by remember(bytes) { mutableStateOf(false) }
        DialogoNummia(
            onDismissRequest = { if (!restaurando) pendiente = null },
            icon = { Icon(Iconos.Subir, null) },
            title = { Text("¿Restaurar esta copia?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Text("Tus datos actuales se sustituirán por los del archivo. Antes, Nummia guardará una copia de lo que tienes ahora en el móvil.")
                    if (cifrada) {
                        CampoContrasena(clave, { clave = it; error = null }, "Contraseña de la copia", error)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !restaurando && (!cifrada || clave.isNotEmpty()), onClick = {
                    restaurando = true
                    alcance.launch {
                        val r = vm.importar(bytes, clave.takeIf { cifrada })
                        restaurando = false
                        val fallo = r.exceptionOrNull()
                        if (cifrada && fallo is io.github.alexcorrea2001lemon.nummia.data.copia.CopiaInvalida && fallo.message?.startsWith("Contraseña") == true) {
                            error = fallo.message // se queda abierto para volver a intentarlo
                        } else {
                            pendiente = null
                            snackbar.showSnackbar(r.fold({ "Copia restaurada: $it movimientos" }, { it.message ?: "No se pudo restaurar" }))
                        }
                    }
                }) { Text(if (restaurando) "Restaurando…" else "Restaurar") }
            },
            dismissButton = { TextButton(enabled = !restaurando, onClick = { pendiente = null }) { Text("Cancelar") } },
        )
    }
}

/** Antes de guardar la copia: protegerla con una contraseña (recomendado) o guardarla sin ella. */
@Composable
private fun DialogoContrasenaCopia(alCerrar: () -> Unit, alElegir: (String?) -> Unit) {
    var clave by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    DialogoNummia(
        onDismissRequest = alCerrar,
        icon = { Icon(Iconos.Escudo, null) },
        title = { Text("Proteger la copia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Text(
                    "Con contraseña, nadie puede ver tus datos aunque encuentre el archivo. Apúntala bien: sin ella la copia no se puede abrir.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CampoContrasena(clave, { clave = it; error = null }, "Contraseña (mínimo 6)", null)
                CampoContrasena(repetir, { repetir = it; error = null }, "Repítela", error)
            }
        },
        confirmButton = {
            Button(onClick = {
                error = when {
                    clave.length < 6 -> "Usa al menos 6 caracteres"
                    clave != repetir -> "Las dos contraseñas no coinciden"
                    else -> null
                }
                if (error == null) alElegir(clave)
            }, modifier = Modifier.height(48.dp)) { Text("Proteger y guardar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = { alElegir(null) }, modifier = Modifier.height(48.dp)) { Text("Sin contraseña") } },
    )
}

@Composable
private fun CampoContrasena(valor: String, alCambiar: (String) -> Unit, etiqueta: String, error: String?) {
    var ver by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedTextField(
        valor, alCambiar, label = { Text(etiqueta) }, singleLine = true, isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (ver) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
        trailingIcon = { TextButton(onClick = { ver = !ver }) { Text(if (ver) "Ocultar" else "Ver") } },
        modifier = Modifier.fillMaxWidth(),
    )
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
        DialogoNummia(
            onDismissRequest = { leyendo = null },
            title = { Text(titulo) },
            text = { Column { Text(texto, style = MaterialTheme.typography.bodyMedium) } },
            confirmButton = { TextButton(onClick = { leyendo = null }) { Text("Cerrar") } },
        )
    }
}

private val TEXTO_PRIVACIDAD = "Privacidad" to """Nummia guarda solo lo que tú escribes: gastos, ingresos, devoluciones, personas y ajustes.

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
