package com.encaja.app.avisos

// NOTA: depende de Android (AlarmManager, PendingIntent), no compilado en este entorno.

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa (y cancela) en el móvil los avisos de una actividad: una alarma X minutos
 * antes de que empiece ("llevar a…") y/o X minutos antes de que termine ("recoger
 * a…"), según [CoverageNeed.avisoLlevarMin] y [CoverageNeed.avisoRecogerMin]. Cuando
 * salta, [AvisoReceiver] muestra la notificación.
 *
 * Las alarmas son locales a cada teléfono, y cada móvil programa solo las que le tocan
 * (ver SincronizadorDeAvisos): las de quien creó la actividad y las de quien lleva o recoge.
 */
@Singleton
class ProgramadorDeAvisos @Inject constructor(@ApplicationContext private val contexto: Context) {

    enum class Tipo { LLEVAR, RECOGER }

    /** Programa los avisos de [need] (quitando antes los que tuviera), si aún están en el
     * futuro. Independiente de [CoverageNeed.requiereDesplazamiento]: un aviso es un
     * recordatorio del inicio/fin de la actividad, haga falta o no que alguien la
     * acompañe. */
    fun programar(
        need: CoverageNeed,
        nombreNino: String?,
        avisarLlevar: Boolean = true,
        avisarRecoger: Boolean = true
    ) {
        cancelar(need.id)
        if (avisarLlevar) need.avisoLlevarMin?.let { programarUno(need, nombreNino, Tipo.LLEVAR, it) }
        if (avisarRecoger) need.avisoRecogerMin?.let { programarUno(need, nombreNino, Tipo.RECOGER, it) }
    }

    fun cancelar(id: CoverageNeedId) {
        val alarmas = contexto.getSystemService(AlarmManager::class.java) ?: return
        Tipo.values().forEach { tipo ->
            alarmas.cancel(pendingIntent(id, tipo, null, null))
        }
    }

    private fun programarUno(need: CoverageNeed, nombreNino: String?, tipo: Tipo, minutosAntes: Int) {
        val hora: LocalTime = if (tipo == Tipo.LLEVAR) need.horaInicio else need.horaFin
        val instante = need.fecha.atTime(hora).minusMinutes(minutosAntes.toLong())
        if (instante.isBefore(LocalDateTime.now())) return

        // Si la actividad requiere que alguien la acompañe, el título lo deja claro
        // ("Llevar a…"/"Recoger a…"); si no, es solo un recordatorio genérico de que
        // empieza o termina.
        val quien = nombreNino?.let { " a $it" } ?: ""
        val titulo = if (need.requiereDesplazamiento) {
            if (tipo == Tipo.LLEVAR) "Llevar$quien · ${need.descripcion}" else "Recoger$quien · ${need.descripcion}"
        } else {
            if (tipo == Tipo.LLEVAR) "Empieza · ${need.descripcion}" else "Termina · ${need.descripcion}"
        }
        val texto = if (tipo == Tipo.LLEVAR) {
            "Empieza a las ${hora.formatoCorto()} (dentro de ${textoMinutos(minutosAntes)})."
        } else {
            "Termina a las ${hora.formatoCorto()} (dentro de ${textoMinutos(minutosAntes)})."
        }

        val millis = instante.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmas = contexto.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(need.id, tipo, titulo, texto)
        // Alarma exacta si el sistema lo permite (Android 12+ puede exigir permiso);
        // si no, una aproximada, que puede retrasarse unos minutos.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmas.canScheduleExactAlarms()) {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun pendingIntent(id: CoverageNeedId, tipo: Tipo, titulo: String?, texto: String?): PendingIntent {
        val intent = Intent(contexto, AvisoReceiver::class.java).apply {
            action = "com.encaja.app.AVISO_ACTIVIDAD"
            putExtra(AvisoReceiver.EXTRA_ID, codigo(id, tipo))
            putExtra(AvisoReceiver.EXTRA_TITULO, titulo)
            putExtra(AvisoReceiver.EXTRA_TEXTO, texto)
        }
        return PendingIntent.getBroadcast(
            contexto, codigo(id, tipo), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Código distinto para cada actividad y tipo de aviso, estable entre ejecuciones. */
    private fun codigo(id: CoverageNeedId, tipo: Tipo): Int = (id.value + "#" + tipo.name).hashCode()

    private fun LocalTime.formatoCorto() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    private fun textoMinutos(min: Int): String = when {
        min < 60 -> "$min min"
        min % 60 == 0 -> if (min == 60) "1 hora" else "${min / 60} horas"
        else -> "${min / 60} h ${min % 60} min"
    }
}
