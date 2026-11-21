package io.github.alexcorrea2001lemon.nummia.data.importar

import androidx.room.withTransaction
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.Origen
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.AvisoCambios
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import java.util.UUID
import javax.inject.Inject

data class ResultadoImportacion(val gastos: Int, val reembolsos: Int, val ingresos: Int)

/** Guarda lo leído del Excel. [pagadosPorOtros]: índice del gasto → persona que lo devuelve. Todo o nada (una transacción). */
class ImportadorExcel @Inject constructor(
    private val db: NummiaDb,
    private val reloj: Reloj,
    private val aviso: AvisoCambios,
) {
    suspend fun gastosEnElAnio(anio: Int): Int = db.gastos().volcar().count { it.deletedAt == null && it.fecha.year == anio }

    suspend fun importar(a: AnalisisExcel, pagadosPorOtros: Map<Int, String?>): ResultadoImportacion {
        val r = db.withTransaction {
            val ahora = reloj.ahoraMs()
            var g = db.gastos().maxCodigo() ?: 0
            var rr = db.reembolsos().maxCodigo() ?: 0
            var i = db.ingresos().maxCodigo() ?: 0
            fun cod(p: String, n: Int) = "$p-${n.toString().padStart(4, '0')}"
            val historial = ArrayList<HistorialEntity>()
            val categorias = HashMap<String, String?>()
            var nReembolsos = 0
            a.gastos.forEachIndexed { idx, x ->
                val id = UUID.randomUUID().toString()
                val otro = idx in pagadosPorOtros
                val categoria = categorias.getOrPut(x.nombre) { db.gastos().categoriaHabitual(x.nombre) }
                val codigo = cod("G", ++g)
                db.gastos().guardar(
                    GastoEntity(
                        id = id, codigo = codigo, fecha = x.fecha, concepto = x.nombre, tienda = x.nombre, categoriaId = categoria,
                        importe = x.importe, meLoDevuelven = otro, personaId = if (otro) pagadosPorOtros[idx] else null,
                        importeEsperado = if (otro) x.importe else null, notas = "", estado = Estado.CONFIRMADO,
                        createdAt = ahora, updatedAt = ahora, deletedAt = null,
                    ),
                )
                historial += HistorialEntity(0, "gasto", id, "importado", null, codigo, Origen.IMPORTACION, ahora)
                if (x.reembolso != null) {
                    db.reembolsos().guardar(
                        ReembolsoEntity(
                            id = UUID.randomUUID().toString(), codigo = cod("R", ++rr), gastoId = id, fecha = null, importe = x.reembolso,
                            personaId = if (otro) pagadosPorOtros[idx] else null, nota = "Importado del Excel",
                            createdAt = ahora, updatedAt = ahora, deletedAt = null,
                        ),
                    )
                    nReembolsos++
                }
            }
            a.ingresos.forEach { x ->
                val id = UUID.randomUUID().toString()
                val codigo = cod("I", ++i)
                db.ingresos().guardar(
                    IngresoEntity(
                        id = id, codigo = codigo, fecha = x.fecha, concepto = x.concepto,
                        tipo = if (x.esSueldo) TipoIngreso.SUELDO else TipoIngreso.EXTRA, importe = x.importe,
                        notas = if (x.fechaAproximada) "fecha aproximada (importado)" else "", estado = Estado.CONFIRMADO,
                        createdAt = ahora, updatedAt = ahora, deletedAt = null,
                    ),
                )
                historial += HistorialEntity(0, "ingreso", id, "importado", null, codigo, Origen.IMPORTACION, ahora)
            }
            db.historial().insertar(historial)
            ResultadoImportacion(a.gastos.size, nReembolsos, a.ingresos.size)
        }
        aviso.cambio()
        return r
    }
}
