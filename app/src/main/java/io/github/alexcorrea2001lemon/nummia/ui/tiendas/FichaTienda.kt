package io.github.alexcorrea2001lemon.nummia.ui.tiendas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.data.repo.real
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PantallaAjustes
import io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMov
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.IconosCategoria
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.aFila
import io.github.alexcorrea2001lemon.nummia.ui.graficos.GraficoBarras
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class Ficha(
    val nombre: String, val total: Long, val veces: Int, val primera: LocalDate?, val ultima: LocalDate?,
    val categoria: CategoriaEntity?, val ultimos12: List<Long>, val etiquetas12: List<String>, val filas: List<FilaMov>,
) {
    val media get() = if (veces == 0) 0 else total / veces
}

@HiltViewModel
class FichaTiendaViewModel @Inject constructor(guardado: SavedStateHandle, private val repo: MovimientosRepo, private val reloj: Reloj) : ViewModel() {
    val nombre: String = guardado.get<String>("nombre").orEmpty()

    val ficha: StateFlow<Ficha?> = combine(repo.gastosDeTienda(nombre), repo.categorias(), repo.personas()) { gs, cats, ps ->
        val porCat = cats.associateBy { it.id }
        val hoy = YearMonth.from(reloj.hoy())
        val meses = (11 downTo 0).map { hoy.minusMonths(it.toLong()) }
        val porMes = gs.groupBy { YearMonth.from(it.gasto.fecha) }.mapValues { (_, l) -> l.sumOf { it.real } }
        Ficha(
            nombre = gs.groupingBy { it.gasto.tienda.trim() }.eachCount().maxByOrNull { it.value }?.key ?: nombre,
            total = gs.sumOf { it.real }, veces = gs.size,
            primera = gs.minOfOrNull { it.gasto.fecha }, ultima = gs.maxOfOrNull { it.gasto.fecha },
            categoria = gs.mapNotNull { it.gasto.categoriaId }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.let(porCat::get),
            ultimos12 = meses.map { porMes[it] ?: 0L },
            etiquetas12 = meses.map { it.month.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, io.github.alexcorrea2001lemon.nummia.ui.comun.ES).uppercase() },
            filas = gs.sortedByDescending { it.gasto.fecha }.take(50).map { it.aFila(ps.associate { p -> p.id to p.nombre }, cats.associate { c -> c.id to c.nombre }) },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categorias: StateFlow<List<CategoriaEntity>> = repo.categorias().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pone una categoría a todos los gastos de esta tienda. */
    fun categoriaParaTodos(categoria: String?) = viewModelScope.launch {
        repo.cambiarCategoria(repo.gastosDeTienda(nombre).first().map { it.gasto.id }, categoria)
    }
}

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun FichaTiendaScreen(vm: FichaTiendaViewModel, alAbrirGasto: (String) -> Unit, alVolver: () -> Unit) {
    val f by vm.ficha.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    var eligiendo by remember { mutableStateOf(false) }

    PantallaAjustes(f?.nombre ?: vm.nombre, alVolver) {
        val ficha = f ?: return@PantallaAjustes
        if (ficha.veces == 0) {
            Tarjeta { EstadoVacio(Iconos.Bolsa, "Sin gastos aquí", "Cuando apuntes gastos en ${vm.nombre} aparecerán en esta ficha.") }
            return@PantallaAjustes
        }
        Tarjeta(fondo = c.heroe) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                val cat = ficha.categoria
                if (cat != null) Avatar(cat.nombre, Color(cat.color).copy(alpha = 0.2f), Color(cat.color), IconosCategoria.de(cat.icono)) else Avatar(ficha.nombre)
                Column(Modifier.weight(1f)) {
                    Text("Llevas gastado", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                    Text(formatearEuros(ficha.total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.padding(top = Espacio.s)) {
                Dato("Veces", "${ficha.veces}", Modifier.weight(1f))
                Dato("Media", formatearEuros(ficha.media), Modifier.weight(1f))
                Dato("Última", ficha.ultima?.let(FECHA::format) ?: "—", Modifier.weight(1f))
            }
        }
        Tarjeta {
            Text("Últimos 12 meses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            GraficoBarras(ficha.ultimos12.map { listOf(it) }, listOf(MaterialTheme.colorScheme.primary), ficha.etiquetas12, "Gasto en ${ficha.nombre} en los últimos 12 meses")
        }
        Tarjeta(relleno = false) {
            Row(Modifier.fillMaxWidth().clickable { eligiendo = true }.padding(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Categoría", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    Text(ficha.categoria?.nombre ?: "Sin categoría", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                Text("Cambiar en todos", color = MaterialTheme.colorScheme.primary)
            }
        }
        Text("Sus gastos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Tarjeta(relleno = false) {
            ficha.filas.forEachIndexed { i, fila ->
                if (i > 0) HorizontalDivider(color = c.borde)
                FilaMovimiento(fila, false, { alAbrirGasto(fila.id) })
            }
        }
    }

    if (eligiendo) {
        AlertDialog(
            onDismissRequest = { eligiendo = false },
            title = { Text("Categoría para todos sus gastos") },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text("Ninguna", Modifier.fillMaxWidth().clickable { vm.categoriaParaTodos(null); eligiendo = false }.padding(vertical = Espacio.m))
                    categorias.forEach { cat ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.categoriaParaTodos(cat.id); eligiendo = false }.padding(vertical = Espacio.s),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                        ) {
                            Avatar(cat.nombre, Color(cat.color).copy(alpha = 0.2f), Color(cat.color), IconosCategoria.de(cat.icono))
                            Text(cat.nombre)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { eligiendo = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        Text(valor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
