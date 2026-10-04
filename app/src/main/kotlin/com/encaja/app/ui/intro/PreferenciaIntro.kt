package com.encaja.app.ui.intro

import android.content.Context

private const val PREFS = "encaja_prefs"
private const val CLAVE_VISTA = "intro_vista"
private const val CLAVE_CODIGO = "intro_codigo_pendiente"
private const val CLAVE_CREAR = "intro_crear_familia"

/**
 * Guarda si ya se mostró la intro y el código de invitación que se escribió en ella
 * (se canjea al entrar con una cuenta, porque hasta entonces no hay sesión).
 */
object PreferenciaIntro {
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun vista(c: Context): Boolean = prefs(c).getBoolean(CLAVE_VISTA, false)
    fun marcarVista(c: Context) = prefs(c).edit().putBoolean(CLAVE_VISTA, true).apply()

    fun guardarCodigo(c: Context, codigo: String) =
        prefs(c).edit().putString(CLAVE_CODIGO, codigo.trim().uppercase()).apply()

    fun codigoPendiente(c: Context): String? =
        prefs(c).getString(CLAVE_CODIGO, null)?.takeIf { it.isNotBlank() }

    fun borrarCodigo(c: Context) = prefs(c).edit().remove(CLAVE_CODIGO).apply()

    /** El usuario eligió "Crear una familia nueva" en la intro: al entrar con su cuenta se le
     * ofrece primero crearla. */
    fun guardarIntencionCrear(c: Context) = prefs(c).edit().putBoolean(CLAVE_CREAR, true).apply()
    fun intencionCrear(c: Context): Boolean = prefs(c).getBoolean(CLAVE_CREAR, false)
    fun borrarIntencionCrear(c: Context) = prefs(c).edit().remove(CLAVE_CREAR).apply()
}
