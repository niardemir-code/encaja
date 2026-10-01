package com.encaja.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable

/**
 * Colores de todos los interruptores de la app. Apagado, el interruptor se ve igual
 * de "sólido" que encendido (solo cambia de lado y de color), en vez de aclararse
 * como si estuviera deshabilitado, que es lo que hace Material por defecto.
 */
@Composable
fun coloresInterruptorEncaja(): SwitchColors {
    val extra = LocalEncajaExtraColors.current
    val scheme = MaterialTheme.colorScheme
    return SwitchDefaults.colors(
        checkedThumbColor = extra.onAcento,
        checkedTrackColor = extra.acento,
        checkedBorderColor = extra.acento,
        uncheckedThumbColor = scheme.onSurfaceVariant,
        uncheckedTrackColor = scheme.surfaceVariant,
        uncheckedBorderColor = scheme.onSurfaceVariant
    )
}
