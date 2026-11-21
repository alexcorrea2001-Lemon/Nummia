package io.github.alexcorrea2001lemon.nummia.data.importar

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/** Una celda: texto o número. [formula] = la celda tiene fórmula ([numero] es su último resultado guardado). */
data class Celda(val texto: String? = null, val numero: BigDecimal? = null, val formula: Boolean = false, val estilo: Int? = null)

class ExcelNoValido(motivo: String) : Exception(motivo)

/**
 * Lector mínimo de .xlsx (un .zip con XML) para la primera hoja, sin bibliotecas grandes:
 * así el APK no crece. Lee textos, números y el resultado guardado de las fórmulas.
 */
object LectorXlsx {
    private const val MAX_ENTRADA = 30L * 1024 * 1024 // protección contra archivos enormes o comprimidos de forma maliciosa

    /** Todas las hojas, en orden: nombre → celdas. */
    fun leerHojas(bytes: ByteArray): List<Pair<String, Map<String, Celda>>> {
        val archivos = descomprimir(bytes)
        val compartidas = archivos["xl/sharedStrings.xml"]?.let(::textosCompartidos).orEmpty()
        val hojas = ArrayList<Pair<String, String>>() // nombre, r:id
        analizar(archivos.getValue("xl/workbook.xml"), object : DefaultHandler() {
            override fun startElement(uri: String?, local: String?, q: String, a: Attributes) {
                if (q == "sheet") hojas += (a.getValue("name") ?: "Hoja") to (a.getValue("r:id") ?: "")
            }
        })
        val destinos = HashMap<String, String>()
        archivos["xl/_rels/workbook.xml.rels"]?.let { rels ->
            analizar(rels, object : DefaultHandler() {
                override fun startElement(uri: String?, local: String?, q: String, a: Attributes) {
                    if (q == "Relationship") destinos[a.getValue("Id")] = a.getValue("Target")
                }
            })
        }
        return hojas.mapNotNull { (nombre, rid) ->
            val ruta = destinos[rid]?.let { if (it.startsWith("/")) it.removePrefix("/") else "xl/$it" } ?: return@mapNotNull null
            archivos[ruta]?.let { nombre to celdas(it, compartidas) }
        }
    }

    fun leer(bytes: ByteArray): Map<String, Celda> {
        val archivos = descomprimir(bytes)
        val compartidas = archivos["xl/sharedStrings.xml"]?.let(::textosCompartidos).orEmpty()
        val hoja = rutaPrimeraHoja(archivos) ?: throw ExcelNoValido("El archivo no tiene ninguna hoja.")
        val xml = archivos[hoja] ?: throw ExcelNoValido("No se encuentra la hoja dentro del archivo.")
        return celdas(xml, compartidas)
    }

    private fun descomprimir(bytes: ByteArray): Map<String, ByteArray> {
        val res = HashMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory || !e.name.startsWith("xl/") || !e.name.endsWith(".xml") && !e.name.endsWith(".rels")) continue
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val n = zip.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_ENTRADA) throw ExcelNoValido("El archivo es demasiado grande.")
                        out.write(buf, 0, n)
                    }
                    res[e.name] = out.toByteArray()
                }
            }
        } catch (e: ExcelNoValido) {
            throw e
        } catch (e: Exception) {
            throw ExcelNoValido("No es un archivo .xlsx válido.")
        }
        if ("xl/workbook.xml" !in res) throw ExcelNoValido("No es un archivo .xlsx válido.")
        return res
    }

    private fun analizar(xml: ByteArray, manejador: DefaultHandler) {
        val f = SAXParserFactory.newInstance().apply { isNamespaceAware = false }
        // Sin entidades externas (seguridad).
        listOf(
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
        ).forEach { (k, v) -> runCatching { f.setFeature(k, v) } }
        try {
            f.newSAXParser().parse(InputSource(ByteArrayInputStream(xml)), manejador)
        } catch (e: Exception) {
            throw ExcelNoValido("El archivo está dañado.")
        }
    }

    private fun textosCompartidos(xml: ByteArray): List<String> {
        val lista = ArrayList<String>()
        analizar(xml, object : DefaultHandler() {
            val actual = StringBuilder()
            var enSi = false
            var enT = false
            var enRph = false
            override fun startElement(uri: String?, local: String?, q: String, a: Attributes?) {
                when (q) { "si" -> { enSi = true; actual.setLength(0) }; "t" -> enT = enSi && !enRph; "rPh" -> enRph = true }
            }
            override fun endElement(uri: String?, local: String?, q: String) {
                when (q) { "si" -> { enSi = false; lista += actual.toString() }; "t" -> enT = false; "rPh" -> enRph = false }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) { if (enT) actual.append(ch, start, length) }
        })
        return lista
    }

    private fun rutaPrimeraHoja(archivos: Map<String, ByteArray>): String? {
        var rid: String? = null
        analizar(archivos.getValue("xl/workbook.xml"), object : DefaultHandler() {
            override fun startElement(uri: String?, local: String?, q: String, a: Attributes) {
                if (q == "sheet" && rid == null) rid = a.getValue("r:id")
            }
        })
        var destino: String? = null
        archivos["xl/_rels/workbook.xml.rels"]?.let { rels ->
            analizar(rels, object : DefaultHandler() {
                override fun startElement(uri: String?, local: String?, q: String, a: Attributes) {
                    if (q == "Relationship" && a.getValue("Id") == rid) destino = a.getValue("Target")
                }
            })
        }
        val ruta = destino?.let { if (it.startsWith("/")) it.removePrefix("/") else "xl/$it" }
        return ruta?.takeIf { it in archivos } ?: "xl/worksheets/sheet1.xml".takeIf { it in archivos }
    }

    private fun celdas(xml: ByteArray, compartidas: List<String>): Map<String, Celda> {
        val res = HashMap<String, Celda>()
        analizar(xml, object : DefaultHandler() {
            var ref: String? = null
            var tipo: String? = null
            var estilo: Int? = null
            var formula = false
            val valor = StringBuilder()
            val enLinea = StringBuilder()
            var enV = false
            var enT = false
            override fun startElement(uri: String?, local: String?, q: String, a: Attributes) {
                when (q) {
                    "c" -> { ref = a.getValue("r"); tipo = a.getValue("t"); estilo = a.getValue("s")?.toIntOrNull(); formula = false; valor.setLength(0); enLinea.setLength(0) }
                    "f" -> formula = true
                    "v" -> enV = true
                    "t" -> enT = true
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (enV) valor.append(ch, start, length) else if (enT) enLinea.append(ch, start, length)
            }
            override fun endElement(uri: String?, local: String?, q: String) {
                when (q) {
                    "v" -> enV = false
                    "t" -> enT = false
                    "c" -> {
                        val r = ref ?: return
                        val v = valor.toString()
                        val celda = when (tipo) {
                            "s" -> Celda(texto = v.trim().toIntOrNull()?.let(compartidas::getOrNull), formula = formula)
                            "inlineStr" -> Celda(texto = enLinea.toString(), formula = formula)
                            "str" -> Celda(texto = v, formula = formula)
                            "b", "e" -> null
                            else -> v.trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()?.let { Celda(numero = it, formula = formula) }
                        }
                        if (celda != null) res[r.uppercase()] = celda.copy(estilo = estilo)
                        else if (estilo != null) res[r.uppercase()] = Celda(estilo = estilo) // celda vacía con formato (p. ej. fila de totales)
                    }
                }
            }
        })
        return res
    }
}
