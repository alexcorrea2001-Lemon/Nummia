@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.alexcorrea2001lemon.nummia.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.alexcorrea2001lemon.nummia.data.ajustes.Ajustes
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.AcercaScreen
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.AjustesScreen
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.AjustesViewModel
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.AparienciaScreen
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.DatosScreen
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.PersonasScreen
import io.github.alexcorrea2001lemon.nummia.ui.ajustes.SueldoScreen
import io.github.alexcorrea2001lemon.nummia.ui.calendario.CalendarioScreen
import io.github.alexcorrea2001lemon.nummia.ui.comun.EstadoVacio
import io.github.alexcorrea2001lemon.nummia.ui.estadisticas.EstadisticasScreen
import io.github.alexcorrea2001lemon.nummia.ui.comun.Iconos
import io.github.alexcorrea2001lemon.nummia.ui.comun.rememberHaptica
import io.github.alexcorrea2001lemon.nummia.ui.detalle.DetalleScreen
import io.github.alexcorrea2001lemon.nummia.ui.formulario.FormularioGastoScreen
import io.github.alexcorrea2001lemon.nummia.ui.formulario.FormularioIngresoScreen
import io.github.alexcorrea2001lemon.nummia.ui.formulario.FormularioReembolsoScreen
import io.github.alexcorrea2001lemon.nummia.ui.inicio.InicioScreen
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.MovimientosScreen
import io.github.alexcorrea2001lemon.nummia.ui.theme.Espacio

private data class Pestana(val ruta: String, val nombre: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana(Rutas.INICIO, "Inicio", Iconos.Inicio),
    Pestana(Rutas.MOVIMIENTOS, "Movimientos", Iconos.Movimientos),
    Pestana(Rutas.CALENDARIO, "Calendario", Iconos.Calendario),
    Pestana(Rutas.ESTADISTICAS, "Estadísticas", Iconos.Estadisticas),
    Pestana(Rutas.INFORMES, "Informes", Iconos.Informes),
)

@Composable
fun AppNummia(ajustes: Ajustes, alTerminarBienvenida: (Long?) -> Unit) {
    if (!ajustes.bienvenidaVista) {
        Bienvenida(alTerminar = alTerminarBienvenida)
        return
    }
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    val esPrincipal = ruta in Rutas.PRINCIPALES
    var menuAbierto by rememberSaveable { mutableStateOf(false) }
    val haptica = rememberHaptica()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(esPrincipal, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    pestanas.forEach { p ->
                        NavigationBarItem(
                            selected = ruta == p.ruta,
                            onClick = { if (ruta != p.ruta) irAPestana(nav, p.ruta) },
                            icon = { Icon(p.icono, null) },
                            label = { TextoAjustado(p.nombre) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (ruta == Rutas.INICIO || ruta == Rutas.MOVIMIENTOS) {
                MenuAnadir(menuAbierto, alAlternar = { haptica(); menuAbierto = !menuAbierto }) { destino ->
                    menuAbierto = false
                    nav.navigate(destino)
                }
            }
        },
    ) { relleno ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalTransicionCompartida provides this) {
                NavHost(
                    navController = nav,
                    startDestination = Rutas.INICIO,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.97f, animationSpec = tween(220)) },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(160)) + scaleOut(targetScale = 0.97f, animationSpec = tween(160)) },
                ) {
                    grafo(nav, snackbar, relleno)
                }
            }
        }
        if (menuAbierto) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuAbierto = false },
            )
        }
    }
}

private fun NavGraphBuilder.grafo(nav: NavHostController, snackbar: SnackbarHostState, relleno: PaddingValues) {
    fun abrirGasto(id: String) = nav.navigate(Rutas.detalle(id))
    fun abrirIngreso(id: String) = nav.navigate(Rutas.ingreso(id))

    pantalla(Rutas.INICIO) {
        InicioScreen(
            hiltViewModel(), ::abrirGasto, ::abrirIngreso,
            alVerTodos = { irAPestana(nav, Rutas.MOVIMIENTOS) },
            alBuscar = { irAPestana(nav, Rutas.MOVIMIENTOS) },
            alAjustes = { nav.navigate(Rutas.AJUSTES) },
            alAnadirGasto = { nav.navigate(Rutas.gasto()) },
            alDrive = { nav.navigate(Rutas.DRIVE) },
            relleno = relleno,
        )
    }
    pantalla(Rutas.MOVIMIENTOS) {
        MovimientosScreen(hiltViewModel(), snackbar, ::abrirGasto, ::abrirIngreso, { nav.navigate(Rutas.AJUSTES) }, { nav.navigate(Rutas.gasto()) }, relleno)
    }
    pantalla(Rutas.CALENDARIO) {
        CalendarioScreen(
            hiltViewModel(), ::abrirGasto, ::abrirIngreso,
            alAnadirEn = { nav.navigate(Rutas.gasto(fecha = it)) },
            alAjustes = { nav.navigate(Rutas.AJUSTES) }, relleno = relleno,
        )
    }
    pantalla(Rutas.ESTADISTICAS) { EstadisticasScreen(hiltViewModel(), ::abrirGasto, { nav.navigate(Rutas.AJUSTES) }, relleno) }
    pantalla(Rutas.INFORMES) { Proximamente(Iconos.Informes, "Informes", relleno, version = "1.3.0") }

    pantalla(Rutas.DETALLE, listOf(navArgument("id") { type = NavType.StringType })) {
        DetalleScreen(
            hiltViewModel(), snackbar, alVolver = { nav.popBackStack() },
            alEditar = { nav.navigate(Rutas.gasto(it)) },
            alAnadirReembolso = { nav.navigate(Rutas.reembolso(it)) },
            alAbrirGasto = { nav.navigate(Rutas.detalle(it)) { popUpTo(Rutas.DETALLE) { inclusive = true } } },
        )
    }
    pantalla(
        Rutas.GASTO,
        listOf(
            navArgument("id") { type = NavType.StringType; defaultValue = "" },
            navArgument("paraOtro") { type = NavType.BoolType; defaultValue = false },
            navArgument("fecha") { type = NavType.StringType; defaultValue = "" },
        ),
    ) { FormularioGastoScreen(hiltViewModel(), snackbar) { nav.popBackStack() } }
    pantalla(Rutas.INGRESO, listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })) {
        FormularioIngresoScreen(hiltViewModel()) { nav.popBackStack() }
    }
    pantalla(Rutas.REEMBOLSO, listOf(navArgument("gastoId") { type = NavType.StringType; defaultValue = "" })) {
        FormularioReembolsoScreen(hiltViewModel()) { nav.popBackStack() }
    }

    pantalla(Rutas.AJUSTES) { AjustesScreen(hiltViewModel(), { nav.popBackStack() }) { nav.navigate(it) } }
    pantalla(Rutas.APARIENCIA) { AparienciaScreen(ajustesVm(nav), { nav.popBackStack() }) }
    pantalla(Rutas.SUELDO) { SueldoScreen(ajustesVm(nav), { nav.popBackStack() }) }
    pantalla(Rutas.PERSONAS) { PersonasScreen(ajustesVm(nav), { nav.popBackStack() }) }
    pantalla(Rutas.DATOS) { DatosScreen(ajustesVm(nav), snackbar, { nav.navigate(it) }) { nav.popBackStack() } }
    pantalla(Rutas.PAGOS) { io.github.alexcorrea2001lemon.nummia.ui.pagos.PagosScreen(hiltViewModel()) { nav.popBackStack() } }
    pantalla(Rutas.PAPELERA) { io.github.alexcorrea2001lemon.nummia.ui.papelera.PapeleraScreen(hiltViewModel(), snackbar) { nav.popBackStack() } }
    pantalla(Rutas.IMPORTAR) {
        io.github.alexcorrea2001lemon.nummia.ui.importar.ImportarScreen(hiltViewModel(), { nav.popBackStack() }) { irAPestana(nav, Rutas.INICIO) }
    }
    pantalla(Rutas.DRIVE) { io.github.alexcorrea2001lemon.nummia.ui.drive.DriveScreen(hiltViewModel(), snackbar) { nav.popBackStack() } }
    pantalla(Rutas.ACERCA) { AcercaScreen(ajustesVm(nav), hiltViewModel(), { nav.popBackStack() }) }
}

/**
 * Cambia de pestaña como la barra inferior: Inicio siempre queda debajo, así «Atrás» y el botón Inicio
 * funcionan desde cualquier pestaña, se llegue como se llegue.
 */
private fun irAPestana(nav: NavHostController, ruta: String) {
    if (ruta == Rutas.INICIO) {
        if (!nav.popBackStack(Rutas.INICIO, inclusive = false)) nav.navigate(Rutas.INICIO) { launchSingleTop = true }
        return
    }
    nav.navigate(ruta) {
        popUpTo(Rutas.INICIO) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Etiqueta que se encoge un poco si no cabe (textos grandes o pantallas estrechas), en vez de cortarse. */
@Composable
private fun TextoAjustado(texto: String) {
    val base = MaterialTheme.typography.labelSmall
    var escala by remember(texto) { mutableStateOf(1f) }
    Text(
        texto,
        style = base.copy(fontSize = base.fontSize * escala, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.didOverflowWidth && escala > 0.6f) escala *= 0.92f },
    )
}

/** Las subpantallas de Ajustes comparten el mismo ViewModel. */
@Composable
private fun ajustesVm(nav: NavHostController): AjustesViewModel {
    val padre = remember(nav.currentBackStackEntry) { runCatching { nav.getBackStackEntry(Rutas.AJUSTES) }.getOrNull() }
    return if (padre != null) hiltViewModel(padre) else hiltViewModel()
}

private fun NavGraphBuilder.pantalla(
    ruta: String,
    argumentos: List<androidx.navigation.NamedNavArgument> = emptyList(),
    contenido: @Composable () -> Unit,
) {
    composable(ruta, argumentos) {
        CompositionLocalProvider(LocalAmbitoAnimado provides this) { contenido() }
    }
}

@Composable
private fun Proximamente(icono: ImageVector, titulo: String, relleno: PaddingValues, version: String = "1.1.0") {
    Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
        EstadoVacio(icono, titulo, "Esta sección llega en la versión $version. Nummia te avisará cuando esté lista.")
    }
}

@Composable
private fun MenuAnadir(abierto: Boolean, alAlternar: () -> Unit, alElegir: (String) -> Unit) {
    val giro by animateFloatAsState(if (abierto) 45f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "giro")
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        val opciones = listOf(
            Triple("Gasto", Iconos.Bolsa, Rutas.gasto()),
            Triple("Ingreso", Iconos.Euro, Rutas.ingreso()),
            Triple("Reembolso", Iconos.FlechaAbajo, Rutas.reembolso()),
            Triple("Pago para otra persona", Iconos.Persona, Rutas.gasto(paraOtro = true)),
        )
        opciones.forEachIndexed { i, (texto, icono, destino) ->
            AnimatedVisibility(
                abierto,
                enter = fadeIn(tween(160, delayMillis = (opciones.size - i) * 30)) + slideInVertically(spring(dampingRatio = 0.75f, stiffness = 500f)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically { it / 2 },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 2.dp) {
                        Text(texto, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = Espacio.m, vertical = Espacio.s))
                    }
                    SmallFloatingActionButton(onClick = { alElegir(destino) }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(icono, texto)
                    }
                }
            }
        }
        FloatingActionButton(onClick = alAlternar, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Icon(Iconos.Mas, if (abierto) "Cerrar menú" else "Añadir", Modifier.size(28.dp).rotate(giro))
        }
    }
}
