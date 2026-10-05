package com.encaja.app.avisos

// NOTA: depende de Android (BroadcastReceiver, NotificationManager), no compilado en este entorno.

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.encaja.app.MainActivity
import com.encaja.app.R

/** Recibe la alarma de un aviso de actividad y muestra la notificación. */
class AvisoReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val texto = intent.getStringExtra(EXTRA_TEXTO) ?: ""
        val fecha = intent.getStringExtra(EXTRA_FECHA)
        val necesidadId = intent.getStringExtra(EXTRA_NECESIDAD)

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        crearCanal(context)

        val abrirApp = PendingIntent.getActivity(
            // Un requestCode distinto por aviso: si no, Android reutilizaría el PendingIntent
            // de otro aviso y los datos de la actividad se mezclarían.
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (fecha != null && necesidadId != null) {
                    putExtra(MainActivity.EXTRA_ABRIR_FECHA, fecha)
                    putExtra(MainActivity.EXTRA_ABRIR_NECESIDAD, necesidadId)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()
        try {
            manager.notify(id, notificacion)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones (Android 13+): no se puede mostrar.
        }
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Avisos de actividades", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios de llevar o recoger a los niños"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    companion object {
        const val CANAL = "avisos_actividades"
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_TEXTO = "texto"
        const val EXTRA_FECHA = "fecha"
        const val EXTRA_NECESIDAD = "necesidad"
    }
}
