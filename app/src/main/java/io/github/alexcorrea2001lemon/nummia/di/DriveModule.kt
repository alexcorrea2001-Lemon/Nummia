package io.github.alexcorrea2001lemon.nummia.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alexcorrea2001lemon.nummia.data.ajustes.AjustesRepo
import io.github.alexcorrea2001lemon.nummia.data.copia.CopiaLocal
import io.github.alexcorrea2001lemon.nummia.data.db.ClaveDb
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.data.drive.ApiGoogle
import io.github.alexcorrea2001lemon.nummia.data.drive.CuentaGoogle
import io.github.alexcorrea2001lemon.nummia.data.drive.Sincronizador
import io.github.alexcorrea2001lemon.nummia.data.drive.SyncDb
import io.github.alexcorrea2001lemon.nummia.data.drive.TrabajoSync
import io.github.alexcorrea2001lemon.nummia.data.repo.AvisoCambios
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveModule {
    /** Estado de la sincronización en su propia base de datos (cifrada con la misma clave). */
    @Provides
    @Singleton
    fun syncDb(@ApplicationContext context: Context, @Suppress("UNUSED_PARAMETER") principal: NummiaDb): SyncDb =
        Room.databaseBuilder(context, SyncDb::class.java, "nummia_sync.db")
            .openHelperFactory(SupportOpenHelperFactory(ClaveDb.obtener(context)))
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun sincronizador(db: NummiaDb, sync: SyncDb, reloj: Reloj, copia: CopiaLocal, cliente: OkHttpClient, cuenta: CuentaGoogle): Sincronizador =
        Sincronizador(db, sync.sync(), reloj, copia) { correo ->
            ApiGoogle(cliente, { renovar -> cuenta.token(correo.takeIf { '@' in it }, renovar) })
        }

    @Provides
    @Singleton
    fun avisoCambios(@ApplicationContext context: Context, s: dagger.Lazy<Sincronizador>, ajustes: AjustesRepo): AvisoCambios {
        val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return AvisoCambios {
            alcance.launch {
                io.github.alexcorrea2001lemon.nummia.ui.widget.WidgetBalance.actualizar(context)
                val a = ajustes.ajustes.first()
                if (a.notificaciones) io.github.alexcorrea2001lemon.nummia.data.avisos.TrabajoAvisos.pronto(context)
                val sinc = s.get()
                if (sinc.correo() == null) return@launch
                sinc.marcarCambio()
                TrabajoSync.pronto(context, a.driveSoloWifi)
            }
        }
    }
}
