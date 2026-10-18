package com.encaja.app.domain.repository

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

interface AvailabilityRepository {
    suspend fun obtenerDisponibilidad(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<AvailabilityBlock>
    /** Todos los bloques (de cualquier fecha) que comparten [grupoRepeticionId]: una serie. */
    suspend fun obtenerBloquesDelGrupo(familyId: FamilyId, grupoRepeticionId: String): List<AvailabilityBlock>
    suspend fun guardarBloque(familyId: FamilyId, bloque: AvailabilityBlock)
    /** Guarda muchos bloques de golpe (en tandas): una ocupación de varios meses tarda segundos, no minutos. */
    suspend fun guardarBloques(familyId: FamilyId, bloques: List<AvailabilityBlock>)
    suspend fun eliminarBloque(familyId: FamilyId, caregiverId: CaregiverId, fecha: LocalDate, horaInicio: java.time.LocalTime)
    /** Borra muchos bloques de golpe (en tandas), mucho más rápido que uno a uno. */
    suspend fun eliminarBloques(familyId: FamilyId, bloques: List<AvailabilityBlock>)
}