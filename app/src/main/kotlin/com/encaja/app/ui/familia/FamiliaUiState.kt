package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import java.time.LocalDate

/** Quién tiene asignado el cuidado ese día — null si no hay patrón ni anulación para esa fecha. */
data class DiaAsignado(val fecha: LocalDate, val caregiverId: CaregiverId?)

data class DiaDisponibilidadCuidador(val fecha: LocalDate, val bloqueos: List<AvailabilityBlock>) {
    val libre: Boolean get() = bloqueos.isEmpty()
}

data class CuidadorDisponibilidadSemana(val caregiver: Caregiver, val dias: List<DiaDisponibilidadCuidador>)

data class FamiliaUiState(
    val diasAsignacion: List<DiaAsignado>,
    val cuidadores: List<CuidadorDisponibilidadSemana>
)
