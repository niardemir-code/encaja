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
val CalidoPrimaryContainer = Color(0xFFE7E2FA)
val CalidoOnPrimaryContainer = Color(0xFF3B2F86)
val CalidoSecondaryContainer = Color(0xFFF3EEFB)
val CalidoOnSecondaryContainer = Color(0xFF231F45)
val CalidoTertiaryContainer = Color(0xFFFBEFD6)
val CalidoOnTertiaryContainer = Color(0xFF7A5A16)
val CalidoError = Color(0xFFC24C3F)
val CalidoErrorContainer = Color(0xFFFAE4E1)
val CalidoOnErrorContainer = Color(0xFF7A2E24)
val CalidoBackground = Color(0xFFFBF7F1)
val CalidoOnBackground = Color(0xFF231F45)
val CalidoSurface = Color(0xFFFFFFFF)
val CalidoOnSurface = Color(0xFF231F45)
val CalidoOnSurfaceVariant = Color(0xFF655F86)
val CalidoOutline = Color(0xFFDDD7EF)
val CalidoVerde = Color(0xFF4C7A2B)
val CalidoVerdeContainer = Color(0xFFEAF3E1)

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
