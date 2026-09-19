package com.encaja.app.domain.repository

import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.FamilyId

interface ChildRepository {
    suspend fun obtenerNinos(familyId: FamilyId): List<Child>
    suspend fun guardarNinos(familyId: FamilyId, ninos: List<Child>)
    suspend fun eliminarNino(familyId: FamilyId, childId: ChildId)
}
