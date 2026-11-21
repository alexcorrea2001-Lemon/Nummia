package io.github.alexcorrea2001lemon.nummia.data.drive

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Drive y Hojas de cálculo en memoria, para probar la sincronización sin red. */
class FakeNube : Nube {
    class Archivo(val id: String, var nombre: String, val mime: String, val padre: String?, var bytes: ByteArray = ByteArray(0)) {
        var version = 1
        val pestanas = LinkedHashMap<Int, String>() // sheetId → título
        val celdas = HashMap<String, MutableList<MutableList<JsonElement>>>()
    }

    val archivos = LinkedHashMap<String, Archivo>()
    var fallarEscrituras = false
    private var siguiente = 0

    fun hoja(nombre: String) = archivos.values.first { it.nombre == nombre && it.mime == ApiGoogle.HOJA }
    fun filas(hoja: String, pestana: String): List<List<JsonElement>> = hoja(hoja).celdas[pestana].orEmpty()

    /** Simula que alguien edita una celda desde el ordenador (fila y columna empiezan en 1). */
    fun editarDesdeOrdenador(hoja: String, pestana: String, fila: Int, columna: Int, valor: JsonElement) {
        val a = hoja(hoja)
        poner(a, pestana, fila, columna, valor)
        a.version++
    }

    fun borrarFilaDesdeOrdenador(hoja: String, pestana: String, fila: Int) {
        val a = hoja(hoja)
        a.celdas[pestana]?.removeAt(fila - 1)
        a.version++
    }

    private fun poner(a: Archivo, pestana: String, fila: Int, columna: Int, valor: JsonElement) {
        val t = a.celdas.getOrPut(pestana) { mutableListOf() }
        while (t.size < fila) t += mutableListOf<JsonElement>()
        val f = t[fila - 1]
        while (f.size < columna) f += JsonPrimitive("")
        f[columna - 1] = valor
    }

    override suspend fun buscar(nombre: String, padre: String?, mime: String?) =
        archivos.values.filter { it.nombre == nombre && (padre == null || it.padre == padre) && (mime == null || it.mime == mime) }
            .map { ArchivoDrive(it.id, it.nombre, it.version.toString(), it.bytes.size.toLong()) }

    override suspend fun listar(q: String, orden: String): List<ArchivoDrive> {
        val padre = Regex("'([^']+)' in parents").find(q)?.groupValues?.get(1)
        val mime = Regex("mimeType = '([^']+)'").find(q)?.groupValues?.get(1)
        return archivos.values.filter { it.padre == padre && (mime == null || it.mime == mime) }
            .sortedByDescending { it.nombre }.map { ArchivoDrive(it.id, it.nombre, it.version.toString(), it.bytes.size.toLong()) }
    }

    override suspend fun crear(nombre: String, mime: String, padre: String?): String {
        val a = Archivo("f${++siguiente}", nombre, mime, padre)
        if (mime == ApiGoogle.HOJA) a.pestanas[0] = "Hoja 1"
        archivos[a.id] = a
        return a.id
    }

    override suspend fun modificado(id: String): String? = archivos[id]?.version?.toString()

    override suspend fun subir(nombre: String, padre: String, bytes: ByteArray): String {
        val id = crear(nombre, "application/octet-stream", padre)
        archivos.getValue(id).bytes = bytes
        return id
    }

    override suspend fun descargar(id: String): ByteArray = archivos.getValue(id).bytes

    override suspend fun borrar(id: String) { archivos.remove(id) }

    override suspend fun pestanas(id: String): Map<String, Int> = archivos.getValue(id).pestanas.entries.associate { it.value to it.key }

    override suspend fun actualizar(id: String, peticiones: JsonArray) {
        val a = archivos.getValue(id)
        for (p in peticiones.map { it.jsonObject }) {
            p["addSheet"]?.let { val pr = it.jsonObject.getValue("properties").jsonObject; a.pestanas[pr.getValue("sheetId").jsonPrimitive.int] = pr.texto("title") }
            p["updateSheetProperties"]?.let {
                val pr = it.jsonObject.getValue("properties").jsonObject
                pr["title"]?.let { t -> a.pestanas[pr.getValue("sheetId").jsonPrimitive.int] = t.jsonPrimitive.content }
            }
            p["deleteDimension"]?.let {
                val r = it.jsonObject.getValue("range").jsonObject
                val nombre = a.pestanas.getValue(r.getValue("sheetId").jsonPrimitive.int)
                val t = a.celdas[nombre] ?: return@let
                val desde = r.getValue("startIndex").jsonPrimitive.int
                repeat(r.getValue("endIndex").jsonPrimitive.int - desde) { if (desde < t.size) t.removeAt(desde) }
            }
        }
        a.version++
    }

    private fun JsonObject.texto(k: String) = getValue(k).jsonPrimitive.content

    override suspend fun leer(id: String, rangos: List<String>): List<List<List<JsonElement>>> {
        val a = archivos.getValue(id)
        return rangos.map { r ->
            val t = a.celdas[r.substringBefore('!')].orEmpty().map { it.toList() }
            t.dropLastWhile { f -> f.all { (it as? JsonPrimitive)?.content.isNullOrEmpty() } }
        }
    }

    override suspend fun escribir(id: String, datos: List<Pair<String, List<List<JsonElement>>>>, formulas: Boolean) {
        if (datos.isEmpty()) return
        if (fallarEscrituras) throw java.io.IOException("sin red")
        val a = archivos.getValue(id)
        for ((rango, filas) in datos) {
            val pestana = rango.substringBefore('!')
            val celda = Regex("""([A-Z]+)(\d+)""").find(rango.substringAfter('!'))!!
            val col0 = celda.groupValues[1].fold(0) { acc, ch -> acc * 26 + (ch - 'A' + 1) }
            val fila0 = celda.groupValues[2].toInt()
            filas.forEachIndexed { i, f -> f.forEachIndexed { j, v -> poner(a, pestana, fila0 + i, col0 + j, v) } }
        }
        a.version++
    }
}
