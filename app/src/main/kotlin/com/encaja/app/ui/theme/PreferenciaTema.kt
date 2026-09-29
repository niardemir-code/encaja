package com.encaja.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** "Sistema" sigue el claro/oscuro del teléfono; los otros dos lo fuerzan. */
enum class TemaPreferido { SISTEMA, CLARO, OSCURO }

private const val PREFS = "encaja_prefs"
private const val CLAVE_TEMA = "tema_preferido"

/**
 * Preferencia de tema, guardada en SharedPreferences para que sobreviva a cerrar
 * la app. [cargar] se llama una vez al arrancar (MainActivity); [elegir] se llama
 * desde Ajustes y, al ser un estado de Compose, recompone el tema al instante,
 * sin tener que reiniciar la app.
 */
object PreferenciaTema {
    val actual = mutableStateOf(TemaPreferido.SISTEMA)

    fun cargar(context: Context) {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_TEMA, null)
        actual.value = guardado?.let { runCatching { TemaPreferido.valueOf(it) }.getOrNull() } ?: TemaPreferido.SISTEMA
    }

    fun elegir(context: Context, tema: TemaPreferido) {
        actual.value = tema
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE_TEMA, tema.name).apply()
    }
}
