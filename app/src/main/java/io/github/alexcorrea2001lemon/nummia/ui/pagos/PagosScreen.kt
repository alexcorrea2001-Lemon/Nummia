package io.github.alexcorrea2001lemon.nummia.ui.pagos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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

@Composable
fun PagosScreen(vm: PagosViewModel, alVolver: () -> Unit) {
    val pagos by vm.pagos.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    var editando by remember { mutableStateOf<PagoMensualEntity?>(null) }
    var creando by remember { mutableStateOf(false) }
    var abierto by remember { mutableStateOf<String?>(null) }

    val cobros by vm.cobros.collectAsStateWithLifecycle()
    var editandoCobro by remember { mutableStateOf<IngresoFijoEntity?>(null) }
    var creandoCobro by remember { mutableStateOf(false) }

    PantallaAjustes("Pagos y cobros fijos", alVolver) {
        Text("Pagos y suscripciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val lista = pagos.orEmpty()
        if (pagos != null && lista.isEmpty()) {
            Tarjeta {
                EstadoVacio(
                    Iconos.Repetir, "Sin pagos fijos",
                    "Gimnasio, móvil, suscripciones, el seguro del coche… Cada mes, cada 3 meses o cada año: aparecerán en Inicio para confirmarlos con un toque, aunque el precio cambie.",
                    accion = "Añadir pago", alPulsar = { creando = true },
                )
            }
            Text("También puedes activar «Se paga cada mes» al apuntar un gasto.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        } else if (lista.isNotEmpty()) {
            val activos = lista.filter { it.activo }
            val alAnio = activos.sumOf { io.github.alexcorrea2001lemon.nummia.domain.calculo.costeAnual(it.importe, it.cadaMeses) }
            Tarjeta {
                Text("Te cuestan al año", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                Text(formatearEuros(alAnio), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("${formatearEuros(alAnio / 12)} al mes de media · ${activos.size} pagos activos", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                lista.forEachIndexed { n, p ->
                    if (n > 0) HorizontalDivider(color = c.borde)
                    Column(Modifier.fillMaxWidth().clickable { abierto = if (abierto == p.id) null else p.id }.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (p.activo) MaterialTheme.colorScheme.onSurface else c.textoSuave)
                                val hoy = remember { vm.hoy() }
                                val proximo = remember(p) { io.github.alexcorrea2001lemon.nummia.domain.calculo.proximoCobro(p.dia, p.cadaMeses, p.mesDesde, hoy) }
                                Text(
                                    "${formatearEuros(p.importe)} · ${frecuencia(p.cadaMeses)}" +
                                        if (p.activo) " · próximo: ${FECHA_COBRO.format(proximo).removeSuffix(".")}${if (proximo.year != hoy.year) " ${proximo.year}" else ""}" else " · en pausa",
                                    style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                                )
                                if (p.cadaMeses > 1) Text("${formatearEuros(io.github.alexcorrea2001lemon.nummia.domain.calculo.costeAnual(p.importe, p.cadaMeses))} al año", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                            Switch(checked = p.activo, onCheckedChange = { vm.guardar(p.copy(activo = it)) })
                        }
                        AnimatedVisibility(abierto == p.id) {
                            val flujo = remember(p.id) { vm.historial(p.id) }
                            val hist by flujo.collectAsStateWithLifecycle(emptyList())
                            Column(Modifier.padding(top = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                                Text("Lo que has pagado", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                                if (hist.isEmpty()) Text("Aún no hay pagos confirmados.", style = MaterialTheme.typography.bodySmall)
                                hist.take(12).forEach { g ->
                                    Row {
                                        Text(MES.format(g.fecha), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(formatearEuros(g.importe), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                                    TextButton(onClick = { editando = p }) { Text("Editar") }
                                    TextButton(onClick = { vm.borrar(p) }) { Text("Borrar", color = c.gasto) }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { creando = true }) { Text("+ Añadir pago") }
        }

        // ---------- Cobros: lo que entra cada mes ----------
        Text("Cobros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = Espacio.m))
        val listaCobros = cobros.orEmpty()
        if (cobros != null && listaCobros.isEmpty()) {
            Tarjeta {
                Text("Lo que cobras cada mes", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "Un alquiler, una pensión, una paga… Cada mes aparecerá en Inicio para confirmarlo con un toque. También puedes activar «Se cobra cada mes» al apuntar un ingreso.",
                    style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                )
                TextButton(onClick = { creandoCobro = true }) { Text("+ Añadir cobro") }
            }
        } else if (listaCobros.isNotEmpty()) {
            val activos = listaCobros.filter { it.activo }
            Tarjeta {
                Text("Al mes", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                Text(formatearEuros(activos.sumOf { it.importe }, conSigno = true), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = c.ingreso)
                Text("${activos.size} cobros activos", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
            }
            Tarjeta(relleno = false, modifier = Modifier.animateContentSize()) {
                listaCobros.forEachIndexed { n, f ->
                    if (n > 0) HorizontalDivider(color = c.borde)
                    Column(Modifier.fillMaxWidth().clickable { abierto = if (abierto == f.id) null else f.id }.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (f.activo) MaterialTheme.colorScheme.onSurface else c.textoSuave)
                                Text("${formatearEuros(f.importe)} · día ${f.dia}${if (f.activo) "" else " · en pausa"}", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            }
                            Switch(checked = f.activo, onCheckedChange = { vm.guardarCobro(f.copy(activo = it)) })
                        }
                        AnimatedVisibility(abierto == f.id) {
                            val flujo = remember(f.id) { vm.historialCobro(f.id) }
                            val hist by flujo.collectAsStateWithLifecycle(emptyList())
                            Column(Modifier.padding(top = Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                                Text("Lo que has cobrado", style = MaterialTheme.typography.labelMedium, color = c.textoSuave)
                                if (hist.isEmpty()) Text("Aún no hay cobros confirmados.", style = MaterialTheme.typography.bodySmall)
                                hist.take(12).forEach { i ->
                                    Row {
                                        Text(MES.format(i.fecha), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(formatearEuros(i.importe), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                                    TextButton(onClick = { editandoCobro = f }) { Text("Editar") }
                                    TextButton(onClick = { vm.borrarCobro(f) }) { Text("Borrar", color = c.gasto) }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { creandoCobro = true }) { Text("+ Añadir cobro") }
        }
    }

    if (creandoCobro) DialogoPago(null, alCerrar = { creandoCobro = false }, titulo = "Nuevo cobro mensual") { concepto, importe, dia -> vm.nuevoCobro(concepto, importe, dia); creandoCobro = false }
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
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                OutlinedTextField(concepto, { concepto = it; error = null }, label = { Text("Concepto") }, singleLine = true)
                OutlinedTextField(
                    importe, { importe = it; error = null }, label = { Text("Importe") }, suffix = { Text("€") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    dia, { dia = it.filter(Char::isDigit).take(2); error = null }, label = { Text("Día del mes (1–31)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (conFrecuencia != null) {
                    Text("Se paga", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        listOf(1 to "Cada mes", 3 to "Cada 3 meses", 12 to "Cada año").forEach { (n, t) ->
                            androidx.compose.material3.FilterChip(selected = cada == n, onClick = { cada = n }, label = { Text(t, maxLines = 1) })
                        }
                    }
                    AnimatedVisibility(cada > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                            Text("Próximo cobro en", style = MaterialTheme.typography.labelLarge)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                                (0L until 12L).map { esteMes.plusMonths(it) }.forEach { ym ->
                                    androidx.compose.material3.FilterChip(
                                        selected = mesCobro == ym, onClick = { mesCobro = ym },
                                        label = { Text(MES_CORTO.format(ym).removeSuffix(".") + if (ym.year != esteMes.year) " ${ym.year % 100}" else "", maxLines = 1) },
                                    )
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
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
