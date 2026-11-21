package io.github.alexcorrea2001lemon.nummia.data.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ErrorDrive(val codigo: Int, mensaje: String) : IOException(mensaje)

data class ArchivoDrive(val id: String, val nombre: String, val modificado: String?, val tamano: Long?)

/**
 * Llamadas REST a Drive v3 y Sheets v4 (sin bibliotecas de Google, para que el APK no crezca).
 * [token] da el token de acceso; si Google lo rechaza (401), se pide uno nuevo una vez.
 */
class ApiGoogle(
    private val cliente: OkHttpClient,
    private val token: suspend (renovar: Boolean) -> String,
    private val drive: String = "https://www.googleapis.com/",
    private val sheets: String = "https://sheets.googleapis.com/",
) : Nube {
    private val json = Json { ignoreUnknownKeys = true }
    private val tipoJson = "application/json; charset=utf-8".toMediaType()
    private fun cuerpo(o: JsonElement) = o.toString().toRequestBody(tipoJson)

    private suspend fun llamar(construir: (Request.Builder) -> Request.Builder): String = withContext(Dispatchers.IO) {
        var renovar = false
        repeat(2) { intento ->
            val r = construir(Request.Builder()).header("Authorization", "Bearer ${token(renovar)}").build()
            cliente.newCall(r).execute().use { resp ->
                val texto = resp.body?.string().orEmpty()
                if (resp.isSuccessful) return@withContext texto
                if (resp.code == 401 && intento == 0) { renovar = true; return@use }
                val mensaje = runCatching { json.parseToJsonElement(texto).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content }.getOrNull()
                throw ErrorDrive(resp.code, traducir(mensaje) ?: "Error ${resp.code} de Google")
            }
        }
        throw ErrorDrive(401, "Google no acepta el acceso. Vuelve a conectar tu Drive.")
    }

    /** Los errores más habituales de configuración, en español y con qué hacer. */
    private fun traducir(m: String?): String? {
        if (m == null) return null
        if ("has not been used in project" in m || "is disabled" in m) {
            val api = when {
                "sheets" in m.lowercase() -> "Google Sheets API"
                "drive" in m.lowercase() -> "Google Drive API"
                else -> "API de Google"
            }
            return "Falta activar la «$api» en tu proyecto de Google Cloud (APIs y servicios → Biblioteca). Si acabas de activarla, espera unos minutos y vuelve a sincronizar."
        }
        return m
    }

    override suspend fun correo(): String? = runCatching {
        obj(llamar { it.url("${drive}drive/v3/about?fields=user(emailAddress)") })["user"]?.jsonObject?.get("emailAddress")?.jsonPrimitive?.content
    }.getOrNull()

    private fun obj(texto: String) = json.parseToJsonElement(texto).jsonObject

    // ---------------- Drive

    override suspend fun buscar(nombre: String, padre: String?, mime: String?): List<ArchivoDrive> {
        val q = buildString {
            append("name = '${nombre.replace("'", "\\'")}' and trashed = false")
            if (padre != null) append(" and '$padre' in parents")
            if (mime != null) append(" and mimeType = '$mime'")
        }
        return listar(q, "modifiedTime desc")
    }

    override suspend fun listar(q: String, orden: String): List<ArchivoDrive> {
        val url = "${drive}drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("q", q).addQueryParameter("orderBy", orden)
            .addQueryParameter("fields", "files(id,name,modifiedTime,size)").addQueryParameter("pageSize", "200").build()
        return obj(llamar { it.url(url) })["files"]!!.jsonArray.map {
            val o = it.jsonObject
            ArchivoDrive(o["id"]!!.jsonPrimitive.content, o["name"]!!.jsonPrimitive.content, o["modifiedTime"]?.jsonPrimitive?.content, o["size"]?.jsonPrimitive?.content?.toLongOrNull())
        }
    }

    override suspend fun crear(nombre: String, mime: String, padre: String?): String {
        val datos = buildJsonObject {
            put("name", nombre); put("mimeType", mime)
            if (padre != null) putJsonArray("parents") { add(kotlinx.serialization.json.JsonPrimitive(padre)) }
        }
        return obj(llamar { it.url("${drive}drive/v3/files?fields=id").post(cuerpo(datos)) })["id"]!!.jsonPrimitive.content
    }

    /** Fecha de la última modificación, o null si ya no existe (o está en la papelera). */
    override suspend fun modificado(id: String): String? = try {
        val o = obj(llamar { it.url("${drive}drive/v3/files/$id?fields=modifiedTime,trashed") })
        if (o["trashed"]?.jsonPrimitive?.content == "true") null else o["modifiedTime"]?.jsonPrimitive?.content
    } catch (e: ErrorDrive) {
        if (e.codigo == 404) null else throw e
    }

    override suspend fun subir(nombre: String, padre: String, bytes: ByteArray, mime: String): String {
        val meta = buildJsonObject { put("name", nombre); putJsonArray("parents") { add(kotlinx.serialization.json.JsonPrimitive(padre)) } }
        val partes = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody(tipoJson))
            .addPart(bytes.toRequestBody(mime.toMediaType()))
            .build()
        return obj(llamar { it.url("${drive}upload/drive/v3/files?uploadType=multipart&fields=id").post(partes) })["id"]!!.jsonPrimitive.content
    }

    override suspend fun descargar(id: String): ByteArray = withContext(Dispatchers.IO) {
        var renovar = false
        repeat(2) { intento ->
            val r = Request.Builder().url("${drive}drive/v3/files/$id?alt=media").header("Authorization", "Bearer ${token(renovar)}").build()
            cliente.newCall(r).execute().use { resp ->
                if (resp.isSuccessful) return@withContext resp.body!!.bytes()
                if (resp.code == 401 && intento == 0) { renovar = true; return@use }
                throw ErrorDrive(resp.code, "No se pudo descargar la copia (${resp.code}).")
            }
        }
        throw ErrorDrive(401, "Google no acepta el acceso.")
    }

    override suspend fun borrar(id: String) { llamar { it.url("${drive}drive/v3/files/$id").delete() } }

    // ---------------- Sheets

    override suspend fun pestanas(id: String): Map<String, Int> =
        obj(llamar { it.url("${sheets}v4/spreadsheets/$id?fields=sheets.properties(sheetId,title)") })["sheets"]!!.jsonArray
            .associate { s -> s.jsonObject["properties"]!!.jsonObject.let { it["title"]!!.jsonPrimitive.content to it["sheetId"]!!.jsonPrimitive.content.toInt() } }

    override suspend fun actualizar(id: String, peticiones: JsonArray) {
        if (peticiones.isEmpty()) return
        llamar { it.url("${sheets}v4/spreadsheets/$id:batchUpdate").post(cuerpo(buildJsonObject { put("requests", peticiones) })) }
    }

    /** Valores sin formato: números como números y fechas como número de serie. */
    override suspend fun leer(id: String, rangos: List<String>): List<List<List<JsonElement>>> {
        val url = "${sheets}v4/spreadsheets/$id/values:batchGet".toHttpUrl().newBuilder().apply {
            rangos.forEach { addQueryParameter("ranges", it) }
            addQueryParameter("valueRenderOption", "UNFORMATTED_VALUE")
            addQueryParameter("dateTimeRenderOption", "SERIAL_NUMBER")
        }.build()
        return obj(llamar { it.url(url) })["valueRanges"]!!.jsonArray.map { vr ->
            vr.jsonObject["values"]?.jsonArray?.map { fila -> fila.jsonArray.toList() }.orEmpty()
        }
    }

    override suspend fun escribir(id: String, datos: List<Pair<String, List<List<JsonElement>>>>, formulas: Boolean) {
        if (datos.isEmpty()) return
        val c = buildJsonObject {
            put("valueInputOption", if (formulas) "USER_ENTERED" else "RAW")
            put("data", buildJsonArray {
                datos.forEach { (rango, filas) ->
                    add(buildJsonObject {
                        put("range", rango)
                        put("values", JsonArray(filas.map { JsonArray(it) }))
                    })
                }
            })
        }
        llamar { it.url("${sheets}v4/spreadsheets/$id/values:batchUpdate").post(cuerpo(c)) }
    }

    companion object {
        const val CARPETA = "application/vnd.google-apps.folder"
        const val HOJA = "application/vnd.google-apps.spreadsheet"
    }
}
