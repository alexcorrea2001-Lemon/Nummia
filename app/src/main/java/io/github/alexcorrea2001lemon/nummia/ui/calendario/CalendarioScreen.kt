package io.github.alexcorrea2001lemon.nummia.ui.calendario

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.ui.comun.FilaMovimiento
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.Tarjeta
import io.github.alexcorrea2001lemon.nummia.ui.comun.fechaLarga
import io.github.alexcorrea2001lemon.nummia.ui.comun.nombreMes
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio
import io.github.alexcorrea2001lemon.nummia.ui.theme.LocalColoresNummia
import java.time.LocalDate

private val DIAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun CalendarioScreen(
    vm: CalendarioViewModel,
    alAbrirGasto: (String) -> Unit,
    alAbrirIngreso: (String) -> Unit,
    alAnadirEn: (LocalDate) -> Unit,
    alAjustes: () -> Unit,
    relleno: PaddingValues,
) {
    var eligiendoMes by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val e by vm.estado.collectAsStateWithLifecycle()
    val c = LocalColoresNummia.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, bottom = relleno.calculateBottomPadding() + 96.dp),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(start = Espacio.xs, top = Espacio.s), verticalAlignment = Alignment.CenterVertically) {
                Text("Calendario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = alAjustes) { Icon(Iconos.Ajustes, "Ajustes") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::anterior) { Icon(Iconos.ChevronIzq, if (e.semana) "Semana anterior" else "Mes anterior") }
                // Tocar el mes abre «Ir a…» para saltar a cualquier mes y año.
                TextButton(onClick = { eligiendoMes = true }) {
                    // El nombre del mes entra por el lado hacia el que avanzas.
                    AnimatedContent(
                        targetState = e.mes,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it * dir } + fadeIn()) togetherWith
                                (slideOutVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it * dir } + fadeOut())
                        },
                        label = "mes",
                    ) { mes ->
                        Text(nombreMes(mes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Icon(Iconos.FlechaAbajo, null, Modifier.size(16.dp))
                }
                IconButton(onClick = vm::siguiente) { Icon(Iconos.ChevronDer, if (e.semana) "Semana siguiente" else "Mes siguiente") }
                Spacer(Modifier.weight(1f))
                FilterChip(selected = e.semana, onClick = vm::alternarVista, label = { Text("Semana") })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
                Text("Gasto real ${formatearEuros(e.gastoMes, discreto = e.discreto)}", style = MaterialTheme.typography.bodySmall, color = c.gasto)
                Text("Ingresos ${formatearEuros(e.ingresoMes, discreto = e.discreto)}", style = MaterialTheme.typography.bodySmall, color = c.ingreso)
            }
        }
        item {
            // Deslizar: la cuadrícula sigue al dedo y, al soltar, pasa de mes con efecto muelle (o vuelve a su sitio).
            val alcance = rememberCoroutineScope()
            val arrastre = remember { Animatable(0f) }
            val umbral = with(LocalDensity.current) { 64.dp.toPx() }
            val haptica = io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica()
            Column(
                Modifier.pointerInput(e.semana) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val d = arrastre.value
                            if (d > umbral) { haptica(); vm.anterior() } else if (d < -umbral) { haptica(); vm.siguiente() }
                            alcance.launch { arrastre.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) }
                        },
                        onDragCancel = { alcance.launch { arrastre.animateTo(0f, spring()) } },
                        onHorizontalDrag = { cambio, d -> cambio.consume(); alcance.launch { arrastre.snapTo(arrastre.value + d * 0.5f) } },
                    )
                },
            ) {
                Row(Modifier.fillMaxWidth()) {
                    DIAS.forEach { Text(it, style = MaterialTheme.typography.labelMedium, color = c.textoSuave, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(Espacio.xs))
                AnimatedContent(
                    targetState = e.filas,
                    modifier = Modifier.graphicsLayer {
                        translationX = arrastre.value
                        alpha = 1f - (kotlin.math.abs(arrastre.value) / (umbral * 6)).coerceAtMost(0.3f)
                    },
                    transitionSpec = {
                        // Hacia delante entra por la derecha; hacia atrás, por la izquierda.
                        val antes = initialState.firstOrNull()?.firstOrNull()?.fecha
                        val ahora = targetState.firstOrNull()?.firstOrNull()?.fecha
                        val dir = if (antes == null || ahora == null || ahora > antes) 1 else -1
                        val muelle = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        ((slideInHorizontally(muelle) { it * dir / 2 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(muelle) { -it * dir / 2 } + fadeOut(tween(160))))
                            .using(SizeTransform(clip = true))
                    },
                    label = "cuadricula",
                    contentKey = { filas -> filas.firstOrNull()?.firstOrNull()?.fecha },
                ) { filas ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filas.forEach { semana ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                semana.forEach { celda -> CeldaDia(celda, e.discreto, Modifier.weight(1f)) { vm.elegir(celda.fecha) } }
                            }
                        }
                    }
                }
            }
        }
        item {
            Tarjeta {
                Text(fechaLarga(e.elegido), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val t = e.totalDia
                if (t != null && (t.gasto != 0L || t.ingreso != 0L)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espacio.l)) {
                        if (t.gasto != 0L) Text("Gasto real ${formatearEuros(t.gasto, discreto = e.discreto)}", style = MaterialTheme.typography.bodySmall, color = if (t.gasto > 0) c.gasto else c.ingreso)
                        if (t.ingreso != 0L) Text("Ingresos ${formatearEuros(t.ingreso, discreto = e.discreto)}", style = MaterialTheme.typography.bodySmall, color = c.ingreso)
                    }
                }
                if (e.movimientosDia.isEmpty()) {
                    Text("Nada apuntado este día.", style = MaterialTheme.typography.bodyMedium, color = c.textoSuave)
                }
                Button(onClick = { alAnadirEn(e.elegido) }) { Icon(Iconos.Mas, null, Modifier.size(18.dp)); Text("  Añadir en este día") }
            }
        }
        if (e.movimientosDia.isNotEmpty()) {
            item {
                Tarjeta(relleno = false) {
                    e.movimientosDia.forEach { f -> FilaMovimiento(f, e.discreto, { if (f.esGasto) alAbrirGasto(f.id) else alAbrirIngreso(f.id) }) }
                }
            }
        }
    }
    if (eligiendoMes) {
        io.github.alexcorrea2001lemon.nummia.ui.comun.SelectorMes(e.mes, vm.hoy(), alCerrar = { eligiendoMes = false }) { vm.irAMes(it); eligiendoMes = false }
    }
}

@Composable
private fun CeldaDia(celda: Celda, discreto: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val c = LocalColoresNummia.current
    val esquema = MaterialTheme.colorScheme
    val fondo by animateColorAsState(if (celda.elegido) esquema.primary else Color.Transparent, label = "fondo")
    val texto = when {
        celda.elegido -> esquema.onPrimary
        !celda.delMes -> c.textoSuave.copy(alpha = 0.45f)
        else -> esquema.onSurface
    }
    val dia = celda.dia
    val descripcion = buildString {
        append(fechaLarga(celda.fecha))
        if (dia != null && dia.gasto != 0L) append(", gasto ${formatearEuros(dia.gasto)}")
        if (dia != null && dia.ingreso != 0L) append(", ingresos ${formatearEuros(dia.ingreso)}")
        if (dia?.previsto == true) append(", con movimientos previstos")
    }
    Column(
        modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .then(if (celda.esHoy && !celda.elegido) Modifier.border(BorderStroke(1.5.dp, esquema.primary), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = alPulsar)
            .semantics { contentDescription = descripcion }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text("${celda.fecha.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (celda.esHoy) FontWeight.Bold else FontWeight.Medium, color = texto)
        if (dia != null && celda.delMes) {
            val sobre = celda.elegido
            if (dia.gasto != 0L) {
                Text(
                    if (discreto) "••" else (if (dia.gasto > 0) "−" else "+") + compacto(dia.gasto),
                    fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1,
                    color = if (sobre) esquema.onPrimary else if (dia.gasto > 0) c.gasto else c.ingreso,
                )
            }
            if (dia.ingreso != 0L) {
                Text(if (discreto) "••" else "+" + compacto(dia.ingreso), fontSize = 10.sp, lineHeight = 11.sp, maxLines = 1, color = if (sobre) esquema.onPrimary else c.ingreso)
            }
            if (dia.previsto) {
                Box(Modifier.size(6.dp).border(1.2.dp, if (sobre) esquema.onPrimary else c.textoSuave, CircleShape))
            }
        }
    }
}
