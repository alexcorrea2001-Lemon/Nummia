package io.github.alexcorrea2001lemon.nummia.data.drive

/**
 * Decide qué hacer con cada fila comparando tres versiones: la del móvil, la de la hoja y la
 * «instantánea» (cómo estaban ambas en la última sincronización). No toca nada: devuelve un plan.
 *
 * - Solo cambió el móvil → se escribe en la hoja (o se borra la fila).
 * - Solo cambió la hoja → se aplica en el móvil (o se borra en el móvil).
 * - Cambiaron los dos de forma distinta → conflicto: no se toca y se pregunta.
 * - Fila de la hoja sin id → alta hecha desde el ordenador.
 */
data class Plan(
    val actualizarRemoto: List<Triple<Int, String, Canon>> = emptyList(),
    val anadirRemoto: List<Pair<String, Canon>> = emptyList(),
    val borrarRemoto: List<Int> = emptyList(),
    val aplicarLocal: List<Pair<String, Canon>> = emptyList(),
    val borrarLocal: List<String> = emptyList(),
    val nuevasDesdeRemoto: List<Pair<Int, Canon>> = emptyList(),
    val conflictos: List<Conflicto> = emptyList(),
    val invalidas: List<Pair<Int, String>> = emptyList(),
    /** Instantánea tras ejecutar el plan (sin contar las nuevas desde la hoja, que añade quien las cree). */
    val instantanea: Map<String, String> = emptyMap(),
) {
    val hayCambiosRemotos get() = actualizarRemoto.isNotEmpty() || anadirRemoto.isNotEmpty() || borrarRemoto.isNotEmpty()
    val hayCambiosLocales get() = aplicarLocal.isNotEmpty() || borrarLocal.isNotEmpty() || nuevasDesdeRemoto.isNotEmpty()
}

data class Conflicto(val id: String, val local: Canon?, val remoto: Canon?, val fila: Int?)

fun reconciliar(
    local: Map<String, Canon>,
    remoto: List<FilaRemota>,
    instantanea: Map<String, String>,
    conflictosAbiertos: Set<String> = emptySet(),
): Plan {
    val vistos = HashSet<String>()
    val remotoPorId = LinkedHashMap<String, FilaRemota>()
    val nuevas = ArrayList<Pair<Int, Canon>>()
    val invalidas = ArrayList<Pair<Int, String>>()
    for (r in remoto) {
        when {
            r.canon == null -> invalidas += r.fila to (r.error ?: "Fila no válida")
            r.id == null || !vistos.add(r.id) -> nuevas += r.fila to r.canon // sin id, o id repetido (fila copiada)
            else -> remotoPorId[r.id] = r
        }
    }
    // Las filas no válidas con id cuentan como «presentes sin cambios» para no borrar nada por error.
    val invalidasConId = remoto.filter { it.canon == null && it.id != null }.mapNotNull { it.id }.toSet()

    val actualizar = ArrayList<Triple<Int, String, Canon>>()
    val anadir = ArrayList<Pair<String, Canon>>()
    val borrarR = ArrayList<Int>()
    val aplicar = ArrayList<Pair<String, Canon>>()
    val borrarL = ArrayList<String>()
    val conflictos = ArrayList<Conflicto>()
    val nueva = HashMap<String, String>()

    val ids = LinkedHashSet<String>().apply { addAll(local.keys); addAll(remotoPorId.keys); addAll(instantanea.keys) }
    for (id in ids) {
        if (id in invalidasConId) { instantanea[id]?.let { nueva[id] = it }; continue }
        val l = local[id]?.let(::firma)
        val r = remotoPorId[id]?.canon?.let(::firma)
        val s = instantanea[id]
        val fila = remotoPorId[id]?.fila
        when {
            id in conflictosAbiertos -> { s?.let { nueva[id] = it } }
            l == r -> { if (l != null) nueva[id] = l }
            r == s -> { // solo cambió el móvil
                when {
                    l == null -> fila?.let(borrarR::add)
                    fila == null -> { anadir += id to local.getValue(id); nueva[id] = l }
                    else -> { actualizar += Triple(fila, id, local.getValue(id)); nueva[id] = l }
                }
            }
            l == s -> { // solo cambió la hoja
                if (r == null) borrarL += id else { aplicar += id to remotoPorId.getValue(id).canon!!; nueva[id] = r }
            }
            else -> { // cambiaron los dos
                conflictos += Conflicto(id, local[id], remotoPorId[id]?.canon, fila)
                s?.let { nueva[id] = it }
            }
        }
    }
    return Plan(actualizar, anadir, borrarR.sortedDescending(), aplicar, borrarL, nuevas, conflictos, invalidas, nueva)
}
