package io.github.alexcorrea2001lemon.nummia

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.fragment.app.FragmentActivity
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.Cerrojo
import io.github.alexcorrea2001lemon.nummia.ui.bloqueo.PantallaBloqueo
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexcorrea2001lemon.nummia.ui.nav.AppNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.NummiaTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val vm: MainViewModel by viewModels()

    /** Accesos directos del icono y botón del widget. */
    private fun atender(i: Intent?) {
        vm.destino.value = when (i?.action) {
            ACCION_GASTO -> io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.gasto()
            ACCION_INGRESO -> io.github.alexcorrea2001lemon.nummia.ui.nav.Rutas.ingreso()
            else -> return
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        atender(intent)
    }

    companion object {
        const val ACCION_GASTO = "io.github.alexcorrea2001lemon.nummia.NUEVO_GASTO"
        const val ACCION_INGRESO = "io.github.alexcorrea2001lemon.nummia.NUEVO_INGRESO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.ajustes.value == null }
        if (savedInstanceState == null) atender(intent)
        setContent {
            val ajustes by vm.ajustes.collectAsStateWithLifecycle()
            val a = ajustes ?: return@setContent
            val bloqueada by Cerrojo.bloqueada.collectAsStateWithLifecycle()
            val categorias by vm.categorias.collectAsStateWithLifecycle()
            val cerrada = a.bloqueo && bloqueada
            NummiaTheme(a) {
                androidx.compose.runtime.CompositionLocalProvider(io.github.alexcorrea2001lemon.nummia.ui.comun.LocalCategorias provides categorias) {
                Box(Modifier.fillMaxSize()) {
                    // Mientras está bloqueada, lo de debajo no se lee con TalkBack.
                    Box(if (cerrada) Modifier.clearAndSetSemantics { } else Modifier) {
                        AppNummia(a, alTerminarBienvenida = vm::terminarBienvenida)
                    }
                    if (cerrada) PantallaBloqueo(a.iconoLogo, a.iconoColor)
                }
                }
            }
        }
    }
}
