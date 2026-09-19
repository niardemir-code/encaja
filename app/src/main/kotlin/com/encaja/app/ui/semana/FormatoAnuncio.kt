package com.encaja.app.ui.semana

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA_ANUNCIO: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es"))

/** "21 sept, 18:05" en vez del LocalDateTime en bruto, para firmar cada anuncio del tablón. */
fun formatearFechaAnuncio(fecha: LocalDateTime): String = fecha.format(FORMATO_FECHA_ANUNCIO)
