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
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Surface
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
fun EstadisticasScreen(vm: EstadisticasViewModel, alAbrirGasto: (String) -> Unit, alAjustes: () -> Unit, relleno: PaddingValues, alInformes: () -> Unit = {}, alTienda: (String) -> Unit = {}) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        io.github.alexcorrea2001lemon.nummia.ui.nav.Saltos.estadisticas.collect { salto ->
            if (salto != null) { vm.ir(salto.first, salto.second); io.github.alexcorrea2001lemon.nummia.ui.nav.Saltos.estadisticas.value = null }
        }
    }
    val e by vm.estado.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.statusBarsPadding().padding(start = Espacio.xl, end = Espacio.s, top = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            Text("Estadísticas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // Los informes (PDF y Excel) viven aquí: es donde se miran los números.
            androidx.compose.material3.FilledTonalButton(onClick = alInformes, contentPadding = PaddingValues(horizontal = Espacio.m)) {
                Icon(Iconos.Informes, null, Modifier.size(18.dp))
                Text(" Informes")
            }
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
                    PestanaEst.DONDE -> pestanaDonde(e, vm, alTienda)
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
            Linea("Devoluciones", m.resumen.reembolsos, e.discreto)
            Linea("Gasto real", m.resumen.gastoReal, e.discreto, LocalColoresNummia.current.gasto)
            HorizontalDivider(color = LocalColoresNummia.current.borde)
            Linea("Balance", m.resumen.balance, e.discreto, if (m.resumen.balance >= 0) LocalColoresNummia.current.ingreso else LocalColoresNummia.current.gasto, signo = true, fuerte = true)
            LineaTexto("Ahorrado", formatearPorMil(m.resumen.ahorroPorMil))
            m.tardio?.let { Text("Actualizado: devolución recibida el ${fechaLarga(it).lowercase()}", style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave) }
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
                ejeY = true, discreto = e.discreto,
                nombres = listOf(nombreMes(e.sel.mes.minusMonths(1)), nombreMes(e.sel.mes)),
                etiquetaPunto = { "Día ${it + 1}" },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
                Leyenda(actual, nombreMes(e.sel.mes))
                Leyenda(anterior, nombreMes(e.sel.mes.minusMonths(1)))
            }
        }
    }
    if (m.reparto.isNotEmpty()) item { Reparto(m.reparto, e.sel.porCategoria, e.discreto, vm::agruparPorCategoria) }
    m.cifras?.takeIf { it.dias > 0 || it.gastos > 0 }?.let { cf -> item { CifrasDelMes(cf, e.discreto) } }
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
    val nombre: (Int) -> String = { n -> nombreMes(YearMonth.of(a.anio, n)).substringBefore(' ') }
    // 1) Lo importante, en grande y con una frase
    item {
        val c = LocalColoresNummia.current
        Tarjeta(fondo = c.heroe) {
            Text("Balance de ${a.anio}", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
            ImporteAnimado(
                a.total.balance, MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (a.total.balance >= 0) c.ingreso else c.gasto, conSigno = true, discreto = e.discreto,
            )
            a.total.ahorroPorMil?.let {
                Text(
                    if (it >= 0) "Has ahorrado el ${formatearPorMil(it)} de lo que has ingresado" else "Has gastado más de lo que has ingresado",
                    style = MaterialTheme.typography.bodyMedium, color = c.textoSuave,
                )
            }
            HorizontalDivider(color = c.borde, modifier = Modifier.padding(vertical = Espacio.s))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Ingresos", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    Text(formatearEuros(a.total.ingresos, discreto = e.discreto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.ingreso, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    Text("Gasto real", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    Text(formatearEuros(a.total.gastoReal, discreto = e.discreto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.gasto, maxLines = 1)
                }
                Column(Modifier.weight(1f)) {
                    Text("Gasto al mes", style = MaterialTheme.typography.bodySmall, color = c.textoSuave)
                    Text(formatearEuros(a.mediaGasto, discreto = e.discreto), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
    // 2) El año de un vistazo
    item {
        Tarjeta {
            val c = LocalColoresNummia.current
            Text("Mes a mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            GraficoBarras(
                a.meses.map { listOf(it.ingresos, it.gastoReal) }, listOf(c.ingreso, c.gasto), INICIALES,
                "Ingresos y gasto real de cada mes de ${a.anio}", alPulsar = { vm.verMes(it + 1) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) { Leyenda(c.ingreso, "Ingresos"); Leyenda(c.gasto, "Gasto real") }
        }
    }
    // 3) Lo destacado, en frases
    item {
        val c = LocalColoresNummia.current
        Tarjeta {
            Text("Lo destacado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            a.mesMasGasto?.let { m ->
                Destacado(Iconos.Estadisticas, c.gasto, "Mes que más gastaste", "${nombre(m)} · ${formatearEuros(a.meses[m - 1].gastoReal, discreto = e.discreto)}") { vm.verMes(m) }
            }
            a.mesMenosGasto?.takeIf { it != a.mesMasGasto }?.let { m ->
                Destacado(Iconos.Check, c.ingreso, "Mes que menos gastaste", "${nombre(m)} · ${formatearEuros(a.meses[m - 1].gastoReal, discreto = e.discreto)}") { vm.verMes(m) }
            }
            Destacado(Iconos.Euro, MaterialTheme.colorScheme.primary, "Te han devuelto", formatearEuros(a.total.reembolsos, discreto = e.discreto), null)
        }
    }
    // 4) Cada mes, en una lista sencilla con su barra
    item {
        val c = LocalColoresNummia.current
        Tarjeta(relleno = false) {
            // La barra entera es lo que entró ese mes; la parte roja, lo que gastaste de ello. El resto lo ahorraste.
            Text(
                "La barra es lo que entró cada mes y lo rojo, lo que gastaste. Toca un mes para verlo.",
                style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                modifier = Modifier.padding(start = Espacio.l, end = Espacio.l, top = Espacio.m),
            )
            a.meses.forEachIndexed { i, r ->
                if (r.ingresos == 0L && r.gastoReal == 0L) return@forEachIndexed
                Column(Modifier.fillMaxWidth().clickable { vm.verMes(i + 1) }.padding(horizontal = Espacio.l, vertical = Espacio.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nombre(i + 1), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(formatearEuros(r.balance, conSigno = true, discreto = e.discreto), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (r.balance >= 0) c.ingreso else c.gasto)
                    }
                    val parte = io.github.alexcorrea2001lemon.nummia.domain.calculo.parteGastada(r.gastoReal, r.ingresos) ?: 1f
                    val pct = io.github.alexcorrea2001lemon.nummia.domain.calculo.porcentajeGastado(r.gastoReal, r.ingresos)
                    val ancho by androidx.compose.animation.core.animateFloatAsState(parte, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow), label = "barra")
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).background(c.ingreso.copy(alpha = 0.25f), RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth(ancho).fillMaxHeight().background(c.gasto.copy(alpha = 0.8f), RoundedCornerShape(3.dp)))
                    }
                    Text(
                        "Gastaste ${formatearEuros(r.gastoReal, discreto = e.discreto)}" + when {
                            pct == null -> " · no entró dinero"
                            pct > 100 -> " · más de lo que entró"
                            else -> " · el $pct % de lo que entró"
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.textoSuave,
                    )
                }
            }
        }
    }
}

@Composable
private fun Destacado(icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, titulo: String, valor: String, alPulsar: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().let { if (alPulsar != null) it.clickable(onClick = alPulsar) else it }.padding(vertical = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        io.github.alexcorrea2001lemon.nummia.ui.comun.Avatar("", color.copy(alpha = 0.16f), color, icono)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
            Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ------------------------------------------------------------------ Comparar

private fun LazyListScope.pestanaComparar(e: EstadoEst, vm: EstadisticasViewModel) {
    val c = e.comparacion ?: return
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            ElegirAnio(e.sel.anioA, e.anios, vm::anioA, principal = false)
            Text("  frente a  ", color = LocalColoresNummia.current.textoSuave)
            ElegirAnio(e.sel.anioB, e.anios, vm::anioB, principal = true)
        }
    }
    item {
        val col = LocalColoresNummia.current
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            // Gastar más es malo (rojo); ingresar o ahorrar más es bueno (verde).
            TarjetaComparar("Has gastado", c.a.anio, c.b.anio, c.a.total.gastoReal, c.b.total.gastoReal, c.porMilGasto, masEsBueno = false, discreto = e.discreto)
            TarjetaComparar("Has ingresado", c.a.anio, c.b.anio, c.a.total.ingresos, c.b.total.ingresos, c.porMilIngreso, masEsBueno = true, discreto = e.discreto)
            TarjetaComparar("Has ahorrado", c.a.anio, c.b.anio, c.a.total.balance, c.b.total.balance, c.porMilBalance, masEsBueno = true, discreto = e.discreto)
        }
    }
    item {
        Tarjeta {
            Text("Gasto de cada mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val principal = MaterialTheme.colorScheme.primary
            val suave = LocalColoresNummia.current.textoSuave
            GraficoLineas(
                listOf(SerieLinea(c.a.meses.map { it.gastoReal }, suave.copy(alpha = 0.6f), discontinua = true), SerieLinea(c.b.meses.map { it.gastoReal }, principal)),
                listOf("Ene", "Jun", "Dic"),
                "Gasto real de cada mes en ${c.a.anio} y ${c.b.anio}",
                ejeY = true, discreto = e.discreto,
                nombres = listOf("${c.a.anio}", "${c.b.anio}"),
                etiquetaPunto = { listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")[it] },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) { Leyenda(principal, "${c.b.anio}"); Leyenda(suave, "${c.a.anio} (discontinua)") }
        }
    }
}

/** «El mes en cifras»: media al día, a qué ritmo vas, cuántas compras y días sin gastar. */
@Composable
private fun CifrasDelMes(cf: io.github.alexcorrea2001lemon.nummia.domain.calculo.CifrasMes, discreto: Boolean) {
    val c = LocalColoresNummia.current
    Tarjeta {
        Text("El mes en cifras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        cf.prevision?.let { p ->
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Column(Modifier.fillMaxWidth().padding(Espacio.m)) {
                    Text("Si sigues a este ritmo", style = MaterialTheme.typography.bodySmall)
                    Text("acabarás el mes en ${formatearEuros(p, discreto = discreto)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Cifra("Media al día", formatearEuros(cf.mediaDia, discreto = discreto), Modifier.weight(1f))
            Cifra("Gasto medio", cf.gastoMedio?.let { formatearEuros(it, discreto = discreto) } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Cifra("Gastos apuntados", "${cf.gastos}", Modifier.weight(1f))
            Cifra(
                "Días sin gastar", if (cf.dias > 0) "${cf.diasSinGastar} de ${cf.dias}" else "—", Modifier.weight(1f),
                color = if (cf.dias > 0 && cf.diasSinGastar * 2 >= cf.dias) c.ingreso else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Cifra(titulo: String, valor: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = LocalColoresNummia.current.textoSuave)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
    }
}

/** Dos barras, una por año, y una etiqueta con la diferencia en color (bueno o malo). */
@Composable
private fun TarjetaComparar(titulo: String, anioA: Int, anioB: Int, a: Long, b: Long, porMil: Int?, masEsBueno: Boolean, discreto: Boolean) {
    val col = LocalColoresNummia.current
    val maximo = maxOf(kotlin.math.abs(a), kotlin.math.abs(b)).coerceAtLeast(1)
    val diferencia = b - a
    val bueno = if (masEsBueno) diferencia >= 0 else diferencia <= 0
    val colorDif = if (diferencia == 0L) col.textoSuave else if (bueno) col.ingreso else col.gasto
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val texto = when {
                diferencia == 0L -> "Igual"
                else -> (if (diferencia > 0) "+" else "−") + formatearEuros(kotlin.math.abs(diferencia), discreto = discreto) +
                    (porMil?.let { " · ${formatearPorMil(kotlin.math.abs(it))}" } ?: "")
            }
            io.github.alexcorrea2001lemon.nummia.ui.comun.Etiqueta(texto, colorDif.copy(alpha = 0.16f), colorDif)
        }
        listOf(anioA to a, anioB to b).forEachIndexed { i, (anio, v) ->
            val fraccion by animateFloatAsState((kotlin.math.abs(v).toFloat() / maximo).coerceIn(0.02f, 1f), spring(dampingRatio = 0.8f, stiffness = 200f), label = "comparar")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                Text("$anio", style = MaterialTheme.typography.bodyMedium, color = if (i == 1) MaterialTheme.colorScheme.onSurface else col.textoSuave, modifier = Modifier.width(44.dp))
                Box(Modifier.weight(1f).height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().background(if (i == 1) MaterialTheme.colorScheme.primary else col.textoSuave, RoundedCornerShape(5.dp)))
                }
                Text(formatearEuros(v, discreto = discreto), style = MaterialTheme.typography.bodyMedium, fontWeight = if (i == 1) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.widthIn(min = 92.dp), textAlign = TextAlign.End, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ElegirAnio(actual: Int, anios: List<Int>, alElegir: (Int) -> Unit, principal: Boolean = false) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        if (principal) {
            androidx.compose.material3.Button(onClick = { abierto = true }) { Text("$actual", fontWeight = FontWeight.Bold) }
        } else {
            OutlinedButton(onClick = { abierto = true }) { Text("$actual") }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            anios.forEach { a -> DropdownMenuItem(text = { Text("$a") }, onClick = { alElegir(a); abierto = false }) }
        }
    }
}

// ------------------------------------------------------------------ Dónde gasto

private fun LazyListScope.pestanaDonde(e: EstadoEst, vm: EstadisticasViewModel, alTienda: (String) -> Unit) {
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
        Tarjeta(Modifier.animateItem().clickable { alTienda(g.nombre) }) {
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
