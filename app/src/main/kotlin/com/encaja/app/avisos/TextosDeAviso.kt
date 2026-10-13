package com.encaja.app.avisos

import android.content.Context
import com.encaja.app.R

/**
 * Los textos de la notificación de un aviso de actividad. Se construyen en el momento en que
 * salta la alarma (no al programarla), así salen en el idioma que tenga el móvil entonces.
 */
object TextosDeAviso {

    /** Título y texto del aviso: [hora] es "HH:mm" y [minutosAntes] la antelación elegida. */
    fun construir(
        contexto: Context,
        tipo: ProgramadorDeAvisos.Tipo,
        nombreNino: String?,
        descripcion: String,
        acompanar: Boolean,
        hora: String,
        minutosAntes: Int
    ): Pair<String, String> {
        val llevar = tipo == ProgramadorDeAvisos.Tipo.LLEVAR
        // Si la actividad requiere que alguien la acompañe, el título lo deja claro
        // ("Llevar a…"/"Recoger a…"); si no, es solo un recordatorio genérico de que
        // empieza o termina.
        val titulo = when {
            acompanar && nombreNino != null ->
                contexto.getString(if (llevar) R.string.aviso_llevar_a else R.string.aviso_recoger_a, nombreNino, descripcion)
            acompanar ->
                contexto.getString(if (llevar) R.string.aviso_llevar else R.string.aviso_recoger, descripcion)
            else ->
                contexto.getString(if (llevar) R.string.aviso_empieza else R.string.aviso_termina, descripcion)
        }
        val texto = contexto.getString(
            if (llevar) R.string.aviso_texto_empieza else R.string.aviso_texto_termina,
            hora, duracion(contexto, minutosAntes)
        )
        return titulo to texto
    }

    private fun duracion(contexto: Context, min: Int): String = when {
        min < 60 -> contexto.getString(R.string.aviso_dur_min, min)
        min % 60 == 0 -> contexto.resources.getQuantityString(R.plurals.aviso_dur_horas, min / 60, min / 60)
        else -> contexto.getString(R.string.aviso_dur_h_min, min / 60, min % 60)
    }
}
