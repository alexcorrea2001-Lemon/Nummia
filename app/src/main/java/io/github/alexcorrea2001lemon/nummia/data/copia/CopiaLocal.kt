package io.github.alexcorrea2001lemon.nummia.data.copia

import androidx.room.withTransaction
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.Origen
import io.github.alexcorrea2001lemon.nummia.data.db.PagoMensualEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoFijoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PresupuestoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

class CopiaInvalida(motivo: String) : Exception(motivo)

@Serializable data class GastoDto(
    val id: String, val codigo: String, val fecha: Long, val concepto: String, val tienda: String, val categoriaId: String?,
    val importe: Long, val meLoDevuelven: Boolean, val personaId: String?, val importeEsperado: Long?, val notas: String,
    val estado: String, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?, val pagoMensualId: String? = null,
)
@Serializable data class PagoDto(
    val id: String, val concepto: String, val tienda: String, val categoriaId: String?, val importe: Long, val dia: Int,
    val activo: Boolean, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?,
)
@Serializable data class PresupuestoDto(val id: String, val ambito: String, val ref: String?, val limite: Long, val createdAt: Long, val updatedAt: Long)
@Serializable data class ReembolsoDto(
    val id: String, val codigo: String, val gastoId: String, val fecha: Long?, val importe: Long, val personaId: String?,
    val nota: String, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?,
)
@Serializable data class IngresoDto(
    val id: String, val codigo: String, val fecha: Long, val concepto: String, val tipo: String, val importe: Long,
    val notas: String, val estado: String, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?,
    val ingresoFijoId: String? = null,
)
@Serializable data class IngresoFijoDto(
    val id: String, val concepto: String, val tipo: String, val importe: Long, val dia: Int,
    val activo: Boolean, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?,
)
@Serializable data class PersonaDto(val id: String, val nombre: String, val createdAt: Long, val deletedAt: Long?)
@Serializable data class CategoriaDto(val id: String, val nombre: String, val icono: String, val color: Long, val predefinida: Boolean, val orden: Int)
@Serializable data class HistorialDto(val entidad: String, val entidadId: String, val campo: String, val antes: String?, val despues: String?, val origen: String, val fecha: Long)

@Serializable
data class Copia(
    val formato: String = "nummia",
    val version: Int = VERSION,
    val creada: Long,
    val gastos: List<GastoDto>,
    val reembolsos: List<ReembolsoDto>,
    val ingresos: List<IngresoDto>,
    val personas: List<PersonaDto>,
    val categorias: List<CategoriaDto>,
    val historial: List<HistorialDto>,
    val pagos: List<PagoDto> = emptyList(),
    val presupuestos: List<PresupuestoDto> = emptyList(),
    val ingresosFijos: List<IngresoFijoDto> = emptyList(),
) {
    companion object { const val VERSION = 1 }
}

/** Copia completa en un archivo `.nummia` (JSON comprimido con gzip). */
@Singleton
class CopiaLocal @Inject constructor(private val db: NummiaDb) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Con [contrasena], la copia sale cifrada (ver [CifradoCopia]). */
    suspend fun exportar(ahora: Long, contrasena: String? = null): ByteArray {
        val c = Copia(
            creada = ahora,
            gastos = db.gastos().volcar().map { it.dto() },
            reembolsos = db.reembolsos().volcar().map { it.dto() },
            ingresos = db.ingresos().volcar().map { it.dto() },
            personas = db.personas().volcar().map { PersonaDto(it.id, it.nombre, it.createdAt, it.deletedAt) },
            categorias = db.categorias().volcar().map { CategoriaDto(it.id, it.nombre, it.icono, it.color, it.predefinida, it.orden) },
            historial = db.historial().volcar().map { HistorialDto(it.entidad, it.entidadId, it.campo, it.antes, it.despues, it.origen.name, it.fecha) },
            presupuestos = db.presupuestos().volcar().map { PresupuestoDto(it.id, it.ambito, it.ref, it.limite, it.createdAt, it.updatedAt) },
            ingresosFijos = db.ingresosFijos().volcar().map { IngresoFijoDto(it.id, it.concepto, it.tipo.name, it.importe, it.dia, it.activo, it.createdAt, it.updatedAt, it.deletedAt) },
            pagos = db.pagos().volcar().map { PagoDto(it.id, it.concepto, it.tienda, it.categoriaId, it.importe, it.dia, it.activo, it.createdAt, it.updatedAt, it.deletedAt, it.cadaMeses, it.desde) },
        )
        val salida = ByteArrayOutputStream()
        GZIPOutputStream(salida).use { it.write(json.encodeToString(Copia.serializer(), c).toByteArray(Charsets.UTF_8)) }
        val plano = salida.toByteArray()
        return if (contrasena.isNullOrEmpty()) plano else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { CifradoCopia.cifrar(plano, contrasena) }
    }

    /** Lanza [CopiaCifrada] si hace falta contraseña y no se ha dado; [CopiaInvalida] si es incorrecta o está dañada. */
    fun leer(bytes: ByteArray, contrasena: String? = null): Copia {
        val gz = if (CifradoCopia.estaCifrada(bytes)) {
            if (contrasena.isNullOrEmpty()) throw CopiaCifrada()
            CifradoCopia.descifrar(bytes, contrasena)
        } else bytes
        val texto = try {
            GZIPInputStream(ByteArrayInputStream(gz)).use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            throw CopiaInvalida("El archivo no es una copia de Nummia.")
        }
        val c = try { json.decodeFromString(Copia.serializer(), texto) } catch (e: Exception) { throw CopiaInvalida("La copia está dañada.") }
        if (c.formato != "nummia") throw CopiaInvalida("El archivo no es una copia de Nummia.")
        if (c.version > Copia.VERSION) throw CopiaInvalida("La copia es de una versión más nueva de Nummia. Actualiza la app.")
        return c
    }

    /** Sustituye TODOS los datos por los de la copia, en una sola transacción. */
    suspend fun restaurar(c: Copia) = db.withTransaction {
        db.vaciar().apply { gastos(); reembolsos(); ingresos(); personas(); categorias(); historial(); pagos(); presupuestos(); ingresosFijos() }
        c.ingresosFijos.forEach { db.ingresosFijos().guardar(IngresoFijoEntity(it.id, it.concepto, TipoIngreso.valueOf(it.tipo), it.importe, it.dia, it.activo, it.createdAt, it.updatedAt, it.deletedAt)) }
        c.presupuestos.forEach { db.presupuestos().guardar(PresupuestoEntity(it.id, it.ambito, it.ref, it.limite, it.createdAt, it.updatedAt)) }
        c.pagos.forEach { db.pagos().guardar(PagoMensualEntity(it.id, it.concepto, it.tienda, it.categoriaId, it.importe, it.dia, it.activo, it.createdAt, it.updatedAt, it.deletedAt, it.cadaMeses, it.desde)) }
        c.personas.forEach { db.personas().guardar(PersonaEntity(it.id, it.nombre, it.createdAt, it.deletedAt)) }
        c.categorias.forEach { db.categorias().guardar(CategoriaEntity(it.id, it.nombre, it.icono, it.color, it.predefinida, it.orden)) }
        c.gastos.forEach { db.gastos().guardar(it.entidad()) }
        c.reembolsos.forEach { db.reembolsos().guardar(it.entidad()) }
        c.ingresos.forEach { db.ingresos().guardar(it.entidad()) }
        db.historial().insertar(c.historial.map { HistorialEntity(0, it.entidad, it.entidadId, it.campo, it.antes, it.despues, Origen.valueOf(it.origen), it.fecha) })
    }

    private fun GastoEntity.dto() = GastoDto(id, codigo, fecha.toEpochDay(), concepto, tienda, categoriaId, importe, meLoDevuelven, personaId, importeEsperado, notas, estado.name, createdAt, updatedAt, deletedAt, pagoMensualId)
    private fun GastoDto.entidad() = GastoEntity(id, codigo, LocalDate.ofEpochDay(fecha), concepto, tienda, categoriaId, importe, meLoDevuelven, personaId, importeEsperado, notas, Estado.valueOf(estado), createdAt, updatedAt, deletedAt, pagoMensualId)
    private fun ReembolsoEntity.dto() = ReembolsoDto(id, codigo, gastoId, fecha?.toEpochDay(), importe, personaId, nota, createdAt, updatedAt, deletedAt)
    private fun ReembolsoDto.entidad() = ReembolsoEntity(id, codigo, gastoId, fecha?.let(LocalDate::ofEpochDay), importe, personaId, nota, createdAt, updatedAt, deletedAt)
    private fun IngresoEntity.dto() = IngresoDto(id, codigo, fecha.toEpochDay(), concepto, tipo.name, importe, notas, estado.name, createdAt, updatedAt, deletedAt, ingresoFijoId)
    private fun IngresoDto.entidad() = IngresoEntity(id, codigo, LocalDate.ofEpochDay(fecha), concepto, TipoIngreso.valueOf(tipo), importe, notas, Estado.valueOf(estado), createdAt, updatedAt, deletedAt, ingresoFijoId)
}
