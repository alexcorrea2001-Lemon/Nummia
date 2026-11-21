package io.github.alexcorrea2001lemon.nummia.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alexcorrea2001lemon.nummia.data.db.ClaveDb
import io.github.alexcorrea2001lemon.nummia.data.db.NummiaDb
import io.github.alexcorrea2001lemon.nummia.domain.reloj.Reloj
import io.github.alexcorrea2001lemon.nummia.domain.reloj.RelojSistema
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun db(@ApplicationContext context: Context): NummiaDb {
        System.loadLibrary("sqlcipher")
        val fabrica = SupportOpenHelperFactory(ClaveDb.obtener(context))
        return Room.databaseBuilder(context, NummiaDb::class.java, "nummia.db")
            .openHelperFactory(fabrica)
            .addMigrations(NummiaDb.MIGRACION_1_2)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RelojModule {
    @Binds abstract fun reloj(r: RelojSistema): Reloj
}
