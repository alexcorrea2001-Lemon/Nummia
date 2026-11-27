package io.github.alexcorrea2001lemon.nummia.ui.formulario

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.alexcorrea2001lemon.nummia.domain.ticket.LineaOcr
import io.github.alexcorrea2001lemon.nummia.domain.ticket.juntarFilas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/** Qué ha pasado al escanear un ticket. */
sealed interface ResultadoEscaneo {
    /** El texto del ticket, ya ordenado por filas. */
    data class Texto(val texto: String) : ResultadoEscaneo
    data object Cancelado : ResultadoEscaneo
    data class Error(val motivo: String) : ResultadoEscaneo
}

/** [ocupado] es verdadero mientras se lee la foto; [abrir] abre el escáner. */
class EscanerTicket(val ocupado: Boolean, val abrir: () -> Unit)

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}

/**
 * Escanear un ticket: el escáner de documentos de Google Play Services recorta y endereza la foto
 * (no hace falta permiso de cámara) y el reconocimiento de texto se hace en el móvil: la foto no sale de él.
 * Después de leerla se borra.
 */
@Composable
fun rememberEscanerTicket(alResultado: (ResultadoEscaneo) -> Unit): EscanerTicket {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    val resultado by rememberUpdatedState(alResultado)
    var ocupado by remember { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val uri = if (r.resultCode == Activity.RESULT_OK) GmsDocumentScanningResult.fromActivityResultIntent(r.data)?.pages?.firstOrNull()?.imageUri else null
        if (uri == null) {
            ocupado = false
            resultado(if (r.resultCode == Activity.RESULT_OK) ResultadoEscaneo.Error("No se ha recibido la foto") else ResultadoEscaneo.Cancelado)
        } else alcance.launch {
            val leido = runCatching { textoDe(ctx, uri) }
            ocupado = false
            resultado(leido.fold({ ResultadoEscaneo.Texto(it) }, { ResultadoEscaneo.Error("No se ha podido leer la foto") }))
        }
    }

    return EscanerTicket(ocupado) {
        val actividad = ctx.actividad()
        if (actividad == null || ocupado) return@EscanerTicket
        ocupado = true
        val opciones = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true) // también sirve una foto que ya tengas
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(opciones).getStartScanIntent(actividad)
            .addOnSuccessListener { lanzador.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener {
                ocupado = false
                resultado(ResultadoEscaneo.Error("El escáner no está disponible en este móvil (necesita Google Play Services al día)"))
            }
    }
}

/** Reconoce el texto de la foto y lo devuelve ordenado por filas; después borra la foto. */
private suspend fun textoDe(ctx: Context, uri: Uri): String = withContext(Dispatchers.Default) {
    val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val r = reconocedor.process(InputImage.fromFilePath(ctx, uri)).await()
        val lineas = r.textBlocks.flatMap { it.lines }.mapNotNull { l ->
            l.boundingBox?.let { LineaOcr(l.text, it.left, it.top, it.bottom) }
        }
        if (lineas.isEmpty()) r.text else juntarFilas(lineas)
    } finally {
        reconocedor.close()
        if (uri.scheme == "file") runCatching { uri.path?.let { File(it).delete() } }
    }
}
