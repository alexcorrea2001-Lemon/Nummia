package io.github.alexcorrea2001lemon.nummia.data.drive

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** Lo que la sincronización necesita de Google (lo implementa [ApiGoogle]; en las pruebas, una nube falsa). */
interface Nube {
    suspend fun buscar(nombre: String, padre: String?, mime: String? = null): List<ArchivoDrive>
    suspend fun listar(q: String, orden: String = "name"): List<ArchivoDrive>
    suspend fun crear(nombre: String, mime: String, padre: String?): String
    suspend fun modificado(id: String): String?
    suspend fun subir(nombre: String, padre: String, bytes: ByteArray, mime: String = "application/octet-stream"): String
    suspend fun descargar(id: String): ByteArray
    suspend fun borrar(id: String)
    suspend fun pestanas(id: String): Map<String, Int>
    suspend fun actualizar(id: String, peticiones: JsonArray)
    suspend fun leer(id: String, rangos: List<String>): List<List<List<JsonElement>>>
    /** Correo de la cuenta de Google (no hace falta permiso extra). */
    suspend fun correo(): String? = null
    suspend fun escribir(id: String, datos: List<Pair<String, List<List<JsonElement>>>>, formulas: Boolean = false)
}
