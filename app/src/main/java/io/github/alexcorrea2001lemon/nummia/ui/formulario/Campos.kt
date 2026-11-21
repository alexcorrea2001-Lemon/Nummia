package io.github.alexcorrea2001lemon.nummia.ui.formulario

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
/**
 * Importe grande, centrado, con el teclado propio de Nummia: números, coma, sumas y restas
 * («12+3,50»). Se abre solo al empezar y al tocar el importe; «Listo» lo cierra.
 */
@Composable
fun CampoImporteGrande(
    valor: String, alCambiar: (String) -> Unit, error: String?, foco: FocusRequester, etiqueta: String = "Importe",
    alListo: () -> Unit = {},
) {
    val c = LocalColoresNummia.current
    var abierto by rememberSaveable { mutableStateOf(valor.isEmpty()) }
    val gestor = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(valor.isEmpty()) { if (valor.isEmpty()) abierto = true } // tras «Guardar y otro»
    // Si se abre el teclado del móvil (al escribir el concepto), el nuestro se cierra: nunca los dos a la vez.
    val tecladoMovil = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    LaunchedEffect(tecladoMovil) { if (tecladoMovil) abierto = false }
    val total = remember(valor) {
        if (io.github.alexcorrea2001lemon.nummia.domain.dinero.esOperacion(valor)) {
            io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporteOperacion(valor).getOrNull()
        } else null
    }
    Column(Modifier.fillMaxWidth().padding(vertical = Espacio.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                .clickable { gestor.clearFocus(); abierto = true }
                .focusRequester(foco).focusable()
                .semantics { contentDescription = "$etiqueta: ${valor.ifEmpty { "vacío" }}. Toca para escribir" }
                .padding(vertical = Espacio.s),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                valor.ifEmpty { "0,00" }, fontSize = if (valor.length > 9) 36.sp else 48.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                color = if (valor.isEmpty()) c.textoSuave.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Text(" €", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = c.textoSuave)
        }
        if (total != null) {
            Text("= ${io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros(total)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        androidx.compose.animation.AnimatedVisibility(abierto) {
            TecladoImporte(valor, alCambiar) { abierto = false; alListo() }
        }
    }
}

/** Teclado numérico grande con + y −. */
@Composable
fun TecladoImporte(valor: String, alCambiar: (String) -> Unit, alListo: () -> Unit) {
    val haptica = androidx.compose.ui.platform.LocalHapticFeedback.current
    fun pulsar(k: String) {
        haptica.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        val ultimo = valor.substringAfterLast('+').substringAfterLast('−')
        val nuevo = when (k) {
            "⌫" -> valor.dropLast(1)
            "+", "−" -> when {
                valor.isEmpty() -> valor
                valor.last() == '+' || valor.last() == '−' -> valor.dropLast(1) + k
                valor.last() == ',' -> valor.dropLast(1) + k
                else -> valor + k
            }
            "," -> if (',' in ultimo) valor else valor + (if (ultimo.isEmpty()) "0," else ",")
            else -> if (',' in ultimo && ultimo.substringAfter(',').length >= 2) valor else valor + k
        }
        if (nuevo.length <= 24) alCambiar(nuevo)
    }
    val filas = listOf(listOf("7", "8", "9", "⌫"), listOf("4", "5", "6", "+"), listOf("1", "2", "3", "−"), listOf(",", "0", "00", "Listo"))
    Column(Modifier.fillMaxWidth().padding(top = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                fila.forEach { k ->
                    val esListo = k == "Listo"
                    val esOp = k == "+" || k == "−" || k == "⌫"
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(MaterialTheme.shapes.small)
                            .background(
                                when {
                                    esListo -> MaterialTheme.colorScheme.primary
                                    esOp -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            )
                            .clickable { if (esListo) alListo() else if (k == "00") { pulsar("0"); pulsar("0") } else pulsar(k) }
                            .semantics {
                                contentDescription = when (k) { "⌫" -> "Borrar"; "+" -> "Sumar"; "−" -> "Restar"; "," -> "Coma"; else -> k }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            k, fontSize = if (esListo) 16.sp else 22.sp, fontWeight = if (esListo) FontWeight.Bold else FontWeight.Medium,
                            color = if (esListo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** Hoy · Ayer · Otra fecha (calendario). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(fecha: LocalDate, hoy: LocalDate, alCambiar: (LocalDate) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Text("Fecha", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.padding(start = Espacio.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            FilterChip(selected = fecha == hoy, onClick = { alCambiar(hoy) }, label = { Text("Hoy") })
            FilterChip(selected = fecha == hoy.minusDays(1), onClick = { alCambiar(hoy.minusDays(1)) }, label = { Text("Ayer") })
            val otra = fecha != hoy && fecha != hoy.minusDays(1)
            FilterChip(
                selected = otra, onClick = { abierto = true },
                leadingIcon = { Icon(Iconos.Calendario, null) },
                label = { Text(if (otra) fechaCorta(fecha) else "Otra fecha") },
            )
        }
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { alCambiar(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

@Composable
fun DialogoNombre(titulo: String, inicial: String = "", alCerrar: () -> Unit, alAceptar: (String) -> Unit) {
    var texto by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { OutlinedTextField(texto, { texto = it }, singleLine = true, label = { Text("Nombre") }) },
        confirmButton = { TextButton(onClick = { alAceptar(texto.trim()) }, enabled = texto.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
