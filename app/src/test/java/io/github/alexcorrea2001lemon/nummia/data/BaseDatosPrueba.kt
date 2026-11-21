package io.github.alexcorrea2001lemon.nummia.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import java.time.LocalDate

/** Reloj fijo y controlable para las pruebas. */
class RelojFijo(var fecha: LocalDate = LocalDate.of(2030, 3, 15), var ms: Long = 1_000L) : Reloj {
    override fun hoy(): LocalDate = fecha
    override fun ahoraMs(): Long = ms++
}

/** Base de datos en memoria y sin cifrado (el cifrado solo existe en el móvil real). */
fun dbPrueba(): NummiaDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), NummiaDb::class.java)
    .allowMainThreadQueries()
    .build()
