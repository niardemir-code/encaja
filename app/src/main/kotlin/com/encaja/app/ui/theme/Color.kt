package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose, no compilado en este entorno.
// Paleta de los dos estilos elegidos para el rediseño de Encaja:
//  - "Cálido": tema claro, el de siempre.
//  - "Nocturno": tema oscuro.
// EncajaTheme (Theme.kt) elige uno u otro según el tema del sistema del
// teléfono; ninguna pantalla debería usar estos valores directamente, solo
// a través de MaterialTheme.colorScheme o LocalEncajaExtraColors.

import androidx.compose.ui.graphics.Color

// ---- Cálido (claro) ----
val CalidoPrimary = Color(0xFF3B2F86)
val CalidoOnPrimary = Color(0xFFFFFFFF)
val CalidoPrimaryContainer = Color(0xFFE3E0F8)
val CalidoOnPrimaryContainer = Color(0xFF3B2F86)
val CalidoSecondaryContainer = Color(0xFFE9E6F9)
val CalidoOnSecondaryContainer = Color(0xFF231F45)
val CalidoTertiaryContainer = Color(0xFFFCE7C2)
val CalidoOnTertiaryContainer = Color(0xFF7A5A16)
val CalidoError = Color(0xFFC24C3F)
val CalidoErrorContainer = Color(0xFFFAE4E1)
val CalidoOnErrorContainer = Color(0xFF7A2E24)
val CalidoBackground = Color(0xFFF6F5FC)
val CalidoOnBackground = Color(0xFF231F45)
val CalidoSurface = Color(0xFFFFFFFF)
val CalidoOnSurface = Color(0xFF231F45)
val CalidoOnSurfaceVariant = Color(0xFF6B6690)
val CalidoOutline = Color(0xFFDDD7EF)
val CalidoVerde = Color(0xFF1E6B45)
val CalidoVerdeContainer = Color(0xFFD6F1E3)
// Extras del diseño (ver EncajaExtraColors): tarjeta lavanda suave de la cabecera de
// Semana, violeta del botón "Publicar", ámbar fuerte de la franja/punto/chip de aviso
// y fondo crema de la tarjeta de aviso.
val CalidoTarjetaSuave = Color(0xFFEFEDFB)
val CalidoAcento = Color(0xFF6C5CE7)
val CalidoOnAcento = Color(0xFFFFFFFF)
val CalidoAmbar = Color(0xFFF2B33D)
val CalidoCremaAviso = Color(0xFFFDF0DA)
val CalidoRosaHueco = Color(0xFFFBE7E4)

// ---- Nocturno (oscuro) ----
val NocturnoPrimary = Color(0xFFFFB776)
val NocturnoOnPrimary = Color(0xFF15131F)
val NocturnoPrimaryContainer = Color(0xFF2A2438)
val NocturnoOnPrimaryContainer = Color(0xFFFFB776)
val NocturnoSecondaryContainer = Color(0xFF1E1B2C)
val NocturnoOnSecondaryContainer = Color(0xFFF5F3FF)
val NocturnoTertiaryContainer = Color(0xFF362B18)
val NocturnoOnTertiaryContainer = Color(0xFFFFC857)
val NocturnoError = Color(0xFFFF6B6B)
val NocturnoErrorContainer = Color(0xFF3A2024)
val NocturnoOnErrorContainer = Color(0xFFFF9D9D)
val NocturnoBackground = Color(0xFF15131F)
val NocturnoOnBackground = Color(0xFFF5F3FF)
val NocturnoSurface = Color(0xFF1E1B2C)
val NocturnoOnSurface = Color(0xFFF5F3FF)
val NocturnoOnSurfaceVariant = Color(0xFFA79FC7)
val NocturnoOutline = Color(0xFF35304A)
val NocturnoVerde = Color(0xFF7BD88F)
val NocturnoVerdeContainer = Color(0xFF1E2E22)
val NocturnoTarjetaSuave = Color(0xFF1F1C30)
val NocturnoAcento = Color(0xFF8B7CF6)
val NocturnoOnAcento = Color(0xFFFFFFFF)
val NocturnoAmbar = Color(0xFFF2B33D)
val NocturnoCremaAviso = Color(0xFF2E2618)
val NocturnoRosaHueco = Color(0xFF34201F)
