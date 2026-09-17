package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*

/**
 * Caso de uso central de la app. Para cada necesidad de cobertura del día,
 * comprueba tres cosas en orden: si hay alguien asignado, si esa persona
 * está disponible a esa hora, y si además puede desplazarse cuando la
 * tarea lo requiere (el caso de un cuidador que solo puede "estar con"
 * el niño, no recogerlo o llevarlo a ningún sitio).
 */
class CalcularHuecosDelDia(
    private val caregivers: List<Caregiver>,
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones,
    private val disponibilidad: List<AvailabilityBlock>
) {
    operator fun invoke(needs: List<CoverageNeed>): List<Hueco> {
        return needs.mapNotNull { need ->
            val asignadoId = resolverAsignacion(need.fecha, patrones, anulaciones)

            when {
                asignadoId == null -> Hueco(need, MotivoHueco.SIN_ASIGNACION)
                !estaDisponible(asignadoId, need) -> Hueco(need, MotivoHueco.ASIGNADO_NO_DISPONIBLE)
                !puedeCubrirEnPersona(asignadoId, need) -> Hueco(need, MotivoHueco.ASIGNADO_SIN_DESPLAZAMIENTO)
                else -> null
            }
        }
    }

    private fun estaDisponible(caregiverId: CaregiverId, need: CoverageNeed): Boolean {
        val bloqueoEseDia = disponibilidad.filter {
            it.caregiverId == caregiverId && it.fecha == need.fecha
        }
        return bloqueoEseDia.none { it.solapaCon(need.horaInicio, need.horaFin) }
    }

    /**
     * Si la tarea no requiere desplazamiento, cualquiera asignado vale.
     * Si lo requiere, hace falta que el cuidador pueda desplazarse. Un
     * caregiverId que no aparezca en la lista se trata como "puede":
     * evita bloquear por un dato incompleto en vez de avisar de un hueco.
     */
    private fun puedeCubrirEnPersona(caregiverId: CaregiverId, need: CoverageNeed): Boolean {
        if (!need.requiereDesplazamiento) return true
        val caregiver = caregivers.find { it.id == caregiverId } ?: return true
        return caregiver.puedeDesplazarse
    }
}