package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.icono.IconoApp
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImagenLogo
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.FormaTarjetas
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import io.github.alexcorrea2001lemon.nummia.ui.theme.ModoAcento
import io.github.alexcorrea2001lemon.nummia.ui.theme.TamanoTexto
import io.github.alexcorrea2001lemon.nummia.ui.theme.Tema
import io.github.alexcorrea2001lemon.nummia.ui.theme.TipoLetra

// ------------------------------------------------------------------ Apariencia

/** Muestra en vivo: cambia con cada ajuste (tema, color, letra, forma de tarjetas, colores de ingreso y gasto). */
@Composable
private fun VistaPrevia() {
    val c = LocalColoresNummia.current
    Tarjeta(fondo = c.heroe) {
        Text("Así se ve ahora", style = MaterialTheme.typography.labelLarge, color = c.textoSuave)
        Text(formatearEuros(152_075L), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
            Text("+ ${formatearEuros(180_000L)}", color = c.ingreso, fontWeight = FontWeight.SemiBold)
            Text("− ${formatearEuros(27_925L)}", color = c.gasto, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = Espacio.l, vertical = Espacio.s)) {
                Text("Guardar", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = Espacio.l, vertical = Espacio.s)) {
                Text("Compras", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun MiniaturaTema(t: Tema, elegido: Boolean, alPulsar: () -> Unit, modifier: Modifier) {
    val c = LocalColoresNummia.current
    val (fondo, tarjeta, texto) = when (t) {
        Tema.AUTOMATICO -> Triple(Color(0xFFF6F6F9), Color(0xFF1A1C23), Color(0xFF14151A))
        Tema.CLARO -> Triple(Color(0xFFF6F6F9), Color.White, Color(0xFF14151A))
        Tema.OSCURO -> Triple(Color(0xFF0E0F13), Color(0xFF1A1C23), Color(0xFFECEDF1))
        Tema.AMOLED -> Triple(Color.Black, Color(0xFF121214), Color(0xFFF2F2F2))
    }
    Surface(
        onClick = alPulsar, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.primary else c.borde),
        modifier = modifier.semantics { selected = elegido; role = Role.RadioButton },
    ) {
        Column(Modifier.padding(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            // Mini pantalla: el fondo, una tarjeta y dos líneas de texto con el color de acento.
            val acento = MaterialTheme.colorScheme.primary
            Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(10.dp))) {
                if (t == Tema.AUTOMATICO) {
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f).height(56.dp).background(Color(0xFFF6F6F9)))
                        Box(Modifier.weight(1f).height(56.dp).background(Color(0xFF0E0F13)))
                    }
                } else Box(Modifier.fillMaxWidth().height(56.dp).background(fondo))
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(acento))
                    Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(6.dp)).background(if (t == Tema.AUTOMATICO) Color.Gray.copy(alpha = 0.35f) else tarjeta))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Espacio.xs)) {
                Column(Modifier.weight(1f)) {
                    Text(t.nombre, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(t.descripcion, style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                }
                if (elegido) Icon(Iconos.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AparienciaScreen(vm: AjustesViewModel, alVolver: () -> Unit) {
    val a by vm.ajustes.collectAsStateWithLifecycle()
    var personalizando by rememberSaveable { mutableStateOf(false) }
    val c = LocalColoresNummia.current
    PantallaAjustes("Apariencia", alVolver) {
        VistaPrevia()

        TituloAjustes("Tema")
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Tema.entries.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    fila.forEach { t -> MiniaturaTema(t, a.tema == t, { vm.tema(t) }, Modifier.weight(1f)) }
                }
            }
        }

        TituloAjustes("Color de la app")
        Tarjeta {
            SelectorColores(
                seleccionado = a.acento.takeIf { a.modoAcento == ModoAcento.PALETA }, alElegir = vm::acento,
                personalizado = Color(a.acentoPersonalizado).takeIf { a.modoAcento == ModoAcento.PERSONALIZADO },
                alPersonalizar = { personalizando = true },
                alFondo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ({ vm.acentoFondo() }) else null,
                fondoElegido = a.modoAcento == ModoAcento.FONDO_PANTALLA,
            )
            Text(
                when (a.modoAcento) {
                    ModoAcento.PALETA -> a.acento.nombre
                    ModoAcento.PERSONALIZADO -> "Personalizado"
                    ModoAcento.FONDO_PANTALLA -> "Colores del fondo de pantalla"
                },
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            )
        }
        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Paleta, "Fondo con tu color", "Un toque del color de la app en el fondo y las tarjetas", a.tinteFondo, vm::tinteFondo)
        }

        TituloAjustes("Letra")
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            TipoLetra.entries.forEach { l ->
                val elegido = a.letra == l
                val familia = when (l) { TipoLetra.SISTEMA -> FontFamily.Default; TipoLetra.SERIFA -> FontFamily.Serif; TipoLetra.MONO -> FontFamily.Monospace }
                Surface(
                    onClick = { vm.letra(l) }, shape = MaterialTheme.shapes.medium, color = if (elegido) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(if (elegido) 2.dp else 1.dp, if (elegido) MaterialTheme.colorScheme.primary else c.borde),
                    modifier = Modifier.weight(1f).semantics { selected = elegido; role = Role.RadioButton },
                ) {
                    Column(Modifier.padding(Espacio.m), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Aa", style = MaterialTheme.typography.headlineMedium.copy(fontFamily = familia), fontWeight = FontWeight.Bold)
                        Text(l.nombre, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        NotaAjuste(a.letra.descripcion)

        TituloAjustes("Tamaño del texto")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            TamanoTexto.entries.forEach { t -> FilterChip(selected = a.tamanoTexto == t, onClick = { vm.tamano(t) }, label = { Text(t.nombre) }) }
        }

        TituloAjustes("Forma de las tarjetas")
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            FormaTarjetas.entries.forEach { f ->
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

        TituloAjustes("Accesibilidad y comodidad")
        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Ojo, "Colores para daltonismo", "Ingresos en azul y gastos en naranja, en vez de verde y rojo", a.coloresAccesibles, vm::coloresAccesibles)
            HorizontalDivider(color = c.borde)
            OpcionInterruptor(Iconos.Movimientos, "Listas compactas", "Filas más bajas: caben más movimientos en pantalla", a.compacto, vm::compacto)
            HorizontalDivider(color = c.borde)
            OpcionInterruptor(Iconos.Check, "Vibración", "Una pequeña vibración al tocar y al guardar", a.vibracion, vm::vibracion)
        }

        TituloAjustes("Icono de la app")
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

        // Tarjetas de Inicio: qué se ve y en qué orden.
        TituloAjustes("Tarjetas de Inicio")
        NotaAjuste("Elige qué ves en Inicio y en qué orden. El balance del mes siempre va arriba.")
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

/** Paleta de colores en círculos. Con [alPersonalizar] y [alFondo] suma los dos círculos especiales al final. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorColores(
    seleccionado: Acento?, alElegir: (Acento) -> Unit,
    personalizado: Color? = null, alPersonalizar: (() -> Unit)? = null,
    alFondo: (() -> Unit)? = null, fondoElegido: Boolean = false,
) {
    @Composable
    fun Circulo(elegido: Boolean, descripcion: String, alPulsar: () -> Unit, relleno: @Composable () -> Unit) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .border(2.dp, if (elegido) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                .clickable(onClick = alPulsar).padding(4.dp)
                .semantics { selected = elegido; role = Role.RadioButton; contentDescription = descripcion },
            contentAlignment = Alignment.Center,
        ) { relleno() }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Acento.entries.forEach { ac ->
            Circulo(ac == seleccionado, ac.nombre, { alElegir(ac) }) { Box(Modifier.fillMaxWidth().aspectRatio(1f).background(ac.color, CircleShape)) }
        }
        if (alPersonalizar != null) {
            val arcoiris = Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
            Circulo(personalizado != null, "Color personalizado", alPersonalizar) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(personalizado?.let { Brush.linearGradient(listOf(it, it)) } ?: arcoiris, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (personalizado != null) Iconos.Check else Iconos.Mas, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (alFondo != null) {
            Circulo(fondoElegido, "Colores del fondo de pantalla", alFondo) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).background(Brush.linearGradient(listOf(Color(0xFF6D8BFF), Color(0xFFFF8FB1), Color(0xFFFFC857))), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (fondoElegido) Icon(Iconos.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
