package io.github.alexcorrea2001lemon.nummia.ui.importar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.importar.ExcelAnual
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.formulario.DialogoNombre
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.format.DateTimeFormatter

private val FECHA = DateTimeFormatter.ofPattern("dd/MM")

@Composable
fun ImportarScreen(vm: ImportarViewModel, alVolver: () -> Unit, alTerminar: () -> Unit) {
    val e by vm.estado.collectAsStateWithLifecycle()
    val personas by vm.personas.collectAsStateWithLifecycle()
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.elegir(uri) }
    // Se deja elegir cualquier archivo: según la app de archivos (o Drive), el Excel puede aparecer con otro tipo y quedar gris.
    // Después se comprueba que de verdad es un Excel.
    val elegir = { abrir.launch(arrayOf("*/*")) }

    PantallaAjustes("Importar Excel", alVolver) {
        AnimatedContent(e, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it::class }, label = "importar") { s ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
                when (s) {
                    EstadoImportar.Inicial -> Tarjeta {
                        EstadoVacio(
                            Iconos.Subir, "Trae tu Excel de cuentas",
                            "Elige el archivo .xlsx que llevabas a mano. Antes de guardar nada verás un resumen para comprobar que cuadra mes a mes con tu Excel.",
                            accion = "Elegir archivo", alPulsar = elegir,
                        )
                    }
                    EstadoImportar.Leyendo, EstadoImportar.Importando -> Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Espacio.m),
                    ) {
                        CircularProgressIndicator()
                        Text(if (s == EstadoImportar.Leyendo) "Leyendo el Excel…" else "Guardando una copia e importando…")
                    }
                    is EstadoImportar.Error -> Tarjeta {
                        EstadoVacio(Iconos.Aviso, "No se ha podido leer", s.mensaje, accion = "Elegir otro archivo", alPulsar = elegir)
                    }
                    is EstadoImportar.Hecho -> Tarjeta {
                        EstadoVacio(
                            Iconos.Check, "¡Importado!",
                            "${s.r.gastos} gastos, ${s.r.reembolsos} reembolsos y ${s.r.ingresos} ingresos de ${s.anio}. Ya los tienes en Movimientos y en Estadísticas.",
                            accion = "Listo", alPulsar = alTerminar,
                        )
                    }
                    is EstadoImportar.Vista -> Vista(s, personas, vm, elegir)
                }
            }
        }
    }
}

@Composable
private fun Vista(v: EstadoImportar.Vista, personas: List<PersonaEntity>, vm: ImportarViewModel, elegir: () -> Unit) {
    val a = v.analisis
    val c = LocalColoresNummia.current
    var verAvisos by remember { mutableStateOf(false) }
    var verTodos by remember { mutableStateOf(false) }
    var eligiendo by remember { mutableStateOf<Int?>(null) }
    var nuevaPara by remember { mutableStateOf<Int?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    Tarjeta {
        Text(v.archivo, style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Año", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.cambiarAnio(a.anio - 1) }) { Icon(Iconos.ChevronIzq, "Año anterior") }
            Text("${a.anio}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { vm.cambiarAnio(a.anio + 1) }) { Icon(Iconos.ChevronDer, "Año siguiente") }
        }
        Text("${a.gastos.size} gastos · ${a.reembolsos} reembolsos · ${a.ingresos.size} ingresos", fontWeight = FontWeight.SemiBold)
        Text("Pagado ${formatearEuros(a.gastos.sumOf { it.importe })} · Ingresos ${formatearEuros(a.ingresos.sumOf { it.importe })}", color = c.textoSuave)
    }

    if (v.yaHay > 0) {
        Tarjeta(fondo = c.avisoFondo) {
            Text("Ya tienes ${v.yaHay} gastos en ${a.anio}", fontWeight = FontWeight.SemiBold)
            Text("Si ya importaste este Excel, se duplicarían. Impórtalo solo si esos gastos son otros.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    // Mes a mes frente al Excel
    Text("Gasto real de cada mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Tarjeta(relleno = false) {
        a.meses.filter { it.gastoRealCalculado != 0L || it.gastoRealEnExcel != null && it.gastoRealEnExcel != 0L }.forEachIndexed { i, m ->
            if (i > 0) HorizontalDivider(color = c.borde)
            Row(Modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.m), verticalAlignment = Alignment.CenterVertically) {
                Text(ExcelAnual.MESES[m.mes - 1], modifier = Modifier.weight(1f))
                Text(formatearEuros(m.gastoRealCalculado), style = MaterialTheme.typography.bodyLarge)
                Icon(
                    if (m.coincide) Iconos.Check else Iconos.Aviso,
                    if (m.coincide) "Cuadra con el Excel" else "No cuadra con el Excel (${formatearEuros(m.gastoRealEnExcel ?: 0)})",
                    tint = if (m.coincide) c.ingreso else c.aviso, modifier = Modifier.padding(start = Espacio.s).size(18.dp),
                )
            }
        }
    }

    if (a.avisos.isNotEmpty()) {
        Tarjeta(fondo = c.avisoFondo, modifier = Modifier.animateContentSize()) {
            Row(Modifier.fillMaxWidth().clickable { verAvisos = !verAvisos }, verticalAlignment = Alignment.CenterVertically) {
                Text("${a.avisos.size} avisos", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (verAvisos) "Ocultar" else "Ver", color = MaterialTheme.colorScheme.primary)
            }
            if (verAvisos) a.avisos.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }

    // Pagos por otras personas
    Text("¿Pagaste algo por otra persona?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Márcalo y elige quién te lo devuelve: aparecerá en «Te deben». Te enseño primero los que tienen reembolso.",
        style = MaterialTheme.typography.bodyMedium, color = c.textoSuave,
    )
    val candidatos = a.gastos.withIndex().filter { verTodos || it.value.reembolso != null || it.index in v.porOtros }
    Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
        candidatos.forEachIndexed { n, (i, g) ->
            if (n > 0) HorizontalDivider(color = c.borde)
            val marcado = i in v.porOtros
            val quien = v.porOtros[i]?.let { id -> personas.firstOrNull { it.id == id }?.nombre }
            Row(
                Modifier.fillMaxWidth().clickable { if (marcado) vm.porOtro(i, null, false) else eligiendo = i }.padding(horizontal = Espacio.s, vertical = Espacio.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = marcado, onCheckedChange = { if (it) eligiendo = i else vm.porOtro(i, null, false) })
                Column(Modifier.weight(1f)) {
                    Text("${g.nombre} · ${FECHA.format(g.fecha)}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        buildString {
                            append(formatearEuros(g.importe))
                            g.reembolso?.let { append(" · devuelto ${formatearEuros(it)}") }
                            if (marcado) append(" · ${quien ?: "sin persona"}")
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                    )
                }
            }
        }
        if (candidatos.isEmpty()) Text("Ningún gasto tiene reembolso.", modifier = Modifier.padding(Espacio.l), color = c.textoSuave)
        HorizontalDivider(color = c.borde)
        TextButton(onClick = { verTodos = !verTodos }, modifier = Modifier.padding(horizontal = Espacio.s)) {
            Text(if (verTodos) "Ver solo los que tienen reembolso" else "Ver todos los gastos (${a.gastos.size})")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        OutlinedButton(onClick = elegir) { Text("Otro archivo") }
        Button(onClick = { confirmar = true }, modifier = Modifier.weight(1f)) { Text("Importar") }
    }

    eligiendo?.let { i ->
        AlertDialog(
            onDismissRequest = { eligiendo = null },
            title = { Text("¿Quién te lo devuelve?") },
            text = {
                Column {
                    personas.forEach { p ->
                        Text(p.nombre, Modifier.fillMaxWidth().clickable { vm.porOtro(i, p.id, true); eligiendo = null }.padding(vertical = Espacio.m))
                    }
                    Text("+ Nueva persona", color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().clickable { nuevaPara = i; eligiendo = null }.padding(vertical = Espacio.m))
                    Text("No lo sé todavía", color = c.textoSuave, modifier = Modifier.fillMaxWidth().clickable { vm.porOtro(i, null, true); eligiendo = null }.padding(vertical = Espacio.m))
                }
            },
            confirmButton = { TextButton(onClick = { eligiendo = null }) { Text("Cancelar") } },
        )
    }
    nuevaPara?.let { i -> DialogoNombre("Nueva persona", alCerrar = { nuevaPara = null }) { vm.nuevaPersona(it, i); nuevaPara = null } }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Importar ${a.gastos.size} gastos y ${a.ingresos.size} ingresos?") },
            text = { Text("Antes se guarda una copia de seguridad de lo que tienes ahora (en el móvil y, si está conectado, en tu Drive).") },
            confirmButton = { Button(onClick = { confirmar = false; vm.importar() }) { Text("Importar") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } },
        )
    }
}
