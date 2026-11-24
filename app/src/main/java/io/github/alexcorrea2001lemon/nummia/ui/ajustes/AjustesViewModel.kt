package io.github.alexcorrea2001lemon.nummia.ui.ajustes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.data.ajustes.Ajustes
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaInvalida
import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaLocal
import io.github.alexcorrea2001lemon.nummia.data.db.PersonaEntity
import io.github.alexcorrea2001lemon.nummia.data.repo.MovimientosRepo
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import io.github.alexcorrea2001lemon.nummia.ui.theme.TamanoTexto
import io.github.alexcorrea2001lemon.nummia.ui.theme.Tema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ajustesRepo: AjustesRepo,
    private val repo: MovimientosRepo,
    private val copia: CopiaLocal,
    private val reloj: Reloj,
    private val sinc: io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador,
    private val csv: io.github.alexcorrea2001lemon.nummia.data.informes.ExportadorCsv,
) : ViewModel() {
    val ajustes: StateFlow<Ajustes> = ajustesRepo.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())
    val personas: StateFlow<List<PersonaEntity>> = repo.personas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun tema(t: Tema) = viewModelScope.launch { ajustesRepo.tema(t) }
    fun acento(a: Acento) = viewModelScope.launch { ajustesRepo.acento(a) }
    fun acentoPersonalizado(argb: Long) = viewModelScope.launch { ajustesRepo.acentoPersonalizado(argb) }
    fun acentoFondo() = viewModelScope.launch { ajustesRepo.acentoFondoPantalla() }
    fun tamano(t: TamanoTexto) = viewModelScope.launch { ajustesRepo.tamanoTexto(t) }
    fun discreto(v: Boolean) = viewModelScope.launch { ajustesRepo.modoDiscreto(v) }
    fun soloWifi(v: Boolean) = viewModelScope.launch { ajustesRepo.actualizacionesSoloWifi(v) }
    fun buscarActualizaciones(v: Boolean) = viewModelScope.launch { ajustesRepo.buscarActualizaciones(v) }
    fun notificaciones(v: Boolean) = viewModelScope.launch { ajustesRepo.notificaciones(v) }
    fun avisoSueldo(v: Boolean) = viewModelScope.launch { ajustesRepo.avisoSueldo(v) }
    fun avisoPagos(v: Boolean) = viewModelScope.launch { ajustesRepo.avisoPagos(v) }
    fun avisoPresupuestos(v: Boolean) = viewModelScope.launch { ajustesRepo.avisoPresupuestos(v) }
    fun bloqueo(v: Boolean) = viewModelScope.launch { ajustesRepo.bloqueo(v) }
    fun vibracion(v: Boolean) = viewModelScope.launch { ajustesRepo.vibracion(v) }
    fun compacto(v: Boolean) = viewModelScope.launch { ajustesRepo.compacto(v) }
    fun tarjetasInicio(orden: List<String>, ocultas: Set<String>) = viewModelScope.launch { ajustesRepo.tarjetasInicio(orden, ocultas) }
    fun formaTarjetas(f: io.github.alexcorrea2001lemon.nummia.ui.theme.FormaTarjetas) = viewModelScope.launch { ajustesRepo.formaTarjetas(f) }

    /** Solo se guarda: el cambio real del icono lo hace NummiaApp al pasar la app a segundo plano. */
    fun icono(logo: Logo, color: Acento) = viewModelScope.launch { ajustesRepo.icono(logo, color) }

    fun sueldo(c: Long?) = viewModelScope.launch {
        ajustesRepo.sueldoHabitual(c)
        repo.asegurarSueldoPrevisto(reloj.hoy(), c)
    }

    fun nuevaPersona(n: String) = viewModelScope.launch { repo.crearPersona(n) }
    fun renombrar(p: PersonaEntity, n: String) = viewModelScope.launch { repo.renombrarPersona(p, n) }
    fun borrarPersona(p: PersonaEntity) = viewModelScope.launch { repo.borrarPersona(p) }

    suspend fun exportar(contrasena: String? = null): ByteArray = copia.exportar(reloj.ahoraMs(), contrasena)
    suspend fun copiaGuardada() = ajustesRepo.ultimaCopiaLocal(reloj.hoy().toEpochDay())
    suspend fun exportarCsv(): ByteArray = csv.zip()

    /** Antes de restaurar se guarda una copia del estado actual en el móvil. */
    suspend fun importar(bytes: ByteArray, contrasena: String? = null): Result<Int> = runCatching {
        // Descifrar (si tiene contraseña) y leer cuesta un poco: fuera del hilo principal.
        val c = withContext(Dispatchers.Default) { copia.leer(bytes, contrasena) }
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "copias").apply { mkdirs() }
            File(dir, "antes_de_importar_${reloj.ahoraMs()}.nummia").writeBytes(copia.exportar(reloj.ahoraMs()))
        }
        kotlinx.coroutines.withTimeoutOrNull(20_000) { sinc.copiaAhora() } // también en tu Drive, si está conectado
        copia.restaurar(c)
        if (sinc.correo() != null) {
            sinc.trasRestaurar() // la hoja de Drive quedará igual que el móvil
            io.github.alexcorrea2001lemon.nummia.data.drive.TrabajoSync.pronto(context, ajustes.first().driveSoloWifi, 5)
        }
        repo.prepararDatos()
        repo.asegurarSueldoPrevisto(reloj.hoy(), ajustes.first().sueldoHabitual)
        c.gastos.count { it.deletedAt == null } + c.ingresos.count { it.deletedAt == null }
    }.recoverCatching { e -> throw if (e is CopiaInvalida || e is io.github.alexcorrea2001lemon.nummia.data.copia.CopiaCifrada) e else CopiaInvalida("No se pudo leer la copia.") }
}
