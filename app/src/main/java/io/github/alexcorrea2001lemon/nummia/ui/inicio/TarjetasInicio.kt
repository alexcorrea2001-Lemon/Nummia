package io.github.alexcorrea2001lemon.nummia.ui.inicio

/** Tarjetas de Inicio que se pueden ordenar u ocultar (el balance del mes siempre va arriba). */
enum class TarjetaInicio(val clave: String, val nombre: String) {
    INDICADORES("indicadores", "Ingresos, pagado y gasto real"),
    PREVISION("prevision", "A fin de mes te quedarán"),
    PRESUPUESTOS("presupuestos", "Presupuestos"),
    PENDIENTES("pendientes", "Pendientes y te deben"),
    TOP("top", "En qué se va"),
    AHORRADO("ahorrado", "Ahorrado en el año"),
    ULTIMOS("ultimos", "Últimos movimientos"),
}

/** Orden guardado («ultimos,ahorrado,…»): lo que no se reconoce se ignora y lo que falta va al final. */
fun ordenTarjetas(guardado: String): List<TarjetaInicio> {
    val porClave = TarjetaInicio.entries.associateBy { it.clave }
    val elegidas = guardado.split(',').mapNotNull { porClave[it.trim()] }.distinct()
    return elegidas + TarjetaInicio.entries.filter { it !in elegidas }
}

/** Sube ([pasos] < 0) o baja una tarjeta, sin salirse de la lista. */
fun mover(lista: List<TarjetaInicio>, t: TarjetaInicio, pasos: Int): List<TarjetaInicio> {
    val i = lista.indexOf(t)
    val j = (i + pasos).coerceIn(0, lista.lastIndex)
    if (i < 0 || i == j) return lista
    return lista.toMutableList().apply { add(j, removeAt(i)) }
}
