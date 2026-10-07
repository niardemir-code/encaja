package com.encaja.app.ui.common

// NOTA: depende de Android (ActivityManager, NotificationManager); no compilado en este entorno.

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import java.io.File
import kotlin.system.exitProcess

/**
 * Baja de la cuenta en este dispositivo cuando un administrador la desvincula de la familia:
 * no debe quedar ningún dato de la familia (ni caché, ni avisos programados, ni sesión).
 *
 * Se recuerda, por cuenta, que estuvo vinculada a una familia. Así la baja se detecta también
 * si ocurrió con la app cerrada o sin conexión: al abrirla de nuevo, el servidor confirma que
 * la cuenta ya no pertenece a ninguna familia y se borra todo.
 */
object BajaDeDispositivo {
    private const val PREFS = "baja_dispositivo"
    private const val CLAVE_UID = "uid"

    fun recordarMembresia(contexto: Context, uid: String) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLAVE_UID, uid).apply()
    }

    fun tuvoMembresia(contexto: Context, uid: String): Boolean =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_UID, null) == uid

    /**
     * Borra TODOS los datos de la app en este dispositivo (base de datos local, caché de
     * Firestore, preferencias, sesión iniciada, avisos programados y notificaciones) y cierra
     * la app. Es equivalente a «Borrar datos» en los ajustes de Android, así que la próxima
     * vez que se abra empieza desde cero. No vuelve si funciona.
     */
    fun borrarTodo(contexto: Context) {
        // Notificaciones ya mostradas. (Las alarmas pendientes las quita el propio borrado de
        // datos del sistema, y antes se han cancelado una a una las que se conocían.)
        runCatching { contexto.getSystemService(NotificationManager::class.java)?.cancelAll() }

        val borrado = runCatching {
            contexto.getSystemService(ActivityManager::class.java)?.clearApplicationUserData() == true
        }.getOrDefault(false)

        if (!borrado) {
            // Plan B: vaciar a mano el directorio de datos de la app y cerrarla.
            runCatching {
                File(contexto.applicationInfo.dataDir).listFiles()
                    ?.filter { it.name != "lib" }
                    ?.forEach { it.deleteRecursively() }
            }
            exitProcess(0)
        }
    }
}
