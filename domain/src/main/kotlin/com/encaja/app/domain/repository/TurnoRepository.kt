package com.encaja.app.domain.repository

import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo

interface TurnoRepository {
    suspend fun obtenerTurnos(familyId: FamilyId): List<TurnoTrabajo>
    suspend fun guardarTurno(familyId: FamilyId, turno: TurnoTrabajo)
    suspend fun eliminarTurno(familyId: FamilyId, turnoId: TurnoId)
}
