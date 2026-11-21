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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(titulo: String, alVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Iconos.Atras, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
fun OpcionAjuste(icono: ImageVector, titulo: String, subtitulo: String? = null, alPulsar: (() -> Unit)? = null, final: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (alPulsar != null) it.clickable(onClick = alPulsar) else it }.padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Avatar("", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, icono)
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
    OpcionAjuste(icono, titulo, subtitulo, alPulsar = { alCambiar(!valor) }) { Switch(checked = valor, onCheckedChange = alCambiar) }
}

val SinRelleno = PaddingValues()
