package com.encaja.app.avisos

// NOTA: depende de Firebase Cloud Messaging/Hilt, no compilado en este entorno.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.encaja.app.MainActivity
import com.encaja.app.R
import com.encaja.app.domain.repository.DispositivoRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Recibe las notificaciones push que manda el servidor (ver la carpeta functions/):
 *  - "anuncio": alguien publicó en el tablón → se muestra una notificación.
 *  - "sync_avisos": otro miembro creó o cambió una actividad que me afecta → se vuelven a
 *    programar en este móvil los avisos, sin necesidad de abrir la app.
 * Los mensajes llegan solo con datos (sin bloque "notification") para que siempre pase por
 * aquí, también con la app cerrada, y se vean con el icono y el canal de Encaja.
 */
@AndroidEntryPoint
class EncajaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var dispositivos: DispositivoRepository
    @Inject lateinit var sincronizador: SincronizadorDeAvisos

    override fun onNewToken(token: String) {
        runBlocking { dispositivos.registrarToken(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos = mensaje.data
        when (datos["tipo"]) {
            "anuncio" -> mostrarAnuncio(datos["titulo"] ?: "Tablón de la familia", datos["texto"] ?: "")
            "sync_avisos" -> runBlocking { runCatching { sincronizador.reprogramar() } }
        }
    }

    private fun mostrarAnuncio(titulo: String, texto: String) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        crearCanal(this)
        val abrirApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Al tocarla, se abre Semana desplazada hasta el tablón.
                putExtra(MainActivity.EXTRA_ABRIR_TABLON, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(this, CANAL_TABLON)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()
        try {
            // Id distinto por anuncio para que dos seguidos no se pisen.
            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notificacion)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones (Android 13+).
        }
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL_TABLON, "Tablón de la familia", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos cuando alguien publica en el tablón"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    companion object {
        const val CANAL_TABLON = "tablon_familia"
    }
}
