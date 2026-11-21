package io.github.alexcorrea2001lemon.nummia.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import java.time.LocalDate

@Entity(tableName = "gastos", indices = [Index("fecha"), Index("codigo", unique = true)])
data class GastoEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val fecha: LocalDate,
    val concepto: String,
    val tienda: String,
    val categoriaId: String? = null,
    val importe: Long,
    val meLoDevuelven: Boolean = false,
    val personaId: String? = null,
    val importeEsperado: Long? = null,
    val notas: String = "",
    val estado: Estado = Estado.CONFIRMADO,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** Pago mensual del que sale este gasto (desde la v1.3.0). */
    val pagoMensualId: String? = null,
)

@Entity(tableName = "reembolsos", indices = [Index("gastoId"), Index("codigo", unique = true)])
data class ReembolsoEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val gastoId: String,
    val fecha: LocalDate?,
    val importe: Long,
    val personaId: String? = null,
    val nota: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "ingresos", indices = [Index("fecha"), Index("codigo", unique = true)])
data class IngresoEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val fecha: LocalDate,
    val concepto: String,
    val tipo: TipoIngreso,
    val importe: Long,
    val notas: String = "",
    val estado: Estado = Estado.CONFIRMADO,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val createdAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val icono: String,
    val color: Long,
    val predefinida: Boolean,
    val orden: Int,
)

enum class Origen { MOVIL, ORDENADOR, IMPORTACION, RESTAURACION }

@Entity(tableName = "historial", indices = [Index("entidadId")])
data class HistorialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entidad: String,
    val entidadId: String,
    val campo: String,
    val antes: String?,
    val despues: String?,
    val origen: Origen,
    val fecha: Long,
)

/** Gasto con TODOS sus reembolsos (incluidos los borrados: filtrar con [activos]). */
data class GastoConReembolsos(
    @Embedded val gasto: GastoEntity,
    @Relation(parentColumn = "id", entityColumn = "gastoId")
    val reembolsos: List<ReembolsoEntity>,
) {
    val activos: List<ReembolsoEntity> get() = reembolsos.filter { it.deletedAt == null }
}

/** Algo que se paga cada mes (el precio puede cambiar). Cada mes se crea un gasto PREVISTO enlazado. */
@Entity(tableName = "pagos_mensuales")
data class PagoMensualEntity(
    @PrimaryKey val id: String,
    val concepto: String,
    val tienda: String,
    val categoriaId: String? = null,
    /** Precio actual: el del próximo previsto. */
    val importe: Long,
    /** Día del mes (1–31; en meses más cortos, el último día). */
    val dia: Int,
    val activo: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Límite de gasto mensual: total, de una categoría ([ref] = id) o de una tienda ([ref] = nombre). [ambito] = nombre de Ambito. */
@Entity(tableName = "presupuestos")
data class PresupuestoEntity(
    @PrimaryKey val id: String,
    val ambito: String,
    val ref: String?,
    val limite: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
