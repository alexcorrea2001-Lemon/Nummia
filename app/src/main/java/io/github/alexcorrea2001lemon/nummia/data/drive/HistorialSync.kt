package io.github.alexcorrea2001lemon.nummia.data.drive

/** Una sincronización: qué se subió a la hoja, qué llegó del ordenador y si hubo conflictos o un error. */
data class EntradaSync(
    val fecha: Long,
    val subidos: Int = 0,
    val nuevos: Int = 0,
    val editados: Int = 0,
    val borrados: Int = 0,
    val conflictos: Int = 0,
    val error: String? = null,
) {
    fun resumen(): String {
        if (error != null) return "No se pudo sincronizar: $error"
        val partes = buildList {
            if (subidos > 0) add("$subidos ${if (subidos == 1) "subido" else "subidos"}")
            CambiosOrdenador(nuevos, editados, borrados).texto()?.let { add(it.removePrefix("Desde el ordenador: ") + " desde el ordenador") }
            if (conflictos > 0) add("$conflictos ${if (conflictos == 1) "conflicto" else "conflictos"}")
        }
        return partes.joinToString(" · ").ifEmpty { "Sin cambios" }
    }

    /** Una línea por entrada; el error va al final y puede llevar cualquier texto (sin saltos de línea). */
    fun guardar(): String = listOf(fecha, subidos, nuevos, editados, borrados, conflictos).joinToString(";") + ";" + (error?.replace('\n', ' ') ?: "")

    companion object {
        fun leer(linea: String): EntradaSync? {
            val p = linea.split(";", limit = 7)
            if (p.size < 7) return null
            val n = p.take(6).map { it.toLongOrNull() ?: return null }
            return EntradaSync(n[0], n[1].toInt(), n[2].toInt(), n[3].toInt(), n[4].toInt(), n[5].toInt(), p[6].ifEmpty { null })
        }

        /** Las entradas guardadas, la más reciente primero. */
        fun lista(guardado: String?): List<EntradaSync> = guardado.orEmpty().lines().mapNotNull { if (it.isBlank()) null else leer(it) }
    }
}

/** Añade [e] al principio y se queda con las 15 últimas. */
fun anadirHistorial(guardado: String?, e: EntradaSync, maximo: Int = 15): String =
    (listOf(e) + EntradaSync.lista(guardado)).take(maximo).joinToString("\n") { it.guardar() }
