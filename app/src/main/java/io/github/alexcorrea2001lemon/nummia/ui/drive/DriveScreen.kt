package io.github.alexcorrea2001lemon.nummia.ui.drive

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.drive.ArchivoDrive
import io.github.alexcorrea2001lemon.nummia.data.drive.ConflictoEntity
import io.github.alexcorrea2001lemon.nummia.data.drive.Pestana
import io.github.alexcorrea2001lemon.nummia.data.drive.desdeFirma
import io.github.alexcorrea2001lemon.nummia.data.drive.fechaDeCopia
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.OpcionAjuste
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.OpcionInterruptor
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun cuando(ms: Long): String {
    val f = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
    val hoy = LocalDate.now()
    return when (f.toLocalDate()) {
        hoy -> "hoy a las ${HORA.format(f)}"
        hoy.minusDays(1) -> "ayer a las ${HORA.format(f)}"
        else -> FECHA_HORA.format(f)
    }
}

/** Una línea legible de una fila guardada como firma (o «Borrado»). */
private fun describir(pestana: String, firma: String?): String {
    if (firma == null) return "Borrado"
    val c = desdeFirma(firma)
    fun fecha(s: String) = s.toLongOrNull()?.let { FECHA.format(LocalDate.ofEpochDay(it)) }.orEmpty()
    fun euros(s: String) = s.toLongOrNull()?.let { formatearEuros(it) }.orEmpty()
    return runCatching {
        when (Pestana.valueOf(pestana)) {
            Pestana.GASTOS -> "${c[3]} · ${euros(c[4])} · ${fecha(c[1])}"
            Pestana.INGRESOS -> "${c[2]} · ${euros(c[4])} · ${fecha(c[1])}"
            Pestana.REEMBOLSOS -> "Devolución de ${c[1]} · ${euros(c[3])}"
        }
    }.getOrDefault("—")
}

@Composable
fun DriveScreen(vm: DriveViewModel, snackbar: SnackbarHostState, alVolver: () -> Unit) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val evento by vm.evento.collectAsStateWithLifecycle()
    val copias by vm.copias.collectAsStateWithLifecycle()
    val trabajando by vm.trabajando.collectAsStateWithLifecycle()
    val errorConexion by vm.errorConexion.collectAsStateWithLifecycle()
    val conectando by vm.conectando.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var restaurando by remember { mutableStateOf<ArchivoDrive?>(null) }
    var desconectando by remember { mutableStateOf(false) }

    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { vm.resultadoPermiso(it.resultCode, it.data) }

    LaunchedEffect(evento) {
        when (val ev = evento) {
            is EventoDrive.PedirPermiso -> { permiso.launch(IntentSenderRequest.Builder(ev.pendiente).build()); vm.eventoVisto() }
            is EventoDrive.Mensaje -> { vm.eventoVisto(); snackbar.showSnackbar(ev.texto) }
            else -> Unit
        }
    }
    LaunchedEffect(e.conectado) { if (e.conectado && copias == null) vm.cargarCopias() }

    PantallaAjustes("Tu Drive", alVolver) {
        if (!e.conectado) {
            Tarjeta {
                EstadoVacio(
                    Iconos.Nube, "Guarda tus datos en tu Drive",
                    "Nummia crea en tu Google Drive la carpeta «Nummia_Datos» con una hoja de cálculo por año que también puedes editar desde el ordenador, y guarda copias de seguridad cada día. Solo puede ver los archivos que ella misma crea.",
                    accion = if (conectando) null else "Conectar con Google", alPulsar = vm::conectar,
                )
                if (conectando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            errorConexion?.let { m ->
                Tarjeta(fondo = LocalColoresNummia.current.avisoFondo) {
                    Text("No se ha podido conectar", fontWeight = FontWeight.SemiBold)
                    Text(m, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {

        // ---- Estado
        Tarjeta(modifier = Modifier.animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Text("Conectado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (e.ocupado) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Text(e.correo.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
            Text(
                when {
                    e.ocupado -> "Sincronizando…"
                    e.ultima != null -> "Última sincronización: ${cuando(e.ultima!!)}" + if (e.pendiente) " · hay cambios por subir" else ""
                    else -> "Todavía no se ha sincronizado"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (e.ultima != null && !e.ocupado) {
                Text(
                    e.cambiosOrdenador ?: "Sin cambios desde el ordenador",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (e.cambiosOrdenador != null) MaterialTheme.colorScheme.primary else LocalColoresNummia.current.textoSuave,
                )
            }
            AnimatedVisibility(e.error != null && !e.ocupado) {
                Text(e.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.aviso)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Button(onClick = vm::sincronizarAhora, enabled = !e.ocupado) { Text("Sincronizar ahora") }
                e.carpeta?.let { id ->
                    OutlinedButton(onClick = {
                        runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/drive/folders/$id"))) }
                    }) { Text("Abrir en Drive") }
                }
            }
        }

        // ---- Conflictos
        if (e.conflictos.isNotEmpty()) {
            Text("Cambios en los dos sitios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Has cambiado lo mismo en el móvil y en el ordenador. Elige con qué versión te quedas; la otra se descarta.",
                style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
            )
            e.conflictos.forEach { c -> TarjetaConflicto(c, vm::resolver) }
        }

        // ---- Avisos de filas que no se pudieron leer
        if (e.avisos.isNotEmpty()) {
            Tarjeta(fondo = LocalColoresNummia.current.avisoFondo) {
                Text("Filas de la hoja que no se han podido leer", fontWeight = FontWeight.SemiBold)
                e.avisos.take(8).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                if (e.avisos.size > 8) Text("y ${e.avisos.size - 8} más", style = MaterialTheme.typography.bodySmall)
                Text("Corrígelas en la hoja y se leerán en la próxima sincronización.", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ---- Ajustes
        Tarjeta(relleno = false) {
            OpcionInterruptor(Iconos.Wifi, "Solo con Wi-Fi", "Sincroniza solo cuando estés conectado a una red Wi-Fi", e.soloWifi, vm::soloWifi)
        }

        // ---- Copias de seguridad
        Text("Copias de seguridad en tu Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Se guarda una al día si has cambiado algo. Se conservan las de los últimos 7 días, 4 semanas y 12 meses.",
            style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave,
        )
        if (trabajando) LinearProgressIndicator(Modifier.fillMaxWidth())
        Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
            OpcionAjuste(Iconos.Subir, "Hacer una copia ahora", null) { if (!trabajando) vm.copiaAhora() }
            when (val lista = copias) {
                null -> Row(Modifier.fillMaxWidth().padding(Espacio.l), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                else -> lista.forEach { a ->
                    HorizontalDivider(color = LocalColoresNummia.current.borde)
                    val f = fechaDeCopia(a.nombre)
                    OpcionAjuste(
                        Iconos.Descargar,
                        f?.let { FECHA_HORA.format(it) } ?: a.nombre,
                        a.tamano?.let { "${(it + 1023) / 1024} KB" },
                    ) { if (!trabajando) restaurando = a }
                }
            }
        }

        Tarjeta(relleno = false) {
            OpcionAjuste(Iconos.Cerrar, "Desconectar Drive", "Deja de sincronizar. No se borra nada.") { desconectando = true }
        }
        }
    }

    (evento as? EventoDrive.OfrecerRecuperar)?.let { ev ->
        DialogoNummia(
            onDismissRequest = { vm.eventoVisto(); vm.sincronizarAhora() },
            title = { Text("Hay una copia en tu Drive") },
            text = { Text("Este móvil está vacío y en tu Drive hay una copia del ${fechaDeCopia(ev.copia.nombre)?.let { FECHA_HORA.format(it) } ?: ev.copia.nombre}. ¿La recuperas?") },
            confirmButton = { Button(onClick = { vm.eventoVisto(); vm.restaurar(ev.copia) }) { Text("Recuperar") } },
            dismissButton = { TextButton(onClick = { vm.eventoVisto(); vm.sincronizarAhora() }) { Text("Empezar de cero") } },
        )
    }

    restaurando?.let { a ->
        DialogoNummia(
            onDismissRequest = { restaurando = null },
            title = { Text("¿Recuperar esta copia?") },
            text = { Text("Los datos del móvil se sustituirán por los de la copia y la hoja de tu Drive quedará igual. Antes se guarda una copia de lo que tienes ahora, por si acaso.") },
            confirmButton = { Button(onClick = { restaurando = null; vm.restaurar(a) }) { Text("Recuperar") } },
            dismissButton = { TextButton(onClick = { restaurando = null }) { Text("Cancelar") } },
        )
    }

    if (desconectando) {
        DialogoNummia(
            onDismissRequest = { desconectando = false },
            title = { Text("¿Desconectar Drive?") },
            text = { Text("El móvil deja de sincronizar. No se borra nada ni del móvil ni de tu Drive; puedes volver a conectar cuando quieras.") },
            confirmButton = { TextButton(onClick = { desconectando = false; vm.desconectar() }) { Text("Desconectar") } },
            dismissButton = { TextButton(onClick = { desconectando = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TarjetaConflicto(c: ConflictoEntity, resolver: (ConflictoEntity, Boolean) -> Unit) {
    Tarjeta {
        Text("En el móvil", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave)
        Text(describir(c.pestana, c.local), style = MaterialTheme.typography.bodyLarge)
        Text("En el ordenador", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave)
        Text(describir(c.pestana, c.remoto), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            OutlinedButton(onClick = { resolver(c, true) }) { Text("Usar la del móvil") }
            OutlinedButton(onClick = { resolver(c, false) }) { Text("Usar la del ordenador") }
        }
    }
}
