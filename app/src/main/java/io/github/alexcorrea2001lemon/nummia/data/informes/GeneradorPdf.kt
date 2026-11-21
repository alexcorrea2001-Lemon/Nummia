package io.github.alexcorrea2001lemon.nummia.data.informes

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearPorMil
import java.io.ByteArrayOutputStream

/**
 * PDF con el PdfDocument de Android (sin bibliotecas: el APK no crece).
 * A4, márgenes de 40 pt, tablas que siguen en la página siguiente repitiendo la cabecera.
 */
class GeneradorPdf(private val acento: Int = 0xFF7C5CD6.toInt()) {
    private val ancho = 595
    private val alto = 842
    private val margen = 40f
    private val util = ancho - 2 * margen

    private val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = 0xFF1F2937.toInt() }
    private val suave = Paint(normal).apply { color = 0xFF6B7280.toInt(); textSize = 8.5f }
    private val negrita = Paint(normal).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val titulo = Paint(negrita).apply { textSize = 20f; color = 0xFF111827.toInt() }
    private val seccion = Paint(negrita).apply { textSize = 12.5f; color = acento }
    private val cifra = Paint(negrita).apply { textSize = 15f; color = 0xFF111827.toInt() }
    private val linea = Paint().apply { color = 0xFFE5E7EB.toInt(); strokeWidth = 0.7f }
    private val franja = Paint().apply { color = 0xFFF5F3FF.toInt() }

    private lateinit var doc: PdfDocument
    private var pagina: PdfDocument.Page? = null
    private lateinit var c: Canvas
    private var y = 0f
    private var numero = 0
    private var pie = ""

    fun generar(inf: Informe, generado: String): ByteArray {
        doc = PdfDocument()
        pie = "Nummia · ${inf.titulo} · $generado"
        numero = 0
        nuevaPagina()
        c.drawText(inf.titulo, margen, y + 18, titulo)
        y += 26
        c.drawText(inf.periodo, margen, y + 10, suave)
        y += 24

        if (Contenido.RESUMEN in inf.contenidos) resumen(inf)
        if (Contenido.MESES in inf.contenidos && inf.meses.isNotEmpty()) {
            tabla(
                "Mes a mes", listOf("Mes", "Ingresos", "Pagado", "Reembolsos", "Gasto real", "Balance"), listOf(0.24f, 0.152f, 0.152f, 0.152f, 0.152f, 0.152f),
                inf.meses.map { (m, r) -> listOf(m, e(r.ingresos), e(r.pagado), e(r.reembolsos), e(r.gastoReal), e(r.balance)) },
                total = listOf("Total", e(inf.resumen.ingresos), e(inf.resumen.pagado), e(inf.resumen.reembolsos), e(inf.resumen.gastoReal), e(inf.resumen.balance)),
            )
        }
        if (Contenido.DEBEN in inf.contenidos) {
            tabla(
                "Lo que me deben", listOf("Persona", "Fecha", "Concepto", "Esperado", "Devuelto", "Falta"), listOf(0.17f, 0.13f, 0.3f, 0.135f, 0.13f, 0.135f),
                inf.deben.map { listOf(it.persona, fechaCorta(it.fecha), it.concepto, e(it.esperado), e(it.devuelto), e(it.falta)) },
                total = listOf("Total", "", "", "", "", e(inf.totalDeben)), vacio = "Nadie te debe nada.",
            )
        }
        fun grupos(t: String, g: List<io.github.alexcorrea2001lemon.nummia.domain.calculo.GrupoGasto>) = tabla(
            t, listOf(if (t == "Por tienda") "Tienda" else "Categoría", "Gasto real", "Veces", "Media"), listOf(0.46f, 0.2f, 0.14f, 0.2f),
            g.map { listOf(it.nombre, e(it.total), it.veces.toString(), e(it.media)) }, vacio = "Sin gastos en este periodo.",
        )
        if (Contenido.TIENDAS in inf.contenidos) grupos("Por tienda", inf.tiendas)
        if (Contenido.CATEGORIAS in inf.contenidos) grupos("Por categoría", inf.categorias)
        if (Contenido.MOVIMIENTOS in inf.contenidos) {
            tabla(
                "Movimientos", listOf("Fecha", "Código", "Concepto", "Tienda / tipo", "Importe", "Real"), listOf(0.12f, 0.1f, 0.3f, 0.2f, 0.14f, 0.14f),
                inf.movimientos.map { m ->
                    listOf(fechaCorta(m.fecha), m.codigo, m.concepto, m.tienda, (if (m.esGasto) "−" else "+") + e(m.importe), e(m.real))
                },
                vacio = "Sin movimientos en este periodo.",
            )
        }
        cerrarPagina()
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun e(c: Long) = formatearEuros(c)

    private fun nuevaPagina() {
        cerrarPagina()
        numero++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
        pagina = p
        c = p.canvas
        y = margen
    }

    private fun cerrarPagina() {
        val p = pagina ?: return
        c.drawLine(margen, alto - 30f, ancho - margen, alto - 30f, linea)
        c.drawText(pie, margen, alto - 18f, suave)
        val n = "Página $numero"
        c.drawText(n, ancho - margen - suave.measureText(n), alto - 18f, suave)
        doc.finishPage(p)
        pagina = null
    }

    private fun asegurar(necesario: Float): Boolean {
        if (y + necesario > alto - 50f) { nuevaPagina(); return true }
        return false
    }

    private fun resumen(inf: Informe) {
        asegurar(110f)
        c.drawText("Resumen", margen, y + 12, seccion)
        y += 22
        val r = inf.resumen
        val datos = listOf(
            "Ingresos" to e(r.ingresos), "Pagado" to e(r.pagado), "Reembolsos" to e(r.reembolsos),
            "Gasto real" to e(r.gastoReal), "Balance" to e(r.balance), "Ahorro" to formatearPorMil(r.ahorroPorMil),
        )
        val w = util / 3
        datos.forEachIndexed { i, (k, v) ->
            val x = margen + (i % 3) * w
            val yy = y + (i / 3) * 40
            c.drawText(k, x, yy + 10, suave)
            c.drawText(v, x, yy + 28, cifra)
        }
        y += 90
    }

    /** Tabla: la primera columna a la izquierda y el resto a la derecha si son cifras. */
    private fun tabla(
        nombre: String, cabecera: List<String>, anchos: List<Float>, filas: List<List<String>>,
        total: List<String>? = null, vacio: String = "",
    ) {
        val altoFila = 16f
        asegurar(22f + altoFila * 3)
        y += 6
        c.drawText(nombre, margen, y + 12, seccion)
        y += 22
        val xs = anchos.runningFold(margen) { acc, f -> acc + f * util }
        val derecha = cabecera.indices.map { i -> i > 0 && filas.firstOrNull()?.getOrNull(i)?.let { it.endsWith("€") || it.all(Char::isDigit) } == true }
        fun fila(celdas: List<String>, p: Paint, fondo: Boolean) {
            if (fondo) c.drawRect(margen, y, margen + util, y + altoFila, franja)
            celdas.forEachIndexed { i, t ->
                val w = xs[i + 1] - xs[i] - 6
                val texto = recortar(t, p, w)
                val x = if (derecha.getOrElse(i) { false }) xs[i + 1] - 4 - p.measureText(texto) else xs[i] + 2
                c.drawText(texto, x, y + 11.5f, p)
            }
            y += altoFila
        }
        fun cabeceraTabla() { fila(cabecera, negrita, false); c.drawLine(margen, y, margen + util, y, linea) }
        cabeceraTabla()
        if (filas.isEmpty()) {
            c.drawText(vacio, margen + 2, y + 12, suave)
            y += altoFila + 8
            return
        }
        filas.forEachIndexed { n, f ->
            if (asegurar(altoFila)) { c.drawText("$nombre (sigue)", margen, y + 12, seccion); y += 22; cabeceraTabla() }
            fila(f, normal, n % 2 == 1)
        }
        if (total != null) {
            asegurar(altoFila)
            c.drawLine(margen, y, margen + util, y, linea)
            fila(total, negrita, false)
        }
        y += 12
    }

    private fun recortar(t: String, p: Paint, w: Float): String {
        if (p.measureText(t) <= w) return t
        val n = p.breakText(t, true, w - p.measureText("…"), null)
        return t.take(n.coerceAtLeast(0)) + "…"
    }
}
