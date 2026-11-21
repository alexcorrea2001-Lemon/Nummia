package io.github.alexcorrea2001lemon.nummia.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.DibujoLogo
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo

@Composable
fun Bienvenida(alTerminar: (Long?) -> Unit) {
    var sueldo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(Espacio.xxl),
            verticalArrangement = Arrangement.spacedBy(Espacio.xl),
        ) {
            Spacer(Modifier.height(Espacio.xl))
            DibujoLogo(Logo.A, Acento.VIOLETA.color, Modifier.size(80.dp))
            Text("Bienvenido a Nummia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Apunta lo que gastas y lo que ganas. Nummia hace las cuentas por ti.", style = MaterialTheme.typography.bodyLarge, color = LocalColoresNummia.current.textoSuave)
            Punto(Iconos.Bolsa, "Gastos y reembolsos", "Aunque te devuelvan el dinero en varias veces o redondeen hacia arriba.")
            Punto(Iconos.Persona, "Lo que te deben", "Si pagas algo por otra persona, Nummia te recuerda cuánto falta.")
            Punto(Iconos.Escudo, "Tus datos son tuyos", "Se guardan cifrados en tu móvil. Sin anuncios ni rastreo.")
            OutlinedTextField(
                sueldo, { sueldo = it; error = null }, label = { Text("Tu sueldo habitual (opcional)") }, suffix = { Text("€") },
                singleLine = true, isError = error != null, supportingText = { Text(error ?: "Se apuntará como previsto a final de cada mes.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (sueldo.isBlank()) alTerminar(null)
                    else leerImporte(sueldo).fold({ alTerminar(it) }, { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Empezar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Punto(icono: ImageVector, titulo: String, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m), verticalAlignment = Alignment.Top) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
        }
    }
}
