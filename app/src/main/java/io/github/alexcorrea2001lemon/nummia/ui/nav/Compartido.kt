@file:OptIn(ExperimentalSharedTransitionApi::class)

package io.github.alexcorrea2001lemon.nummia.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalTransicionCompartida = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAmbitoAnimado = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Elemento que «vuela» de la lista al detalle (pilar 1). Sin ámbitos disponibles no hace nada. */
@Composable
fun Modifier.compartido(clave: String): Modifier {
    val t = LocalTransicionCompartida.current ?: return this
    val a = LocalAmbitoAnimado.current ?: return this
    return with(t) {
        this@compartido.sharedBounds(
            sharedContentState = rememberSharedContentState(key = clave),
            animatedVisibilityScope = a,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
