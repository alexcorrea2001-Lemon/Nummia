package io.github.alexcorrea2001lemon.nummia.ui.estadisticas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearPorMil
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.ImporteAnimado
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaLarga
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMesMinuscula
import io.github.alexcorrea2001lemon.nummia.ui.graficos.GraficoBarras
import io.github.alexcorrea2001lemon.nummia.ui.graficos.GraficoDonut
import io.github.alexcorrea2001lemon.nummia.ui.graficos.GraficoLineas
import io.github.alexcorrea2001lemon.nummia.ui.graficos.SerieLinea
import io.github.alexcorrea2001lemon.nummia.ui.graficos.coloresGrafico
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.YearMonth

private val INICIALES = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(vm: EstadisticasViewModel, alAbrirGasto: (String) -> Unit, alAjustes: () -> Unit, relleno: PaddingValues) {
    val e by vm.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.statusBarsPadding().padding(start = Espacio.xl, end = Espacio.s, top = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            Text("Estadísticas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = alAjustes) { Icon(Iconos.Ajustes, "Ajustes") }
        }
        PrimaryScrollableTabRow(selectedTabIndex = e.sel.pestana.ordinal, edgePadding = Espacio.l, containerColor = MaterialTheme.colorScheme.background) {
            PestanaEst.entries.forEach { p ->
                Tab(selected = e.sel.pestana == p, onClick = { vm.pestana(p) }, text = { Text(p.nombre) })
            }
        }
        AnimatedContent(e.sel.pestana, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pestana") { p ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, top = Espacio.l, bottom = relleno.calculateBottomPadding() + Espacio.xxl),
                verticalArrangement = Arrangement.spacedBy(Espacio.l),
            ) {
                when (p) {
                    PestanaEst.MES -> pestanaMes(e, vm)
                    PestanaEst.ANIO -> pestanaAnio(e, vm)
                    PestanaEst.COMPARAR -> pestanaComparar(e, vm)
                    PestanaEst.DONDE -> pestanaDonde(e, vm)
                }
            }
        }
    }

    if (e.sel.tiendaAbierta != null) {
        ModalBottomSheet(onDismissRequest = { vm.abrirTienda(null) }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = Espacio.l)) {
                Text(e.sel.tiendaAbierta.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = Espacio.xl))
                Text("${e.historialTienda.size} gastos · ${e.sel.periodo.nombre.lowercase()}", color = LocalColoresNummia.current.textoSuave, modifier = Modifier.padding(horizontal = Espacio.xl, vertical = Espacio.xs))
                LazyColumn {
                    items(e.historialTienda, key = { it.id }) { f -> FilaMovimiento(f, e.discreto, { vm.abrirTienda(null); alAbrirGasto(f.id) }) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Mes

private fun LazyListScope.pestanaMes(e: EstadoEst, vm: EstadisticasViewModel) {
    item { Selector(nombreMes(e.sel.mes), vm::mesAnterior, vm::mesSiguiente) }
    val m = e.mes
    if (m.vacio && !e.cargando) {
        item { EstadoVacio(Iconos.Estadisticas, "Sin datos en ${nombreMesMinuscula(e.sel.mes)}", "Cuando apuntes gastos o ingresos verás aquí el resumen y los gráficos.") }
        return
    }
    item {
        Tarjeta {
            Linea("Sueldo", m.sueldo, e.discreto)
            Linea("Otros ingresos", m.otros, e.discreto)
            Linea("Pagado", m.resumen.pagado, e.discreto)
            Linea("Reembolsos", m.resumen.reembolsos, e.discreto)
            Linea("Gasto real", m.resumen.gastoReal, e.discreto, LocalColoresNummia.current.gasto)
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            Linea("Balance", m.resumen.balance, e.discreto, if (m.resumen.balance >= 0) LocalColoresNummia.current.ingreso else LocalColoresNummia.current.gasto, signo = true, fuerte = true)
            LineaTexto("Ahorrado", formatearPorMil(m.resumen.ahorroPorMil))
            m.tardio?.let { Text("Actualizado: reembolso recibido el ${fechaLarga(it).lowercase()}", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave) }
        }
    }
    item {
        Tarjeta {
            Text("Gasto acumulado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val actual = MaterialTheme.colorScheme.primary
            val anterior = LocalColoresNummia.current.textoSuave
            GraficoLineas(
                listOf(SerieLinea(m.acumuladoAnterior, anterior.copy(alpha = 0.6f), discontinua = true), SerieLinea(m.acumulado, actual)),
                listOf("1", "${(m.acumulado.size + 1) / 2}", "${m.acumulado.size}"),
                "Gasto acumulado: ${formatearEuros(m.acumulado.lastOrNull() ?: 0)} este mes y ${formatearEuros(m.acumuladoAnterior.lastOrNull() ?: 0)} el mes anterior",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
                Leyenda(actual, nombreMes(e.sel.mes))
                Leyenda(anterior, nombreMes(e.sel.mes.minusMonths(1)))
            }
        }
    }
    if (m.reparto.isNotEmpty()) item { Reparto(m.reparto, e.sel.porCategoria, e.discreto, vm::agruparPorCategoria) }
    m.diaMas?.let { (dia, importe) ->
        item {
            Tarjeta {
                Text("Día de más gasto", style = MaterialTheme.typography.bodyMedium, color = LocalColoresNummia.current.textoSuave)
                Text("${fechaLarga(dia)} · ${formatearEuros(importe, discreto = e.discreto)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Reparto(grupos: List<GrupoGasto>, porCategoria: Boolean, discreto: Boolean, alCambiar: (Boolean) -> Unit) {
    val colores = coloresGrafico()
    val principales = grupos.filter { it.total > 0 }.take(5)
    val resto = grupos.filter { it.total > 0 }.drop(5).sumOf { it.total }
    val partes = principales.map { it.nombre to it.total } + if (resto > 0) listOf("Otros" to resto) else emptyList()
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reparto del gasto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            FilterChip(selected = !porCategoria, onClick = { alCambiar(false) }, label = { Text("Por tienda") })
            FilterChip(selected = porCategoria, onClick = { alCambiar(true) }, label = { Text("Por categoría") })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
            GraficoDonut(partes, colores, "Reparto: " + partes.joinToString { "${it.first} ${formatearEuros(it.second)}" }, lado = 132.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                partes.forEachIndexed { i, (n, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(colores[i % colores.size], CircleShape))
                        Text(n, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp))
                        Text(formatearEuros(v, discreto = discreto), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Año

private fun LazyListScope.pestanaAnio(e: EstadoEst, vm: EstadisticasViewModel) {
    item { Selector("${e.sel.anio}", vm::anioAnterior, vm::anioSiguiente) }
    val a = e.anual ?: return
    if (a.mesMasGasto == null && a.total.ingresos == 0L && !e.cargando) {
        item { EstadoVacio(Iconos.Estadisticas, "Sin datos en ${a.anio}", "Cuando apuntes movimientos de este año verás aquí su resumen.") }
        return
    }
    item {
        val c = LocalColoresNummia.current
        val mes: (Int?) -> String = { it?.let { n -> nombreMes(YearMonth.of(a.anio, n)).substringBefore(' ') } ?: "—" }
        val datos = listOf(
            "Sueldos" to formatearEuros(a.sueldos, discreto = e.discreto),
            "Otros ingresos" to formatearEuros(a.otrosIngresos, discreto = e.discreto),
            "Ingresos" to formatearEuros(a.total.ingresos, discreto = e.discreto),
            "Pagado" to formatearEuros(a.total.pagado, discreto = e.discreto),
            "Reembolsos" to formatearEuros(a.total.reembolsos, discreto = e.discreto),
            "Gasto real" to formatearEuros(a.total.gastoReal, discreto = e.discreto),
            "Balance" to formatearEuros(a.total.balance, conSigno = true, discreto = e.discreto),
            "Ahorrado" to formatearPorMil(a.total.ahorroPorMil),
            "Gasto medio al mes" to formatearEuros(a.mediaGasto, discreto = e.discreto),
            "Ingreso medio al mes" to formatearEuros(a.mediaIngreso, discreto = e.discreto),
            "Mes de más gasto" to mes(a.mesMasGasto),
            "Mes de menos gasto" to mes(a.mesMenosGasto),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            datos.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    fila.forEach { (t, v) ->
                        Tarjeta(Modifier.weight(1f)) {
                            Text(t, style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                            Text(v, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    item {
        Tarjeta {
            Text("Ingresos y gasto real por mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val c = LocalColoresNummia.current
            GraficoBarras(
                a.meses.map { listOf(it.ingresos, it.gastoReal) }, listOf(c.ingreso, c.gasto), INICIALES,
                "Ingresos y gasto real de cada mes de ${a.anio}", alPulsar = { vm.verMes(it + 1) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) { Leyenda(c.ingreso, "Ingresos"); Leyenda(c.gasto, "Gasto real") }
            Text("Toca un mes para verlo en detalle.", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
        }
    }
    item {
        Tarjeta {
            Text("Ahorro acumulado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ImporteAnimado(e.ahorroAcumulado.lastOrNull() ?: 0, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), discreto = e.discreto)
            GraficoLineas(listOf(SerieLinea(e.ahorroAcumulado, MaterialTheme.colorScheme.primary)), listOf("Ene", "Jun", "Dic"), "Ahorro acumulado mes a mes en ${a.anio}")
        }
    }
    item {
        Tarjeta(relleno = false) {
            Row(Modifier.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
                Text("Mes", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1.2f))
                Text("Ingresos", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Gasto", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Balance", style = MaterialTheme.typography.labelMedium, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            a.meses.forEachIndexed { i, r ->
                HorizontalDivider(color = LocalColoresNummia.current.borde)
                Row(Modifier.fillMaxWidth().clickable { vm.verMes(i + 1) }.padding(horizontal = Espacio.l, vertical = Espacio.m)) {
                    Text(nombreMes(YearMonth.of(a.anio, i + 1)).substringBefore(' '), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                    Cifra(r.ingresos, e.discreto, Modifier.weight(1f))
                    Cifra(r.gastoReal, e.discreto, Modifier.weight(1f))
                    Cifra(r.balance, e.discreto, Modifier.weight(1f), if (r.balance >= 0) LocalColoresNummia.current.ingreso else LocalColoresNummia.current.gasto)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Comparar

private fun LazyListScope.pestanaComparar(e: EstadoEst, vm: EstadisticasViewModel) {
    val c = e.comparacion ?: return
    item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            ElegirAnio(e.sel.anioA, e.anios, vm::anioA)
            Text("frente a", color = LocalColoresNummia.current.textoSuave)
            ElegirAnio(e.sel.anioB, e.anios, vm::anioB)
        }
    }
    item {
        val col = LocalColoresNummia.current
        Tarjeta {
            Text(frase("gastado", c.diferenciaGasto, c.porMilGasto, c.a.anio, c.b.anio, e.discreto), style = MaterialTheme.typography.bodyLarge)
            Text(frase("ingresado", c.diferenciaIngreso, c.porMilIngreso, c.a.anio, c.b.anio, e.discreto), style = MaterialTheme.typography.bodyLarge)
            Text(frase("ahorrado", c.diferenciaBalance, c.porMilBalance, c.a.anio, c.b.anio, e.discreto), style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider(color = col.borde)
            Row {
                Text("", modifier = Modifier.weight(1.2f))
                listOf("${c.a.anio}", "${c.b.anio}", "Diferencia").forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = col.textoSuave, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
            FilaComparar("Ingresos", c.a.total.ingresos, c.b.total.ingresos, e.discreto)
            FilaComparar("Gasto real", c.a.total.gastoReal, c.b.total.gastoReal, e.discreto)
            FilaComparar("Balance", c.a.total.balance, c.b.total.balance, e.discreto)
            Row {
                Text("Ahorrado", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                Text(formatearPorMil(c.a.total.ahorroPorMil), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text(formatearPorMil(c.b.total.ahorroPorMil), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text("", modifier = Modifier.weight(1f))
            }
        }
    }
    item {
        Tarjeta {
            Text("Gasto real mes a mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val principal = MaterialTheme.colorScheme.primary
            val suave = LocalColoresNummia.current.textoSuave
            GraficoLineas(
                listOf(SerieLinea(c.a.meses.map { it.gastoReal }, suave.copy(alpha = 0.6f), discontinua = true), SerieLinea(c.b.meses.map { it.gastoReal }, principal)),
                listOf("Ene", "Jun", "Dic"),
                "Gasto real de cada mes en ${c.a.anio} y ${c.b.anio}",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) { Leyenda(principal, "${c.b.anio}"); Leyenda(suave, "${c.a.anio}") }
        }
    }
}

private fun frase(verbo: String, diferencia: Long, porMil: Int?, a: Int, b: Int, discreto: Boolean): String {
    val pct = porMil?.let { " (${formatearPorMil(kotlin.math.abs(it))})" }.orEmpty()
    return when {
        diferencia == 0L -> "En $b has $verbo lo mismo que en $a."
        diferencia > 0 -> "En $b has $verbo ${formatearEuros(diferencia, discreto = discreto)} más que en $a$pct."
        else -> "En $b has $verbo ${formatearEuros(-diferencia, discreto = discreto)} menos que en $a$pct."
    }
}

@Composable
private fun FilaComparar(titulo: String, a: Long, b: Long, discreto: Boolean) {
    Row {
        Text(titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
        Cifra(a, discreto, Modifier.weight(1f))
        Cifra(b, discreto, Modifier.weight(1f))
        Text(formatearEuros(b - a, conSigno = true, discreto = discreto), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun ElegirAnio(actual: Int, anios: List<Int>, alElegir: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { abierto = true }) { Text("$actual"); Icon(Iconos.ChevronDer, null, Modifier.size(16.dp)) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            anios.forEach { a -> DropdownMenuItem(text = { Text("$a") }, onClick = { alElegir(a); abierto = false }) }
        }
    }
}

// ------------------------------------------------------------------ Dónde gasto

private fun LazyListScope.pestanaDonde(e: EstadoEst, vm: EstadisticasViewModel) {
    item {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Periodo.entries.forEach { p -> FilterChip(selected = e.sel.periodo == p, onClick = { vm.periodo(p) }, label = { Text(p.nombre) }) }
        }
    }
    if (e.ranking.isEmpty() && !e.cargando) {
        item { EstadoVacio(Iconos.Bolsa, "Aún no hay gastos", "Cuando apuntes gastos verás aquí en qué sitios se va tu dinero.") }
        return
    }
    val maximo = e.ranking.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    items(e.ranking, key = { it.nombre }) { g ->
        val fraccion by animateFloatAsState((g.total.toFloat() / maximo).coerceIn(0.02f, 1f), label = "barra")
        Tarjeta(Modifier.animateItem().clickable { vm.abrirTienda(g.nombre) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatearEuros(g.total, discreto = e.discreto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
            }
            Text(
                "${g.veces} ${if (g.veces == 1) "vez" else "veces"} · media ${formatearEuros(g.media, discreto = e.discreto)} por visita",
                style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave,
            )
        }
    }
}

// ------------------------------------------------------------------ Piezas comunes

@Composable
private fun Selector(titulo: String, anterior: () -> Unit, siguiente: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = anterior) { Icon(Iconos.ChevronIzq, "Anterior") }
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = Espacio.s))
        IconButton(onClick = siguiente) { Icon(Iconos.ChevronDer, "Siguiente") }
    }
}

@Composable
private fun Linea(titulo: String, valor: Long, discreto: Boolean, color: Color = Color.Unspecified, signo: Boolean = false, fuerte: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, color = if (fuerte) MaterialTheme.colorScheme.onSurface else LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f))
        Text(formatearEuros(valor, conSigno = signo, discreto = discreto), style = if (fuerte) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun LineaTexto(titulo: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge, color = LocalColoresNummia.current.textoSuave, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Cifra(valor: Long, discreto: Boolean, modifier: Modifier, color: Color = Color.Unspecified) {
    Text(formatearEuros(valor, discreto = discreto).removeSuffix(" €"), style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.End, modifier = modifier, maxLines = 1)
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
    }
}
