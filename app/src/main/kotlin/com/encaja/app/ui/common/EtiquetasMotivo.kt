package com.encaja.app.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.MotivoNoDisponibilidad

@StringRes
private fun idEtiquetaDeMotivo(motivo: MotivoNoDisponibilidad): Int = when (motivo) {
    MotivoNoDisponibilidad.TRABAJO -> R.string.motivo_trabajo
    MotivoNoDisponibilidad.MEDICO -> R.string.motivo_medico
    MotivoNoDisponibilidad.VIAJE -> R.string.motivo_viaje
    MotivoNoDisponibilidad.VACACIONES -> R.string.motivo_vacaciones
    MotivoNoDisponibilidad.OTRO -> R.string.motivo_otro
}

/** El nombre del motivo de no disponibilidad en el idioma del móvil («Trabajo», «Feina», «Work»). */
@Composable
fun etiquetaDeMotivo(motivo: MotivoNoDisponibilidad): String = stringResource(idEtiquetaDeMotivo(motivo))

/** El motivo de serie de [CategoriaDisponibilidad] si su nombre sigue siendo el de serie (no lo ha renombrado la familia). */
private fun CategoriaDisponibilidad.motivoSiNombreDeSerie(): MotivoNoDisponibilidad? {
    val motivo = base ?: return null
    return if (nombre == CategoriasBase.predeterminadas.first { it.base == motivo }.nombre) motivo else null
}

/**
 * El nombre de la categoría tal como se enseña: las 5 de serie que la familia no ha renombrado
 * salen en el idioma del móvil («Feina», «Work»…); si la familia les puso nombre propio, o es una
 * categoría propia, se respeta tal cual. Lo guardado en los datos no cambia.
 */
@Composable
fun CategoriaDisponibilidad.nombreVisible(): String {
    val motivo = motivoSiNombreDeSerie() ?: return nombre
    return stringResource(idEtiquetaDeMotivo(motivo))
}

/** Igual que [nombreVisible], para código que no es composable (filtros, comparaciones). */
fun CategoriaDisponibilidad.nombreVisible(context: Context): String {
    val motivo = motivoSiNombreDeSerie() ?: return nombre
    return context.getString(idEtiquetaDeMotivo(motivo))
}

/** Versión corta del nombre del motivo, para la casilla del día de la cuadrícula, que es estrecha. */
@Composable
fun etiquetaCortaDeMotivo(motivo: MotivoNoDisponibilidad): String = stringResource(
    when (motivo) {
        MotivoNoDisponibilidad.TRABAJO -> R.string.motivo_corto_trabajo
        MotivoNoDisponibilidad.MEDICO -> R.string.motivo_corto_medico
        MotivoNoDisponibilidad.VIAJE -> R.string.motivo_corto_viaje
        MotivoNoDisponibilidad.VACACIONES -> R.string.motivo_corto_vacaciones
        MotivoNoDisponibilidad.OTRO -> R.string.motivo_corto_otro
    }
)
