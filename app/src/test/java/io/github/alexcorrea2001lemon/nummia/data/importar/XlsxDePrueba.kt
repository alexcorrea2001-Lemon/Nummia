package io.github.alexcorrea2001lemon.nummia.data.importar

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Crea un .xlsx mínimo en memoria con datos inventados: celda → texto (String), número (Double/Int) o fórmula ("=…" con resultado). */
object XlsxDePrueba {
    data class Formula(val f: String, val resultado: Double)

    fun crear(celdas: Map<String, Any>): ByteArray {
        val textos = celdas.values.filterIsInstance<String>().distinct()
        val filas = celdas.entries.groupBy { Regex("\\d+").find(it.key)!!.value.toInt() }.toSortedMap()
        val hoja = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            filas.forEach { (n, cs) ->
                append("<row r=\"$n\">")
                cs.forEach { (ref, v) ->
                    when (v) {
                        is String -> append("<c r=\"$ref\" t=\"s\"><v>${textos.indexOf(v)}</v></c>")
                        is Formula -> append("<c r=\"$ref\"><f>${v.f}</f><v>${v.resultado}</v></c>")
                        else -> append("<c r=\"$ref\"><v>$v</v></c>")
                    }
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        val compartidas = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${textos.size}">""")
            textos.forEach { append("<si><t>${it.replace("&", "&amp;").replace("<", "&lt;")}</t></si>") }
            append("</sst>")
        }
        val libro = """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Hoja 1" sheetId="1" r:id="rId1"/></sheets></workbook>"""
        val rels = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/hoja1.xml"/></Relationships>"""
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun poner(n: String, t: String) { z.putNextEntry(ZipEntry(n)); z.write(t.toByteArray()); z.closeEntry() }
            poner("xl/workbook.xml", libro)
            poner("xl/_rels/workbook.xml.rels", rels)
            poner("xl/sharedStrings.xml", compartidas)
            poner("xl/worksheets/hoja1.xml", hoja)
        }
        return out.toByteArray()
    }

    /** Excel inventado con la misma estructura que el real. */
    fun ejemplo(): Map<String, Any> = mapOf(
        "D2" to Formula("SUM(D9:D23)-SUM(E9:E23)", 47.55), "D3" to 1500.0,
        "C8" to "Gasto", "D8" to "Precio", "E8" to "Reembolso",
        "C9" to "Cafetería(08/01)", "D9" to 2.75,
        "C10" to "Librería(12/01)", "D10" to 51.8, "E10" to 10.0,
        "C11" to "cafeteria(20/01)", "D11" to 3,
        "G9" to "Panadería(03/02)", "H9" to 1.2,
        "H3" to 1500,
        "C38" to "Cine(05/07)", "D38" to 8.5,
        "C56" to "Ganancias extraordinarias", "C57" to "Regalo", "D57" to 50,
        "H56" to 99, "I60" to Formula("SUM(I56:I59)", 99.0),
    )
}
