package io.github.alexcorrea2001lemon.nummia.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.alexcorrea2001lemon.nummia.BuildConfig
import io.github.alexcorrea2001lemon.nummia.data.actualizar.Actualizador
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RedModule {
    @Provides
    @Singleton
    fun cliente(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun actualizador(cliente: OkHttpClient, @ApplicationContext context: Context): Actualizador =
        Actualizador(cliente, BuildConfig.VERSION_NAME, File(context.cacheDir, "updates"))
}
