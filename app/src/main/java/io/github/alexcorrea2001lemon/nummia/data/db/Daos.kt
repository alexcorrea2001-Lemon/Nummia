package io.github.alexcorrea2001lemon.nummia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GastoDao {
    @Upsert suspend fun guardar(g: GastoEntity)
    @Query("SELECT * FROM gastos WHERE id = :id") suspend fun obtener(id: String): GastoEntity?

    @Transaction
    @Query("SELECT * FROM gastos WHERE id = :id")
    fun observar(id: String): Flow<GastoConReembolsos?>

    @Transaction
    @Query("SELECT * FROM gastos WHERE id = :id")
    suspend fun observarUnaVez(id: String): GastoConReembolsos?

    @Transaction
    @Query("SELECT * FROM gastos WHERE deletedAt IS NULL AND fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, createdAt DESC")
    fun entre(desde: LocalDate, hasta: LocalDate): Flow<List<GastoConReembolsos>>

    @Transaction
    @Query("SELECT * FROM gastos WHERE deletedAt IS NULL ORDER BY fecha DESC, createdAt DESC")
    fun todos(): Flow<List<GastoConReembolsos>>

    @Transaction
    @Query("SELECT * FROM gastos WHERE deletedAt IS NULL AND meLoDevuelven = 1 ORDER BY fecha DESC")
    fun conDevolucion(): Flow<List<GastoConReembolsos>>

    @Query("SELECT MAX(CAST(SUBSTR(codigo, 3) AS INTEGER)) FROM gastos") suspend fun maxCodigo(): Int?

    @Query("SELECT categoriaId FROM gastos WHERE tienda = :tienda COLLATE NOCASE AND categoriaId IS NOT NULL AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun categoriaHabitual(tienda: String): String?

    @Query("SELECT DISTINCT tienda FROM gastos WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 200")
    suspend fun tiendasRecientes(): List<String>
    @Query("UPDATE gastos SET deletedAt = :ts, updatedAt = :ts WHERE id = :id") suspend fun marcarBorrado(id: String, ts: Long)
    @Query("UPDATE gastos SET deletedAt = NULL, updatedAt = :ts WHERE id = :id") suspend fun restaurar(id: String, ts: Long)
    @Query("SELECT * FROM gastos") suspend fun volcar(): List<GastoEntity>

    @Transaction
    @Query("SELECT * FROM gastos WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun borrados(): Flow<List<GastoConReembolsos>>
    @Query("SELECT id FROM gastos WHERE deletedAt IS NOT NULL AND deletedAt < :limite") suspend fun borradosAntesDe(limite: Long): List<String>
    @Query("DELETE FROM gastos WHERE id IN (:ids)") suspend fun eliminar(ids: List<String>)
    @Query("UPDATE gastos SET categoriaId = NULL, updatedAt = :ts WHERE categoriaId = :categoria") suspend fun quitarCategoria(categoria: String, ts: Long)
}

@Dao
interface ReembolsoDao {
    @Upsert suspend fun guardar(r: ReembolsoEntity)
    @Query("SELECT * FROM reembolsos WHERE id = :id") suspend fun obtener(id: String): ReembolsoEntity?
    @Query("SELECT MAX(CAST(SUBSTR(codigo, 3) AS INTEGER)) FROM reembolsos") suspend fun maxCodigo(): Int?
    @Query("UPDATE reembolsos SET deletedAt = :ts, updatedAt = :ts WHERE id = :id") suspend fun marcarBorrado(id: String, ts: Long)
    @Query("SELECT * FROM reembolsos") suspend fun volcar(): List<ReembolsoEntity>
    @Query("DELETE FROM reembolsos WHERE gastoId IN (:gastos)") suspend fun eliminarDeGastos(gastos: List<String>)
    @Query("DELETE FROM reembolsos WHERE deletedAt IS NOT NULL AND deletedAt < :limite") suspend fun eliminarBorradosAntesDe(limite: Long)
}

@Dao
interface IngresoDao {
    @Upsert suspend fun guardar(i: IngresoEntity)
    @Query("SELECT * FROM ingresos WHERE id = :id") suspend fun obtener(id: String): IngresoEntity?
    @Query("SELECT * FROM ingresos WHERE id = :id") fun observar(id: String): Flow<IngresoEntity?>

    @Query("SELECT * FROM ingresos WHERE deletedAt IS NULL AND fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC, createdAt DESC")
    fun entre(desde: LocalDate, hasta: LocalDate): Flow<List<IngresoEntity>>

    @Query("SELECT * FROM ingresos WHERE deletedAt IS NULL ORDER BY fecha DESC, createdAt DESC")
    fun todos(): Flow<List<IngresoEntity>>

    @Query("SELECT COUNT(*) FROM ingresos WHERE deletedAt IS NULL AND tipo = :tipo AND fecha BETWEEN :desde AND :hasta")
    suspend fun contarTipoEntre(tipo: TipoIngreso, desde: LocalDate, hasta: LocalDate): Int

    @Query("SELECT MAX(CAST(SUBSTR(codigo, 3) AS INTEGER)) FROM ingresos") suspend fun maxCodigo(): Int?
    @Query("UPDATE ingresos SET deletedAt = :ts, updatedAt = :ts WHERE id = :id") suspend fun marcarBorrado(id: String, ts: Long)
    @Query("UPDATE ingresos SET deletedAt = NULL, updatedAt = :ts WHERE id = :id") suspend fun restaurar(id: String, ts: Long)
    @Query("SELECT * FROM ingresos") suspend fun volcar(): List<IngresoEntity>
    @Query("SELECT * FROM ingresos WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC") fun borrados(): Flow<List<IngresoEntity>>
    @Query("SELECT id FROM ingresos WHERE deletedAt IS NOT NULL AND deletedAt < :limite") suspend fun borradosAntesDe(limite: Long): List<String>
    @Query("DELETE FROM ingresos WHERE id IN (:ids)") suspend fun eliminar(ids: List<String>)
}

@Dao
interface PagoMensualDao {
    @Upsert suspend fun guardar(p: PagoMensualEntity)
    @Query("SELECT * FROM pagos_mensuales WHERE id = :id") suspend fun obtener(id: String): PagoMensualEntity?
    @Query("SELECT * FROM pagos_mensuales WHERE deletedAt IS NULL ORDER BY dia, concepto COLLATE NOCASE") fun todos(): Flow<List<PagoMensualEntity>>
    @Query("SELECT * FROM pagos_mensuales WHERE deletedAt IS NULL AND activo = 1") suspend fun activos(): List<PagoMensualEntity>
    @Query("SELECT * FROM pagos_mensuales") suspend fun volcar(): List<PagoMensualEntity>

    /** Gastos de este pago en un mes, incluidos los borrados («este mes no»): así no se vuelve a crear. */
    @Query("SELECT COUNT(*) FROM gastos WHERE pagoMensualId = :pago AND fecha BETWEEN :desde AND :hasta")
    suspend fun gastosEnMes(pago: String, desde: LocalDate, hasta: LocalDate): Int

    @Query("SELECT * FROM gastos WHERE pagoMensualId = :pago AND deletedAt IS NULL AND estado = 'CONFIRMADO' ORDER BY fecha DESC LIMIT 24")
    fun historial(pago: String): Flow<List<GastoEntity>>

    @Query("SELECT id FROM gastos WHERE pagoMensualId = :pago AND estado = 'PREVISTO' AND deletedAt IS NULL")
    suspend fun previstosDe(pago: String): List<String>
}

@Dao
interface IngresoFijoDao {
    @Upsert suspend fun guardar(f: IngresoFijoEntity)
    @Query("SELECT * FROM ingresos_fijos WHERE id = :id") suspend fun obtener(id: String): IngresoFijoEntity?
    @Query("SELECT * FROM ingresos_fijos WHERE deletedAt IS NULL ORDER BY dia, concepto COLLATE NOCASE") fun todos(): Flow<List<IngresoFijoEntity>>
    @Query("SELECT * FROM ingresos_fijos WHERE deletedAt IS NULL AND activo = 1") suspend fun activos(): List<IngresoFijoEntity>
    @Query("SELECT * FROM ingresos_fijos") suspend fun volcar(): List<IngresoFijoEntity>

    /** Ingresos de este fijo en un mes, incluidos los borrados («este mes no»): así no se vuelve a crear. */
    @Query("SELECT COUNT(*) FROM ingresos WHERE ingresoFijoId = :fijo AND fecha BETWEEN :desde AND :hasta")
    suspend fun ingresosEnMes(fijo: String, desde: LocalDate, hasta: LocalDate): Int

    @Query("SELECT * FROM ingresos WHERE ingresoFijoId = :fijo AND deletedAt IS NULL AND estado = 'CONFIRMADO' ORDER BY fecha DESC LIMIT 24")
    fun historial(fijo: String): Flow<List<IngresoEntity>>

    @Query("SELECT id FROM ingresos WHERE ingresoFijoId = :fijo AND estado = 'PREVISTO' AND deletedAt IS NULL")
    suspend fun previstosDe(fijo: String): List<String>
}

@Dao
interface PresupuestoDao {
    @Upsert suspend fun guardar(p: PresupuestoEntity)
    @Query("DELETE FROM presupuestos WHERE id = :id") suspend fun borrar(id: String)
    @Query("SELECT * FROM presupuestos ORDER BY CASE ambito WHEN 'TOTAL' THEN 0 WHEN 'CATEGORIA' THEN 1 ELSE 2 END, ref") fun todos(): Flow<List<PresupuestoEntity>>
    @Query("SELECT * FROM presupuestos") suspend fun volcar(): List<PresupuestoEntity>
}

@Dao
interface PersonaDao {
    @Upsert suspend fun guardar(p: PersonaEntity)
    @Query("SELECT * FROM personas WHERE deletedAt IS NULL ORDER BY nombre COLLATE NOCASE") fun todas(): Flow<List<PersonaEntity>>
    @Query("SELECT * FROM personas") suspend fun volcar(): List<PersonaEntity>
}

@Dao
interface CategoriaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertarSiNoExisten(c: List<CategoriaEntity>)
    @Upsert suspend fun guardar(c: CategoriaEntity)
    @Query("SELECT * FROM categorias ORDER BY orden") fun todas(): Flow<List<CategoriaEntity>>
    @Query("SELECT * FROM categorias") suspend fun volcar(): List<CategoriaEntity>
    @Query("DELETE FROM categorias WHERE id = :id AND predefinida = 0") suspend fun borrar(id: String)
}

@Dao
interface HistorialDao {
    @Insert suspend fun insertar(h: List<HistorialEntity>)
    @Query("SELECT * FROM historial WHERE entidadId = :id ORDER BY fecha DESC, id DESC") fun de(id: String): Flow<List<HistorialEntity>>
    @Query("SELECT * FROM historial") suspend fun volcar(): List<HistorialEntity>
    @Query("DELETE FROM historial WHERE entidadId IN (:ids)") suspend fun eliminarDe(ids: List<String>)
}

@Dao
interface VaciarDao {
    @Query("DELETE FROM gastos") suspend fun gastos()
    @Query("DELETE FROM reembolsos") suspend fun reembolsos()
    @Query("DELETE FROM ingresos") suspend fun ingresos()
    @Query("DELETE FROM personas") suspend fun personas()
    @Query("DELETE FROM categorias") suspend fun categorias()
    @Query("DELETE FROM historial") suspend fun historial()
    @Query("DELETE FROM pagos_mensuales") suspend fun pagos()
    @Query("DELETE FROM presupuestos") suspend fun presupuestos()
    @Query("DELETE FROM ingresos_fijos") suspend fun ingresosFijos()
}
