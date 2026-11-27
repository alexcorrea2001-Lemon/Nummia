package io.github.alexcorrea2001lemon.nummia.domain.dinero

/** El texto del importe mientras se escribe y dónde está el cursor (0 = delante de todo). */
data class EdicionImporte(val texto: String, val cursor: Int)

private const val LARGO_MAXIMO = 24
private fun esOperador(c: Char) = c == '+' || c == '-' || c == '−'

/**
 * Aplica una tecla del teclado de Nummia en la posición del [cursor]: cifras, «,», «+», «−» y «⌫».
 * Al final del texto se comporta como siempre; en medio permite cambiar una cifra sin borrar lo de detrás.
 * Los puntos de miles («1.381,64» al editar) se quitan antes, para que el resultado se pueda leer.
 * Si la tecla no cabe (otra coma, un tercer decimal, un operador junto a otro…), no cambia nada.
 */
fun teclearImporte(texto: String, cursor: Int, tecla: String): EdicionImporte {
    val c0 = cursor.coerceIn(0, texto.length)
    val t = texto.replace(".", "")
    val c = c0 - texto.take(c0).count { it == '.' }
    val sinCambio = EdicionImporte(t, c)
    fun poner(trozo: String): EdicionImporte {
        val nuevo = t.substring(0, c) + trozo + t.substring(c)
        return if (nuevo.length > LARGO_MAXIMO) sinCambio else EdicionImporte(nuevo, c + trozo.length)
    }
    // El número en el que está el cursor: desde el operador anterior hasta el siguiente.
    val inicio = (c - 1 downTo 0).firstOrNull { esOperador(t[it]) }?.plus(1) ?: 0
    val fin = (c until t.length).firstOrNull { esOperador(t[it]) } ?: t.length
    val numero = t.substring(inicio, fin)
    val coma = numero.indexOf(',').let { if (it < 0) -1 else inicio + it }

    return when (tecla) {
        "⌫" -> if (c == 0) sinCambio else EdicionImporte(t.removeRange(c - 1, c), c - 1)
        "+", "−" -> when {
            c == t.length -> when {
                t.isEmpty() -> sinCambio
                esOperador(t.last()) || t.last() == ',' -> EdicionImporte(t.dropLast(1) + tecla, t.length)
                else -> poner(tecla)
            }
            c == 0 || esOperador(t[c - 1]) || esOperador(t[c]) || t[c - 1] == ',' -> sinCambio
            else -> poner(tecla)
        }
        "," -> when {
            coma >= 0 -> sinCambio
            fin - c > 2 -> sinCambio // dejaría más de 2 decimales
            c == inicio -> poner("0,")
            else -> poner(",")
        }
        else -> {
            if (tecla.length != 1 || !tecla[0].isDigit()) sinCambio
            else if (coma in 0 until c && fin - coma - 1 >= 2) sinCambio // ya hay 2 decimales
            else poner(tecla)
        }
    }
}
