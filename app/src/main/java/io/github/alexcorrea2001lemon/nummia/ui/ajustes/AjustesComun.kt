package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(titulo: String, alVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    // Título grande que se encoge al bajar (como los Ajustes del sistema): más claro y con más aire.
    val plegado = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(plegado.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.LargeTopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Iconos.Atras, "Volver") } },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = plegado,
            )
        },
    ) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(horizontal = Espacio.l).navigationBarsPadding()
                .padding(bottom = Espacio.xxl),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
            content = contenido,
        )
    }
}

@Composable
/**
 * Fila de ajustes. La acción al pulsar es el ÚLTIMO parámetro, para que `OpcionAjuste(...) { accion }`
 * nunca se ejecute al dibujar (antes el bloque final era contenido componible y se lanzaba solo).
 */
fun OpcionAjuste(icono: ImageVector, titulo: String, subtitulo: String? = null, final: (@Composable () -> Unit)? = null, alPulsar: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (alPulsar != null) it.clickable(onClick = alPulsar) else it }.padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        // Icono dentro de una pastilla con el color de la app: se reconoce cada fila de un vistazo.
        androidx.compose.material3.Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            androidx.compose.foundation.layout.Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(icono, null, modifier = Modifier.size(22.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        }
        when {
            final != null -> final()
            alPulsar != null -> Icon(Iconos.ChevronDer, null, tint = LocalColoresNummia.current.textoSuave)
        }
    }
}

@Composable
fun OpcionInterruptor(icono: ImageVector, titulo: String, subtitulo: String?, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    OpcionAjuste(icono, titulo, subtitulo, final = { Switch(checked = valor, onCheckedChange = alCambiar) }) { alCambiar(!valor) }
}

val SinRelleno = PaddingValues()

/** Título de un grupo de ajustes. Va marcado como encabezado para TalkBack. */
@Composable
fun TituloAjustes(texto: String) {
    Text(
        texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Espacio.s, top = Espacio.xs).semantics { heading() },
    )
}

/** Explicación breve bajo un grupo de ajustes. */
@Composable
fun NotaAjuste(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.padding(horizontal = Espacio.s))
}

/** «Última copia: hoy», «hace 3 días»… Con null avisa de que aún no hay ninguna. */
internal fun textoUltimaCopia(dias: Long?): String = when {
    dias == null -> "Todavía no has guardado ninguna copia en un archivo"
    dias <= 0L -> "Última copia en un archivo: hoy"
    dias == 1L -> "Última copia en un archivo: ayer"
    else -> "Última copia en un archivo: hace $dias días"
}
