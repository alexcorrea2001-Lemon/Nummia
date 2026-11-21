package io.github.alexcorrea2001lemon.nummia.ui.nav

import io.github.alexcorrea2001lemon.nummia.ui.estadisticas.PestanaEst
import io.github.alexcorrea2001lemon.nummia.ui.movimientos.Pestana
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.YearMonth

/** Saltos entre pestañas con algo ya elegido (p. ej. de Inicio a Estadísticas → Dónde gasto de ese mes). */
object Saltos {
    val estadisticas = MutableStateFlow<Pair<PestanaEst, YearMonth>?>(null)
    val movimientos = MutableStateFlow<Pestana?>(null)
}
