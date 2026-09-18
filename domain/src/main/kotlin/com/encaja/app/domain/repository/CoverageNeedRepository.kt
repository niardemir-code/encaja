package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

interface CoverageNeedRepository {
    suspend fun obtenerNeeds(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<CoverageNeed>
}