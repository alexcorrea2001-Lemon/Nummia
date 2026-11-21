package io.github.alexcorrea2001lemon.nummia.ui.importar

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaLocal
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador
import io.github.alexcorrea2001lemon.nummia.data.importar.AnalisisExcel
import io.github.alexcorrea2001lemon.nummia.data.importar.Celda
import io.github.alexcorrea2001lemon.nummia.data.importar.ExcelAnual
import io.github.alexcorrea2001lemon.nummia.data.importar.ExcelNoValido
import io.github.alexcorrea2001lemon.nummia.data.importar.ImportadorExcel
import io.github.alexcorrea2001lemon.nummia.data.importar.LectorXlsx
import io.github.alexcorrea2001lemon.nummia.data.importar.ResultadoImportacion
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

sealed interface EstadoImportar {
    data object Inicial : EstadoImportar
    data object Leyendo : EstadoImportar
    data class Vista(
        val archivo: String,
        val analisis: AnalisisExcel,
        val yaHay: Int,
        /** Índice del gasto → persona (o null si aún no se ha elegido quién). */
        val porOtros: Map<Int, String?> = emptyMap(),
    ) : EstadoImportar
    data object Importando : EstadoImportar
    data class Hecho(val r: ResultadoImportacion, val anio: Int) : EstadoImportar
    data class Error(val mensaje: String) : EstadoImportar
}

@HiltViewModel
class ImportarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importador: ImportadorExcel,
    private val repo: MovimientosRepo,
    private val copia: CopiaLocal,
    private val sinc: Sincronizador,
    private val reloj: Reloj,
) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoImportar>(EstadoImportar.Inicial)
    val estado: StateFlow<EstadoImportar> = _estado.asStateFlow()
    val personas: StateFlow<List<PersonaEntity>> = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var celdas: Map<String, Celda> = emptyMap()

    fun elegir(uri: Uri) = viewModelScope.launch {
        _estado.value = EstadoImportar.Leyendo
        _estado.value = try {
            val nombre = nombreDe(uri)
            celdas = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw ExcelNoValido("No se pudo abrir el archivo.")
                LectorXlsx.leer(bytes)
            }
            vista(nombre, ExcelAnual.anioDelNombre(nombre) ?: reloj.hoy().year)
        } catch (e: ExcelNoValido) {
            EstadoImportar.Error(e.message ?: "No se pudo leer el archivo.")
        } catch (e: Exception) {
            EstadoImportar.Error("No se pudo leer el archivo. ¿Es un .xlsx?")
        }
    }

    private suspend fun vista(nombre: String, anio: Int): EstadoImportar {
        val a = withContext(Dispatchers.Default) { ExcelAnual.analizar(celdas, anio) }
        if (a.vacio) return EstadoImportar.Error("No se ha encontrado ningún gasto ni ingreso con la estructura esperada (gastos en las filas 9–23 y 38–52).")
        return EstadoImportar.Vista(nombre, a, importador.gastosEnElAnio(anio))
    }

    fun cambiarAnio(anio: Int) = viewModelScope.launch {
        val v = _estado.value as? EstadoImportar.Vista ?: return@launch
        _estado.value = vista(v.archivo, anio)
    }

    fun porOtro(indice: Int, persona: String?, marcado: Boolean) {
        val v = _estado.value as? EstadoImportar.Vista ?: return
        _estado.value = v.copy(porOtros = if (marcado) v.porOtros + (indice to persona) else v.porOtros - indice)
    }

    fun nuevaPersona(nombre: String, indice: Int) = viewModelScope.launch {
        val id = repo.crearPersona(nombre)
        porOtro(indice, id, true)
    }

    fun importar() = viewModelScope.launch {
        val v = _estado.value as? EstadoImportar.Vista ?: return@launch
        _estado.value = EstadoImportar.Importando
        _estado.value = runCatching {
            // Antes de importar: copia en el móvil y, si está conectado, en tu Drive.
            withContext(Dispatchers.IO) {
                val dir = File(context.filesDir, "copias").apply { mkdirs() }
                File(dir, "antes_de_importar_excel_${reloj.ahoraMs()}.nummia").writeBytes(copia.exportar(reloj.ahoraMs()))
            }
            withTimeoutOrNull(20_000) { sinc.copiaAhora() }
            EstadoImportar.Hecho(importador.importar(v.analisis, v.porOtros), v.analisis.anio)
        }.getOrElse { EstadoImportar.Error("No se pudo importar. No se ha guardado nada.") }
    }

    fun reiniciar() { _estado.value = EstadoImportar.Inicial }

    private fun nombreDe(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: "archivo.xlsx"
}
