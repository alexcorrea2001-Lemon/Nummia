package io.github.alexcorrea2001lemon.nummia.ui.tiendas

import io.github.alexcorrea2001lemon.nummia.data.repo.TiendaResumen
import java.text.Normalizer

enum class OrdenTiendas(val texto: String) { VECES("Más veces"), GASTO("Más gasto"), NOMBRE("A–Z") }

private fun sinTildes(s: String) = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Las tiendas que coinciden con [busqueda] (sin tildes ni mayúsculas), en el [orden] elegido. */
fun filtrarTiendas(l: List<TiendaResumen>, busqueda: String, orden: OrdenTiendas): List<TiendaResumen> {
    val b = sinTildes(busqueda)
    val f = if (b.isEmpty()) l else l.filter { b in sinTildes(it.nombre) }
    return when (orden) {
        OrdenTiendas.VECES -> f.sortedWith(compareByDescending<TiendaResumen> { it.veces }.thenByDescending { it.total })
        OrdenTiendas.GASTO -> f.sortedWith(compareByDescending<TiendaResumen> { it.total }.thenByDescending { it.veces })
        OrdenTiendas.NOMBRE -> f.sortedBy { sinTildes(it.nombre) }
    }
}

/** Dos letras para el círculo: «Lucía» → «LU», «Panadería Sol» → «PS». */
fun iniciales(nombre: String): String {
    val p = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        p.isEmpty() -> "?"
        p.size == 1 -> p[0].take(2).uppercase()
        else -> "${p[0].first()}${p[1].first()}".uppercase()
    }
}
