package io.github.alexcorrea2001lemon.nummia.domain.calculo

enum class Ambito { TOTAL, CATEGORIA, TIENDA }

/** Un límite mensual. [ref] es el id de la categoría o el nombre de la tienda (null en TOTAL). */
data class Limite(val id: String, val ambito: Ambito, val ref: String?, val limite: Long)

/** Un gasto con su categoría y tienda, para saber a qué presupuesto pertenece. */
data class GastoDePresupuesto(val mov: MovGasto, val tienda: String, val categoriaId: String?)

enum class Nivel { BIEN, CERCA, PASADO }

data class EstadoPresupuesto(val limite: Limite, val gastado: Long) {
    /** % gastado ×10. */
    val porMil: Int get() = porMil(gastado.coerceAtLeast(0), limite.limite) ?: 0
    val nivel: Nivel get() = when {
        porMil >= 1000 -> Nivel.PASADO
        porMil >= 800 -> Nivel.CERCA
        else -> Nivel.BIEN
    }
    val queda: Long get() = limite.limite - gastado
}

/**
 * Gasto real del periodo que cuenta para cada presupuesto (solo lo CONFIRMADO; los reembolsos restan).
 * Las tiendas se comparan sin mayúsculas ni tildes.
 */
fun estadoPresupuestos(limites: List<Limite>, gastos: List<GastoDePresupuesto>, reembolsos: List<MovReembolso>): List<EstadoPresupuesto> {
    val devuelto = reembolsos.groupBy { it.gastoId }.mapValues { (_, r) -> r.sumOf { it.importe } }
    val confirmados = gastos.filter { it.mov.estado == Estado.CONFIRMADO }
    fun real(g: GastoDePresupuesto) = g.mov.importe - (devuelto[g.mov.id] ?: 0)
    return limites.map { l ->
        val suyos = when (l.ambito) {
            Ambito.TOTAL -> confirmados
            Ambito.CATEGORIA -> confirmados.filter { it.categoriaId == l.ref }
            Ambito.TIENDA -> confirmados.filter { l.ref != null && claveTexto(it.tienda) == claveTexto(l.ref) }
        }
        EstadoPresupuesto(l, suyos.sumOf(::real))
    }
}

/** Umbrales que se acaban de cruzar (800 = 80 %, 1000 = 100 %), para avisar una sola vez de cada uno. */
fun umbralesCruzados(e: EstadoPresupuesto): List<Int> = listOf(800, 1000).filter { e.porMil >= it }
