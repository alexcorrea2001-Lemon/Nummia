package io.github.alexcorrea2001lemon.nummia.data.drive

/** Lo que llegó de la hoja (cambios hechos en el ordenador) en la última sincronización. */
data class CambiosOrdenador(val nuevos: Int = 0, val editados: Int = 0, val borrados: Int = 0) {
    val total get() = nuevos + editados + borrados

    fun texto(): String? {
        if (total == 0) return null
        val partes = listOfNotNull(
            nuevos.takeIf { it > 0 }?.let { "$it ${if (it == 1) "nuevo" else "nuevos"}" },
            editados.takeIf { it > 0 }?.let { "$it ${if (it == 1) "cambiado" else "cambiados"}" },
            borrados.takeIf { it > 0 }?.let { "$it ${if (it == 1) "borrado" else "borrados"}" },
        )
        val frase = if (partes.size == 1) partes[0] else partes.dropLast(1).joinToString(", ") + " y " + partes.last()
        return "Desde el ordenador: $frase"
    }

    fun guardar() = "$nuevos,$editados,$borrados"

    companion object {
        fun leer(s: String?): CambiosOrdenador? =
            s?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 3 }?.let { CambiosOrdenador(it[0], it[1], it[2]) }
    }
}
