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
 * Cambios manuales para fechas concretas, que anulan el patrón solo esa
 * semana sin tocar la regla de fondo. Un Map en vez de una lista: como
 * mucho una anulación por fecha, así el tipo ya lo garantiza y no hace
 * falta decidir "cuál gana" si hubiera dos para el mismo día.
 */
typealias Anulaciones = Map<LocalDate, CaregiverId>

/**
 * Resuelve, para una fecha dada, quién tiene asignado el cuidado según
 * el patrón, salvo que exista una anulación manual para ese día exacto.
 */
fun resolverAsignacion(
    fecha: LocalDate,
    patrones: List<PatronCuidado>,
    anulaciones: Anulaciones
): CaregiverId? {
    anulaciones[fecha]?.let { return it }
    return patrones.firstOrNull { it.diaSemana == fecha.dayOfWeek }?.caregiverId
}