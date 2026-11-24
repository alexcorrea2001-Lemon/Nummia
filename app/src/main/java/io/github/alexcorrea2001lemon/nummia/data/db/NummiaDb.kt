package io.github.alexcorrea2001lemon.nummia.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GastoEntity::class, ReembolsoEntity::class, IngresoEntity::class,
        PersonaEntity::class, CategoriaEntity::class, HistorialEntity::class, PagoMensualEntity::class,
        PresupuestoEntity::class, IngresoFijoEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Conversores::class)
abstract class NummiaDb : RoomDatabase() {
    abstract fun gastos(): GastoDao
    abstract fun reembolsos(): ReembolsoDao
    abstract fun ingresos(): IngresoDao
    abstract fun personas(): PersonaDao
    abstract fun categorias(): CategoriaDao
    abstract fun historial(): HistorialDao
    abstract fun vaciar(): VaciarDao
    abstract fun pagos(): PagoMensualDao
    abstract fun presupuestos(): PresupuestoDao
    abstract fun ingresosFijos(): IngresoFijoDao

    companion object {
        /** v1.4.0: presupuestos. Solo añade una tabla. */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `presupuestos` (`id` TEXT NOT NULL, `ambito` TEXT NOT NULL, `ref` TEXT, " +
                        "`limite` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }
        /** v2.0.0: ingresos que se cobran cada mes. Solo añade. */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ingresos` ADD COLUMN `ingresoFijoId` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ingresos_fijos` (`id` TEXT NOT NULL, `concepto` TEXT NOT NULL, `tipo` TEXT NOT NULL, " +
                        "`importe` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `activo` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                )
            }
        }
        /** v2.4.0: pagos trimestrales y anuales. Solo añade. */
        val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pagos_mensuales` ADD COLUMN `cadaMeses` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `pagos_mensuales` ADD COLUMN `desde` TEXT")
            }
        }
        val MIGRACIONES: Array<Migration> get() = arrayOf(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)

        /** v1.3.0: pagos mensuales. Solo añade: no toca ningún dato existente. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gastos` ADD COLUMN `pagoMensualId` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pagos_mensuales` (`id` TEXT NOT NULL, `concepto` TEXT NOT NULL, `tienda` TEXT NOT NULL, " +
                        "`categoriaId` TEXT, `importe` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `activo` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
                )
            }
        }

        /** Categorías de serie. Son opcionales: un gasto puede no tener ninguna. */
        val CATEGORIAS_INICIALES = listOf(
            CategoriaEntity("alimentacion", "Alimentación", "restaurant", 0xFF4ADE80, true, 0),
            CategoriaEntity("transporte", "Transporte", "bus", 0xFF60A5FA, true, 1),
            CategoriaEntity("compras", "Compras", "shopping", 0xFFF472B6, true, 2),
            CategoriaEntity("suscripciones", "Suscripciones", "autorenew", 0xFFA78BFA, true, 3),
            CategoriaEntity("ocio", "Ocio", "sports_esports", 0xFFFB923C, true, 4),
            CategoriaEntity("viajes", "Viajes", "flight", 0xFF22D3EE, true, 5),
            CategoriaEntity("vivienda", "Vivienda", "home", 0xFFFBBF24, true, 6),
            CategoriaEntity("salud", "Salud", "favorite", 0xFFFB7185, true, 7),
            CategoriaEntity("educacion", "Educación", "school", 0xFF818CF8, true, 8),
            CategoriaEntity("tramites", "Trámites", "description", 0xFF94A3B8, true, 9),
            CategoriaEntity("otros", "Otros", "more", 0xFF9BAAA2, true, 10),
        )
    }
}
