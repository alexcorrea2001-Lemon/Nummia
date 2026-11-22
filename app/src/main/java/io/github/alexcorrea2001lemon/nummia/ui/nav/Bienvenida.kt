package io.github.alexcorrea2001lemon.nummia.ui.nav

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImagenLogo
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo

/**
 * Primera vez, en dos pasos: qué es Nummia (y el sueldo, si quieres) y cómo empezar:
 * conectar tu Drive, importar tu Excel o empezar de cero. [alTerminar] recibe el sueldo y la pantalla a la que ir.
 */
@Composable
fun Bienvenida(alTerminar: (Long?, String?) -> Unit) {
    var sueldo by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var paso by rememberSaveable { mutableStateOf(1) }
    var sueldoLeido by rememberSaveable { mutableStateOf<Long?>(null) }
    androidx.activity.compose.BackHandler(paso == 2) { paso = 1 }
    Surface(color = MaterialTheme.colorScheme.background) {
        androidx.compose.animation.AnimatedContent(
            paso,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (androidx.compose.animation.slideInHorizontally { it * dir / 3 } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally { -it * dir / 3 } + androidx.compose.animation.fadeOut())
            },
            label = "paso",
        ) { p ->
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(Espacio.xxl),
                verticalArrangement = Arrangement.spacedBy(Espacio.xl),
            ) {
                Spacer(Modifier.height(Espacio.xl))
                Indicador(p)
                if (p == 1) {
                    ImagenLogo(Logo.A, Acento.VIOLETA, Modifier.size(80.dp))
                    Text("Bienvenido a Nummia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Apunta lo que gastas y lo que ganas. Nummia hace las cuentas por ti.", style = MaterialTheme.typography.bodyLarge, color = LocalColoresNummia.current.textoSuave)
                    Punto(Iconos.Bolsa, "Gastos y devoluciones", "Aunque te devuelvan el dinero en varias veces o redondeen hacia arriba.")
                    Punto(Iconos.Persona, "Lo que te deben", "Si pagas algo por otra persona, Nummia te recuerda cuánto falta.")
                    Punto(Iconos.Escudo, "Tus datos son tuyos", "Se guardan cifrados en tu móvil. Sin anuncios ni rastreo.")
                    OutlinedTextField(
                        sueldo, { sueldo = it; error = null }, label = { Text("Tu sueldo habitual (opcional)") }, suffix = { Text("€") },
                        singleLine = true, isError = error != null, supportingText = { Text(error ?: "Se apuntará como previsto a final de cada mes.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (sueldo.isBlank()) { sueldoLeido = null; paso = 2 }
                            else leerImporte(sueldo).fold({ sueldoLeido = it; paso = 2 }, { error = (it as? io.github.alexcorrea2001lemon.nummia.domain.dinero.ImporteInvalido)?.motivo })
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Siguiente", fontWeight = FontWeight.Bold) }
                } else {
                    Text("¿Cómo quieres empezar?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Puedes cambiarlo cuando quieras desde Ajustes.", style = MaterialTheme.typography.bodyLarge, color = LocalColoresNummia.current.textoSuave)
                    Opcion(Iconos.Nube, "Conectar mi Google Drive", "Tus datos en una hoja de cálculo que también puedes editar en el ordenador, y copias de seguridad cada día.") {
                        alTerminar(sueldoLeido, Rutas.DRIVE)
                    }
                    Opcion(Iconos.Subir, "Importar mi Excel", "Si ya llevabas tus cuentas en un Excel, tráelas con un resumen antes de guardar nada.") {
                        alTerminar(sueldoLeido, Rutas.IMPORTAR)
                    }
                    Opcion(Iconos.Mas, "Empezar de cero", "Apunta tu primer gasto ahora mismo.") {
                        alTerminar(sueldoLeido, null)
                    }
                    androidx.compose.material3.TextButton(onClick = { paso = 1 }) { Text("Volver") }
                }
            }
        }
    }
}

@Composable
private fun Indicador(paso: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        (1..2).forEach { i ->
            androidx.compose.foundation.layout.Box(
                Modifier.size(width = if (i == paso) 24.dp else 8.dp, height = 8.dp)
                    .androidxBackground(if (i == paso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

private fun Modifier.androidxBackground(color: androidx.compose.ui.graphics.Color) =
    this.then(Modifier.background(color, androidx.compose.foundation.shape.RoundedCornerShape(50)))

@Composable
private fun Opcion(icono: ImageVector, titulo: String, texto: String, alPulsar: () -> Unit) {
    Surface(
        onClick = alPulsar, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalColoresNummia.current.borde),
    ) {
        Row(Modifier.fillMaxWidth().padding(Espacio.l), horizontalArrangement = Arrangement.spacedBy(Espacio.m), verticalAlignment = Alignment.CenterVertically) {
            io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar("", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, icono)
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
            }
            Icon(Iconos.ChevronDer, null, tint = LocalColoresNummia.current.textoSuave)
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
