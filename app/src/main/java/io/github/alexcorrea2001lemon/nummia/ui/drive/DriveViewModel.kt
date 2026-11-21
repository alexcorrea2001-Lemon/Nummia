package io.github.alexcorrea2001lemon.nummia.ui.drive

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.drive.ArchivoDrive
import io.github.alexcorrea2001lemon.nummia.data.drive.ConflictoEntity
import io.github.alexcorrea2001lemon.nummia.data.drive.CuentaGoogle
import io.github.alexcorrea2001lemon.nummia.data.drive.Meta
import io.github.alexcorrea2001lemon.nummia.data.drive.ResultadoConexion
import io.github.alexcorrea2001lemon.nummia.data.drive.ResultadoSync
import io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador
import io.github.alexcorrea2001lemon.nummia.data.drive.TrabajoSync
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstadoDrive(
    val correo: String? = null,
    val carpeta: String? = null,
    val ultima: Long? = null,
    val error: String? = null,
    val pendiente: Boolean = false,
    val avisos: List<String> = emptyList(),
    /** «Desde el ordenador: 2 nuevos y 1 cambiado» de la última sincronización (null si no llegó nada). */
    val cambiosOrdenador: String? = null,
    val conflictos: List<ConflictoEntity> = emptyList(),
    val ocupado: Boolean = false,
    val soloWifi: Boolean = false,
) {
    val conectado get() = correo != null
}

/** Lo que se muestra una sola vez: diálogos y mensajes. */
sealed interface EventoDrive {
    data class PedirPermiso(val pendiente: PendingIntent) : EventoDrive
    data class OfrecerRecuperar(val copia: ArchivoDrive) : EventoDrive
    data class Mensaje(val texto: String) : EventoDrive
}

@HiltViewModel
class DriveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sinc: Sincronizador,
    private val cuenta: CuentaGoogle,
    private val ajustes: AjustesRepo,
    private val repo: MovimientosRepo,
) : ViewModel() {
    val estado: StateFlow<EstadoDrive> = combine(sinc.metaFlujo(), sinc.conflictos(), sinc.ocupado, ajustes.ajustes) { meta, conflictos, ocupado, a ->
        val m = meta.associate { it.clave to it.valor }
        EstadoDrive(
            correo = m[Meta.CUENTA], carpeta = m[Meta.CARPETA], ultima = m[Meta.ULTIMA]?.toLongOrNull(), error = m[Meta.ERROR],
            pendiente = m[Meta.PENDIENTE] == "1", avisos = m[Meta.AVISOS]?.lines().orEmpty().filter { it.isNotBlank() },
            cambiosOrdenador = io.github.alexcorrea2001lemon.nummia.data.drive.CambiosOrdenador.leer(m[Meta.CAMBIOS_ORDENADOR])?.texto(),
            conflictos = conflictos, ocupado = ocupado, soloWifi = a.driveSoloWifi,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDrive())

    private val _evento = MutableStateFlow<EventoDrive?>(null)
    val evento: StateFlow<EventoDrive?> = _evento.asStateFlow()
    fun eventoVisto() = _evento.update { null }

    private val _copias = MutableStateFlow<List<ArchivoDrive>?>(null)
    val copias: StateFlow<List<ArchivoDrive>?> = _copias.asStateFlow()
    private val _trabajando = MutableStateFlow(false)
    val trabajando: StateFlow<Boolean> = _trabajando.asStateFlow()

    /** Último error al conectar: se queda en pantalla (no solo en un aviso que desaparece). */
    private val _errorConexion = MutableStateFlow<String?>(null)
    val errorConexion: StateFlow<String?> = _errorConexion.asStateFlow()
    private val _conectando = MutableStateFlow(false)
    val conectando: StateFlow<Boolean> = _conectando.asStateFlow()

    fun conectar() = viewModelScope.launch {
        _errorConexion.value = null
        _conectando.value = true
        tratar(cuenta.conectar(null))
    }

    fun resultadoPermiso(codigo: Int, datos: Intent?) = viewModelScope.launch {
        if (datos == null) {
            _conectando.value = false
            _errorConexion.value = if (codigo == android.app.Activity.RESULT_CANCELED) {
                "Google ha cerrado la ventana sin dar acceso (código de resultado $codigo). Si no has cancelado tú, revisa en Google Cloud el cliente Android (paquete y huella SHA-1)."
            } else "Google no ha devuelto respuesta (código de resultado $codigo)."
            return@launch
        }
        tratar(cuenta.resultado(datos))
    }

    private suspend fun tratar(r: ResultadoConexion) {
        when (r) {
            is ResultadoConexion.PideConsentimiento -> _evento.value = EventoDrive.PedirPermiso(r.pendiente)
            is ResultadoConexion.Error -> { _conectando.value = false; _errorConexion.value = r.mensaje }
            is ResultadoConexion.Conectado -> {
                _conectando.value = false
                _errorConexion.value = null
                val nueva = sinc.correo() == null
                sinc.conectar(r.correo ?: "Google")
                TrabajoSync.programar(context, true, ajustes.ajustes.first().driveSoloWifi)
                // Móvil nuevo o app reinstalada: si hay copias en tu Drive, se ofrece recuperar la última.
                if (nueva && sinc.datosVacios()) {
                    val ultima = runCatching { sinc.copias().firstOrNull() }.getOrNull()
                    if (ultima != null) { _evento.value = EventoDrive.OfrecerRecuperar(ultima); return }
                }
                sincronizarAhora()
            }
        }
    }

    fun sincronizarAhora() = viewModelScope.launch {
        when (val r = sinc.sincronizar()) {
            is ResultadoSync.Hecho -> _evento.value = EventoDrive.Mensaje(
                if (r.conflictos > 0) "Sincronizado. Hay ${r.conflictos} cambio(s) que tienes que decidir." else "Todo sincronizado",
            )
            is ResultadoSync.Fallo -> _evento.value = EventoDrive.Mensaje(r.mensaje)
            ResultadoSync.NoConectado -> Unit
        }
    }

    fun desconectar() = viewModelScope.launch {
        sinc.desconectar()
        cuenta.olvidar()
        TrabajoSync.programar(context, false, false)
        _copias.value = null
        _evento.value = EventoDrive.Mensaje("Drive desconectado. Tus datos siguen en el móvil y en tu Drive.")
    }

    fun soloWifi(v: Boolean) = viewModelScope.launch {
        ajustes.driveSoloWifi(v)
        TrabajoSync.programar(context, estado.value.conectado, v)
    }

    fun resolver(c: ConflictoEntity, movil: Boolean) = viewModelScope.launch {
        sinc.resolver(c, movil)
        TrabajoSync.pronto(context, estado.value.soloWifi, 0)
    }

    fun cargarCopias() = viewModelScope.launch {
        _copias.value = runCatching { sinc.copias() }.getOrElse {
            _evento.value = EventoDrive.Mensaje("No se pudieron leer las copias. Comprueba la conexión.")
            emptyList()
        }
    }

    fun copiaAhora() = conTrabajo {
        sinc.copiaAhora().onSuccess { _evento.value = EventoDrive.Mensaje("Copia guardada en tu Drive") }
            .onFailure { _evento.value = EventoDrive.Mensaje("No se pudo guardar la copia. Comprueba la conexión.") }
        cargarCopias()
    }

    fun restaurar(a: ArchivoDrive) = conTrabajo {
        runCatching { sinc.restaurar(a) }
            .onSuccess {
                repo.prepararDatos()
                _evento.value = EventoDrive.Mensaje("Copia recuperada")
                sincronizarAhora()
            }
            .onFailure { _evento.value = EventoDrive.Mensaje("No se pudo recuperar la copia. Tus datos no se han tocado.") }
    }

    private fun conTrabajo(bloque: suspend () -> Unit) = viewModelScope.launch {
        _trabajando.value = true
        try { bloque() } finally { _trabajando.value = false }
    }
}
