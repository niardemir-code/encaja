package com.encaja.app.domain.repository

import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId

interface FamilyUnitRepository {
    suspend fun obtenerUnidades(familyId: FamilyId): List<FamilyUnit>
    suspend fun guardarUnidades(familyId: FamilyId, unidades: List<FamilyUnit>)
    suspend fun eliminarUnidad(familyId: FamilyId, unidadId: FamilyUnitId)
}
