package io.github.alexcorrea2001lemon.nummia.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * Ir rápido a cualquier mes: el año con flechas o con los atajos de los últimos años,
 * y los 12 meses en cuadrícula. Un toque y listo.
 */
@Composable
fun SelectorMes(actual: YearMonth, hoy: YearMonth, alCerrar: () -> Unit, alElegir: (YearMonth) -> Unit) {
    var anio by remember { mutableIntStateOf(actual.year) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Ir a…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { anio-- }) { Icon(Iconos.ChevronIzq, "Año anterior") }
                    Text("$anio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(onClick = { anio++ }) { Icon(Iconos.ChevronDer, "Año siguiente") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    // Saltos rápidos de año
                    listOf(hoy.year - 3, hoy.year - 2, hoy.year - 1, hoy.year).forEach { a ->
                        TextButton(onClick = { anio = a }) {
                            Text("$a", fontWeight = if (a == anio) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                (0 until 4).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        (0 until 3).forEach { col ->
                            val m = fila * 3 + col + 1
                            val ym = YearMonth.of(anio, m)
                            val elegido = ym == actual
                            val esHoy = ym == hoy
                            val nombre = ym.month.getDisplayName(TextStyle.SHORT_STANDALONE, ES).trimEnd('.').replaceFirstChar { it.uppercase() }
                            Box(
                                Modifier.weight(1f).height(44.dp).clip(MaterialTheme.shapes.small)
                                    .background(
                                        when {
                                            elegido -> MaterialTheme.colorScheme.primary
                                            esHoy -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                    )
                                    .clickable { alElegir(ym) }
                                    .semantics {
                                        role = Role.Button; selected = elegido
                                        contentDescription = nombreMes(ym)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    nombre, fontWeight = if (elegido || esHoy) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { alElegir(hoy) }) { Text("Hoy") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}

/** Chip «Hoy»: solo aparece cuando no estás en el mes actual y te devuelve a él con un toque. */
@androidx.compose.runtime.Composable
fun ChipHoy(visible: Boolean, alPulsar: () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.8f),
    ) {
        androidx.compose.material3.AssistChip(
            onClick = alPulsar,
            label = { androidx.compose.material3.Text("Hoy") },
            modifier = androidx.compose.ui.Modifier.semantics { contentDescription = "Volver al mes actual" },
        )
    }
}
