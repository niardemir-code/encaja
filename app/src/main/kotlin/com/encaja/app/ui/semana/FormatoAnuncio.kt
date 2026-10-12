package com.encaja.app.ui.semana

import com.encaja.app.ui.common.localeApp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "21 sept, 18:05" en vez del LocalDateTime en bruto, para firmar cada anuncio del tablón. */
fun formatearFechaAnuncio(fecha: LocalDateTime, locale: Locale = localeApp()): String =
    fecha.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", locale))
