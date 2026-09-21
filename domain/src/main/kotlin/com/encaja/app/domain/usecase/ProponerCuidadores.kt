package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*

/**
 * Dado un hueco, propone quién podría cubrirlo: solo cuidadores libres a
 * esa hora, excluyendo a quien no puede desplazarse si la necesidad lo
 * requiere, y ordenados por quién menos tramos lleva cubiertos esta
 * semana — el mismo criterio de reparto justo que vimos en el semáforo.
 */
class ProponerCuidadores(
    private val caregivers: List<Caregiver>,
    private val disponibilidad: List<AvailabilityBlock>,
    private val tramosCubiertosEstaSemana: Map<CaregiverId, Int>
) {
    operator fun invoke(hueco: Hueco): List<Caregiver> {
        val need = hueco.need

        return caregivers
            .filter { caregiver ->
                val libre = disponibilidad
                    .filter { it.caregiverId == caregiver.id }
                    .none { it.ocupa(need.fecha, need.horaInicio, need.horaFin) }

                val aptoParaDesplazarse = !need.requiereDesplazamiento || caregiver.puedeDesplazarse

                libre && aptoParaDesplazarse
            }
            .sortedBy { tramosCubiertosEstaSemana[it.id] ?: 0 }
    }
}
