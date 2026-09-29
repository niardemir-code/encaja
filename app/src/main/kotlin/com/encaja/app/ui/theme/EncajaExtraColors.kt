package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose, no compilado en este entorno.
// Colores adicionales que Material3 no cubre de fábrica (el "verde de todo
// cubierto" del semáforo y de las casillas libres de disponibilidad), a
// juego con el resto del ColorScheme del tema activo (Cálido/Nocturno).

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class EncajaExtraColors(
    val verdeContainer: Color,
    val onVerdeContainer: Color
)

private val ColoresPorDefecto = EncajaExtraColors(
    verdeContainer = CalidoVerdeContainer,
    onVerdeContainer = CalidoVerde
)

val LocalEncajaExtraColors = staticCompositionLocalOf { ColoresPorDefecto }
