package com.encaja.app.domain.repository

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

interface AvailabilityRepository {
    suspend fun obtenerDisponibilidad(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<AvailabilityBlock>
    suspend fun guardarBloque(familyId: FamilyId, bloque: AvailabilityBlock)
    suspend fun eliminarBloque(familyId: FamilyId, caregiverId: CaregiverId, fecha: LocalDate, horaInicio: java.time.LocalTime)
}