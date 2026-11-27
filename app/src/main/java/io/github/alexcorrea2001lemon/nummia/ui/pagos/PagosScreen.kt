package io.github.alexcorrea2001lemon.nummia.ui.pagos

import androidx.compose.material3.SegmentedButton
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import io.github.alexcorrea2001lemon.nummia.ui.comun.DialogoNummia
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PagoMensualEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoFijoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PagosViewModel @Inject constructor(private val repo: MovimientosRepo, private val reloj: Reloj) : ViewModel() {
    val pagos: StateFlow<List<PagoMensualEntity>?> = repo.pagos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun historial(id: String): Flow<List<GastoEntity>> = repo.historialPago(id)

    fun guardar(p: PagoMensualEntity) = viewModelScope.launch {
        repo.guardarPago(p)
        repo.asegurarPagosPrevistos(reloj.hoy())
    }

    fun nuevo(concepto: String, importe: Long, dia: Int, cadaMeses: Int = 1, desde: java.time.YearMonth? = null) = guardar(
        PagoMensualEntity(
            UUID.randomUUID().toString(), concepto.trim(), concepto.trim(), null, importe, dia, createdAt = reloj.ahoraMs(), updatedAt = reloj.ahoraMs(),
            cadaMeses = cadaMeses, desde = desde?.toString(),
        ),
    )
    fun hoy() = reloj.hoy()

    fun borrar(p: PagoMensualEntity) = viewModelScope.launch { repo.borrarPago(p.id) }

    // Cobros fijos
    val cobros: StateFlow<List<IngresoFijoEntity>?> = repo.ingresosFijos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun historialCobro(id: String): Flow<List<IngresoEntity>> = repo.historialIngresoFijo(id)
    fun guardarCobro(f: IngresoFijoEntity) = viewModelScope.launch {
        repo.guardarIngresoFijo(f)
        repo.asegurarIngresosPrevistos(reloj.hoy())
    }
    fun nuevoCobro(concepto: String, importe: Long, dia: Int) = guardarCobro(
        IngresoFijoEntity(UUID.randomUUID().toString(), concepto.trim(), TipoIngreso.OTRO, importe, dia, createdAt = reloj.ahoraMs(), updatedAt = reloj.ahoraMs()),
    )
    fun borrarCobro(f: IngresoFijoEntity) = viewModelScope.launch { repo.borrarIngresoFijo(f.id) }
}

private val MES = DateTimeFormatter.ofPattern("MM/yyyy")
private val MES_CORTO = DateTimeFormatter.ofPattern("MMM", java.util.Locale.forLanguageTag("es-ES"))
private val FECHA_COBRO = DateTimeFormatter.ofPattern("d MMM", java.util.Locale.forLanguageTag("es-ES"))
private fun frecuencia(n: Int) = when (n) { 1 -> "cada mes"; 3 -> "cada 3 meses"; 12 -> "cada año"; else -> "cada $n meses" }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PagosScreen(vm: PagosViewModel, snackbar: androidx.compose.material3.SnackbarHostState? = null, alVolver: () -> Unit) {
    val pagos by vm.pagos.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    var editando by remember { mutableStateOf<PagoMensualEntity?>(null) }
    var creando by remember { mutableStateOf(false) }
    var abiertoPago by remember { mutableStateOf<PagoMensualEntity?>(null) }
    var pestana by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }

    val cobros by vm.cobros.collectAsStateWithLifecycle()
    var editandoCobro by remember { mutableStateOf<IngresoFijoEntity?>(null) }
    var creandoCobro by remember { mutableStateOf(false) }
    var abiertoCobro by remember { mutableStateOf<IngresoFijoEntity?>(null) }
    val hoy = remember { vm.hoy() }
    val tnum = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
    fun proximo(p: PagoMensualEntity) = io.github.alexcorrea2001lemon.nummia.domain.calculo.proximoCobro(p.dia, p.cadaMeses, p.mesDesde, hoy)
    fun fecha(f: java.time.LocalDate) = FECHA_COBRO.format(f).removeSuffix(".") + if (f.year != hoy.year) " ${f.year}" else ""

    fun borrarPago(p: PagoMensualEntity) {
        vm.borrar(p)
        snackbar?.let { sb -> alcance.launch {
            val r = sb.showSnackbar("Has borrado «${p.concepto}»", actionLabel = "Deshacer", duration = androidx.compose.material3.SnackbarDuration.Long)
            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) vm.guardar(p)
        } }
    }
    fun borrarCobro(f: IngresoFijoEntity) {
        vm.borrarCobro(f)
        snackbar?.let { sb -> alcance.launch {
            val r = sb.showSnackbar("Has borrado «${f.concepto}»", actionLabel = "Deshacer", duration = androidx.compose.material3.SnackbarDuration.Long)
            if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) vm.guardarCobro(f)
        } }
    }

    PantallaAjustes("Pagos y cobros fijos", alVolver) {
        val lista = pagos.orEmpty()
        val listaCobros = cobros.orEmpty()
        androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Pagos · ${lista.size}", "Cobros · ${listaCobros.size}").forEachIndexed { i, t ->
                SegmentedButton(
                    selected = pestana == i, onClick = { pestana = i },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(t) }
            }
        }
        androidx.compose.animation.AnimatedContent(pestana, label = "pestaña") { p ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
                if (p == 0) {
                    if (pagos != null && lista.isEmpty()) {
                        Tarjeta {
                            EstadoVacio(
                                Iconos.Repetir, "Sin pagos fijos",
                                "Suscripciones, gimnasio, seguros… Aparecerán en Inicio para confirmarlos con un toque.",
                                accion = "Añadir pago", alPulsar = { creando = true },
                            )
                        }
                    } else if (lista.isNotEmpty()) {
                        val activos = lista.filter { it.activo }
                        val alAnio = activos.sumOf { io.github.alexcorrea2001lemon.nummia.domain.calculo.costeAnual(it.importe, it.cadaMeses) }
                        val siguiente = activos.minByOrNull { proximo(it) }
                        Tarjeta(fondo = c.heroe) {
                            Text("Te cuestan al año", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                            Text(formatearEuros(alAnio), style = MaterialTheme.typography.headlineMedium.merge(tnum), fontWeight = FontWeight.Bold)
                            Text("${formatearEuros(alAnio / 12)} al mes de media", style = MaterialTheme.typography.bodySmall.merge(tnum), color = c.textoSuave)
                            if (siguiente != null) {
                                androidx.compose.material3.Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                                    Row(Modifier.fillMaxWidth().padding(Espacio.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                                        Icon(Iconos.Calendario, null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Próximo: ${siguiente.concepto}, el ${fecha(proximo(siguiente))}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(formatearEuros(siguiente.importe), style = MaterialTheme.typography.bodyMedium.merge(tnum), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                            lista.forEach { pg ->
                                FilaFija(
                                    pg.concepto,
                                    "${formatearEuros(pg.importe)} · ${frecuencia(pg.cadaMeses)} · " + if (pg.activo) fecha(proximo(pg)) else "en pausa",
                                    pg.activo, alCambiar = { vm.guardar(pg.copy(activo = it)) },
                                ) { abiertoPago = pg }
                                HorizontalDivider(color = c.borde)
                            }
                            FilaAnadir("Añadir pago") { creando = true }
                        }
                        Text("Toca un pago para ver su historial, editarlo o borrarlo.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave, modifier = Modifier.padding(horizontal = Espacio.s))
                    }
                } else {
                    if (cobros != null && listaCobros.isEmpty()) {
                        Tarjeta {
                            EstadoVacio(
                                Iconos.Euro, "Sin cobros fijos",
                                "Un alquiler, una pensión, una paga… Aparecerán en Inicio para confirmarlos con un toque.",
                                accion = "Añadir cobro", alPulsar = { creandoCobro = true },
                            )
                        }
                    } else if (listaCobros.isNotEmpty()) {
                        val activos = listaCobros.filter { it.activo }
                        Tarjeta(fondo = c.heroe) {
                            Text("Cobras al mes", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                            Text(formatearEuros(activos.sumOf { it.importe }, conSigno = true), style = MaterialTheme.typography.headlineMedium.merge(tnum), fontWeight = FontWeight.Bold, color = c.ingreso)
                            Text(if (activos.size == 1) "1 cobro activo" else "${activos.size} cobros activos", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                        }
                        Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                            listaCobros.forEach { f ->
                                FilaFija(f.concepto, "${formatearEuros(f.importe)} · día ${f.dia}${if (f.activo) "" else " · en pausa"}", f.activo, alCambiar = { vm.guardarCobro(f.copy(activo = it)) }) { abiertoCobro = f }
                                HorizontalDivider(color = c.borde)
                            }
                            FilaAnadir("Añadir cobro") { creandoCobro = true }
                        }
                    }
                }
            }
        }
    }

    abiertoPago?.let { abierto ->
        val pg = pagos.orEmpty().firstOrNull { it.id == abierto.id } ?: abierto
        val flujo = remember(pg.id) { vm.historial(pg.id) }
        val hist by flujo.collectAsStateWithLifecycle(emptyList())
        val cerrar = { abiertoPago = null }
        io.github.alexcorrea2001lemon.nummia.ui.comun.PanelNummia(alCerrar = cerrar) {
            io.github.alexcorrea2001lemon.nummia.ui.comun.CabeceraPanel(
                { io.github.alexcorrea2001lemon.nummia.ui.comun.AvatarIniciales(pg.concepto.trim().take(1).uppercase().ifEmpty { "?" }, tam = 52.dp) },
                pg.concepto, "${formatearEuros(pg.importe)} ${frecuencia(pg.cadaMeses)} · día ${pg.dia}",
            )
            io.github.alexcorrea2001lemon.nummia.ui.comun.DatosPanel(
                "Próximo" to if (pg.activo) fecha(proximo(pg)) else "En pausa",
                "Al año" to formatearEuros(io.github.alexcorrea2001lemon.nummia.domain.calculo.costeAnual(pg.importe, pg.cadaMeses)),
                "Pagado" to formatearEuros(hist.sumOf { it.importe }),
            )
            HistorialFijo("Lo que has pagado", hist.map { it.fecha to it.importe }, "Aún no hay pagos confirmados.")
            BotonesFijo(pg.activo, alEditar = { cerrar(); editando = pg }, alPausar = { vm.guardar(pg.copy(activo = !pg.activo)) })
            io.github.alexcorrea2001lemon.nummia.ui.comun.AccionPanel(Iconos.Papelera, "Borrar pago", color = MaterialTheme.colorScheme.error) { cerrar(); borrarPago(pg) }
        }
    }
    abiertoCobro?.let { abierto ->
        val f = cobros.orEmpty().firstOrNull { it.id == abierto.id } ?: abierto
        val flujo = remember(f.id) { vm.historialCobro(f.id) }
        val hist by flujo.collectAsStateWithLifecycle(emptyList())
        val cerrar = { abiertoCobro = null }
        io.github.alexcorrea2001lemon.nummia.ui.comun.PanelNummia(alCerrar = cerrar) {
            io.github.alexcorrea2001lemon.nummia.ui.comun.CabeceraPanel(
                { io.github.alexcorrea2001lemon.nummia.ui.comun.AvatarIniciales(f.concepto.trim().take(1).uppercase().ifEmpty { "?" }, tam = 52.dp, fondo = c.ingresoFondo, color = c.ingreso) },
                f.concepto, "${formatearEuros(f.importe)} cada mes · día ${f.dia}",
            )
            io.github.alexcorrea2001lemon.nummia.ui.comun.DatosPanel(
                "Al mes" to formatearEuros(f.importe),
                "Al año" to formatearEuros(f.importe * 12),
                "Cobrado" to formatearEuros(hist.sumOf { it.importe }),
            )
            HistorialFijo("Lo que has cobrado", hist.map { it.fecha to it.importe }, "Aún no hay cobros confirmados.")
            BotonesFijo(f.activo, alEditar = { cerrar(); editandoCobro = f }, alPausar = { vm.guardarCobro(f.copy(activo = !f.activo)) })
            io.github.alexcorrea2001lemon.nummia.ui.comun.AccionPanel(Iconos.Papelera, "Borrar cobro", color = MaterialTheme.colorScheme.error) { cerrar(); borrarCobro(f) }
        }
    }

    if (creandoCobro) DialogoPago(null, alCerrar = { creandoCobro = false }, titulo = "Nuevo cobro") { concepto, importe, dia -> vm.nuevoCobro(concepto, importe, dia); creandoCobro = false }
    editandoCobro?.let { f ->
        DialogoPago(null, alCerrar = { editandoCobro = null }, titulo = "Editar cobro", inicial = Triple(f.concepto, f.importe, f.dia)) { concepto, importe, dia ->
            vm.guardarCobro(f.copy(concepto = concepto.trim(), importe = importe, dia = dia))
            editandoCobro = null
        }
    }

    if (creando) DialogoPago(null, alCerrar = { creando = false }, titulo = "Nuevo pago", conFrecuencia = { concepto, importe, dia, cada, desde ->
        vm.nuevo(concepto, importe, dia, cada, desde); creando = false
    })
    editando?.let { p ->
        DialogoPago(p, alCerrar = { editando = null }, conFrecuencia = { concepto, importe, dia, cada, desde ->
            vm.guardar(
                p.copy(
                    concepto = concepto.trim(), tienda = if (p.tienda == p.concepto) concepto.trim() else p.tienda, importe = importe, dia = dia,
                    cadaMeses = cada, desde = desde?.toString(),
                ),
            )
            editando = null
        })
    }
}

/** Fila de un pago o cobro fijo: se toca para ver el detalle; el interruptor lo pausa. */
@Composable
private fun FilaFija(titulo: String, detalle: String, activo: Boolean, alCambiar: (Boolean) -> Unit, alPulsar: () -> Unit) {
    val c = LocalColoresNummia.current
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Ver detalle", onClick = alPulsar).padding(start = Espacio.l, end = Espacio.m, top = Espacio.m, bottom = Espacio.m),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        io.github.alexcorrea2001lemon.nummia.ui.comun.AvatarIniciales(
            titulo.trim().take(1).uppercase().ifEmpty { "?" },
            fondo = if (activo) null else MaterialTheme.colorScheme.surfaceContainerHighest, color = if (activo) null else c.textoSuave,
        )
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (activo) MaterialTheme.colorScheme.onSurface else c.textoSuave)
            Text(detalle, style = MaterialTheme.typography.bodySmall.merge(androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")), color = c.textoSuave)
        }
        Switch(checked = activo, onCheckedChange = alCambiar, modifier = Modifier.semantics { contentDescription = if (activo) "Activo" else "En pausa" })
    }
}

@Composable
private fun FilaAnadir(texto: String, alPulsar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        Box(
            Modifier.size(40.dp).border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Iconos.Mas, null, tint = MaterialTheme.colorScheme.primary) }
        Text(texto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun HistorialFijo(titulo: String, filas: List<Pair<java.time.LocalDate, Long>>, vacio: String) {
    val c = LocalColoresNummia.current
    Column {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
        if (filas.isEmpty()) Text(vacio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = Espacio.s))
        filas.take(8).forEachIndexed { n, (f, importe) ->
            if (n > 0) HorizontalDivider(color = c.borde)
            Row(Modifier.fillMaxWidth().padding(vertical = Espacio.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Icon(Iconos.Check, null, tint = c.ingreso, modifier = Modifier.size(18.dp))
                Text(io.github.alexcorrea2001lemon.nummia.ui.comun.fechaCorta(f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(formatearEuros(importe), style = MaterialTheme.typography.bodyMedium.merge(androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")))
            }
        }
        if (filas.size > 8) Text("y ${filas.size - 8} más", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
    }
}

@Composable
private fun BotonesFijo(activo: Boolean, alEditar: () -> Unit, alPausar: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Button(onClick = alEditar, modifier = Modifier.weight(1f).height(48.dp)) {
            Icon(Iconos.Lapiz, null, modifier = Modifier.size(18.dp)); Text("  Editar")
        }
        androidx.compose.material3.OutlinedButton(onClick = alPausar, modifier = Modifier.weight(1f).height(48.dp)) {
            Text(if (activo) "Pausar" else "Reanudar")
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DialogoPago(
    p: PagoMensualEntity?, alCerrar: () -> Unit,
    titulo: String = if (p == null) "Nuevo pago mensual" else "Editar pago",
    inicial: Triple<String, Long, Int>? = p?.let { Triple(it.concepto, it.importe, it.dia) },
    /** Solo pagos: con esto se elige si es mensual, trimestral o anual (concepto, importe, día, cada cuántos meses, mes del primer cobro). */
    conFrecuencia: ((String, Long, Int, Int, java.time.YearMonth?) -> Unit)? = null,
    alGuardar: (String, Long, Int) -> Unit = { _, _, _ -> },
) {
    val esteMes = remember { java.time.YearMonth.now() }
    var cada by remember { mutableStateOf(p?.cadaMeses ?: 1) }
    // Para trimestrales y anuales: en qué mes toca el próximo cobro (de los 12 siguientes).
    var mesCobro by remember {
        mutableStateOf(
            p?.takeIf { it.cadaMeses > 1 }?.let {
                val f = io.github.alexcorrea2001lemon.nummia.domain.calculo.proximoCobro(it.dia, it.cadaMeses, it.mesDesde, java.time.LocalDate.now())
                java.time.YearMonth.from(f)
            } ?: esteMes,
        )
    }
    var concepto by remember { mutableStateOf(inicial?.first.orEmpty()) }
    var importe by remember { mutableStateOf(inicial?.second?.let { formatearEuros(it).removeSuffix("€").trim() }.orEmpty()) }
    var dia by remember { mutableStateOf(inicial?.third?.toString() ?: "1") }
    var error by remember { mutableStateOf<String?>(null) }
    DialogoNummia(
        onDismissRequest = alCerrar,
        icon = { Icon(if (conFrecuencia != null) Iconos.Repetir else Iconos.Euro, null) },
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                OutlinedTextField(
                    concepto, { concepto = it; error = null }, label = { Text("Concepto") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Iconos.Lapiz, null) },
                )
                OutlinedTextField(
                    importe, { importe = it; error = null }, label = { Text("Importe") }, suffix = { Text("€") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Iconos.Euro, null) },
                )
                OutlinedTextField(
                    dia, { dia = it.filter(Char::isDigit).take(2); error = null }, label = { Text("Día del mes (1–31)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Iconos.Calendario, null) },
                )
                if (conFrecuencia != null) {
                    Text("¿Cada cuánto se paga?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val opciones = listOf(1 to "Mensual", 3 to "Trimestral", 12 to "Anual")
                        opciones.forEachIndexed { k, (n, t) ->
                            SegmentedButton(
                                selected = cada == n, onClick = { cada = n },
                                shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(k, opciones.size),
                                label = { Text(t, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                    AnimatedVisibility(cada > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            Text("¿En qué mes es el próximo cobro?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                            // Los 12 meses que vienen en una cuadrícula: todo a la vista, sin deslizar.
                            (0L until 12L).map { esteMes.plusMonths(it) }.chunked(4).forEach { fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                                    fila.forEach { ym ->
                                        val elegido = mesCobro == ym
                                        androidx.compose.material3.Surface(
                                            onClick = { mesCobro = ym }, modifier = Modifier.weight(1f).height(44.dp),
                                            shape = MaterialTheme.shapes.small,
                                            color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    MES_CORTO.format(ym).removeSuffix(".").replaceFirstChar { it.uppercase() } + if (ym.year != esteMes.year) " ${ym.year % 100}" else "",
                                                    style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val i = leerImporte(importe).getOrNull()
                val d = dia.toIntOrNull()
                error = when {
                    concepto.isBlank() -> "Escribe un concepto"
                    i == null || i <= 0 -> "Importe no válido"
                    d == null || d !in 1..31 -> "El día tiene que estar entre 1 y 31"
                    else -> null
                }
                if (error == null) {
                    if (conFrecuencia != null) conFrecuencia(concepto, i!!, d!!, cada, if (cada > 1) mesCobro else null)
                    else alGuardar(concepto, i!!, d!!)
                }
            }, modifier = Modifier.height(48.dp)) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = alCerrar, modifier = Modifier.height(48.dp)) { Text("Cancelar") } },
    )
}
