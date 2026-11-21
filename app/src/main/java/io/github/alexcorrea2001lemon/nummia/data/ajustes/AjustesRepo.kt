package io.github.alexcorrea2001lemon.nummia.data.ajustes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexcorrea2001lemon.nummia.ui.theme.Acento
import io.github.alexcorrea2001lemon.nummia.ui.theme.Logo
import io.github.alexcorrea2001lemon.nummia.ui.theme.ModoAcento
import io.github.alexcorrea2001lemon.nummia.ui.theme.TamanoTexto
import io.github.alexcorrea2001lemon.nummia.ui.theme.Tema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Ajustes(
    val tema: Tema = Tema.AUTOMATICO,
    val modoAcento: ModoAcento = ModoAcento.PALETA,
    val acento: Acento = Acento.POR_DEFECTO,
    val acentoPersonalizado: Long = Acento.POR_DEFECTO.argb,
    val iconoLogo: Logo = Logo.A,
    val iconoColor: Acento = Acento.POR_DEFECTO,
    val tamanoTexto: TamanoTexto = TamanoTexto.NORMAL,
    val modoDiscreto: Boolean = false,
    val sueldoHabitual: Long? = null,
    val actualizacionesSoloWifi: Boolean = true,
    val buscarActualizaciones: Boolean = true,
    val bienvenidaVista: Boolean = false,
    val versionDisponible: String? = null,
)

private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

@Singleton
class AjustesRepo internal constructor(private val store: DataStore<Preferences>) {
    @Inject constructor(@ApplicationContext context: Context) : this(context.almacen)

    private object K {
        val tema = stringPreferencesKey("tema")
        val modoAcento = stringPreferencesKey("modo_acento")
        val acento = stringPreferencesKey("acento")
        val acentoPersonalizado = longPreferencesKey("acento_personalizado")
        val iconoLogo = stringPreferencesKey("icono_logo")
        val iconoColor = stringPreferencesKey("icono_color")
        val tamanoTexto = stringPreferencesKey("tamano_texto")
        val modoDiscreto = booleanPreferencesKey("modo_discreto")
        val sueldo = longPreferencesKey("sueldo_habitual")
        val soloWifi = booleanPreferencesKey("actualizar_solo_wifi")
        val buscar = booleanPreferencesKey("buscar_actualizaciones")
        val bienvenida = booleanPreferencesKey("bienvenida_vista")
        val versionDisponible = stringPreferencesKey("version_disponible")
    }

    val ajustes: Flow<Ajustes> = store.data.map { p ->
        val d = Ajustes()
        Ajustes(
            tema = enumO(p[K.tema], d.tema),
            modoAcento = enumO(p[K.modoAcento], d.modoAcento),
            acento = enumO(p[K.acento], d.acento),
            acentoPersonalizado = p[K.acentoPersonalizado] ?: d.acentoPersonalizado,
            iconoLogo = enumO(p[K.iconoLogo], d.iconoLogo),
            iconoColor = enumO(p[K.iconoColor], d.iconoColor),
            tamanoTexto = enumO(p[K.tamanoTexto], d.tamanoTexto),
            modoDiscreto = p[K.modoDiscreto] ?: d.modoDiscreto,
            sueldoHabitual = p[K.sueldo],
            actualizacionesSoloWifi = p[K.soloWifi] ?: d.actualizacionesSoloWifi,
            buscarActualizaciones = p[K.buscar] ?: d.buscarActualizaciones,
            bienvenidaVista = p[K.bienvenida] ?: d.bienvenidaVista,
            versionDisponible = p[K.versionDisponible],
        )
    }

    suspend fun tema(t: Tema) = store.edit { it[K.tema] = t.name }
    suspend fun acento(a: Acento) = store.edit { it[K.acento] = a.name; it[K.modoAcento] = ModoAcento.PALETA.name }
    suspend fun acentoPersonalizado(argb: Long) = store.edit { it[K.acentoPersonalizado] = argb; it[K.modoAcento] = ModoAcento.PERSONALIZADO.name }
    suspend fun acentoFondoPantalla() = store.edit { it[K.modoAcento] = ModoAcento.FONDO_PANTALLA.name }
    suspend fun icono(logo: Logo, color: Acento) = store.edit { it[K.iconoLogo] = logo.name; it[K.iconoColor] = color.name }
    suspend fun tamanoTexto(t: TamanoTexto) = store.edit { it[K.tamanoTexto] = t.name }
    suspend fun modoDiscreto(v: Boolean) = store.edit { it[K.modoDiscreto] = v }
    suspend fun sueldoHabitual(c: Long?) = store.edit { if (c == null) it.remove(K.sueldo) else it[K.sueldo] = c }
    suspend fun actualizacionesSoloWifi(v: Boolean) = store.edit { it[K.soloWifi] = v }
    suspend fun buscarActualizaciones(v: Boolean) = store.edit { it[K.buscar] = v }
    suspend fun bienvenidaVista() = store.edit { it[K.bienvenida] = true }
    suspend fun versionDisponible(v: String?) = store.edit { if (v == null) it.remove(K.versionDisponible) else it[K.versionDisponible] = v }

    private inline fun <reified E : Enum<E>> enumO(v: String?, d: E): E =
        v?.let { s -> enumValues<E>().firstOrNull { it.name == s } } ?: d
}
