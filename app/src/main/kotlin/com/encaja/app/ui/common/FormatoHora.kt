package com.encaja.app.ui.common

import java.time.LocalTime

/** "06:05": hora de 24 h con dos dígitos, el formato único de toda la app. */
fun formatearHora(hora: LocalTime): String =
    "${hora.hour.toString().padStart(2, '0')}:${hora.minute.toString().padStart(2, '0')}"
