package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Cuenta cuántos tramos tiene asignados cada cuidador en un rango de
 * fechas (normalmente la semana en curso). Alimenta la barra de reparto
 * del semáforo y el criterio de orden de ProponerCuidadores.
 */
class CalcularRepartoSemanal(
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones
) {
    operator fun invoke(lunes: LocalDate): Map<CaregiverId, Int> {
        val dias = (0..6).map { lunes.plusDays(it.toLong()) }
        val reparto = mutableMapOf<CaregiverId, Int>()

        dias.forEach { fecha ->
            val asignado = resolverAsignacion(fecha, patrones, anulaciones)
            if (asignado != null) {
                reparto[asignado] = (reparto[asignado] ?: 0) + 1
            }
        }
        return reparto
    }
}

/** Ayuda a obtener el lunes de la semana que contiene una fecha. */
fun LocalDate.lunesDeEstaSemana(): LocalDate {
    var d = this
    while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
    return d
}
