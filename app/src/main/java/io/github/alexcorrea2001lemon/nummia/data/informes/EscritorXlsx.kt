package io.github.alexcorrea2001lemon.nummia.data.informes

import io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Aspecto de una celda de texto. */
enum class Estilo(val indice: Int) { NORMAL(0), NEGRITA(1), CABECERA(5), TITULO(6), SUBTITULO(7), TOTAL(8) }

/** Una celda del Excel que se genera. */
sealed interface Valor {
    data class Texto(val t: String, val estilo: Estilo = Estilo.NORMAL) : Valor
    data class Euros(val centimos: Long, val total: Boolean = false) : Valor
    data class Fecha(val f: LocalDate) : Valor
    data class Numero(val n: Long, val total: Boolean = false) : Valor
    data class Vacio(val total: Boolean = false) : Valor
}

/** [cabecera] = número de fila (1, 2…) de la cabecera de la tabla: se fija al desplazarse y lleva filtros. */
data class HojaXlsx(val nombre: String, val filas: List<List<Valor>>, val anchos: List<Int> = emptyList(), val cabecera: Int? = null)

/**
 * Escritor mínimo de .xlsx (sin bibliotecas: el APK no crece). Cabeceras en el color de Nummia, filas
 * alternas, totales destacados, cabecera fija y filtros. Números como números, euros con formato
 * «1.234,56 €» y fechas como fechas. Se abre en Excel, Google Sheets y LibreOffice.
 */
object EscritorXlsx {
    private const val DESFASE_SERIAL = 25569L

    /** Índices de estilo (cellXfs). Los usa también la vista previa. */
    const val EUROS = 2
    const val FECHA = 3
    const val EUROS_NEGRITA = 4
    const val EUROS_TOTAL = 9
    const val NUMERO_TOTAL = 10
    const val EUROS_FRANJA = 11
    const val FECHA_FRANJA = 12
    const val TEXTO_FRANJA = 13
    const val NUMERO_FRANJA = 14
    val ESTILOS_EUROS = setOf(EUROS, EUROS_NEGRITA, EUROS_TOTAL, EUROS_FRANJA)
    val ESTILOS_FECHA = setOf(FECHA, FECHA_FRANJA)

    private val ESTILOS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
        """<numFmts count="2"><numFmt numFmtId="164" formatCode="#,##0.00\ &quot;€&quot;"/><numFmt numFmtId="165" formatCode="dd/mm/yyyy"/></numFmts>""" +
        """<fonts count="5"><font><sz val="11"/><color rgb="FF1F2937"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FF1F2937"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font><font><b/><sz val="16"/><color rgb="FF5B3FC4"/><name val="Calibri"/></font>""" +
        """<font><sz val="10"/><color rgb="FF6B7280"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="4"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FF7C5CD6"/><bgColor indexed="64"/></patternFill></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFF3F0FF"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
        """<borders count="3"><border/><border><bottom style="thin"><color rgb="FFE5E7EB"/></bottom></border><border><top style="medium"><color rgb="FF7C5CD6"/></top></border></borders>""" +
        """<cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="15">""" +
        """<xf borderId="1" applyBorder="1"/>""" + // 0 normal
        """<xf fontId="1" borderId="1" applyFont="1" applyBorder="1"/>""" + // 1 negrita
        """<xf numFmtId="164" borderId="1" applyNumberFormat="1" applyBorder="1"/>""" + // 2 euros
        """<xf numFmtId="165" borderId="1" applyNumberFormat="1" applyBorder="1"><alignment horizontal="left"/></xf>""" + // 3 fecha
        """<xf numFmtId="164" fontId="1" borderId="1" applyNumberFormat="1" applyFont="1" applyBorder="1"/>""" + // 4 euros negrita
        """<xf fontId="2" fillId="2" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center"/></xf>""" + // 5 cabecera
        """<xf fontId="3" applyFont="1"/>""" + // 6 título
        """<xf fontId="4" applyFont="1"/>""" + // 7 subtítulo
        """<xf fontId="1" fillId="3" borderId="2" applyFont="1" applyFill="1" applyBorder="1"/>""" + // 8 total texto
        """<xf numFmtId="164" fontId="1" fillId="3" borderId="2" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>""" + // 9 total euros
        """<xf fontId="1" fillId="3" borderId="2" applyFont="1" applyFill="1" applyBorder="1"/>""" + // 10 total número
        """<xf numFmtId="164" fillId="3" borderId="1" applyNumberFormat="1" applyFill="1" applyBorder="1"/>""" + // 11 euros franja
        """<xf numFmtId="165" fillId="3" borderId="1" applyNumberFormat="1" applyFill="1" applyBorder="1"><alignment horizontal="left"/></xf>""" + // 12 fecha franja
        """<xf fillId="3" borderId="1" applyFill="1" applyBorder="1"/>""" + // 13 texto franja
        """<xf fillId="3" borderId="1" applyFill="1" applyBorder="1"/>""" + // 14 número franja
        """</cellXfs></styleSheet>"""

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
                append("</sheets>")
                val filtros = hojas.withIndex().filter { (_, h) -> h.cabecera != null && h.filas.size > h.cabecera }
                if (filtros.isNotEmpty()) {
                    append("<definedNames>")
                    filtros.forEach { (i, h) ->
                        val ancho = h.filas[h.cabecera!! - 1].size
                        append("""<definedName name="_xlnm._FilterDatabase" localSheetId="$i" hidden="1">'${xml(nombreHoja(h.nombre).replace("'", "''"))}'!${'$'}A${'$'}${h.cabecera}:${'$'}${columna(ancho - 1)}${'$'}${ultimaFilaDatos(h)}</definedName>""")
                    }
                    append("</definedNames>")
                }
                append("</workbook>")
            })
            poner("xl/_rels/workbook.xml.rels", buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                hojas.indices.forEach { append("""<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""") }
                append("""<Relationship Id="rId${hojas.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                append("</Relationships>")
            })
            poner("xl/styles.xml", ESTILOS)
            hojas.forEachIndexed { i, h -> poner("xl/worksheets/sheet${i + 1}.xml", hoja(h)) }
        }
        return out.toByteArray()
    }

    /** Última fila de la tabla sin contar la de totales. */
    private fun ultimaFilaDatos(h: HojaXlsx): Int {
        val ultima = h.filas.size
        val esTotal = h.filas.last().any { (it is Valor.Texto && it.estilo == Estilo.TOTAL) || (it is Valor.Euros && it.total) }
        return if (esTotal && ultima - 1 > (h.cabecera ?: 0)) ultima - 1 else ultima
    }

    private fun hoja(h: HojaXlsx): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetViews><sheetView workbookViewId="0" showGridLines="0">""")
        h.cabecera?.let { append("""<pane ySplit="$it" topLeftCell="A${it + 1}" activePane="bottomLeft" state="frozen"/>""") }
        append("</sheetView></sheetViews>")
        if (h.anchos.isNotEmpty()) {
            append("<cols>")
            h.anchos.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        h.filas.forEachIndexed { f, fila ->
            val n = f + 1
            val franja = h.cabecera != null && n > h.cabecera && (n - h.cabecera) % 2 == 0
            val alto = when {
                n == h.cabecera -> """ ht="22" customHeight="1""""
                fila.any { it is Valor.Texto && it.estilo == Estilo.TITULO } -> """ ht="26" customHeight="1""""
                else -> ""
            }
            append("""<row r="$n"$alto>""")
            fila.forEachIndexed { c, v ->
                val ref = columna(c) + n
                when (v) {
                    is Valor.Texto -> {
                        val s = if (franja && v.estilo == Estilo.NORMAL) TEXTO_FRANJA else v.estilo.indice
                        append("""<c r="$ref" t="inlineStr"${if (s != 0) " s=\"$s\"" else ""}><is><t xml:space="preserve">${xml(v.t)}</t></is></c>""")
                    }
                    is Valor.Euros -> append("""<c r="$ref" s="${if (v.total) EUROS_TOTAL else if (franja) EUROS_FRANJA else EUROS}"><v>${BigDecimal(v.centimos).movePointLeft(2).toPlainString()}</v></c>""")
                    is Valor.Fecha -> append("""<c r="$ref" s="${if (franja) FECHA_FRANJA else FECHA}"><v>${v.f.toEpochDay() + DESFASE_SERIAL}</v></c>""")
                    is Valor.Numero -> append("""<c r="$ref" s="${if (v.total) NUMERO_TOTAL else if (franja) NUMERO_FRANJA else 0}"><v>${v.n}</v></c>""")
                    is Valor.Vacio -> if (v.total) append("""<c r="$ref" s="${Estilo.TOTAL.indice}"/>""") else if (franja) append("""<c r="$ref" s="$TEXTO_FRANJA"/>""")
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        if (h.cabecera != null && h.filas.size > h.cabecera) {
            append("""<autoFilter ref="A${h.cabecera}:${columna(h.filas[h.cabecera - 1].size - 1)}${ultimaFilaDatos(h)}"/>""")
        }
        append("""<pageMargins left="0.5" right="0.5" top="0.6" bottom="0.6" header="0.3" footer="0.3"/><pageSetup orientation="landscape" fitToWidth="1" fitToHeight="0"/>""")
        append("</worksheet>")
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

/** El informe, en hojas de Excel: título, periodo y una tabla con cabecera de color en cada hoja. */
fun hojasDeInforme(inf: Informe): List<HojaXlsx> {
    val res = ArrayList<HojaXlsx>()
    fun t(s: String, e: Estilo = Estilo.NORMAL) = Valor.Texto(s, e)
    fun e(c: Long, total: Boolean = false) = Valor.Euros(c, total)
    fun cab(vararg s: String) = s.map { t(it, Estilo.CABECERA) }
    val encabezado = listOf(listOf(t(inf.titulo, Estilo.TITULO)), listOf(t(inf.periodo, Estilo.SUBTITULO)), emptyList<Valor>())
    val primeraFila = encabezado.size + 1 // fila de la cabecera de la tabla

    if (Contenido.RESUMEN in inf.contenidos) {
        val r = inf.resumen
        res += HojaXlsx(
            "Resumen",
            encabezado + listOf(
                cab("Concepto", "Importe"),
                listOf(t("Ingresos"), e(r.ingresos)), listOf(t("Pagado"), e(r.pagado)), listOf(t("Devoluciones"), e(r.reembolsos)),
                listOf(t("Gasto real", Estilo.NEGRITA), Valor.Euros(r.gastoReal)),
                listOf(t("Balance", Estilo.TOTAL), e(r.balance, true)),
                emptyList(),
                listOf(t("Ahorro sobre ingresos", Estilo.SUBTITULO), t(r.ahorroPorMil?.let { "%.1f %%".format(it / 10.0).replace('.', ',') } ?: "—", Estilo.NEGRITA)),
            ),
            listOf(26, 18),
        )
    }
    if (Contenido.MESES in inf.contenidos && inf.meses.isNotEmpty()) {
        res += HojaXlsx(
            "Mes a mes",
            encabezado + listOf(cab("Mes", "Ingresos", "Pagado", "Devoluciones", "Gasto real", "Balance")) +
                inf.meses.map { (m, r: Resumen) -> listOf(t(m), e(r.ingresos), e(r.pagado), e(r.reembolsos), e(r.gastoReal), e(r.balance)) } +
                listOf(listOf(t("Total", Estilo.TOTAL), e(inf.resumen.ingresos, true), e(inf.resumen.pagado, true), e(inf.resumen.reembolsos, true), e(inf.resumen.gastoReal, true), e(inf.resumen.balance, true))),
            listOf(20, 15, 15, 15, 15, 15), primeraFila,
        )
    }
    if (Contenido.MOVIMIENTOS in inf.contenidos) {
        res += HojaXlsx(
            "Movimientos",
            encabezado + listOf(cab("Fecha", "Tipo", "Código", "Concepto", "Tienda / tipo", "Categoría", "Importe", "Devuelto", "Real")) +
                inf.movimientos.map { m ->
                    listOf(Valor.Fecha(m.fecha), t(if (m.esGasto) "Gasto" else "Ingreso"), t(m.codigo), t(m.concepto), t(m.tienda), t(m.categoria), e(m.importe), e(m.devuelto), e(m.real))
                },
            listOf(12, 9, 9, 30, 22, 16, 13, 13, 13), primeraFila,
        )
    }
    fun grupos(nombre: String, g: List<GrupoGasto>) = HojaXlsx(
        nombre,
        encabezado + listOf(cab(if (nombre == "Por tienda") "Tienda" else "Categoría", "Gasto real", "Veces", "Media")) +
            g.map { listOf(t(it.nombre), e(it.total), Valor.Numero(it.veces.toLong()), e(it.media)) } +
            listOf(listOf(t("Total", Estilo.TOTAL), e(g.sumOf { it.total }, true), Valor.Numero(g.sumOf { it.veces }.toLong(), true), Valor.Vacio(true))),
        listOf(28, 15, 9, 15), primeraFila,
    )
    if (Contenido.TIENDAS in inf.contenidos) res += grupos("Por tienda", inf.tiendas)
    if (Contenido.CATEGORIAS in inf.contenidos) res += grupos("Por categoría", inf.categorias)
    if (Contenido.DEBEN in inf.contenidos) {
        res += HojaXlsx(
            "Lo que me deben",
            encabezado + listOf(cab("Persona", "Fecha", "Concepto", "Esperado", "Devuelto", "Falta")) +
                inf.deben.map { d -> listOf(t(d.persona), Valor.Fecha(d.fecha), t(d.concepto), e(d.esperado), e(d.devuelto), e(d.falta)) } +
                listOf(listOf(t("Total", Estilo.TOTAL), Valor.Vacio(true), Valor.Vacio(true), Valor.Vacio(true), Valor.Vacio(true), e(inf.totalDeben, true))),
            listOf(18, 12, 30, 13, 13, 13), primeraFila,
        )
    }
    return res.ifEmpty { listOf(HojaXlsx("Informe", encabezado)) }
}
