package io.github.alexcorrea2001lemon.nummia.data.drive

import androidx.room.withTransaction
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.Origen
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import java.time.LocalDate
import java.util.UUID

/** Nombres de los tipos de ingreso tal y como aparecen en la hoja. */
val TIPOS_EN_HOJA = mapOf(
    TipoIngreso.SUELDO to "Sueldo", TipoIngreso.EXTRA to "Extra", TipoIngreso.VENTA to "Venta",
    TipoIngreso.TRABAJO_EXTRA to "Trabajo extra", TipoIngreso.OTRO to "Otro",
)

/**
 * Lado «móvil» de la sincronización: convierte los datos de Room a la forma canónica de la hoja
 * y aplica en Room lo que llega de la hoja (con origen ORDENADOR en el historial).
 * Solo se sincroniza lo CONFIRMADO; lo previsto se queda en el móvil.
 */
class DatosLocales(private val db: NummiaDb, private val reloj: Reloj) {

    private class Contexto(
        val gastos: List<GastoEntity>, val reembolsos: List<ReembolsoEntity>, val ingresos: List<IngresoEntity>,
        val personas: List<PersonaEntity>, val categorias: List<CategoriaEntity>,
    ) {
        val persona = personas.associate { it.id to it.nombre }
        val categoria = categorias.associate { it.id to it.nombre }
        val gastoPorId = gastos.associateBy { it.id }
    }

    private suspend fun contexto() = Contexto(
        db.gastos().volcar(), db.reembolsos().volcar(), db.ingresos().volcar(), db.personas().volcar(), db.categorias().volcar(),
    )

    private fun importe(c: Long?) = c?.toString().orEmpty()

    /** Años con algo que sincronizar. */
    suspend fun anios(): Set<Int> {
        val c = contexto()
        return (c.gastos.filter { it.deletedAt == null && it.estado == Estado.CONFIRMADO }.map { it.fecha.year } +
            c.ingresos.filter { it.deletedAt == null && it.estado == Estado.CONFIRMADO }.map { it.fecha.year }).toSet()
    }

    suspend fun vacia(): Boolean = db.gastos().volcar().none { it.deletedAt == null } && db.ingresos().volcar().none { it.deletedAt == null }

    suspend fun canon(p: Pestana, anio: Int): Map<String, Canon> {
        val c = contexto()
        return when (p) {
            Pestana.GASTOS -> c.gastos.filter { it.deletedAt == null && it.estado == Estado.CONFIRMADO && it.fecha.year == anio }
                .associate { g ->
                    g.id to listOf(
                        g.codigo, g.fecha.toEpochDay().toString(), g.tienda, g.concepto, g.importe.toString(),
                        g.categoriaId?.let(c.categoria::get).orEmpty(), if (g.meLoDevuelven) "Sí" else "No",
                        g.personaId?.let(c.persona::get).orEmpty(), importe(g.importeEsperado), g.notas,
                    )
                }
            Pestana.REEMBOLSOS -> c.reembolsos.filter { r ->
                val g = c.gastoPorId[r.gastoId]
                r.deletedAt == null && g != null && g.deletedAt == null && g.estado == Estado.CONFIRMADO && g.fecha.year == anio
            }.associate { r ->
                r.id to listOf(
                    r.codigo, c.gastoPorId.getValue(r.gastoId).codigo, r.fecha?.toEpochDay()?.toString().orEmpty(), r.importe.toString(),
                    r.personaId?.let(c.persona::get).orEmpty(), r.nota,
                )
            }
            Pestana.INGRESOS -> c.ingresos.filter { it.deletedAt == null && it.estado == Estado.CONFIRMADO && it.fecha.year == anio }
                .associate { i ->
                    i.id to listOf(i.codigo, i.fecha.toEpochDay().toString(), i.concepto, TIPOS_EN_HOJA.getValue(i.tipo), i.importe.toString(), i.notas)
                }
        }
    }

    // ---------------- Aplicar lo que llega de la hoja

    private suspend fun personaPorNombre(nombre: String): String? {
        if (nombre.isBlank()) return null
        db.personas().volcar().firstOrNull { it.deletedAt == null && claveTexto(it.nombre) == claveTexto(nombre) }?.let { return it.id }
        val p = PersonaEntity(UUID.randomUUID().toString(), nombre.trim(), reloj.ahoraMs())
        db.personas().guardar(p)
        return p.id
    }

    private suspend fun categoriaPorNombre(nombre: String): String? {
        if (nombre.isBlank()) return null
        val todas = db.categorias().volcar()
        todas.firstOrNull { claveTexto(it.nombre) == claveTexto(nombre) }?.let { return it.id }
        val nueva = CategoriaEntity(UUID.randomUUID().toString(), nombre.trim(), "more", 0xFF9BAAA2, false, (todas.maxOfOrNull { it.orden } ?: 0) + 1)
        db.categorias().guardar(nueva)
        return nueva.id
    }

    private suspend fun codigoLibre(prefijo: String, propuesto: String, id: String): String {
        val usados = when (prefijo) {
            "G" -> db.gastos().volcar().filter { it.id != id }.map { it.codigo }
            "R" -> db.reembolsos().volcar().filter { it.id != id }.map { it.codigo }
            else -> db.ingresos().volcar().filter { it.id != id }.map { it.codigo }
        }.toSet()
        if (propuesto.isNotBlank() && propuesto !in usados) return propuesto.trim()
        val max = when (prefijo) { "G" -> db.gastos().maxCodigo(); "R" -> db.reembolsos().maxCodigo(); else -> db.ingresos().maxCodigo() } ?: 0
        return "$prefijo-${(max + 1).toString().padStart(4, '0')}"
    }

    private suspend fun anotar(entidad: String, id: String, campo: String) {
        db.historial().insertar(listOf(HistorialEntity(0, entidad, id, campo, null, null, Origen.ORDENADOR, reloj.ahoraMs())))
    }

    /** Crea o actualiza en el móvil. Devuelve el canon final (el código puede cambiar si estaba repetido) o null si no se puede. */
    suspend fun aplicar(p: Pestana, id: String, c: Canon): Canon? = db.withTransaction {
        val ahora = reloj.ahoraMs()
        when (p) {
            Pestana.GASTOS -> {
                val previo = db.gastos().obtener(id)
                val codigo = codigoLibre("G", c[0], id)
                val meLo = c[6] == "Sí"
                db.gastos().guardar(
                    GastoEntity(
                        id = id, codigo = codigo, fecha = LocalDate.ofEpochDay(c[1].toLong()), concepto = c[3], tienda = c[2].ifBlank { c[3] },
                        categoriaId = categoriaPorNombre(c[5]), importe = c[4].toLong(), meLoDevuelven = meLo,
                        personaId = if (meLo) personaPorNombre(c[7]) else null, importeEsperado = if (meLo) c[8].toLongOrNull() else null,
                        notas = c[9], estado = Estado.CONFIRMADO, createdAt = previo?.createdAt ?: ahora, updatedAt = ahora, deletedAt = null,
                        pagoMensualId = previo?.pagoMensualId,
                    ),
                )
                anotar("gasto", id, if (previo == null) "creado" else "editado")
            }
            Pestana.REEMBOLSOS -> {
                val gasto = db.gastos().volcar().firstOrNull { it.codigo == c[1].trim() && it.deletedAt == null } ?: return@withTransaction null
                val previo = db.reembolsos().obtener(id)
                db.reembolsos().guardar(
                    ReembolsoEntity(
                        id = id, codigo = codigoLibre("R", c[0], id), gastoId = gasto.id, fecha = c[2].toLongOrNull()?.let(LocalDate::ofEpochDay),
                        importe = c[3].toLong(), personaId = personaPorNombre(c[4]), nota = c[5],
                        createdAt = previo?.createdAt ?: ahora, updatedAt = ahora, deletedAt = null,
                    ),
                )
                anotar("gasto", gasto.id, "reembolso")
            }
            Pestana.INGRESOS -> {
                val previo = db.ingresos().obtener(id)
                val tipo = TIPOS_EN_HOJA.entries.firstOrNull { claveTexto(it.value) == claveTexto(c[3]) }?.key ?: TipoIngreso.OTRO
                db.ingresos().guardar(
                    IngresoEntity(
                        id = id, codigo = codigoLibre("I", c[0], id), fecha = LocalDate.ofEpochDay(c[1].toLong()), concepto = c[2], tipo = tipo,
                        importe = c[4].toLong(), notas = c[5], estado = Estado.CONFIRMADO,
                        createdAt = previo?.createdAt ?: ahora, updatedAt = ahora, deletedAt = null,
                    ),
                )
                anotar("ingreso", id, if (previo == null) "creado" else "editado")
            }
        }
        canon(p, anioDe(p, id) ?: return@withTransaction null)[id]
    }

    /** Año en que queda el registro en el móvil (para saber en qué hoja va). */
    suspend fun anioDe(p: Pestana, id: String): Int? = when (p) {
        Pestana.GASTOS -> db.gastos().obtener(id)?.fecha?.year
        Pestana.INGRESOS -> db.ingresos().obtener(id)?.fecha?.year
        Pestana.REEMBOLSOS -> db.reembolsos().obtener(id)?.let { db.gastos().obtener(it.gastoId)?.fecha?.year }
    }

    suspend fun borrar(p: Pestana, id: String) = db.withTransaction {
        val ahora = reloj.ahoraMs()
        when (p) {
            Pestana.GASTOS -> { db.gastos().marcarBorrado(id, ahora); anotar("gasto", id, "borrado") }
            Pestana.INGRESOS -> { db.ingresos().marcarBorrado(id, ahora); anotar("ingreso", id, "borrado") }
            Pestana.REEMBOLSOS -> db.reembolsos().obtener(id)?.let { db.reembolsos().marcarBorrado(id, ahora); anotar("gasto", it.gastoId, "reembolso borrado") }
        }
    }
}
