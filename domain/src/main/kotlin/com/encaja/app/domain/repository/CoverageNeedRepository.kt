package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

interface CoverageNeedRepository {
    suspend fun obtenerNeeds(familyId: FamilyId, desde: LocalDate, hasta: LocalDate): List<CoverageNeed>
    suspend fun guardarNeeds(familyId: FamilyId, needs: List<CoverageNeed>)
    suspend fun eliminarNeed(familyId: FamilyId, needId: CoverageNeedId)
    /** Borra varias de golpe (p.ej. una selección hecha a mano en "Todas las actividades"),
     * sin tener que estar todas en la misma serie ni ser del mismo niño. */
    suspend fun eliminarNeeds(familyId: FamilyId, needIds: List<CoverageNeedId>)
    /** Todas las ocurrencias creadas juntas con "Repetir cada semana" (mismo grupoRepeticionId). */
    suspend fun obtenerNeedsDelGrupo(familyId: FamilyId, grupoRepeticionId: String): List<CoverageNeed>
    /** Todas las actividades de la familia, sin filtrar por fecha: para el listado completo
     * de "Todas las actividades" (Ajustes), donde también deben verse las antiguas que ya
     * no aparecen en el rango normal de la Guía. */
    suspend fun obtenerTodosLosNeeds(familyId: FamilyId): List<CoverageNeed>
}