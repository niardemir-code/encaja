package com.encaja.app.ui.common

// NOTA: depende de Android (LocaleManager, Configuration), no compilado en este entorno.
// Idioma de la app elegido en Ajustes, independiente del idioma del teléfono.
//  - Android 13+: se delega en el sistema (LocaleManager); es el mismo ajuste que aparece en
//    Ajustes del teléfono > Idioma de las apps, y el sistema recrea la pantalla solo.
//  - Android 8 a 12: se guarda en SharedPreferences y se aplica al crear la actividad
//    ([envolver]); al cambiar se recrea la actividad.
// "" significa "el del teléfono".

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

object IdiomaApp {
    const val SISTEMA = ""
    private const val PREFS = "encaja_prefs"
    private const val CLAVE = "idioma_app"

    /** Código elegido ("", "es", "ca" o "en"); estado de Compose para que Ajustes lo muestre. */
    val actual = mutableStateOf(SISTEMA)

    private fun leer(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lista = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (lista.isEmpty) SISTEMA else lista[0].language
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, SISTEMA) ?: SISTEMA
        }

    /** Se llama al crear la actividad, antes de dibujar. */
    fun cargar(context: Context) {
        actual.value = leer(context)
    }

    /**
     * Android 8-12: devuelve el contexto con el idioma elegido (y lo fija como idioma por
     * defecto del proceso, que es el que usan los formatos de fecha). Android 13+: no hace
     * nada, el sistema ya lo aplica. Usar en attachBaseContext y al redactar notificaciones.
     */
    fun envolver(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val codigo = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, SISTEMA) ?: SISTEMA
        if (codigo.isEmpty()) return base
        val idioma = Locale.forLanguageTag(codigo)
        Locale.setDefault(idioma)
        val config = Configuration(base.resources.configuration)
        config.setLocale(idioma)
        return base.createConfigurationContext(config)
    }

    fun elegir(activity: Activity, codigo: String) {
        if (codigo == actual.value) return
        actual.value = codigo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (codigo.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(codigo)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE, codigo).apply()
            if (codigo.isEmpty()) Locale.setDefault(Resources.getSystem().configuration.locales[0])
            activity.recreate()
        }
    }
}
