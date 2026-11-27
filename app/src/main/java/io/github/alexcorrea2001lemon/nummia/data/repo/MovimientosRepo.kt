package io.github.alexcorrea2001lemon.nummia.data.repo

import androidx.room.withTransaction
import io.github.alexcorrea2001lemon.nummia.data.db.CategoriaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.GastoConReembolsos
import io.github.alexcorrea2001lemon.nummia.data.db.GastoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.HistorialEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.db.Origen
import io.github.alexcorrea2001lemon.nummia.data.db.PagoMensualEntity
import io.github.alexcorrea2001lemon.nummia.data.db.IngresoFijoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.db.PresupuestoEntity
import io.github.alexcorrea2001lemon.nummia.data.db.ReembolsoEntity
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Ambito
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Estado
import io.github.alexcorrea2001lemon.nummia.domain.calculo.EstadoPresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.GastoDePresupuesto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Limite
import io.github.alexcorrea2001lemon.nummia.domain.calculo.estadoPresupuestos
import io.github.alexcorrea2001lemon.nummia.domain.calculo.Resumen
import io.github.alexcorrea2001lemon.nummia.domain.calculo.TipoIngreso
import io.github.alexcorrea2001lemon.nummia.domain.calculo.claveTexto
import io.github.alexcorrea2001lemon.nummia.domain.calculo.resumen
import io.github.alexcorrea2001lemon.nummia.domain.dinero.formatearEuros
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    /** Solo al crear: «Se paga cada mes» → crea el pago mensual y lo enlaza. */
    val cadaMes: Boolean = false,
)

data class IngresoInput(
    val id: String? = null,
    val fecha: LocalDate,
    val concepto: String,
    val tipo: TipoIngreso,
    val importe: Long,
    val notas: String = "",
    /** «Se cobra cada mes»: true crea el ingreso fijo, false lo quita, null lo deja como está. */
    val cadaMes: Boolean? = null,
)

@Singleton
const val DIAS_PAPELERA = 30

/** Clave para comparar tiendas: sin mayúsculas, tildes, espacios ni signos («Bono bús» = «bonobus»). */
fun claveTienda(s: String): String = claveTexto(s).replace(Regex("[^a-z0-9ñ]"), "")

/** Una tienda con cuántas veces se ha comprado, cuánto en total y la fecha del último gasto. */
data class TiendaResumen(val nombre: String, val veces: Int, val total: Long, val ultima: java.time.LocalDate? = null)

fun PresupuestoEntity.aLimite() = Limite(
    id, runCatching { Ambito.valueOf(ambito) }.getOrDefault(Ambito.TOTAL), ref, limite,
    io.github.alexcorrea2001lemon.nummia.domain.calculo.Periodo.de(periodo),
)

/** Avisa de que algo ha cambiado (para sincronizar con Drive al poco rato). */
fun interface AvisoCambios {
    fun cambio()
}

class MovimientosRepo @Inject constructor(
    private val db: NummiaDb,
    private val reloj: Reloj,
    private val aviso: AvisoCambios,
) {
    /** Para las pruebas: sin aviso. */
    constructor(db: NummiaDb, reloj: Reloj) : this(db, reloj, AvisoCambios {})

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

    suspend fun guardarGasto(input: GastoInput): String {
        val id = db.withTransaction {
            val ahora = reloj.ahoraMs()
            val previo = input.id?.let { db.gastos().obtener(it) }
            val id = previo?.id ?: UUID.randomUUID().toString()
            // La misma tienda escrita con otras mayúsculas o tildes («Bonobús» / «BonoBus») se guarda con la grafía que ya se usa.
            val tienda = grafiaDeTienda(input.tienda.trim().ifEmpty { input.concepto.trim() }, id)
            // Sin categoría: se usa la que ya tiene esa tienda en otros gastos.
            val categoria = input.categoriaId ?: categoriaDeTienda(tienda, id)
            val pagoActual = previo?.pagoMensualId?.let { db.pagos().obtener(it) }?.takeIf { it.deletedAt == null }
            val nuevoPago = if (input.cadaMes && pagoActual == null) UUID.randomUUID().toString() else null
            val nuevo = GastoEntity(
                id = id,
                codigo = previo?.codigo ?: codigo("G", db.gastos().maxCodigo()),
                fecha = input.fecha,
                concepto = input.concepto.trim(),
                tienda = tienda,
                categoriaId = categoria,
                importe = input.importe,
                meLoDevuelven = input.meLoDevuelven,
                personaId = if (input.meLoDevuelven) input.personaId else null,
                importeEsperado = if (input.meLoDevuelven) input.esperado else null,
                notas = input.notas.trim(),
                estado = previo?.estado ?: Estado.CONFIRMADO,
                createdAt = previo?.createdAt ?: ahora,
                updatedAt = ahora,
                deletedAt = previo?.deletedAt,
                pagoMensualId = nuevoPago ?: previo?.pagoMensualId,
            )
            if (nuevoPago != null) {
                db.pagos().guardar(
                    PagoMensualEntity(
                        id = nuevoPago, concepto = nuevo.concepto, tienda = nuevo.tienda, categoriaId = nuevo.categoriaId,
                        importe = nuevo.importe, dia = nuevo.fecha.dayOfMonth, createdAt = ahora, updatedAt = ahora,
                    ),
                )
            }
            db.gastos().guardar(nuevo)
            // Al editar y quitar «Se paga cada mes», el pago deja de repetirse (este gasto se queda).
            if (previo != null && !input.cadaMes && pagoActual != null) borrarPago(pagoActual.id)
            if (previo == null) {
                anotar("gasto", id, "creado", null, nuevo.codigo)
                if (input.meLoDevuelven && input.yaDevuelto) {
                    crearReembolso(id, input.esperado ?: input.importe, reloj.hoy(), input.personaId, "")
                }
            } else {
                anotarCambios(previo, nuevo)
            }
            // Con categoría: los gastos de la misma tienda que no tenían ninguna pasan a tenerla.
            if (categoria != null) ponerCategoriaATienda(tienda, categoria)
            id
        }
        return id
    }

    /** Grafía más usada de una tienda que ya existe (sin mayúsculas, tildes ni espacios); si no existe, la escrita. */
    private suspend fun grafiaDeTienda(tienda: String, excepto: String): String {
        val clave = claveTienda(tienda)
        if (clave.isEmpty()) return tienda
        return db.gastos().volcar().asSequence()
            .filter { it.id != excepto && it.deletedAt == null && claveTienda(it.tienda) == clave }
            .groupingBy { it.tienda.trim() }.eachCount()
            .maxByOrNull { it.value }?.key ?: tienda
    }

    /**
     * Une las tiendas que solo se diferencian en mayúsculas, tildes o espacios, con la grafía más usada.
     * Se llama al abrir la app. Devuelve cuántos gastos han cambiado.
     */
    suspend fun unificarTiendasIguales(): Int {
        val grupos = db.gastos().volcar().filter { it.deletedAt == null && it.tienda.isNotBlank() }
            .groupBy { claveTienda(it.tienda) }
            .values.map { gs -> gs.groupingBy { it.tienda.trim() }.eachCount() }
            .filter { it.size > 1 }
        var n = 0
        for (g in grupos) {
            val destino = g.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { e -> e.key.count { it.isUpperCase() } }).first().key
            n += unirTiendas(g.keys, destino)
        }
        return n
    }

    /** Categoría más reciente de una tienda (sin mayúsculas ni tildes), sin contar el gasto [excepto]. */
    private suspend fun categoriaDeTienda(tienda: String, excepto: String): String? {
        val clave = claveTexto(tienda)
        if (clave.isEmpty()) return null
        return db.gastos().volcar().asSequence()
            .filter { it.id != excepto && it.deletedAt == null && it.categoriaId != null && claveTexto(it.tienda) == clave }
            .maxByOrNull { it.updatedAt }?.categoriaId
    }

    private suspend fun ponerCategoriaATienda(tienda: String, categoria: String): Int {
        val clave = claveTexto(tienda)
        val ahora = reloj.ahoraMs()
        val sin = db.gastos().volcar().filter { it.deletedAt == null && it.categoriaId == null && claveTexto(it.tienda) == clave }
        sin.forEach { db.gastos().guardar(it.copy(categoriaId = categoria, updatedAt = ahora)) }
        return sin.size
    }

    /**
     * Pone categoría a los gastos que no tienen, copiando la que ya tiene su tienda en otros gastos
     * (por ejemplo, tras importar un Excel). Se llama al abrir la app. Devuelve cuántos ha cambiado.
     */
    suspend fun completarCategorias(): Int {
        val n = db.withTransaction {
            val todos = db.gastos().volcar().filter { it.deletedAt == null }
            val porTienda = todos.filter { it.categoriaId != null }.groupBy { claveTexto(it.tienda) }
                .mapValues { (_, gs) -> gs.maxBy { it.updatedAt }.categoriaId }
            val ahora = reloj.ahoraMs()
            val cambiar = todos.filter { it.categoriaId == null && porTienda[claveTexto(it.tienda)] != null }
            cambiar.forEach { db.gastos().guardar(it.copy(categoriaId = porTienda.getValue(claveTexto(it.tienda)), updatedAt = ahora)) }
            cambiar.size
        }
        if (n > 0) aviso.cambio()
        return n
    }

    /** Si el gasto viene de un pago mensual que sigue activo. */
    suspend fun esPagoMensual(g: GastoEntity): Boolean = g.pagoMensualId?.let { db.pagos().obtener(it) }?.let { it.deletedAt == null } == true

    // ---------- Varios a la vez (selección múltiple) ----------

    suspend fun borrarVarios(gastos: Collection<String>, ingresos: Collection<String>) = db.withTransaction {
        gastos.forEach { borrarGasto(it) }
        ingresos.forEach { borrarIngreso(it) }
    }

    suspend fun deshacerVarios(gastos: Collection<String>, ingresos: Collection<String>) = db.withTransaction {
        gastos.forEach { deshacerBorradoGasto(it) }
        ingresos.forEach { deshacerBorradoIngreso(it) }
    }

    /** Pone la misma categoría (o ninguna) a varios gastos. */
    suspend fun cambiarCategoria(gastos: Collection<String>, categoria: String?) = db.withTransaction {
        val ahora = reloj.ahoraMs()
        gastos.mapNotNull { db.gastos().obtener(it) }.forEach { g ->
            val nuevo = g.copy(categoriaId = categoria, updatedAt = ahora)
            db.gastos().guardar(nuevo)
            anotarCambios(g, nuevo)
        }
    }

    /** Pone la misma tienda a varios gastos. */
    suspend fun cambiarTienda(gastos: Collection<String>, tienda: String) = db.withTransaction {
        val t = tienda.trim()
        require(t.isNotEmpty())
        val ahora = reloj.ahoraMs()
        gastos.mapNotNull { db.gastos().obtener(it) }.forEach { g ->
            val nuevo = g.copy(tienda = t, updatedAt = ahora)
            db.gastos().guardar(nuevo)
            anotarCambios(g, nuevo)
        }
    }

    /** «Deshacer» tras editar: el gasto vuelve a como estaba. */
    suspend fun restaurarGasto(antes: GastoEntity) = db.withTransaction {
        db.gastos().guardar(antes.copy(updatedAt = reloj.ahoraMs()))
        anotar("gasto", antes.id, "cambios deshechos", null, null)
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
        val fijoActual = previo?.ingresoFijoId?.let { db.ingresosFijos().obtener(it) }?.takeIf { it.deletedAt == null }
        val nuevoFijo = if (input.cadaMes == true && fijoActual == null) UUID.randomUUID().toString() else null
        val quitarFijo = input.cadaMes == false && fijoActual != null
        val nuevo = IngresoEntity(
            id = id,
            codigo = previo?.codigo ?: codigo("I", db.ingresos().maxCodigo()),
            fecha = input.fecha, concepto = input.concepto.trim(), tipo = input.tipo, importe = input.importe,
            notas = input.notas.trim(), estado = Estado.CONFIRMADO,
            createdAt = previo?.createdAt ?: ahora, updatedAt = ahora, deletedAt = previo?.deletedAt,
            ingresoFijoId = if (quitarFijo) null else nuevoFijo ?: previo?.ingresoFijoId,
        )
        if (nuevoFijo != null) {
            db.ingresosFijos().guardar(
                IngresoFijoEntity(nuevoFijo, nuevo.concepto, nuevo.tipo, nuevo.importe, nuevo.fecha.dayOfMonth, createdAt = ahora, updatedAt = ahora),
            )
        }
        db.ingresos().guardar(nuevo)
        // Al editar y quitar «Se cobra cada mes», deja de repetirse (este ingreso se queda).
        if (quitarFijo) borrarIngresoFijo(fijoActual!!.id)
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

    // ---------- Ingresos fijos (se cobran cada mes) ----------

    fun ingresosFijos(): Flow<List<IngresoFijoEntity>> = db.ingresosFijos().todos()
    fun historialIngresoFijo(id: String): Flow<List<IngresoEntity>> = db.ingresosFijos().historial(id)
    suspend fun esIngresoFijo(i: IngresoEntity): Boolean = i.ingresoFijoId?.let { db.ingresosFijos().obtener(it) }?.let { it.deletedAt == null } == true

    suspend fun guardarIngresoFijo(f: IngresoFijoEntity) = db.withTransaction {
        val previo = db.ingresosFijos().obtener(f.id)
        db.ingresosFijos().guardar(f.copy(dia = f.dia.coerceIn(1, 31), updatedAt = reloj.ahoraMs()))
        // Los previstos aún sin confirmar se ajustan (o desaparecen si se pausa).
        db.ingresosFijos().previstosDe(f.id).forEach { iid ->
            val i = db.ingresos().obtener(iid) ?: return@forEach
            if (!f.activo) { db.ingresos().eliminar(listOf(iid)); db.historial().eliminarDe(listOf(iid)); return@forEach }
            val ym = YearMonth.from(i.fecha)
            db.ingresos().guardar(
                i.copy(importe = f.importe, concepto = f.concepto, tipo = f.tipo, fecha = ym.atDay(f.dia.coerceIn(1, ym.lengthOfMonth())), updatedAt = reloj.ahoraMs()),
            )
        }
        if (previo == null) aviso.cambio()
    }

    /** Deja de existir; los ingresos ya cobrados se quedan, los previstos se quitan. */
    suspend fun borrarIngresoFijo(id: String) = db.withTransaction {
        val f = db.ingresosFijos().obtener(id) ?: return@withTransaction
        db.ingresosFijos().guardar(f.copy(deletedAt = reloj.ahoraMs(), activo = false, updatedAt = reloj.ahoraMs()))
        val previstos = db.ingresosFijos().previstosDe(id)
        db.ingresos().eliminar(previstos)
        db.historial().eliminarDe(previstos)
    }

    /** Crea el ingreso PREVISTO de cada fijo activo para el mes de [hoy] si aún no tiene ninguno (tampoco uno borrado). */
    suspend fun asegurarIngresosPrevistos(hoy: LocalDate) = db.withTransaction {
        val ym = YearMonth.from(hoy)
        for (f in db.ingresosFijos().activos()) {
            if (db.ingresosFijos().ingresosEnMes(f.id, ym.atDay(1), ym.atEndOfMonth()) > 0) continue
            // Un sueldo fijo no se suma al sueldo habitual previsto si ya está.
            if (f.tipo == TipoIngreso.SUELDO && db.ingresos().contarTipoEntre(TipoIngreso.SUELDO, ym.atDay(1), ym.atEndOfMonth()) > 0) continue
            val ahora = reloj.ahoraMs()
            val i = IngresoEntity(
                id = UUID.randomUUID().toString(), codigo = codigo("I", db.ingresos().maxCodigo()),
                fecha = ym.atDay(f.dia.coerceIn(1, ym.lengthOfMonth())), concepto = f.concepto, tipo = f.tipo,
                importe = f.importe, estado = Estado.PREVISTO, createdAt = ahora, updatedAt = ahora, ingresoFijoId = f.id,
            )
            db.ingresos().guardar(i)
            db.historial().insertar(listOf(HistorialEntity(0, "ingreso", i.id, "previsto", null, i.codigo, Origen.MOVIL, ahora)))
        }
    }

    /** Confirma el cobro del mes. Si el importe cambia, el fijo pasa a tener ese importe. */
    suspend fun confirmarIngresoFijo(ingresoId: String, importe: Long) = db.withTransaction {
        val i = requireNotNull(db.ingresos().obtener(ingresoId))
        confirmarIngreso(ingresoId, i.fecha, importe)
        val fid = i.ingresoFijoId ?: return@withTransaction
        val f = db.ingresosFijos().obtener(fid) ?: return@withTransaction
        if (f.importe != importe) db.ingresosFijos().guardar(f.copy(importe = importe, updatedAt = reloj.ahoraMs()))
    }

    /** «Este mes no»: el previsto se quita (queda marcado para no volver a crearse este mes). */
    suspend fun omitirIngreso(ingresoId: String) = db.withTransaction {
        db.ingresos().marcarBorrado(ingresoId, reloj.ahoraMs())
        anotar("ingreso", ingresoId, "este mes no", null, null)
    }

    // ---------- Personas ----------

    suspend fun crearPersona(nombre: String): String {
        val p = PersonaEntity(UUID.randomUUID().toString(), nombre.trim(), reloj.ahoraMs())
        db.personas().guardar(p)
        return p.id
    }

    suspend fun renombrarPersona(p: PersonaEntity, nombre: String) {
        db.personas().guardar(p.copy(nombre = nombre.trim()))
        aviso.cambio()
    }
    suspend fun borrarPersona(p: PersonaEntity) = db.personas().guardar(p.copy(deletedAt = reloj.ahoraMs()))

    // ---------- Pagos mensuales ----------

    fun pagos(): Flow<List<PagoMensualEntity>> = db.pagos().todos()
    fun historialPago(id: String): Flow<List<GastoEntity>> = db.pagos().historial(id)

    suspend fun guardarPago(p: PagoMensualEntity) = db.withTransaction {
        val previo = db.pagos().obtener(p.id)
        db.pagos().guardar(p.copy(dia = p.dia.coerceIn(1, 31), updatedAt = reloj.ahoraMs()))
        // Los previstos aún sin confirmar se ajustan al nuevo precio y día (o desaparecen si se pausa).
        db.pagos().previstosDe(p.id).forEach { gid ->
            val g = db.gastos().obtener(gid) ?: return@forEach
            val ym = YearMonth.from(g.fecha)
            // En pausa, o ese mes ya no le toca (pasó a trimestral o anual): el previsto desaparece.
            if (!p.activo || !io.github.alexcorrea2001lemon.nummia.domain.calculo.tocaEnMes(p.cadaMeses, p.mesDesde, ym)) {
                db.gastos().eliminar(listOf(gid)); db.historial().eliminarDe(listOf(gid)); return@forEach
            }
            db.gastos().guardar(
                g.copy(
                    importe = p.importe, concepto = p.concepto, tienda = p.tienda, categoriaId = p.categoriaId,
                    fecha = ym.atDay(p.dia.coerceIn(1, ym.lengthOfMonth())), updatedAt = reloj.ahoraMs(),
                ),
            )
        }
        if (previo == null) aviso.cambio()
    }

    /** Deja de existir el pago; los gastos ya confirmados se quedan, los previstos se quitan. */
    suspend fun borrarPago(id: String) = db.withTransaction {
        val p = db.pagos().obtener(id) ?: return@withTransaction
        db.pagos().guardar(p.copy(deletedAt = reloj.ahoraMs(), activo = false, updatedAt = reloj.ahoraMs()))
        val previstos = db.pagos().previstosDe(id)
        db.gastos().eliminar(previstos)
        db.historial().eliminarDe(previstos)
    }

    /**
     * Crea el gasto PREVISTO de cada pago activo para el mes de [hoy], si ese mes aún no tiene ninguno
     * (tampoco uno borrado: «este mes no» se respeta). No cuenta en las cifras hasta que se confirma.
     */
    suspend fun asegurarPagosPrevistos(hoy: LocalDate) = db.withTransaction {
        val ym = YearMonth.from(hoy)
        for (p in db.pagos().activos()) {
            // Trimestrales y anuales: solo en sus meses.
            if (!io.github.alexcorrea2001lemon.nummia.domain.calculo.tocaEnMes(p.cadaMeses, p.mesDesde, ym)) continue
            if (db.pagos().gastosEnMes(p.id, ym.atDay(1), ym.atEndOfMonth()) > 0) continue
            val ahora = reloj.ahoraMs()
            val g = GastoEntity(
                id = UUID.randomUUID().toString(), codigo = codigo("G", db.gastos().maxCodigo()),
                fecha = ym.atDay(p.dia.coerceIn(1, ym.lengthOfMonth())), concepto = p.concepto, tienda = p.tienda,
                categoriaId = p.categoriaId, importe = p.importe, estado = Estado.PREVISTO,
                createdAt = ahora, updatedAt = ahora, pagoMensualId = p.id,
            )
            db.gastos().guardar(g)
            db.historial().insertar(listOf(HistorialEntity(0, "gasto", g.id, "previsto", null, g.codigo, Origen.MOVIL, ahora)))
        }
    }

    /** Confirma el pago del mes. Si el importe cambia, el pago mensual pasa a tener ese precio. */
    suspend fun confirmarPago(gastoId: String, importe: Long) = db.withTransaction {
        val g = requireNotNull(db.gastos().obtener(gastoId))
        confirmarGasto(gastoId, g.fecha, importe)
        val pid = g.pagoMensualId ?: return@withTransaction
        val p = db.pagos().obtener(pid) ?: return@withTransaction
        if (p.importe != importe) db.pagos().guardar(p.copy(importe = importe, updatedAt = reloj.ahoraMs()))
    }

    /**
     * Botón «Confirmar» de una notificación: solo si sigue previsto. Si ya lo confirmaste en la app,
     * lo quitaste («Este mes no») o ya no existe, no se toca. Devuelve si se confirmó.
     */
    suspend fun confirmarPagoDesdeAviso(gastoId: String, importe: Long): Boolean = db.withTransaction {
        val g = db.gastos().obtener(gastoId)
        if (g == null || g.deletedAt != null || g.estado != Estado.PREVISTO || importe <= 0) return@withTransaction false
        confirmarPago(gastoId, importe)
        true
    }

    /** Igual para el sueldo previsto: se cobra hoy si es su mes; si no, el último día de su mes. */
    suspend fun confirmarSueldoDesdeAviso(ingresoId: String, importe: Long): Boolean = db.withTransaction {
        val i = db.ingresos().obtener(ingresoId)
        if (i == null || i.deletedAt != null || i.estado != Estado.PREVISTO) return@withTransaction false
        val hoy = reloj.hoy()
        val dia = if (YearMonth.from(hoy) == YearMonth.from(i.fecha)) hoy else YearMonth.from(i.fecha).atEndOfMonth()
        confirmarIngreso(ingresoId, dia, if (importe > 0) importe else i.importe)
        true
    }

    /** «Este mes no»: el previsto se quita (queda marcado para no volver a crearse este mes). */
    suspend fun omitirPago(gastoId: String) = db.withTransaction {
        db.gastos().marcarBorrado(gastoId, reloj.ahoraMs())
        anotar("gasto", gastoId, "este mes no", null, null)
    }

    // ---------- Tiendas ----------

    /** Tiendas con cuántas veces y cuánto (gasto real) desde siempre. */
    fun tiendas(): Flow<List<TiendaResumen>> = db.gastos().todos().map { gs ->
        gs.filter { it.gasto.estado == Estado.CONFIRMADO }
            .groupBy { it.gasto.tienda.trim() }
            .filterKeys { it.isNotEmpty() }
            .map { (n, l) -> TiendaResumen(n, l.size, l.sumOf { it.real }, l.maxOf { it.gasto.fecha }) }
            .sortedByDescending { it.veces }
    }

    /**
     * Une varias tiendas en una (o renombra una): cambia los gastos, los pagos mensuales y los presupuestos.
     * Devuelve cuántos gastos han cambiado.
     */
    suspend fun unirTiendas(origen: Collection<String>, destino: String): Int {
        val nuevo = destino.trim()
        require(nuevo.isNotEmpty())
        val nombres = origen.map { it.trim() }.toSet() - nuevo
        if (nombres.isEmpty()) return 0
        val n = db.withTransaction {
            val ahora = reloj.ahoraMs()
            val cambiados = db.gastos().volcar().filter { it.tienda.trim() in nombres }
            cambiados.forEach { g -> db.gastos().guardar(g.copy(tienda = nuevo, updatedAt = ahora)) }
            db.historial().insertar(cambiados.map { HistorialEntity(0, "gasto", it.id, "tienda", it.tienda, nuevo, Origen.MOVIL, ahora) })
            db.pagos().volcar().filter { it.tienda.trim() in nombres }.forEach { db.pagos().guardar(it.copy(tienda = nuevo, updatedAt = ahora)) }
            db.presupuestos().volcar().filter { p -> p.ambito == Ambito.TIENDA.name && p.ref?.trim()?.let { it in nombres } == true }
                .forEach { db.presupuestos().guardar(it.copy(ref = nuevo, updatedAt = ahora)) }
            cambiados.size
        }
        if (n > 0) aviso.cambio()
        return n
    }

    /** Ficha de una tienda: todos sus gastos confirmados (sin mayúsculas ni tildes). */
    fun gastosDeTienda(nombre: String): Flow<List<GastoConReembolsos>> {
        val clave = claveTienda(nombre)
        return db.gastos().todos().map { l -> l.filter { it.gasto.estado == Estado.CONFIRMADO && claveTienda(it.gasto.tienda) == clave } }
    }

    // ---------- Categorías ----------

    suspend fun crearCategoria(nombre: String, color: Long, icono: String = "more"): String {
        val todas = db.categorias().volcar()
        todas.firstOrNull { claveTexto(it.nombre) == claveTexto(nombre) }?.let { return it.id }
        val c = CategoriaEntity(UUID.randomUUID().toString(), nombre.trim(), icono, color, false, (todas.maxOfOrNull { it.orden } ?: 0) + 1)
        db.categorias().guardar(c)
        return c.id
    }

    /** Guarda el orden elegido arrastrando (el de los chips del formulario y las listas). */
    /** «Repetir hoy»: un gasto nuevo igual (concepto, tienda, categoría, importe y a quién), sin notas ni devoluciones. */
    suspend fun repetirGasto(id: String, fecha: LocalDate): String {
        val g = requireNotNull(db.gastos().obtener(id))
        return guardarGasto(
            GastoInput(
                fecha = fecha, concepto = g.concepto, tienda = g.tienda, categoriaId = g.categoriaId, importe = g.importe,
                meLoDevuelven = g.meLoDevuelven, personaId = g.personaId, esperado = g.importeEsperado,
            ),
        )
    }

    suspend fun ordenarCategorias(ids: List<String>) = db.withTransaction {
        val todas = db.categorias().volcar().associateBy { it.id }
        ids.forEachIndexed { n, id -> todas[id]?.takeIf { it.orden != n }?.let { db.categorias().guardar(it.copy(orden = n)) } }
    }

    suspend fun editarCategoria(c: CategoriaEntity) {
        db.categorias().guardar(c.copy(nombre = c.nombre.trim()))
        aviso.cambio() // el nombre sale en la hoja de Drive
    }

    /** Solo las creadas por ti. Sus gastos se quedan sin categoría. */
    suspend fun borrarCategoria(c: CategoriaEntity) {
        if (c.predefinida) return
        db.withTransaction {
            db.gastos().quitarCategoria(c.id, reloj.ahoraMs())
            db.pagos().volcar().filter { it.categoriaId == c.id }.forEach { db.pagos().guardar(it.copy(categoriaId = null)) }
            db.presupuestos().volcar().filter { it.ambito == Ambito.CATEGORIA.name && it.ref == c.id }.forEach { db.presupuestos().borrar(it.id) }
            db.categorias().borrar(c.id)
        }
        aviso.cambio()
    }

    // ---------- Presupuestos ----------

    fun presupuestos(): Flow<List<PresupuestoEntity>> = db.presupuestos().todos()
    suspend fun guardarPresupuesto(p: PresupuestoEntity) = db.presupuestos().guardar(p.copy(updatedAt = reloj.ahoraMs()))
    suspend fun borrarPresupuesto(id: String) = db.presupuestos().borrar(id)

    /** Cómo va cada presupuesto viendo el mes [ym] (los semanales, su semana; los anuales, el año de [ym]). */
    fun estadoPresupuestos(ym: YearMonth): Flow<List<EstadoPresupuesto>> {
        val hoy = reloj.hoy()
        val semana = io.github.alexcorrea2001lemon.nummia.domain.calculo.rangoPresupuesto(io.github.alexcorrea2001lemon.nummia.domain.calculo.Periodo.SEMANA, ym, hoy)
        val desde = minOf(LocalDate.of(ym.year, 1, 1), semana.start)
        val hasta = maxOf(LocalDate.of(ym.year, 12, 31), semana.endInclusive)
        return combine(presupuestos(), periodo(desde, hasta)) { ps, d ->
            estadoPresupuestos(
                ps.map { it.aLimite() },
                d.gastos.map { GastoDePresupuesto(it.aMov(), it.gasto.tienda, it.gasto.categoriaId) },
                d.movReembolsos, ym, hoy,
            )
        }
    }

    suspend fun estadoPresupuestosAhora(ym: YearMonth): List<EstadoPresupuesto> = estadoPresupuestos(ym).first()

    // ---------- Papelera (30 días) ----------

    fun papeleraGastos(): Flow<List<GastoConReembolsos>> = db.gastos().borrados().map { l -> l.filter { it.gasto.estado == Estado.CONFIRMADO } }
    fun papeleraIngresos(): Flow<List<IngresoEntity>> = db.ingresos().borrados()

    /** Borra para siempre (con sus reembolsos e historial). */
    suspend fun eliminarDefinitivo(gastos: List<String>, ingresos: List<String>) = db.withTransaction {
        if (gastos.isNotEmpty()) {
            db.reembolsos().eliminarDeGastos(gastos)
            db.gastos().eliminar(gastos)
        }
        if (ingresos.isNotEmpty()) db.ingresos().eliminar(ingresos)
        db.historial().eliminarDe(gastos + ingresos)
    }

    /** Lo que lleva más de [dias] días en la papelera desaparece. Se llama al abrir la app. */
    suspend fun purgarPapelera(dias: Int = DIAS_PAPELERA) {
        val limite = reloj.ahoraMs() - dias * 86_400_000L
        // Los previstos omitidos («este mes no») se guardan 62 días para que no se vuelvan a crear ese mes.
        val limitePrevistos = reloj.ahoraMs() - 62 * 86_400_000L
        val gastos = db.gastos().volcar().filter { g ->
            val b = g.deletedAt ?: return@filter false
            if (g.estado == Estado.PREVISTO) b < limitePrevistos else b < limite
        }.map { it.id }
        eliminarDefinitivo(gastos, db.ingresos().borradosAntesDe(limite))
        db.reembolsos().eliminarBorradosAntesDe(limite)
    }

    // ---------- Historial ----------

    private suspend fun anotar(entidad: String, id: String, campo: String, antes: String?, despues: String?) {
        db.historial().insertar(listOf(HistorialEntity(0, entidad, id, campo, antes, despues, Origen.MOVIL, reloj.ahoraMs())))
        aviso.cambio()
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
