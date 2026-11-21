package io.github.alexcorrea2001lemon.nummia.data.actualizar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class Lanzamiento(
    val etiqueta: String,
    val notas: String,
    val apkUrl: String,
    val shaUrl: String,
    val tamano: Long,
) {
    val version get() = etiqueta.removePrefix("v")
    val nombreApk get() = "nummia-$etiqueta.apk"
}

sealed interface EstadoActualizacion {
    data object AlDia : EstadoActualizacion
    data class Disponible(val lanzamiento: Lanzamiento) : EstadoActualizacion
    data class NoSePudoComprobar(val motivo: String) : EstadoActualizacion
}

class DescargaInvalida(motivo: String) : Exception(motivo)

/** «v1.10.0» frente a «1.9.0» → true. Ignora sufijos como «-debug». */
fun esMasNueva(etiqueta: String, actual: String): Boolean {
    fun partes(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }.let { it + List((3 - it.size).coerceAtLeast(0)) { 0 } }
    val a = partes(etiqueta)
    val b = partes(actual)
    for (i in 0 until 3) if (a[i] != b[i]) return a[i] > b[i]
    return false
}

/**
 * Busca la última versión publicada en GitHub Releases, la descarga y comprueba su SHA-256.
 * La instalación (con la comprobación de firma) la hace [Instalador].
 */
class Actualizador(
    private val cliente: OkHttpClient,
    private val versionActual: String,
    private val carpeta: File,
    private val api: String = "https://api.github.com/",
    private val repositorio: String = "alexcorrea2001-Lemon/Nummia",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun comprobar(): EstadoActualizacion = withContext(Dispatchers.IO) {
        val peticion = Request.Builder()
            .url("${api}repos/$repositorio/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        try {
            cliente.newCall(peticion).execute().use { r ->
                when {
                    r.code == 404 -> EstadoActualizacion.AlDia
                    !r.isSuccessful -> EstadoActualizacion.NoSePudoComprobar("GitHub no responde ahora mismo.")
                    else -> {
                        val o = json.parseToJsonElement(r.body!!.string()).jsonObject
                        val etiqueta = o["tag_name"]!!.jsonPrimitive.content
                        if (!esMasNueva(etiqueta, versionActual)) return@use EstadoActualizacion.AlDia
                        val assets = o["assets"]?.jsonArray?.map { it.jsonObject }.orEmpty()
                        fun buscar(nombre: String): JsonObject? = assets.firstOrNull { it["name"]?.jsonPrimitive?.content == nombre }
                        val apk = buscar("nummia-$etiqueta.apk")
                        val sha = buscar("nummia-$etiqueta.apk.sha256")
                        if (apk == null || sha == null) {
                            EstadoActualizacion.NoSePudoComprobar("La versión $etiqueta aún no tiene el instalador listo.")
                        } else {
                            EstadoActualizacion.Disponible(
                                Lanzamiento(
                                    etiqueta = etiqueta,
                                    notas = o["body"]?.jsonPrimitive?.content.orEmpty(),
                                    apkUrl = apk["browser_download_url"]!!.jsonPrimitive.content,
                                    shaUrl = sha["browser_download_url"]!!.jsonPrimitive.content,
                                    tamano = apk["size"]?.jsonPrimitive?.long ?: 0L,
                                ),
                            )
                        }
                    }
                }
            }
        } catch (e: IOException) {
            EstadoActualizacion.NoSePudoComprobar("No hay conexión.")
        } catch (e: Exception) {
            EstadoActualizacion.NoSePudoComprobar("No se pudo comprobar.")
        }
    }

    /** Descarga y verifica el SHA-256. Si algo falla, el archivo se borra. */
    suspend fun descargar(l: Lanzamiento, progreso: (Float) -> Unit = {}): Result<File> = withContext(Dispatchers.IO) {
        carpeta.mkdirs()
        val destino = File(carpeta, l.nombreApk)
        runCatching {
            val esperado = cliente.newCall(Request.Builder().url(l.shaUrl).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                r.body!!.string().trim().split(Regex("\\s+")).first().lowercase()
            }
            if (!esperado.matches(Regex("[0-9a-f]{64}"))) throw DescargaInvalida("La huella de la versión no es válida.")
            val md = MessageDigest.getInstance("SHA-256")
            cliente.newCall(Request.Builder().url(l.apkUrl).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                val cuerpo = r.body!!
                val total = cuerpo.contentLength().takeIf { it > 0 } ?: l.tamano
                var leido = 0L
                destino.outputStream().use { salida ->
                    cuerpo.byteStream().use { entrada ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = entrada.read(buf)
                            if (n < 0) break
                            md.update(buf, 0, n)
                            salida.write(buf, 0, n)
                            leido += n
                            if (total > 0) progreso((leido.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            val real = md.digest().joinToString("") { "%02x".format(it) }
            if (real != esperado) throw DescargaInvalida("El archivo descargado no coincide con el publicado. No se instalará.")
            destino
        }.onFailure { destino.delete() }.recoverCatching { e ->
            throw if (e is DescargaInvalida) e else DescargaInvalida("No se pudo descargar la actualización.")
        }
    }

    /** Borra cualquier instalador que haya quedado de antes. */
    fun limpiar() {
        carpeta.listFiles()?.forEach { it.delete() }
    }
}
