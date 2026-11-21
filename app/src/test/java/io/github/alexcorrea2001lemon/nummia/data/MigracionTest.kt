package io.github.alexcorrea2001lemon.nummia.data

import android.app.Application
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * La base de datos de la v1.2 (versión 1) se actualiza a la versión 2 sin perder nada.
 * La versión 1 se reconstruye a partir del esquema actual quitando lo que añade la migración.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigracionTest {
    private val ctx: Application = ApplicationProvider.getApplicationContext()

    @Test fun deLaVersion1ALa2SinPerderDatos() = runBlocking {
        // 1) Esquema actual creado por Room
        val actual = Room.databaseBuilder(ctx, NummiaDb::class.java, "actual.db").build()
        actual.openHelper.writableDatabase
        val sentencias = ArrayList<String>()
        actual.openHelper.writableDatabase.query("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT IN ('android_metadata','sqlite_sequence','room_master_table')").use { c ->
            while (c.moveToNext()) sentencias += c.getString(0) + "\u0000" + c.getString(1)
        }
        actual.close()

        // 2) Base de datos «como la de la v1.2»: sin pagos_mensuales ni gastos.pagoMensualId, con un gasto dentro
        val fichero = ctx.getDatabasePath("antigua.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(fichero, null).use { db ->
            sentencias.forEach { s ->
                val (nombre, sql) = s.split("\u0000")
                if (nombre == "pagos_mensuales") return@forEach
                db.execSQL(if (nombre == "gastos") sql.replace(Regex(""",\s*`pagoMensualId` TEXT"""), "") else sql)
            }
            assertTrue(sentencias.any { it.startsWith("gastos\u0000") && "pagoMensualId" in it })
            db.insertOrThrow("gastos", null, ContentValues().apply {
                put("id", "g1"); put("codigo", "G-0001"); put("fecha", 21_990L); put("concepto", "Café"); put("tienda", "Cafetería")
                put("importe", 275L); put("meLoDevuelven", 0); put("notas", ""); put("estado", "CONFIRMADO"); put("createdAt", 1L); put("updatedAt", 1L)
            })
            db.version = 1
        }

        // 3) Se abre con la app nueva: migra y Room comprueba que el esquema es exactamente el esperado
        val migrada = Room.databaseBuilder(ctx, NummiaDb::class.java, "antigua.db").addMigrations(NummiaDb.MIGRACION_1_2).build()
        val g = migrada.gastos().obtener("g1")!!
        assertEquals(275L, g.importe)
        assertNull(g.pagoMensualId)
        assertTrue(migrada.pagos().activos().isEmpty())
        migrada.close()
    }
}
