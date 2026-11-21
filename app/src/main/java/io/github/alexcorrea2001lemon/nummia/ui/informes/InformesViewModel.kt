package io.github.alexcorrea2001lemon.nummia.ui.informes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador
import io.github.alexcorrea2001lemon.nummia.data.informes.Contenido
import io.github.alexcorrea2001lemon.nummia.data.informes.EscritorXlsx
import io.github.alexcorrea2001lemon.nummia.data.informes.Formato
import io.github.alexcorrea2001lemon.nummia.data.informes.GeneradorPdf
import io.github.alexcorrea2001lemon.nummia.data.informes.construirInforme
import io.github.alexcorrea2001lemon.nummia.data.informes.hojasDeInforme
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ArchivoInforme(val archivo: File, val formato: Formato) {
    val nombre: String get() = archivo.name
}

data class EstadoInformes(
    val formato: Formato = Formato.PDF,
    val generando: Boolean = false,
    val ultimo: ArchivoInforme? = null,
    val vistaPrevia: Bitmap? = null,
    val historial: List<ArchivoInforme> = emptyList(),
    val driveConectado: Boolean = false,
    val mensaje: String? = null,
)

@HiltViewModel
class InformesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MovimientosRepo,
    private val sinc: Sincronizador,
    private val reloj: Reloj,
) : ViewModel() {
    private val dir = File(context.filesDir, "informes")
    private val _e = MutableStateFlow(EstadoInformes())
    val estado: StateFlow<EstadoInformes> = _e.asStateFlow()
    val personas: StateFlow<List<PersonaEntity>> = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _e.update { it.copy(driveConectado = sinc.correo() != null) }
            cargarHistorial()
        }
    }

    fun formato(f: Formato) = _e.update { it.copy(formato = f) }
    fun mensajeVisto() = _e.update { it.copy(mensaje = null) }

    private suspend fun cargarHistorial() {
        val lista = withContext(Dispatchers.IO) {
            dir.listFiles().orEmpty().filter { it.isFile }.sortedByDescending { it.lastModified() }.mapNotNull { f ->
                Formato.entries.firstOrNull { f.name.endsWith(".${it.extension}") }?.let { ArchivoInforme(f, it) }
            }
        }
        _e.update { it.copy(historial = lista) }
    }

    fun generar(titulo: String, desde: LocalDate, hasta: LocalDate, contenidos: Set<Contenido>, persona: String? = null) = viewModelScope.launch {
        if (_e.value.generando) return@launch
        _e.update { it.copy(generando = true, vistaPrevia = null) }
        val formato = _e.value.formato
        runCatching {
            val datos = repo.todo().first()
            val personas = repo.personas().first().associate { it.id to it.nombre }
            val categorias = repo.categorias().first().associate { it.id to it.nombre }
            withContext(Dispatchers.Default) {
                val inf = construirInforme(titulo, desde, hasta, contenidos, datos.gastos, datos.ingresos, personas, categorias, persona)
                val generado = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now())
                val bytes = when (formato) {
                    Formato.PDF -> GeneradorPdf().generar(inf, generado)
                    Formato.EXCEL -> EscritorXlsx.escribir(hojasDeInforme(inf))
                }
                val base = "Nummia_${titulo}_${inf.periodo}".replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_')
                val sello = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                dir.mkdirs()
                val f = File(dir, "${base}_$sello.${formato.extension}")
                f.writeBytes(bytes)
                // Se guardan los 20 últimos.
                dir.listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(20).forEach { it.delete() }
                ArchivoInforme(f, formato) to (if (formato == Formato.PDF) vistaPrevia(f) else null)
            }
        }.onSuccess { (a, img) ->
            _e.update { it.copy(generando = false, ultimo = a, vistaPrevia = img) }
            cargarHistorial()
        }.onFailure {
            _e.update { it.copy(generando = false, mensaje = "No se pudo crear el informe.") }
        }
    }

    /** Primera página del PDF, para verla antes de compartir. */
    private fun vistaPrevia(f: File): Bitmap? = runCatching {
        ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                r.openPage(0).use { p ->
                    val escala = 1.6f
                    val bmp = Bitmap.createBitmap((p.width * escala).toInt(), (p.height * escala).toInt(), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    }.getOrNull()

    fun abrir(a: ArchivoInforme) {
        _e.update { it.copy(ultimo = a, vistaPrevia = null) }
        if (a.formato == Formato.PDF) viewModelScope.launch {
            val img = withContext(Dispatchers.IO) { vistaPrevia(a.archivo) }
            if (_e.value.ultimo == a) _e.update { it.copy(vistaPrevia = img) }
        }
    }

    fun intentCompartir(a: ArchivoInforme): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", a.archivo)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType(a.formato.mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            "Compartir informe",
        )
    }

    fun guardarEn(uri: android.net.Uri, a: ArchivoInforme) = viewModelScope.launch {
        val ok = runCatching {
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(a.archivo.readBytes()) } }
        }.isSuccess
        mensaje(if (ok) "Informe guardado" else "No se pudo guardar")
    }

    fun guardarEnDrive(a: ArchivoInforme) = viewModelScope.launch {
        val ok = runCatching { withContext(Dispatchers.IO) { sinc.subirInforme(a.nombre, a.archivo.readBytes(), a.formato.mime) } }
        _e.update { it.copy(mensaje = if (ok.getOrNull() == true) "Guardado en tu Drive (Nummia_Datos/Informes)" else "No se pudo guardar en Drive. Comprueba la conexión.") }
    }

    fun borrar(a: ArchivoInforme) = viewModelScope.launch {
        withContext(Dispatchers.IO) { a.archivo.delete() }
        if (_e.value.ultimo == a) _e.update { it.copy(ultimo = null, vistaPrevia = null) }
        cargarHistorial()
    }

    fun hoy(): LocalDate = reloj.hoy()

    fun mensaje(m: String) = _e.update { it.copy(mensaje = m) }
}
