@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.alexcorrea2001lemon.nummia.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.actividad
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
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
)

@Composable
fun AppNummia(ajustes: Ajustes, alTerminarBienvenida: (Long?, String?) -> Unit) {
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
    // «Nuevo gasto» / «Nuevo ingreso» desde el icono o el widget
    val actividadApp = androidx.compose.ui.platform.LocalContext.current.actividad()
    if (actividadApp != null) {
        val principal: io.github.alexcorrea2001lemon.nummia.MainViewModel = hiltViewModel(actividadApp)
        val destino by principal.destino.collectAsStateWithLifecycle()
        LaunchedEffect(destino) {
            destino?.let { nav.navigate(it) { launchSingleTop = true }; principal.destino.value = null }
        }
    }

    // Tablet, plegable abierto o móvil en horizontal: barra lateral y contenido centrado.
    val ancha = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(esPrincipal && !ancha, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
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
            // Al bajar por una lista el botón + se esconde para no tapar nada; al subir vuelve.
            AnimatedVisibility(
                (ruta == Rutas.INICIO || ruta == Rutas.MOVIMIENTOS) && true,
                enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 500f)) + fadeIn(),
                exit = scaleOut(tween(150)) + fadeOut(tween(150)),
            ) {
                MenuAnadir(menuAbierto, alAlternar = { haptica(); menuAbierto = !menuAbierto }) { destino ->
                    menuAbierto = false
                    nav.navigate(destino)
                }
            }
        },
    ) { relleno ->
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
        if (ancha && esPrincipal) {
            androidx.compose.material3.NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                pestanas.forEach { p ->
                    androidx.compose.material3.NavigationRailItem(
                        selected = ruta == p.ruta,
                        onClick = { if (ruta != p.ruta) irAPestana(nav, p.ruta) },
                        icon = { Icon(p.icono, null) },
                        label = { Text(p.nombre, maxLines = 1) },
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
        // Inicio aprovecha la pantalla grande en dos columnas; lo demás se queda centrado y legible.
        val maximo by androidx.compose.animation.core.animateDpAsState(
            if (ruta == Rutas.INICIO && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= ANCHO_DOS_COLUMNAS) 1200.dp else 760.dp,
            label = "ancho",
        )
        SharedTransitionLayout(Modifier.widthIn(max = maximo)) {
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
        }
        }
        // Con el menú + abierto, lo de detrás se oscurece bastante: así las opciones se leen bien.
        AnimatedVisibility(menuAbierto, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f))
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
            alPresupuestos = { nav.navigate(Rutas.PRESUPUESTOS) },
            alEstadisticas = { p, ym -> Saltos.estadisticas.value = p to ym; irAPestana(nav, Rutas.ESTADISTICAS) },
            alPendientes = { nav.navigate(Rutas.DEBEN) },
            relleno = relleno,
        )
    }
    pantalla(Rutas.MOVIMIENTOS) {
        MovimientosScreen(hiltViewModel(), snackbar, ::abrirGasto, ::abrirIngreso, { nav.navigate(Rutas.AJUSTES) }, { nav.navigate(Rutas.gasto()) }, relleno)
    }
    pantalla(Rutas.CALENDARIO) {
        CalendarioScreen(
            hiltViewModel(), ::abrirGasto, ::abrirIngreso,
            alAnadirEn = { fecha, ingreso -> nav.navigate(if (ingreso) Rutas.ingreso(fecha = fecha) else Rutas.gasto(fecha = fecha)) },
            alAjustes = { nav.navigate(Rutas.AJUSTES) }, relleno = relleno,
        )
    }
    pantalla(Rutas.ESTADISTICAS) {
        EstadisticasScreen(hiltViewModel(), ::abrirGasto, { nav.navigate(Rutas.AJUSTES) }, relleno, alInformes = { nav.navigate(Rutas.INFORMES) }, alTienda = { nav.navigate(Rutas.tienda(it)) })
    }
    pantalla(Rutas.INFORMES) {
        io.github.alexcorrea2001lemon.nummia.ui.informes.InformesScreen(hiltViewModel(), snackbar, { nav.navigate(Rutas.AJUSTES) }, relleno, alVolver = { nav.popBackStack() })
    }

    pantalla(Rutas.DETALLE, listOf(navArgument("id") { type = NavType.StringType })) {
        DetalleScreen(
            hiltViewModel(), snackbar, alVolver = { nav.popBackStack() },
            alEditar = { nav.navigate(Rutas.gasto(it)) },
            alAnadirReembolso = { nav.navigate(Rutas.reembolso(it)) },
            alAbrirGasto = { nav.navigate(Rutas.detalle(it)) { popUpTo(Rutas.DETALLE) { inclusive = true } } },
            alTienda = { nav.navigate(Rutas.tienda(it)) },
        )
    }
    pantalla(
        Rutas.GASTO,
        listOf(
            navArgument("id") { type = NavType.StringType; defaultValue = "" },
            navArgument("paraOtro") { type = NavType.BoolType; defaultValue = false },
            navArgument("fecha") { type = NavType.StringType; defaultValue = "" },
        ),
    ) {
        val actividad = requireNotNull(androidx.compose.ui.platform.LocalContext.current.actividad())
        val principal: io.github.alexcorrea2001lemon.nummia.MainViewModel = hiltViewModel(actividad)
        FormularioGastoScreen(hiltViewModel(), snackbar, alEditado = { principal.ofrecerDeshacer(snackbar, it) }) { nav.popBackStack() }
    }
    pantalla(Rutas.INGRESO, listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" }, navArgument("fecha") { type = NavType.StringType; defaultValue = "" })) {
        FormularioIngresoScreen(hiltViewModel()) { nav.popBackStack() }
    }
    pantalla(Rutas.REEMBOLSO, listOf(navArgument("gastoId") { type = NavType.StringType; defaultValue = "" })) {
        FormularioReembolsoScreen(hiltViewModel()) { nav.popBackStack() }
    }

    pantalla(Rutas.AJUSTES) { AjustesScreen(hiltViewModel(), { nav.popBackStack() }) { nav.navigate(it) } }
    pantalla(Rutas.APARIENCIA) { AparienciaScreen(ajustesVm(nav), { nav.popBackStack() }) }
    pantalla(Rutas.SUELDO) { SueldoScreen(ajustesVm(nav), { nav.popBackStack() }) }
    pantalla(Rutas.PERSONAS) { PersonasScreen(ajustesVm(nav), { nav.popBackStack() }) { nav.navigate(Rutas.DEBEN) } }
    pantalla(Rutas.TIENDA, listOf(navArgument("nombre") { type = NavType.StringType })) {
        io.github.alexcorrea2001lemon.nummia.ui.tiendas.FichaTiendaScreen(hiltViewModel(), ::abrirGasto) { nav.popBackStack() }
    }
    pantalla(Rutas.DEBEN) { io.github.alexcorrea2001lemon.nummia.ui.deben.DebenScreen(hiltViewModel(), snackbar, ::abrirGasto) { nav.popBackStack() } }
    pantalla(Rutas.DATOS) { DatosScreen(ajustesVm(nav), snackbar, { nav.navigate(it) }) { nav.popBackStack() } }
    pantalla(Rutas.NOTIFICACIONES) { io.github.alexcorrea2001lemon.nummia.ui.ajustes.NotificacionesScreen(ajustesVm(nav)) { nav.popBackStack() } }
    pantalla(Rutas.SEGURIDAD) { io.github.alexcorrea2001lemon.nummia.ui.ajustes.SeguridadScreen(ajustesVm(nav)) { nav.popBackStack() } }
    pantalla(Rutas.TIENDAS) { io.github.alexcorrea2001lemon.nummia.ui.tiendas.TiendasScreen(hiltViewModel(), snackbar) { nav.popBackStack() } }
    pantalla(Rutas.CATEGORIAS) { io.github.alexcorrea2001lemon.nummia.ui.tiendas.CategoriasScreen(hiltViewModel()) { nav.popBackStack() } }
    pantalla(Rutas.PRESUPUESTOS) { io.github.alexcorrea2001lemon.nummia.ui.presupuestos.PresupuestosScreen(hiltViewModel()) { nav.popBackStack() } }
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

/** A partir de este ancho (dp) Inicio se muestra en dos columnas. */
const val ANCHO_DOS_COLUMNAS = 840

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
private fun MenuAnadir(abierto: Boolean, alAlternar: () -> Unit, alElegir: (String) -> Unit) {
    val giro by animateFloatAsState(if (abierto) 45f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "giro")
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        val opciones = listOf(
            Triple("Gasto", Iconos.Bolsa, Rutas.gasto()),
            Triple("Ingreso", Iconos.Euro, Rutas.ingreso()),
            Triple("Devolución", Iconos.FlechaAbajo, Rutas.reembolso()),
            Triple("Pago para otra persona", Iconos.Persona, Rutas.gasto(paraOtro = true)),
        )
        opciones.forEachIndexed { i, (texto, icono, destino) ->
            AnimatedVisibility(
                abierto,
                enter = fadeIn(tween(160, delayMillis = (opciones.size - i) * 30)) + slideInVertically(spring(dampingRatio = 0.75f, stiffness = 500f)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutVertically { it / 2 },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    // Etiqueta en color inverso (clara sobre fondo oscuro y al revés): máximo contraste.
                    Surface(
                        onClick = { alElegir(destino) }, shape = MaterialTheme.shapes.small,
                        // TalkBack ya lee el botón redondo de al lado: la etiqueta no se repite.
                        modifier = Modifier.clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface, shadowElevation = 4.dp,
                    ) {
                        Text(texto, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = Espacio.m, vertical = Espacio.s))
                    }
                    SmallFloatingActionButton(
                        onClick = { alElegir(destino) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
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
