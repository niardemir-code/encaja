package com.encaja.app

import android.app.Application
import android.content.Context
import com.encaja.app.ui.common.IdiomaApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EncajaApplication : Application() {
    // Android 8-12: el idioma elegido en Ajustes rige también en segundo plano (formatos).
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(IdiomaApp.envolver(base))
    }
}
