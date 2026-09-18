package com.encaja.app.domain.repository

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.FamilyId

interface CaregiverRepository {
    suspend fun obtenerCuidadores(familyId: FamilyId): List<Caregiver>
}