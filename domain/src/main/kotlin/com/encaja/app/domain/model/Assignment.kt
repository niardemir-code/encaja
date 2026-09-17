package com.encaja.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Regla recurrente: "los miércoles cuida Carmen". Se define una vez y
 * se aplica cada semana, igual que un patrón de turno de trabajo.
 */
data class PatronCuidado(
    val diaSemana: DayOfWeek,
    val caregiverId: CaregiverId
)

/**
 * Cambio manual para UNA fecha concreta, que anula el patrón solo esa
 * semana sin tocar la regla de fondo. La semana siguiente el día vuelve
 * a resolverse por el patrón.
 */
data class AnulacionAsignacion(
    val fecha: LocalDate,
    val caregiverId: CaregiverId
)

/**
 * Resuelve, para una fecha dada, quién tiene asignado el cuidado según
 * el patrón, salvo que exista una anulación manual para ese día exacto.
 */
fun resolverAsignacion(
    fecha: LocalDate,
    patrones: List<PatronCuidado>,
    anulaciones: List<AnulacionAsignacion>
): CaregiverId? {
    anulaciones.firstOrNull { it.fecha == fecha }?.let { return it.caregiverId }
    return patrones.firstOrNull { it.diaSemana == fecha.dayOfWeek }?.caregiverId
}
