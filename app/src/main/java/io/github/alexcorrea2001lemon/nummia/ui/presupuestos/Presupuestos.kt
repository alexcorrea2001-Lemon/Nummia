package io.github.alexcorrea2001lemon.nummia.ui.presupuestos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.PresupuestoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Ambito
import io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Nivel
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearPorMil
import io.github.alexcorrea2001lemon.nummia.domain.dinero.leerImporte
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

/** Nombre legible del presupuesto. */
fun nombrePresupuesto(e: EstadoPresupuesto, categorias: Map<String, String>): String = when (e.limite.ambito) {
    Ambito.TOTAL -> "Todo el mes"
    Ambito.CATEGORIA -> categorias[e.limite.ref] ?: "Categoría"
    Ambito.TIENDA -> e.limite.ref ?: "Tienda"
}

/** Barra de un presupuesto, con muelle al crecer. Verde, ámbar al 80 % y roja al pasarse. */
@Composable
fun BarraPresupuesto(e: EstadoPresupuesto, nombre: String, discreto: Boolean = false) {
    val c = LocalColoresNummia.current
    val color = when (e.nivel) { Nivel.BIEN -> MaterialTheme.colorScheme.primary; Nivel.CERCA -> c.aviso; Nivel.PASADO -> c.gasto }
    val fraccion by animateFloatAsState((e.porMil / 1000f).coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 200f), label = "barra")
    val texto = when (e.nivel) {
        Nivel.PASADO -> "Te has pasado ${formatearEuros(-e.queda, discreto = discreto)}"
        else -> "Quedan ${formatearEuros(e.queda, discreto = discreto)}"
    }
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = "$nombre: ${formatearPorMil(e.porMil)} gastado. $texto" },
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${formatearEuros(e.gastado, discreto = discreto)} de ${formatearEuros(e.limite.limite, discreto = discreto)}", style = MaterialTheme.typography.bodyMedium)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraccion).clip(RoundedCornerShape(50)).background(color))
        }
        Text(texto, style = MaterialTheme.typography.bodySmall, color = if (e.nivel == Nivel.BIEN) c.textoSuave else color)
    }
}

@HiltViewModel
class PresupuestosViewModel @Inject constructor(private val repo: MovimientosRepo, private val db: NummiaDb, private val reloj: Reloj) : ViewModel() {
    val estado: StateFlow<List<EstadoPresupuesto>?> = repo.estadoPresupuestos(YearMonth.from(reloj.hoy()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val categorias: StateFlow<List<CategoriaEntity>> = repo.categorias().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    suspend fun tiendas(): List<String> = db.gastos().tiendasRecientes()

    fun guardar(id: String?, ambito: Ambito, ref: String?, limite: Long) = viewModelScope.launch {
        val ahora = reloj.ahoraMs()
        repo.guardarPresupuesto(PresupuestoEntity(id ?: UUID.randomUUID().toString(), ambito.name, ref?.trim(), limite, ahora, ahora))
    }

    fun borrar(id: String) = viewModelScope.launch { repo.borrarPresupuesto(id) }
}

@Composable
fun PresupuestosScreen(vm: PresupuestosViewModel, alVolver: () -> Unit) {
    val lista by vm.estado.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val nombres = categorias.associate { it.id to it.nombre }
    var editando by remember { mutableStateOf<EstadoPresupuesto?>(null) }
    var creando by remember { mutableStateOf(false) }
    val c = LocalColoresNummia.current

    PantallaAjustes("Presupuestos", alVolver) {
        val l = lista.orEmpty()
        if (lista != null && l.isEmpty()) {
            Tarjeta {
                EstadoVacio(
                    Iconos.Euro, "Ponte un límite",
                    "Elige cuánto quieres gastar al mes en total, en una categoría o en una tienda. Verás cómo vas en Inicio y te avisaremos al llegar al 80 % y al 100 %.",
                    accion = "Crear presupuesto", alPulsar = { creando = true },
                )
            }
        } else if (l.isNotEmpty()) {
            Text("Este mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Tarjeta(relleno = false) {
                l.forEachIndexed { n, e ->
                    if (n > 0) HorizontalDivider(color = c.borde)
                    Box(Modifier.fillMaxWidth().clickable { editando = e }.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
                        BarraPresupuesto(e, nombrePresupuesto(e, nombres))
                    }
                }
            }
            TextButton(onClick = { creando = true }) { Text("+ Añadir presupuesto") }
        }
    }

    if (creando) DialogoPresupuesto(null, categorias, vm, alCerrar = { creando = false })
    editando?.let { e -> DialogoPresupuesto(e, categorias, vm, alCerrar = { editando = null }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoPresupuesto(e: EstadoPresupuesto?, categorias: List<CategoriaEntity>, vm: PresupuestosViewModel, alCerrar: () -> Unit) {
    var ambito by remember { mutableStateOf(e?.limite?.ambito ?: Ambito.TOTAL) }
    var ref by remember { mutableStateOf(e?.limite?.ref) }
    var importe by remember { mutableStateOf(e?.limite?.limite?.let { formatearEuros(it).removeSuffix("€").trim() }.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var tiendas by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { tiendas = vm.tiendas() }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(if (e == null) "Nuevo presupuesto" else "Editar presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    listOf(Ambito.TOTAL to "Total", Ambito.CATEGORIA to "Categoría", Ambito.TIENDA to "Tienda").forEach { (a, t) ->
                        FilterChip(selected = ambito == a, onClick = { if (ambito != a) { ambito = a; ref = null; error = null } }, label = { Text(t) })
                    }
                }
                when (ambito) {
                    Ambito.TOTAL -> Text("Todo lo que gastes en el mes.", style = MaterialTheme.typography.bodySmall)
                    Ambito.CATEGORIA -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        categorias.forEach { cat -> FilterChip(selected = ref == cat.id, onClick = { ref = cat.id; error = null }, label = { Text(cat.nombre) }) }
                    }
                    Ambito.TIENDA -> {
                        OutlinedTextField(ref.orEmpty(), { ref = it; error = null }, label = { Text("Tienda") }, singleLine = true)
                        val q = ref.orEmpty().trim().lowercase()
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            tiendas.filter { q.isEmpty() || it.lowercase().contains(q) }.take(8).forEach { t ->
                                FilterChip(selected = ref == t, onClick = { ref = t }, label = { Text(t) })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    importe, { importe = it; error = null }, label = { Text("Límite al mes") }, suffix = { Text("€") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (e != null) TextButton(onClick = { vm.borrar(e.limite.id); alCerrar() }) { Text("Borrar presupuesto", color = LocalColoresNummia.current.gasto) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val l = leerImporte(importe).getOrNull()
                error = when {
                    ambito != Ambito.TOTAL && ref.isNullOrBlank() -> if (ambito == Ambito.CATEGORIA) "Elige una categoría" else "Escribe la tienda"
                    l == null || l <= 0 -> "Escribe un límite válido"
                    else -> null
                }
                if (error == null) { vm.guardar(e?.limite?.id, ambito, ref, l!!); alCerrar() }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
