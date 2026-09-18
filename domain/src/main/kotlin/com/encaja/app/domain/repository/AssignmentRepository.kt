package com.encaja.app.domain.repository

import com.encaja.app.domain.model.Anulaciones
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.PatronCuidado
import java.time.LocalDate

interface AssignmentRepository {
    suspend fun obtenerPatrones(familyId: FamilyId): List<PatronCuidado>
    suspend fun obtenerAnulaciones(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): Anulaciones
    suspend fun guardarPatrones(familyId: FamilyId, patrones: List<PatronCuidado>)
    suspend fun anularParaFecha(familyId: FamilyId, fecha: LocalDate, caregiverId: com.encaja.app.domain.model.CaregiverId)
}