package io.github.alexcorrea2001lemon.nummia.data.drive

import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaLocal
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface ResultadoSync {
    data object NoConectado : ResultadoSync
    data class Hecho(val conflictos: Int, val avisos: List<String>) : ResultadoSync
    data class Fallo(val mensaje: String, val reintentar: Boolean) : ResultadoSync
}

/**
 * Sincroniza el móvil con Drive: carpeta `Nummia_Datos`, una hoja por año y copias de seguridad.
 * Orden seguro: 1) ids de las filas nuevas del ordenador → hoja; 2) cambios → móvil;
 * 3) cambios → hoja; 4) solo entonces se guarda la instantánea. Si algo falla, no se pierde nada.
 */
class Sincronizador(
    private val db: NummiaDb,
    private val sync: SyncDao,
    private val reloj: Reloj,
    private val copia: CopiaLocal,
    private val nube: (correo: String) -> Nube,
) {
    private val datos = DatosLocales(db, reloj)
    private val cerrojo = Mutex()
    private val _ocupado = MutableStateFlow(false)

    /** true mientras se sincroniza (para el icono de Inicio y la pantalla de Drive). */
    val ocupado: StateFlow<Boolean> = _ocupado

    fun metaFlujo() = sync.metaFlujo()
    fun conflictos() = sync.conflictos()

    suspend fun correo(): String? = sync.meta(Meta.CUENTA)

    suspend fun conectar(correo: String) = sync.guardarMeta(MetaEntity(Meta.CUENTA, correo))

    suspend fun desconectar() {
        listOf(Meta.CUENTA, Meta.CARPETA, Meta.CARPETA_COPIAS, Meta.ULTIMA, Meta.ERROR, Meta.PENDIENTE, Meta.FORZAR_SUBIDA, Meta.AVISOS).forEach { sync.borrarMeta(it) }
        sync.vaciarHojas(); sync.vaciarInstantaneas(); sync.vaciarConflictos()
    }

    suspend fun marcarCambio() {
        sync.guardarMeta(MetaEntity(Meta.PENDIENTE, "1"))
        sync.guardarMeta(MetaEntity(Meta.CAMBIOS_DESDE_COPIA, "1"))
    }

    /** Tras restaurar una copia: la hoja se rehace igual que el móvil en la próxima sincronización. */
    suspend fun trasRestaurar() {
        sync.vaciarInstantaneas(); sync.vaciarConflictos()
        sync.guardarMeta(MetaEntity(Meta.FORZAR_SUBIDA, "1"))
        marcarCambio()
    }

    suspend fun sincronizar(): ResultadoSync = cerrojo.withLock {
        val correo = correo() ?: return ResultadoSync.NoConectado
        _ocupado.value = true
        try {
            val n = nube(correo)
            val carpeta = carpeta(n)
            val forzar = sync.meta(Meta.FORZAR_SUBIDA) == "1"
            val hojas = descubrirHojas(n, carpeta)
            val avisos = ArrayList<String>()
            var conflictos = 0
            for (anio in (datos.anios() + hojas.keys).sorted()) {
                conflictos += anioSync(n, carpeta, anio, hojas[anio], forzar, avisos)
            }
            sync.borrarMeta(Meta.FORZAR_SUBIDA); sync.borrarMeta(Meta.PENDIENTE); sync.borrarMeta(Meta.ERROR)
            if (avisos.isEmpty()) sync.borrarMeta(Meta.AVISOS) else sync.guardarMeta(MetaEntity(Meta.AVISOS, avisos.joinToString("\n")))
            sync.guardarMeta(MetaEntity(Meta.ULTIMA, reloj.ahoraMs().toString()))
            copiaDiaria(n, carpeta)
            ResultadoSync.Hecho(conflictos, avisos)
        } catch (e: NecesitaReconectar) {
            fallo(e.message!!, false)
        } catch (e: ErrorDrive) {
            fallo(e.message ?: "Error de Google", e.codigo >= 500 || e.codigo == 429)
        } catch (e: IOException) {
            fallo("Sin conexión con Google. Se reintentará sola.", true)
        } finally {
            _ocupado.value = false
        }
    }

    private suspend fun fallo(m: String, reintentar: Boolean): ResultadoSync {
        sync.guardarMeta(MetaEntity(Meta.ERROR, m))
        return ResultadoSync.Fallo(m, reintentar)
    }

    // ---------------- Carpetas y hojas

    private suspend fun carpeta(n: Nube): String {
        sync.meta(Meta.CARPETA)?.let { if (n.modificado(it) != null) return it }
        val id = n.buscar(CARPETA, null, ApiGoogle.CARPETA).firstOrNull()?.id ?: n.crear(CARPETA, ApiGoogle.CARPETA, null)
        sync.guardarMeta(MetaEntity(Meta.CARPETA, id))
        return id
    }

    /** Hojas conocidas + las que ya existan en Drive (móvil nuevo o app reinstalada). */
    private suspend fun descubrirHojas(n: Nube, carpeta: String): Map<Int, HojaEntity> {
        val conocidas = sync.hojas().associateBy { it.anio }.toMutableMap()
        n.listar("'$carpeta' in parents and mimeType = '${ApiGoogle.HOJA}' and trashed = false").forEach { a ->
            val anio = Regex("""^Nummia_(\d{4})$""").find(a.nombre)?.groupValues?.get(1)?.toInt() ?: return@forEach
            if (conocidas[anio] == null) {
                conocidas[anio] = HojaEntity(anio, a.id, null).also { sync.guardarHoja(it) }
            }
        }
        return conocidas
    }

    private suspend fun crearHoja(n: Nube, carpeta: String, anio: Int): HojaEntity {
        val id = n.crear("Nummia_$anio", ApiGoogle.HOJA, carpeta)
        n.actualizar(id, peticionesHojaNueva())
        n.escribir(id, Pestana.entries.map { p -> "${p.nombre}!A1:${p.letraFinal}1" to listOf(p.columnas.map { JsonPrimitive(it.titulo) } + JsonPrimitive("id")) })
        n.escribir(id, listOf("Resumen!A1:F14" to resumen(anio)), formulas = true)
        n.actualizar(id, formatosHoja())
        return HojaEntity(anio, id, null).also { sync.guardarHoja(it) }
    }

    private suspend fun anioSync(n: Nube, carpeta: String, anio: Int, conocida: HojaEntity?, forzar: Boolean, avisos: MutableList<String>): Int {
        val locales = Pestana.entries.associateWith { datos.canon(it, anio) }
        val instantaneas = Pestana.entries.associateWith { p -> sync.instantanea(p.name, anio).associate { it.entidadId to it.firma } }
        if (conocida == null && locales.values.all { it.isEmpty() }) return 0

        var hoja = conocida ?: crearHoja(n, carpeta, anio)
        var forzarAnio = forzar
        var modificado = n.modificado(hoja.spreadsheetId)
        if (modificado == null) { // la hoja se borró en Drive: se rehace desde el móvil
            Pestana.entries.forEach { sync.borrarInstantanea(it.name, anio) }
            hoja = crearHoja(n, carpeta, anio)
            modificado = n.modificado(hoja.spreadsheetId)
            forzarAnio = true
        }
        val cambiosLocales = Pestana.entries.any { p -> locales.getValue(p).mapValues { firma(it.value) } != instantaneas.getValue(p) }
        if (!forzarAnio && !cambiosLocales && modificado == hoja.modificado) return 0
        sync.guardarMeta(MetaEntity(Meta.CAMBIOS_DESDE_COPIA, "1"))

        val id = hoja.spreadsheetId
        val valores = n.leer(id, Pestana.entries.map { "${it.nombre}!A:${it.letraFinal}" })
        var conflictos = 0
        val escrituras = ArrayList<Pair<String, List<List<JsonElement>>>>()
        val borrados = HashMap<Pestana, List<Int>>()
        val nuevasInstantaneas = HashMap<Pestana, Map<String, String>>()

        // Gastos primero: los reembolsos los buscan por código.
        for (p in listOf(Pestana.GASTOS, Pestana.REEMBOLSOS, Pestana.INGRESOS)) {
            val tabla = valores[Pestana.entries.indexOf(p)]
            val filas = leerFilas(p, tabla)
            val abiertos = sync.conflictosDe(p.name, anio).map { it.entidadId }.toSet()
            val local = datos.canon(p, anio)
            val plan = if (forzarAnio) reconciliarForzandoLocal(local, filas) else reconciliar(local, filas, instantaneas.getValue(p), abiertos)
            val instantanea = plan.instantanea.toMutableMap()

            // 1) Filas nuevas del ordenador: se les da id y código, se escriben en la hoja y luego se crean en el móvil.
            val nuevas = plan.nuevasDesdeRemoto.map { (fila, c) -> Triple(fila, java.util.UUID.randomUUID().toString(), c) }
            if (nuevas.isNotEmpty()) {
                n.escribir(id, nuevas.map { (fila, nid, _) -> "${p.nombre}!${p.letraFinal}$fila" to listOf(listOf(JsonPrimitive(nid))) })
            }
            // 2) Cambios hacia el móvil.
            for ((nid, c) in plan.aplicarLocal) if (datos.aplicar(p, nid, c) == null) avisos += "${p.nombre}: no se pudo aplicar una fila (¿código de gasto inexistente?)"
            plan.borrarLocal.forEach { datos.borrar(p, it) }
            for ((fila, nid, c) in nuevas) {
                val final = datos.aplicar(p, nid, c)
                if (final == null) { avisos += "${p.nombre}, fila $fila: el código del gasto no existe"; continue }
                if (datos.anioDe(p, nid) == anio) instantanea[nid] = firma(final)
                if (final != c) escrituras += "${p.nombre}!A$fila:${p.letraFinal}$fila" to listOf(filaParaEscribir(p, nid, final)) // código asignado
            }
            // 3) Cambios hacia la hoja.
            plan.actualizarRemoto.forEach { (fila, rid, c) -> escrituras += "${p.nombre}!A$fila:${p.letraFinal}$fila" to listOf(filaParaEscribir(p, rid, c)) }
            var siguiente = tabla.size.coerceAtLeast(1) + 1
            plan.anadirRemoto.forEach { (rid, c) -> escrituras += "${p.nombre}!A$siguiente:${p.letraFinal}$siguiente" to listOf(filaParaEscribir(p, rid, c)); siguiente++ }
            if (plan.borrarRemoto.isNotEmpty()) borrados[p] = plan.borrarRemoto
            plan.conflictos.forEach { c ->
                sync.guardarConflicto(ConflictoEntity(p.name, c.id, anio, c.local?.let(::firma), c.remoto?.let(::firma), reloj.ahoraMs()))
            }
            conflictos += plan.conflictos.size + abiertos.size
            plan.invalidas.forEach { (fila, motivo) -> avisos += "${p.nombre}, fila $fila: $motivo" }
            nuevasInstantaneas[p] = instantanea
        }

        n.escribir(id, escrituras)
        if (borrados.isNotEmpty()) {
            val ids = n.pestanas(id)
            n.actualizar(id, buildJsonArray {
                borrados.forEach { (p, filas) ->
                    filas.sortedDescending().forEach { f ->
                        add(buildJsonObject {
                            putJsonObject("deleteDimension") {
                                putJsonObject("range") { put("sheetId", ids.getValue(p.nombre)); put("dimension", "ROWS"); put("startIndex", f - 1); put("endIndex", f) }
                            }
                        })
                    }
                }
            })
        }
        // 4) Todo escrito: se guarda la instantánea y la fecha de la hoja.
        nuevasInstantaneas.forEach { (p, mapa) ->
            sync.borrarInstantanea(p.name, anio)
            sync.guardarInstantanea(mapa.map { InstantaneaEntity(p.name, it.key, anio, it.value) })
        }
        sync.guardarHoja(hoja.copy(modificado = n.modificado(id)))
        return conflictos
    }

    // ---------------- Conflictos

    /** [movil] = true → se queda la versión del móvil; false → la del ordenador. */
    suspend fun resolver(c: ConflictoEntity, movil: Boolean) {
        val p = Pestana.valueOf(c.pestana)
        if (!movil) {
            if (c.remoto == null) datos.borrar(p, c.entidadId) else datos.aplicar(p, c.entidadId, desdeFirma(c.remoto))
        }
        // La instantánea pasa a ser la versión del ordenador: si se quedó la del móvil, la próxima sincronización la sube.
        sync.borrarInstantaneaDe(p.name, c.entidadId)
        c.remoto?.let { sync.guardarInstantanea(listOf(InstantaneaEntity(p.name, c.entidadId, c.anio, it))) }
        sync.borrarConflicto(c.pestana, c.entidadId)
        marcarCambio()
    }

    // ---------------- Copias de seguridad

    private suspend fun carpetaCopias(n: Nube, carpeta: String): String {
        sync.meta(Meta.CARPETA_COPIAS)?.let { if (n.modificado(it) != null) return it }
        val id = n.buscar(CARPETA_COPIAS, carpeta, ApiGoogle.CARPETA).firstOrNull()?.id ?: n.crear(CARPETA_COPIAS, ApiGoogle.CARPETA, carpeta)
        sync.guardarMeta(MetaEntity(Meta.CARPETA_COPIAS, id))
        return id
    }

    private fun ahora(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(reloj.ahoraMs()), ZoneId.systemDefault())

    private suspend fun copiaDiaria(n: Nube, carpeta: String) {
        if (sync.meta(Meta.CAMBIOS_DESDE_COPIA) != "1" || datos.vacia()) return
        val ultima = sync.meta(Meta.ULTIMA_COPIA)?.toLongOrNull()?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).toLocalDate() }
        if (ultima == reloj.hoy()) return
        crearCopia(n, carpeta)
    }

    private suspend fun crearCopia(n: Nube, carpeta: String) {
        val dir = carpetaCopias(n, carpeta)
        n.subir(nombreCopia(ahora()), dir, copia.exportar(reloj.ahoraMs()))
        sync.guardarMeta(MetaEntity(Meta.ULTIMA_COPIA, reloj.ahoraMs().toString()))
        sync.borrarMeta(Meta.CAMBIOS_DESDE_COPIA)
        copiasABorrar(n.listar("'$dir' in parents and trashed = false").map { it.id to fechaDeCopia(it.nombre) }).forEach { runCatching { n.borrar(it) } }
    }

    /** Copia manual o antes de algo arriesgado. No hace nada si no hay Drive conectado. */
    suspend fun copiaAhora(): Result<Unit> = runCatching {
        val correo = correo() ?: return@runCatching
        val n = nube(correo)
        crearCopia(n, carpeta(n))
    }

    suspend fun copias(): List<ArchivoDrive> {
        val correo = correo() ?: return emptyList()
        val n = nube(correo)
        val dir = carpetaCopias(n, carpeta(n))
        return n.listar("'$dir' in parents and trashed = false", "name desc")
    }

    suspend fun restaurar(archivo: ArchivoDrive) {
        val correo = correo() ?: return
        val n = nube(correo)
        val c = copia.leer(n.descargar(archivo.id))
        runCatching { crearCopia(n, carpeta(n)) }
        copia.restaurar(c)
        trasRestaurar()
    }

    suspend fun datosVacios(): Boolean = datos.vacia()

    companion object {
        const val CARPETA = "Nummia_Datos"
        const val CARPETA_COPIAS = "Copias_de_seguridad"
        private val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

        /** Pestaña Resumen con fórmulas (hoja en español: argumentos separados por «;»). */
        fun resumen(anio: Int): List<List<JsonElement>> {
            fun t(s: String) = JsonPrimitive(s)
            val filas = mutableListOf<List<JsonElement>>(listOf(t("Mes"), t("Ingresos"), t("Pagado"), t("Reembolsos"), t("Gasto real"), t("Balance")))
            MESES.forEachIndexed { i, nombre ->
                val m = i + 1
                val f = i + 2
                val desde = "DATE($anio;$m;1)"
                val hasta = "EDATE(DATE($anio;$m;1);1)"
                filas += listOf(
                    t(nombre),
                    t("=SUMIFS(Ingresos!E:E;Ingresos!B:B;\">=\"&$desde;Ingresos!B:B;\"<\"&$hasta)"),
                    t("=SUMIFS(Gastos!E:E;Gastos!B:B;\">=\"&$desde;Gastos!B:B;\"<\"&$hasta)"),
                    t("=SUMPRODUCT((Gastos!B2:B>=$desde)*(Gastos!B2:B<$hasta)*SUMIFS(Reembolsos!D:D;Reembolsos!B:B;Gastos!A2:A))"),
                    t("=C$f-D$f"),
                    t("=B$f-E$f"),
                )
            }
            filas += listOf(t("Total"), t("=SUM(B2:B13)"), t("=SUM(C2:C13)"), t("=SUM(D2:D13)"), t("=SUM(E2:E13)"), t("=SUM(F2:F13)"))
            return filas
        }

        private fun peticionesHojaNueva(): JsonArray = buildJsonArray {
            add(buildJsonObject {
                putJsonObject("updateSpreadsheetProperties") {
                    putJsonObject("properties") { put("locale", "es_ES"); put("timeZone", "Europe/Madrid") }
                    put("fields", "locale,timeZone")
                }
            })
            add(buildJsonObject {
                putJsonObject("updateSheetProperties") {
                    putJsonObject("properties") { put("sheetId", 0); put("title", "Resumen"); putJsonObject("gridProperties") { put("frozenRowCount", 1) } }
                    put("fields", "title,gridProperties.frozenRowCount")
                }
            })
            Pestana.entries.forEachIndexed { i, p ->
                add(buildJsonObject {
                    putJsonObject("addSheet") {
                        putJsonObject("properties") { put("sheetId", i + 1); put("title", p.nombre); putJsonObject("gridProperties") { put("frozenRowCount", 1) } }
                    }
                })
            }
        }

        private fun formatosHoja(): JsonArray = buildJsonArray {
            fun rango(hoja: Int, c0: Int, c1: Int, f0: Int = 1) = buildJsonObject {
                put("sheetId", hoja); put("startRowIndex", f0); put("startColumnIndex", c0); put("endColumnIndex", c1)
            }
            fun formato(hoja: Int, col: Int, tipo: String, patron: String) = add(buildJsonObject {
                putJsonObject("repeatCell") {
                    put("range", rango(hoja, col, col + 1))
                    putJsonObject("cell") { putJsonObject("userEnteredFormat") { putJsonObject("numberFormat") { put("type", tipo); put("pattern", patron) } } }
                    put("fields", "userEnteredFormat.numberFormat")
                }
            })
            fun negrita(hoja: Int) = add(buildJsonObject {
                putJsonObject("repeatCell") {
                    put("range", buildJsonObject { put("sheetId", hoja); put("startRowIndex", 0); put("endRowIndex", 1) })
                    putJsonObject("cell") { putJsonObject("userEnteredFormat") { putJsonObject("textFormat") { put("bold", true) } } }
                    put("fields", "userEnteredFormat.textFormat.bold")
                }
            })
            fun lista(hoja: Int, col: Int, valores: List<String>) = add(buildJsonObject {
                putJsonObject("setDataValidation") {
                    put("range", rango(hoja, col, col + 1))
                    putJsonObject("rule") {
                        putJsonObject("condition") {
                            put("type", "ONE_OF_LIST")
                            putJsonArray("values") { valores.forEach { v -> add(buildJsonObject { put("userEnteredValue", v) }) } }
                        }
                        put("showCustomUi", true); put("strict", false)
                    }
                }
            })
            val euros = "#,##0.00 \"€\""
            negrita(0)
            (1..5).forEach { formato(0, it, "CURRENCY", euros) }
            Pestana.entries.forEachIndexed { i, p ->
                val hoja = i + 1
                negrita(hoja)
                p.columnas.forEachIndexed { c, col ->
                    when (col.tipo) {
                        TipoCelda.FECHA -> formato(hoja, c, "DATE", "dd/mm/yyyy")
                        TipoCelda.IMPORTE, TipoCelda.IMPORTE_OPCIONAL -> formato(hoja, c, "CURRENCY", euros)
                        TipoCelda.SI_NO -> lista(hoja, c, listOf("Sí", "No"))
                        TipoCelda.TEXTO -> Unit
                    }
                }
                if (p == Pestana.INGRESOS) lista(hoja, 3, TIPOS_EN_HOJA.values.toList())
                add(buildJsonObject {
                    putJsonObject("updateDimensionProperties") {
                        putJsonObject("range") { put("sheetId", hoja); put("dimension", "COLUMNS"); put("startIndex", p.columnaId); put("endIndex", p.columnaId + 1) }
                        putJsonObject("properties") { put("hiddenByUser", true) }
                        put("fields", "hiddenByUser")
                    }
                })
            }
        }
    }
}
