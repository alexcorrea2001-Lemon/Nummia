package io.github.alexcorrea2001lemon.nummia.data.informes

import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** CSV «a la española»: separador «;», decimales con coma y BOM para que Excel lea bien las tildes. */
object Csv {
    private const val SEP = ';'
    /** Marca de orden de bytes: sin ella Excel abre el UTF-8 con las tildes rotas. */
    private val BOM = 0xFEFF.toChar()

    fun celda(s: String): String =
        if (s.any { it == SEP || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    /** Céntimos → «1520,75» (sin separador de miles: así las hojas de cálculo lo leen como número). */
    fun importe(c: Long): String {
        val abs = kotlin.math.abs(c)
        return (if (c < 0) "-" else "") + "${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
    }

    fun texto(filas: List<List<String>>): String =
        buildString {
            append(BOM)
            filas.forEach { f -> append(f.joinToString(SEP.toString(), transform = ::celda)).append("\r\n") }
        }
}

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val NOMBRES_TIPO = mapOf(
    TipoIngreso.SUELDO to "Sueldo", TipoIngreso.EXTRA to "Extra", TipoIngreso.VENTA to "Venta",
    TipoIngreso.TRABAJO_EXTRA to "Trabajo extra", TipoIngreso.OTRO to "Otro",
)

/** Todo lo que hay en Nummia en un .zip con un CSV por tipo (sin lo que está en la papelera). */
@Singleton
class ExportadorCsv @Inject constructor(private val db: NummiaDb) {
    suspend fun zip(): ByteArray {
        val gastos = db.gastos().volcar().filter { it.deletedAt == null }.sortedWith(compareBy({ it.fecha }, { it.createdAt }))
        val reembolsos = db.reembolsos().volcar().filter { it.deletedAt == null }
        val ingresos = db.ingresos().volcar().filter { it.deletedAt == null }.sortedWith(compareBy({ it.fecha }, { it.createdAt }))
        val categorias = db.categorias().volcar().associate { it.id to it.nombre }
        val personas = db.personas().volcar().associate { it.id to it.nombre }
        val pagos = db.pagos().volcar().filter { it.deletedAt == null }
        val cobros = db.ingresosFijos().volcar().filter { it.deletedAt == null }

        return withContext(Dispatchers.Default) {
            val vivos = gastos.map { it.id }.toSet()
            val devuelto = reembolsos.filter { it.gastoId in vivos }.groupBy { it.gastoId }.mapValues { (_, rs) -> rs.sumOf { it.importe } }
            val codigoGasto = gastos.associate { it.id to it.codigo }
            fun estado(e: Estado) = if (e == Estado.PREVISTO) "Previsto" else "Hecho"

            val archivos = linkedMapOf(
                "gastos.csv" to listOf(
                    listOf("Código", "Fecha", "Concepto", "Tienda", "Categoría", "Importe", "Devuelto", "Gasto real", "Me lo devuelven", "Persona", "Estado", "Notas"),
                ) + gastos.map { g ->
                    val dev = devuelto[g.id] ?: 0L
                    listOf(
                        g.codigo, FECHA.format(g.fecha), g.concepto, g.tienda, g.categoriaId?.let(categorias::get).orEmpty(),
                        Csv.importe(g.importe), Csv.importe(dev), Csv.importe(g.importe - dev),
                        if (g.meLoDevuelven) "Sí" else "No", g.personaId?.let(personas::get).orEmpty(), estado(g.estado), g.notas,
                    )
                },
                "ingresos.csv" to listOf(listOf("Código", "Fecha", "Concepto", "Tipo", "Importe", "Estado", "Notas")) +
                    ingresos.map { i -> listOf(i.codigo, FECHA.format(i.fecha), i.concepto, NOMBRES_TIPO.getValue(i.tipo), Csv.importe(i.importe), estado(i.estado), i.notas) },
                "devoluciones.csv" to listOf(listOf("Código", "Gasto", "Fecha", "Importe", "Persona", "Nota")) +
                    reembolsos.filter { it.gastoId in vivos }.sortedBy { it.fecha }.map { r ->
                        listOf(r.codigo, codigoGasto[r.gastoId].orEmpty(), r.fecha?.let(FECHA::format).orEmpty(), Csv.importe(r.importe), r.personaId?.let(personas::get).orEmpty(), r.nota)
                    },
                "pagos_y_cobros_fijos.csv" to listOf(listOf("Tipo", "Concepto", "Importe", "Día del mes", "Activo")) +
                    pagos.map { listOf("Pago", it.concepto, Csv.importe(it.importe), it.dia.toString(), if (it.activo) "Sí" else "No") } +
                    cobros.map { listOf("Cobro", it.concepto, Csv.importe(it.importe), it.dia.toString(), if (it.activo) "Sí" else "No") },
            )
            val salida = ByteArrayOutputStream()
            ZipOutputStream(salida).use { z ->
                archivos.forEach { (nombre, filas) ->
                    z.putNextEntry(ZipEntry(nombre))
                    z.write(Csv.texto(filas).toByteArray(Charsets.UTF_8))
                    z.closeEntry()
                }
            }
            salida.toByteArray()
        }
    }
}
