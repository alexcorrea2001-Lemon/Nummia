package io.github.alexcorrea2001lemon.nummia.domain.ticket

import kotlin.math.abs

/** Una línea de texto reconocida en la foto, con su posición (píxeles; arriba < abajo). */
data class LineaOcr(val texto: String, val izquierda: Int, val arriba: Int, val abajo: Int) {
    val centro get() = (arriba + abajo) / 2f
    val alto get() = (abajo - arriba).coerceAtLeast(1)
}

/**
 * En un ticket el concepto va a la izquierda y el precio a la derecha, y el reconocimiento de texto
 * los entrega como bloques distintos. Aquí se vuelven a poner en la misma fila: las líneas cuyo centro
 * vertical coincide se unen de izquierda a derecha, y las filas salen de arriba abajo.
 */
fun juntarFilas(lineas: List<LineaOcr>): String {
    val filas = mutableListOf<MutableList<LineaOcr>>()
    for (l in lineas.sortedBy { it.centro }) {
        val ultima = filas.lastOrNull()?.last()
        if (ultima != null && abs(l.centro - ultima.centro) <= 0.6f * minOf(l.alto, ultima.alto)) filas.last().add(l) else filas.add(mutableListOf(l))
    }
    return filas.joinToString("\n") { fila -> fila.sortedBy { it.izquierda }.joinToString(" ") { it.texto.trim() } }
}
