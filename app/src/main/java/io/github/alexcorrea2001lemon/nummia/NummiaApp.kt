package io.github.alexcorrea2001lemon.nummia

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NummiaApp : Application(), Configuration.Provider {
    @Inject lateinit var fabricaTrabajos: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaTrabajos).build()
}
