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
 * Las bases de datos de versiones anteriores se actualizan sin perder nada.
 * Cada versión antigua se reconstruye a partir del esquema actual quitando lo que añadieron las migraciones posteriores.
 * Room comprueba al abrirla que el esquema final es exactamente el esperado.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigracionTest {
    private val ctx: Application = ApplicationProvider.getApplicationContext()

    /** name → sql del esquema actual. */
    private fun esquemaActual(): List<Pair<String, String>> {
        ctx.getDatabasePath("actual.db").delete()
        val actual = Room.databaseBuilder(ctx, NummiaDb::class.java, "actual.db").build()
        val res = ArrayList<Pair<String, String>>()
        actual.openHelper.writableDatabase.query(
            "SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL AND name NOT IN ('android_metadata','sqlite_sequence','room_master_table')",
        ).use { c -> while (c.moveToNext()) res += c.getString(0) to c.getString(1) }
        actual.close()
        return res
    }

    private fun crearAntigua(nombre: String, version: Int) {
        val esquema = esquemaActual()
        val fichero = ctx.getDatabasePath(nombre).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(fichero, null).use { db ->
            esquema.forEach { (tabla, sql) ->
                if (tabla == "presupuestos" && version < 3) return@forEach
                if (tabla == "pagos_mensuales" && version < 2) return@forEach
                db.execSQL(if (tabla == "gastos" && version < 2) sql.replace(Regex(""",\s*`pagoMensualId` TEXT"""), "") else sql)
            }
            db.insertOrThrow("gastos", null, ContentValues().apply {
                put("id", "g1"); put("codigo", "G-0001"); put("fecha", 21_990L); put("concepto", "Café"); put("tienda", "Cafetería")
                put("importe", 275L); put("meLoDevuelven", 0); put("notas", ""); put("estado", "CONFIRMADO"); put("createdAt", 1L); put("updatedAt", 1L)
            })
            db.version = version
        }
    }

    private fun comprobar(nombre: String) = runBlocking {
        val db = Room.databaseBuilder(ctx, NummiaDb::class.java, nombre).addMigrations(*NummiaDb.MIGRACIONES).build()
        val g = db.gastos().obtener("g1")!!
        assertEquals(275L, g.importe)
        assertNull(g.pagoMensualId)
        assertTrue(db.pagos().activos().isEmpty())
        assertTrue(db.presupuestos().volcar().isEmpty())
        db.close()
    }

    @Test fun desdeLaVersion1() {
        assertTrue(esquemaActual().any { it.first == "gastos" && "pagoMensualId" in it.second })
        crearAntigua("v1.db", 1)
        comprobar("v1.db")
    }

    @Test fun desdeLaVersion2() {
        crearAntigua("v2.db", 2)
        comprobar("v2.db")
    }
}
