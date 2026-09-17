package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*

/**
 * Caso de uso central de la app. Para cada necesidad de cobertura del día,
 * comprueba si hay un cuidador asignado y, si lo hay, si ese cuidador está
 * realmente disponible a esa hora. Es exactamente el cruce que hemos
 * dibujado una y otra vez en las pantallas: asignación (fila de arriba)
 * contra disponibilidad (filas de abajo).
 */
class CalcularHuecosDelDia(
    private val patrones: List<PatronCuidado>,
    private val anulaciones: List<AnulacionAsignacion>,
    private val disponibilidad: List<AvailabilityBlock>
) {
    operator fun invoke(needs: List<CoverageNeed>): List<Hueco> {
        return needs.mapNotNull { need ->
            val asignado = resolverAsignacion(need.fecha, patrones, anulaciones)

            if (asignado == null) {
                Hueco(need, MotivoHueco.SIN_ASIGNACION)
            } else if (!estaDisponible(asignado, need)) {
                Hueco(need, MotivoHueco.ASIGNADO_NO_DISPONIBLE)
            } else {
                null
            }
        }
    }

    private fun estaDisponible(caregiverId: CaregiverId, need: CoverageNeed): Boolean {
        val bloqueoEseDia = disponibilidad.filter {
            it.caregiverId == caregiverId && it.fecha == need.fecha
        }
        return bloqueoEseDia.none { it.solapaCon(need.horaInicio, need.horaFin) }
    }
}
