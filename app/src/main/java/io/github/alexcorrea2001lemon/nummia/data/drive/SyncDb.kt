package io.github.alexcorrea2001lemon.nummia.data.drive

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Base de datos aparte (cifrada igual) con el estado de la sincronización. Nunca entra en las copias. */
@Entity(tableName = "instantanea", primaryKeys = ["pestana", "entidadId"])
data class InstantaneaEntity(val pestana: String, val entidadId: String, val anio: Int, val firma: String)

@Entity(tableName = "hojas", primaryKeys = ["anio"])
data class HojaEntity(val anio: Int, val spreadsheetId: String, val modificado: String?)

@Entity(tableName = "conflictos", primaryKeys = ["pestana", "entidadId"])
data class ConflictoEntity(val pestana: String, val entidadId: String, val anio: Int, val local: String?, val remoto: String?, val fecha: Long)

@Entity(tableName = "meta", primaryKeys = ["clave"])
data class MetaEntity(val clave: String, val valor: String)

@Dao
interface SyncDao {
    @Query("SELECT * FROM instantanea WHERE pestana = :p AND anio = :anio") suspend fun instantanea(p: String, anio: Int): List<InstantaneaEntity>
    @Query("DELETE FROM instantanea WHERE pestana = :p AND anio = :anio") suspend fun borrarInstantanea(p: String, anio: Int)
    @Query("DELETE FROM instantanea WHERE pestana = :p AND entidadId = :id") suspend fun borrarInstantaneaDe(p: String, id: String)
    @Upsert suspend fun guardarInstantanea(i: List<InstantaneaEntity>)
    @Query("DELETE FROM instantanea") suspend fun vaciarInstantaneas()

    @Query("SELECT * FROM hojas") suspend fun hojas(): List<HojaEntity>
    @Upsert suspend fun guardarHoja(h: HojaEntity)
    @Query("DELETE FROM hojas") suspend fun vaciarHojas()

    @Query("SELECT * FROM conflictos ORDER BY fecha DESC") fun conflictos(): Flow<List<ConflictoEntity>>
    @Query("SELECT * FROM conflictos WHERE pestana = :p AND anio = :anio") suspend fun conflictosDe(p: String, anio: Int): List<ConflictoEntity>
    @Upsert suspend fun guardarConflicto(c: ConflictoEntity)
    @Query("DELETE FROM conflictos WHERE pestana = :p AND entidadId = :id") suspend fun borrarConflicto(p: String, id: String)
    @Query("DELETE FROM conflictos") suspend fun vaciarConflictos()

    @Query("SELECT valor FROM meta WHERE clave = :c") suspend fun meta(c: String): String?
    @Query("SELECT * FROM meta") fun metaFlujo(): Flow<List<MetaEntity>>
    @Upsert suspend fun guardarMeta(m: MetaEntity)
    @Query("DELETE FROM meta WHERE clave = :c") suspend fun borrarMeta(c: String)
}

@Database(entities = [InstantaneaEntity::class, HojaEntity::class, ConflictoEntity::class, MetaEntity::class], version = 1, exportSchema = false)
abstract class SyncDb : RoomDatabase() {
    abstract fun sync(): SyncDao
}

/** Claves de la tabla meta. */
object Meta {
    const val CUENTA = "cuenta"
    const val CARPETA = "carpeta"
    const val CARPETA_COPIAS = "carpeta_copias"
    const val ULTIMA = "ultima_sincronizacion"
    const val ERROR = "error"
    const val PENDIENTE = "cambios_pendientes"
    const val FORZAR_SUBIDA = "forzar_subida"
    const val ULTIMA_COPIA = "ultima_copia"
    const val CAMBIOS_DESDE_COPIA = "cambios_desde_copia"
    const val AVISOS = "avisos"
    const val CAMBIOS_ORDENADOR = "cambios_ordenador"
    const val HISTORIAL = "historial_sync"
}
