package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio

/**
 * Ventana de Nummia: sale desde abajo (más cómoda con el pulgar y no salta al crecer), con título grande,
 * icono opcional en un círculo de color y los botones abajo a la derecha.
 * Mismos parámetros que `AlertDialog`, para usarla en su lugar sin más cambios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNummia(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(start = Espacio.xl, end = Espacio.xl, bottom = Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.l),
        ) {
            if (icon != null || title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    if (icon != null) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { icon() }
                        }
                    }
                    if (title != null) {
                        Box(Modifier.semantics { heading() }) {
                            ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)) { title() }
                        }
                    }
                }
            }
            if (text != null) {
                // Contenido largo: se desplaza; los botones siempre quedan a la vista.
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        CompositionLocalProvider { text() }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = Espacio.s),
                horizontalArrangement = Arrangement.spacedBy(Espacio.s, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
