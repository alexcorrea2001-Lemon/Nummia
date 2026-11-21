package io.github.alexcorrea2001lemon.nummia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexcorrea2001lemon.nummia.ui.nav.AppNummia
import io.github.alexcorrea2001lemon.nummia.ui.theme.NummiaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.ajustes.value == null }
        setContent {
            val ajustes by vm.ajustes.collectAsStateWithLifecycle()
            val a = ajustes ?: return@setContent
            NummiaTheme(a) { AppNummia(a, alTerminarBienvenida = vm::terminarBienvenida) }
        }
    }
}
