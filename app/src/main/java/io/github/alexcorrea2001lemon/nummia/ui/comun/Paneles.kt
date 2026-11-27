package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

/**
 * Panel inferior con las acciones de una cosa (una tienda, una persona, un pago).
 * Ya desplaza su contenido: dentro NO se usa verticalScroll ni listas perezosas verticales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelNummia(alCerrar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = Espacio.xl, end = Espacio.xl, bottom = Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
            content = contenido,
        )
    }
}

/** Círculo con las iniciales; con [marcado] muestra un ✓ (modo selección). */
@Composable
fun AvatarIniciales(texto: String, tam: Dp = 40.dp, marcado: Boolean = false, fondo: Color? = null, color: Color? = null) {
    val f = if (marcado) MaterialTheme.colorScheme.primary else fondo ?: MaterialTheme.colorScheme.primaryContainer
    val c = if (marcado) MaterialTheme.colorScheme.onPrimary else color ?: MaterialTheme.colorScheme.onPrimaryContainer
    Surface(shape = CircleShape, color = f, contentColor = c, modifier = Modifier.size(tam)) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(marcado, label = "marca") { m ->
                if (m) Icon(Iconos.Check, null, Modifier.size(tam * 0.5f))
                else Text(texto, fontWeight = FontWeight.SemiBold, style = if (tam > 44.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Cabecera del panel: avatar, nombre y una línea de detalle. */
@Composable
fun CabeceraPanel(avatar: @Composable () -> Unit, titulo: String, detalle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
        avatar()
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
        }
    }
}

/** Fila de cifras pequeñas: «Total · Veces · Media». */
@Composable
fun DatosPanel(vararg datos: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        datos.forEach { (etiqueta, valor) -> DatoPanel(etiqueta, valor) }
    }
}

@Composable
private fun RowScope.DatoPanel(etiqueta: String, valor: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(Espacio.m).semantics(mergeDescendants = true) {}) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave)
            Text(valor, style = MaterialTheme.typography.titleMedium.merge(TextStyle(fontFeatureSettings = "tnum")), fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Acción del panel: icono y texto, con toda la fila pulsable (≥ 52 dp). */
@Composable
fun AccionPanel(icono: ImageVector, texto: String, color: Color? = null, conFlecha: Boolean = false, alPulsar: () -> Unit) {
    val tinte = color ?: MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = alPulsar).padding(horizontal = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        Icon(icono, null, tint = tinte, modifier = Modifier.size(22.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = color ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (conFlecha) Icon(Iconos.ChevronDer, null, tint = LocalColoresNummia.current.textoSuave)
    }
}
