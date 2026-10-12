package com.encaja.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate

/** Ayuda a obtener el lunes de la semana que contiene una fecha. */
fun LocalDate.lunesDeEstaSemana(): LocalDate {
    var d = this
    while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
    return d
}
