package com.encaja.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import com.encaja.app.domain.model.MotivoNoDisponibilidad

/** El nombre del motivo de no disponibilidad en el idioma del móvil («Trabajo», «Feina», «Work»). */
@Composable
fun etiquetaDeMotivo(motivo: MotivoNoDisponibilidad): String = stringResource(
    when (motivo) {
        MotivoNoDisponibilidad.TRABAJO -> R.string.motivo_trabajo
        MotivoNoDisponibilidad.MEDICO -> R.string.motivo_medico
        MotivoNoDisponibilidad.VIAJE -> R.string.motivo_viaje
        MotivoNoDisponibilidad.VACACIONES -> R.string.motivo_vacaciones
        MotivoNoDisponibilidad.OTRO -> R.string.motivo_otro
    }
)

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
