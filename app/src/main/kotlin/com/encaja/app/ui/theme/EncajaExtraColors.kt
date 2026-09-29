package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose, no compilado en este entorno.
// Colores adicionales que Material3 no cubre de fábrica (el "verde de todo
// cubierto" del semáforo y de las casillas libres de disponibilidad, más los
// tonos propios del diseño de la pantalla Semana), a juego con el resto del
// ColorScheme del tema activo (Cálido/Nocturno).

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class EncajaExtraColors(
    val verdeContainer: Color,
    val onVerdeContainer: Color,
    /** Tarjeta lavanda muy suave (cabecera de la semana). */
    val tarjetaSuave: Color,
    /** Violeta del botón principal "Publicar" y su texto. */
    val acento: Color,
    val onAcento: Color,
    /** Ámbar fuerte: franja lateral, punto de "hoy" y chip "Importante" de los avisos. */
    val ambar: Color,
    /** Fondo crema de la tarjeta de aviso y rosado de la tarjeta de hueco. */
    val cremaAviso: Color,
    val rosaHueco: Color
)

val ColoresExtraCalido = EncajaExtraColors(
    verdeContainer = CalidoVerdeContainer,
    onVerdeContainer = CalidoVerde,
    tarjetaSuave = CalidoTarjetaSuave,
    acento = CalidoAcento,
    onAcento = CalidoOnAcento,
    ambar = CalidoAmbar,
    cremaAviso = CalidoCremaAviso,
    rosaHueco = CalidoRosaHueco
)

val ColoresExtraNocturno = EncajaExtraColors(
    verdeContainer = NocturnoVerdeContainer,
    onVerdeContainer = NocturnoVerde,
    tarjetaSuave = NocturnoTarjetaSuave,
    acento = NocturnoAcento,
    onAcento = NocturnoOnAcento,
    ambar = NocturnoAmbar,
    cremaAviso = NocturnoCremaAviso,
    rosaHueco = NocturnoRosaHueco
)

val LocalEncajaExtraColors = staticCompositionLocalOf { ColoresExtraCalido }
