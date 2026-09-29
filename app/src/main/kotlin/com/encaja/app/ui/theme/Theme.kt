package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose, no compilado en este entorno.
// Tema de Encaja: aplica el estilo "Cálido" (claro) o "Nocturno" (oscuro).
// Por defecto sigue el tema del sistema del teléfono, pero se puede forzar
// uno de los dos desde Ajustes > Tema (ver PreferenciaTema.kt); MainActivity
// decide el [darkTheme] que se pasa aquí según esa preferencia.

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val EsquemaCalido = lightColorScheme(
    primary = CalidoPrimary,
    onPrimary = CalidoOnPrimary,
    primaryContainer = CalidoPrimaryContainer,
    onPrimaryContainer = CalidoOnPrimaryContainer,
    secondaryContainer = CalidoSecondaryContainer,
    onSecondaryContainer = CalidoOnSecondaryContainer,
    tertiaryContainer = CalidoTertiaryContainer,
    onTertiaryContainer = CalidoOnTertiaryContainer,
    error = CalidoError,
    errorContainer = CalidoErrorContainer,
    onErrorContainer = CalidoOnErrorContainer,
    background = CalidoBackground,
    onBackground = CalidoOnBackground,
    surface = CalidoSurface,
    onSurface = CalidoOnSurface,
    surfaceVariant = CalidoSecondaryContainer,
    onSurfaceVariant = CalidoOnSurfaceVariant,
    outline = CalidoOutline
)

private val EsquemaNocturno = darkColorScheme(
    primary = NocturnoPrimary,
    onPrimary = NocturnoOnPrimary,
    primaryContainer = NocturnoPrimaryContainer,
    onPrimaryContainer = NocturnoOnPrimaryContainer,
    secondaryContainer = NocturnoSecondaryContainer,
    onSecondaryContainer = NocturnoOnSecondaryContainer,
    tertiaryContainer = NocturnoTertiaryContainer,
    onTertiaryContainer = NocturnoOnTertiaryContainer,
    error = NocturnoError,
    errorContainer = NocturnoErrorContainer,
    onErrorContainer = NocturnoOnErrorContainer,
    background = NocturnoBackground,
    onBackground = NocturnoOnBackground,
    surface = NocturnoSurface,
    onSurface = NocturnoOnSurface,
    surfaceVariant = NocturnoSecondaryContainer,
    onSurfaceVariant = NocturnoOnSurfaceVariant,
    outline = NocturnoOutline
)

@Composable
fun EncajaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EsquemaNocturno else EsquemaCalido
    val coloresExtra = if (darkTheme) ColoresExtraNocturno else ColoresExtraCalido

    CompositionLocalProvider(LocalEncajaExtraColors provides coloresExtra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EncajaTypography,
            content = content
        )
    }
}
