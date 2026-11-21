package io.github.alexcorrea2001lemon.nummia.data.repo

import androidx.room.withTransaction
import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.Origen
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class GastoInput(
    val id: String? = null,
    val fecha: LocalDate,
    val concepto: String,
    val tienda: String,
    val categoriaId: String? = null,
    val importe: Long,
    val meLoDevuelven: Boolean = false,
    val personaId: String? = null,
    val esperado: Long? = null,
    val notas: String = "",
    /** Solo al crear: «¿Ya te lo han dado?» → crea el reembolso de lo esperado. */
    val yaDevuelto: Boolean = false,
)

data class IngresoInput(
    val id: String? = null,
    val fecha: LocalDate,
    val concepto: String,
    val tipo: TipoIngreso,
    val importe: Long,
    val notas: String = "",
)

@Singleton
class MovimientosRepo @Inject constructor(
    private val db: NummiaDb,
    private val reloj: Reloj,
) {
    private val fechaCorta = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // ---------- Lectura ----------

    fun periodo(desde: LocalDate, hasta: LocalDate): Flow<DatosPeriodo> =
        combine(db.gastos().entre(desde, hasta), db.ingresos().entre(desde, hasta)) { g, i -> DatosPeriodo(g, i) }

    fun mes(ym: YearMonth): Flow<DatosPeriodo> = periodo(ym.atDay(1), ym.atEndOfMonth())

    fun resumenMes(ym: YearMonth): Flow<Resumen> = mes(ym).map {
        resumen(ym.atDay(1), ym.atEndOfMonth(), it.movGastos, it.movReembolsos, it.movIngresos)
    }

    fun todo(): Flow<DatosPeriodo> =
        combine(db.gastos().todos(), db.ingresos().todos()) { g, i -> DatosPeriodo(g, i) }

    fun conDevolucion(): Flow<List<GastoConReembolsos>> = db.gastos().conDevolucion()
    fun gasto(id: String): Flow<GastoConReembolsos?> = db.gastos().observar(id)
    fun historial(id: String): Flow<List<HistorialEntity>> = db.historial().de(id)
    fun personas(): Flow<List<PersonaEntity>> = db.personas().todas()
    fun categorias() = db.categorias().todas()

    suspend fun ingreso(id: String): IngresoEntity? = db.ingresos().obtener(id)
    suspend fun gastoUnaVez(id: String): GastoConReembolsos? = db.gastos().observarUnaVez(id)
    suspend fun categoriaHabitual(tienda: String): String? = db.gastos().categoriaHabitual(tienda.trim())

    // ---------- Preparación ----------

    suspend fun prepararDatos() {
        db.categorias().insertarSiNoExisten(NummiaDb.CATEGORIAS_INICIALES)
    }

    // ---------- Gastos ----------

    suspend fun guardarGasto(input: GastoInput): String = db.withTransaction {
        val ahora = reloj.ahoraMs()
        val previo = input.id?.let { db.gastos().obtener(it) }
        val id = previo?.id ?: UUID.randomUUID().toString()
        val nuevo = GastoEntity(
            id = id,
            codigo = previo?.codigo ?: codigo("G", db.gastos().maxCodigo()),
            fecha = input.fecha,
            concepto = input.concepto.trim(),
            tienda = input.tienda.trim().ifEmpty { input.concepto.trim() },
            categoriaId = input.categoriaId,
            importe = input.importe,
            meLoDevuelven = input.meLoDevuelven,
            personaId = if (input.meLoDevuelven) input.personaId else null,
            importeEsperado = if (input.meLoDevuelven) input.esperado else null,
            notas = input.notas.trim(),
            estado = previo?.estado ?: Estado.CONFIRMADO,
            createdAt = previo?.createdAt ?: ahora,
            updatedAt = ahora,
            deletedAt = previo?.deletedAt,
        )
        db.gastos().guardar(nuevo)
        if (previo == null) {
            anotar("gasto", id, "creado", null, nuevo.codigo)
            if (input.meLoDevuelven && input.yaDevuelto) {
                crearReembolso(id, input.esperado ?: input.importe, reloj.hoy(), input.personaId, "")
            }
        } else {
            anotarCambios(previo, nuevo)
        }
        id
    }

    suspend fun borrarGasto(id: String) = db.withTransaction {
        db.gastos().marcarBorrado(id, reloj.ahoraMs())
        anotar("gasto", id, "borrado", null, null)
    }

    suspend fun deshacerBorradoGasto(id: String) = db.withTransaction {
        db.gastos().restaurar(id, reloj.ahoraMs())
        anotar("gasto", id, "restaurado", null, null)
    }

    suspend fun repetirHoy(gastoId: String): String {
        val o = requireNotNull(db.gastos().obtener(gastoId)) { "No existe el gasto $gastoId" }
        return guardarGasto(
            GastoInput(
                fecha = reloj.hoy(), concepto = o.concepto, tienda = o.tienda, categoriaId = o.categoriaId,
                importe = o.importe, meLoDevuelven = o.meLoDevuelven, personaId = o.personaId,
                esperado = o.importeEsperado, notas = o.notas,
            ),
        )
    }

    /** Copia exacta con la MISMA fecha y un código nuevo (botón «Duplicar»). */
    suspend fun duplicarGasto(gastoId: String): String {
        val o = requireNotNull(db.gastos().obtener(gastoId)) { "No existe el gasto $gastoId" }
        return guardarGasto(
            GastoInput(
                fecha = o.fecha, concepto = o.concepto, tienda = o.tienda, categoriaId = o.categoriaId,
                importe = o.importe, meLoDevuelven = o.meLoDevuelven, personaId = o.personaId,
                esperado = o.importeEsperado, notas = o.notas,
            ),
        )
    }

    suspend fun confirmarGasto(id: String, fecha: LocalDate, importe: Long) = db.withTransaction {
        val g = requireNotNull(db.gastos().obtener(id))
        val nuevo = g.copy(fecha = fecha, importe = importe, estado = Estado.CONFIRMADO, updatedAt = reloj.ahoraMs())
        db.gastos().guardar(nuevo)
        anotarCambios(g, nuevo)
    }

    // ---------- Reembolsos ----------

    suspend fun anadirReembolso(gastoId: String, importe: Long, fecha: LocalDate?, personaId: String?, nota: String = ""): String =
        db.withTransaction { crearReembolso(gastoId, importe, fecha, personaId, nota) }

    suspend fun editarReembolso(id: String, importe: Long, fecha: LocalDate?, personaId: String?, nota: String) = db.withTransaction {
        val r = requireNotNull(db.reembolsos().obtener(id))
        db.reembolsos().guardar(r.copy(importe = importe, fecha = fecha, personaId = personaId, nota = nota, updatedAt = reloj.ahoraMs()))
        if (r.importe != importe) anotar("gasto", r.gastoId, "reembolso ${r.codigo}", formatearEuros(r.importe), formatearEuros(importe))
    }

    suspend fun borrarReembolso(id: String) = db.withTransaction {
        val r = requireNotNull(db.reembolsos().obtener(id))
        db.reembolsos().marcarBorrado(id, reloj.ahoraMs())
        anotar("gasto", r.gastoId, "reembolso borrado", "${r.codigo} · ${formatearEuros(r.importe)}", null)
    }

    /** Apunta de golpe lo que faltaba por devolver (o el gasto real restante si no estaba marcado). */
    suspend fun devueltoEntero(gastoId: String): String? = db.withTransaction {
        val g = db.gastos().observarUnaVez(gastoId) ?: return@withTransaction null
        val pendiente = if (g.gasto.meLoDevuelven) g.falta else g.real
        if (pendiente <= 0) null else crearReembolso(gastoId, pendiente, reloj.hoy(), g.gasto.personaId, "")
    }

    private suspend fun crearReembolso(gastoId: String, importe: Long, fecha: LocalDate?, personaId: String?, nota: String): String {
        val ahora = reloj.ahoraMs()
        val r = ReembolsoEntity(
            id = UUID.randomUUID().toString(),
            codigo = codigo("R", db.reembolsos().maxCodigo()),
            gastoId = gastoId, fecha = fecha, importe = importe, personaId = personaId, nota = nota,
            createdAt = ahora, updatedAt = ahora,
        )
        db.reembolsos().guardar(r)
        anotar("gasto", gastoId, "reembolso", null, "${r.codigo} · ${formatearEuros(importe)}")
        return r.id
    }

    // ---------- Ingresos ----------

    suspend fun guardarIngreso(input: IngresoInput): String = db.withTransaction {
        val ahora = reloj.ahoraMs()
        val previo = input.id?.let { db.ingresos().obtener(it) }
        val id = previo?.id ?: UUID.randomUUID().toString()
        val nuevo = IngresoEntity(
            id = id,
            codigo = previo?.codigo ?: codigo("I", db.ingresos().maxCodigo()),
            fecha = input.fecha, concepto = input.concepto.trim(), tipo = input.tipo, importe = input.importe,
            notas = input.notas.trim(), estado = Estado.CONFIRMADO,
            createdAt = previo?.createdAt ?: ahora, updatedAt = ahora, deletedAt = previo?.deletedAt,
        )
        db.ingresos().guardar(nuevo)
        if (previo == null) anotar("ingreso", id, "creado", null, nuevo.codigo) else anotarCambiosIngreso(previo, nuevo)
        id
    }

    suspend fun borrarIngreso(id: String) = db.withTransaction {
        db.ingresos().marcarBorrado(id, reloj.ahoraMs()); anotar("ingreso", id, "borrado", null, null)
    }

    suspend fun deshacerBorradoIngreso(id: String) = db.withTransaction {
        db.ingresos().restaurar(id, reloj.ahoraMs()); anotar("ingreso", id, "restaurado", null, null)
    }

    /** Crea el sueldo PREVISTO del mes de [hoy] (último día) si hay sueldo habitual y aún no existe ninguno. */
    suspend fun asegurarSueldoPrevisto(hoy: LocalDate, sueldoHabitual: Long?) {
        if (sueldoHabitual == null || sueldoHabitual <= 0) return
        db.withTransaction {
            val ym = YearMonth.from(hoy)
            if (db.ingresos().contarTipoEntre(TipoIngreso.SUELDO, ym.atDay(1), ym.atEndOfMonth()) > 0) return@withTransaction
            val ahora = reloj.ahoraMs()
            val i = IngresoEntity(
                id = UUID.randomUUID().toString(), codigo = codigo("I", db.ingresos().maxCodigo()),
                fecha = ym.atEndOfMonth(), concepto = "Sueldo", tipo = TipoIngreso.SUELDO,
                importe = sueldoHabitual, estado = Estado.PREVISTO, createdAt = ahora, updatedAt = ahora,
            )
            db.ingresos().guardar(i)
            anotar("ingreso", i.id, "previsto", null, i.codigo)
        }
    }

    suspend fun confirmarIngreso(id: String, fecha: LocalDate, importe: Long) = db.withTransaction {
        val i = requireNotNull(db.ingresos().obtener(id))
        val nuevo = i.copy(fecha = fecha, importe = importe, estado = Estado.CONFIRMADO, updatedAt = reloj.ahoraMs())
        db.ingresos().guardar(nuevo)
        anotar("ingreso", id, "confirmado", null, "${formatearEuros(importe)} · ${fechaCorta.format(fecha)}")
    }

    // ---------- Personas ----------

    suspend fun crearPersona(nombre: String): String {
        val p = PersonaEntity(UUID.randomUUID().toString(), nombre.trim(), reloj.ahoraMs())
        db.personas().guardar(p)
        return p.id
    }

    suspend fun renombrarPersona(p: PersonaEntity, nombre: String) = db.personas().guardar(p.copy(nombre = nombre.trim()))
    suspend fun borrarPersona(p: PersonaEntity) = db.personas().guardar(p.copy(deletedAt = reloj.ahoraMs()))

    // ---------- Historial ----------

    private suspend fun anotar(entidad: String, id: String, campo: String, antes: String?, despues: String?) {
        db.historial().insertar(listOf(HistorialEntity(0, entidad, id, campo, antes, despues, Origen.MOVIL, reloj.ahoraMs())))
    }

    private suspend fun anotarCambios(a: GastoEntity, b: GastoEntity) {
        val cambios = buildList {
            fun c(campo: String, x: Any?, y: Any?, f: (Any?) -> String? = { it?.toString() }) {
                if (x != y) add(HistorialEntity(0, "gasto", a.id, campo, f(x), f(y), Origen.MOVIL, b.updatedAt))
            }
            val euros: (Any?) -> String? = { (it as Long?)?.let(::formatearEuros) }
            c("importe", a.importe, b.importe, euros)
            c("fecha", a.fecha, b.fecha) { (it as LocalDate?)?.let(fechaCorta::format) }
            c("concepto", a.concepto, b.concepto)
            c("tienda", a.tienda, b.tienda)
            c("categoría", a.categoriaId, b.categoriaId)
            c("me lo devuelven", a.meLoDevuelven, b.meLoDevuelven) { if (it == true) "Sí" else "No" }
            c("quién", a.personaId, b.personaId)
            c("esperado", a.importeEsperado, b.importeEsperado, euros)
            c("notas", a.notas, b.notas)
            c("estado", a.estado, b.estado) { if (it == Estado.PREVISTO) "Previsto" else "Confirmado" }
        }
        if (cambios.isNotEmpty()) db.historial().insertar(cambios)
    }

    private suspend fun anotarCambiosIngreso(a: IngresoEntity, b: IngresoEntity) {
        val cambios = buildList {
            fun c(campo: String, x: String?, y: String?) {
                if (x != y) add(HistorialEntity(0, "ingreso", a.id, campo, x, y, Origen.MOVIL, b.updatedAt))
            }
            c("importe", formatearEuros(a.importe), formatearEuros(b.importe))
            c("fecha", fechaCorta.format(a.fecha), fechaCorta.format(b.fecha))
            c("concepto", a.concepto, b.concepto)
            c("tipo", a.tipo.name, b.tipo.name)
            c("notas", a.notas, b.notas)
        }
        if (cambios.isNotEmpty()) db.historial().insertar(cambios)
    }

    private fun codigo(prefijo: String, max: Int?): String = "$prefijo-${((max ?: 0) + 1).toString().padStart(4, '0')}"
}
