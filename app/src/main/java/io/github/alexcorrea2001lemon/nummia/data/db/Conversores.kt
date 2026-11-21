package io.github.alexcorrea2001lemon.nummia.data.db

import androidx.room.TypeConverter
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import java.time.LocalDate

class Conversores {
    @TypeConverter fun fechaAEpoch(f: LocalDate?): Long? = f?.toEpochDay()
    @TypeConverter fun epochAFecha(d: Long?): LocalDate? = d?.let(LocalDate::ofEpochDay)
    @TypeConverter fun estadoATexto(e: Estado): String = e.name
    @TypeConverter fun textoAEstado(s: String): Estado = Estado.valueOf(s)
    @TypeConverter fun tipoATexto(t: TipoIngreso): String = t.name
    @TypeConverter fun textoATipo(s: String): TipoIngreso = TipoIngreso.valueOf(s)
    @TypeConverter fun origenATexto(o: Origen): String = o.name
    @TypeConverter fun textoAOrigen(s: String): Origen = Origen.valueOf(s)
}
