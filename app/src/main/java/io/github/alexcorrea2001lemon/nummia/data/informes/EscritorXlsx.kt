package io.github.alexcorrea2001lemon.nummia.data.informes

import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Una celda del Excel que se genera. */
sealed interface Valor {
    data class Texto(val t: String, val negrita: Boolean = false) : Valor
    data class Euros(val centimos: Long, val negrita: Boolean = false) : Valor
    data class Fecha(val f: LocalDate) : Valor
    data class Numero(val n: Long) : Valor
    data object Vacio : Valor
}

data class HojaXlsx(val nombre: String, val filas: List<List<Valor>>, val anchos: List<Int> = emptyList())

/**
 * Escritor mínimo de .xlsx (sin bibliotecas: el APK no crece). Números como números, euros con formato
 * «1.234,56 €», fechas como fechas. Se abre en Excel, Google Sheets y LibreOffice.
 */
object EscritorXlsx {
    private const val DESFASE_SERIAL = 25569L

    fun escribir(hojas: List<HojaXlsx>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun poner(n: String, t: String) { z.putNextEntry(ZipEntry(n)); z.write(t.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            poner("[Content_Types].xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
                hojas.indices.forEach { append("""<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""") }
                append("</Types>")
            })
            poner("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            poner("xl/workbook.xml", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
                hojas.forEachIndexed { i, h -> append("""<sheet name="${xml(nombreHoja(h.nombre))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""") }
                append("</sheets></workbook>")
            })
            poner("xl/_rels/workbook.xml.rels", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                hojas.indices.forEach { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
                append("""<Relationship Id="rId${hojas.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("</Relationships>")
            })
            // Estilos: 0 normal · 1 negrita · 2 euros · 3 fecha · 4 euros en negrita
            poner("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="#,##0.00\ &quot;€&quot;"/><numFmt numFmtId="165" formatCode="dd/mm/yyyy"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="5"><xf/><xf fontId="1" applyFont="1"/><xf numFmtId="164" applyNumberFormat="1"/><xf numFmtId="165" applyNumberFormat="1"/><xf numFmtId="164" fontId="1" applyNumberFormat="1" applyFont="1"/></cellXfs></styleSheet>""")
            hojas.forEachIndexed { i, h -> poner("xl/worksheets/sheet${i + 1}.xml", hoja(h)) }
        }
        return out.toByteArray()
    }

    private fun hoja(h: HojaXlsx): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (h.anchos.isNotEmpty()) {
            append("<cols>")
            h.anchos.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        h.filas.forEachIndexed { f, fila ->
            append("""<row r="${f + 1}">""")
            fila.forEachIndexed { c, v ->
                val ref = columna(c) + (f + 1)
                when (v) {
                    is Valor.Texto -> append("""<c r="$ref" t="inlineStr"${if (v.negrita) " s=\"1\"" else ""}><is><t xml:space="preserve">${xml(v.t)}</t></is></c>""")
                    is Valor.Euros -> append("""<c r="$ref" s="${if (v.negrita) 4 else 2}"><v>${BigDecimal(v.centimos).movePointLeft(2).toPlainString()}</v></c>""")
                    is Valor.Fecha -> append("""<c r="$ref" s="3"><v>${v.f.toEpochDay() + DESFASE_SERIAL}</v></c>""")
                    is Valor.Numero -> append("""<c r="$ref"><v>${v.n}</v></c>""")
                    Valor.Vacio -> Unit
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    fun columna(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) { val r = (n - 1) % 26; sb.insert(0, 'A' + r); n = (n - 1) / 26 }
        return sb.toString()
    }

    /** Excel no admite algunos caracteres ni más de 31 letras en el nombre de una hoja. */
    private fun nombreHoja(s: String) = s.replace(Regex("""[\\/?*\[\]:]"""), " ").take(31)

    private fun xml(s: String) = buildString {
        s.forEach { ch ->
            when {
                ch == '&' -> append("&amp;"); ch == '<' -> append("&lt;"); ch == '>' -> append("&gt;"); ch == '"' -> append("&quot;")
                ch.code < 0x20 && ch != '\t' && ch != '\n' && ch != '\r' -> Unit // no válidos en XML
                else -> append(ch)
            }
        }
    }
}

/** El informe, en hojas de Excel. */
fun hojasDeInforme(inf: Informe): List<HojaXlsx> {
    val res = ArrayList<HojaXlsx>()
    fun t(s: String, n: Boolean = false) = Valor.Texto(s, n)
    fun e(c: Long, n: Boolean = false) = Valor.Euros(c, n)
    fun filasResumen(r: Resumen) = listOf(
        listOf(t("Ingresos"), e(r.ingresos)), listOf(t("Pagado"), e(r.pagado)), listOf(t("Reembolsos"), e(r.reembolsos)),
        listOf(t("Gasto real", true), e(r.gastoReal, true)), listOf(t("Balance", true), e(r.balance, true)),
        listOf(t("Ahorro"), t(r.ahorroPorMil?.let { "%.1f %%".format(it / 10.0).replace('.', ',') } ?: "—")),
    )
    if (Contenido.RESUMEN in inf.contenidos) {
        res += HojaXlsx("Resumen", listOf(listOf(t(inf.titulo, true)), listOf(t(inf.periodo)), emptyList<Valor>()) + filasResumen(inf.resumen), listOf(22, 16))
    }
    if (Contenido.MESES in inf.contenidos && inf.meses.isNotEmpty()) {
        res += HojaXlsx(
            "Mes a mes",
            listOf(listOf("Mes", "Ingresos", "Pagado", "Reembolsos", "Gasto real", "Balance").map { t(it, true) }) +
                inf.meses.map { (m, r) -> listOf(t(m), e(r.ingresos), e(r.pagado), e(r.reembolsos), e(r.gastoReal), e(r.balance)) } +
                listOf(listOf(t("Total", true), e(inf.resumen.ingresos, true), e(inf.resumen.pagado, true), e(inf.resumen.reembolsos, true), e(inf.resumen.gastoReal, true), e(inf.resumen.balance, true))),
            listOf(18, 14, 14, 14, 14, 14),
        )
    }
    if (Contenido.MOVIMIENTOS in inf.contenidos) {
        res += HojaXlsx(
            "Movimientos",
            listOf(listOf("Fecha", "Tipo", "Código", "Concepto", "Tienda / tipo", "Categoría", "Importe", "Devuelto", "Real").map { t(it, true) }) +
                inf.movimientos.map { m ->
                    listOf(Valor.Fecha(m.fecha), t(if (m.esGasto) "Gasto" else "Ingreso"), t(m.codigo), t(m.concepto), t(m.tienda), t(m.categoria), e(m.importe), e(m.devuelto), e(m.real))
                },
            listOf(12, 9, 9, 28, 20, 16, 12, 12, 12),
        )
    }
    fun grupos(nombre: String, g: List<io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto>) = HojaXlsx(
        nombre,
        listOf(listOf(nombre.removePrefix("Por ").replaceFirstChar { it.uppercase() }, "Gasto real", "Veces", "Media").map { t(it, true) }) +
            g.map { listOf(t(it.nombre), e(it.total), Valor.Numero(it.veces.toLong()), e(it.media)) },
        listOf(26, 14, 8, 14),
    )
    if (Contenido.TIENDAS in inf.contenidos) res += grupos("Por tienda", inf.tiendas)
    if (Contenido.CATEGORIAS in inf.contenidos) res += grupos("Por categoría", inf.categorias)
    if (Contenido.DEBEN in inf.contenidos) {
        res += HojaXlsx(
            "Lo que me deben",
            listOf(listOf("Persona", "Fecha", "Concepto", "Esperado", "Devuelto", "Falta").map { t(it, true) }) +
                inf.deben.map { d -> listOf(t(d.persona), Valor.Fecha(d.fecha), t(d.concepto), e(d.esperado), e(d.devuelto), e(d.falta)) } +
                listOf(listOf(t("Total", true), Valor.Vacio, Valor.Vacio, Valor.Vacio, Valor.Vacio, e(inf.totalDeben, true))),
            listOf(16, 12, 28, 12, 12, 12),
        )
    }
    return res.ifEmpty { listOf(HojaXlsx("Informe", listOf(listOf(t(inf.titulo, true)), listOf(t(inf.periodo))))) }
}
