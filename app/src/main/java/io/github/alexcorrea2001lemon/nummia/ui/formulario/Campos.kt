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
import androidx.compose.ui.unit.sp
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Importe grande, centrado, con teclado numérico. */
@Composable
fun CampoImporteGrande(valor: String, alCambiar: (String) -> Unit, error: String?, foco: FocusRequester, etiqueta: String = "Importe") {
    val c = LocalColoresNummia.current
    Column(Modifier.fillMaxWidth().padding(vertical = Espacio.m), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = valor,
                onValueChange = { nuevo -> alCambiar(nuevo.filter { it.isDigit() || it == ',' || it == '.' }.take(13)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 48.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = "tnum",
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                decorationBox = { campo ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (valor.isEmpty()) {
                            Text("0,00", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = c.textoSuave.copy(alpha = 0.5f), style = TextStyle(fontFeatureSettings = "tnum"))
                        }
                        campo()
                    }
                },
                modifier = Modifier.width(240.dp).focusRequester(foco).semantics { contentDescription = etiqueta },
            )
            Text(" €", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = c.textoSuave)
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
