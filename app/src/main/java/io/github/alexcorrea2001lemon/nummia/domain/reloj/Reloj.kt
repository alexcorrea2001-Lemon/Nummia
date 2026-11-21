package io.github.alexcorrea2001lemon.nummia.domain.reloj

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Fuente de «ahora», para que las pruebas puedan fijar la fecha. */
interface Reloj {
    fun hoy(): LocalDate
    fun ahoraMs(): Long
}

class RelojSistema @Inject constructor() : Reloj {
    override fun hoy(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    override fun ahoraMs(): Long = System.currentTimeMillis()
}
