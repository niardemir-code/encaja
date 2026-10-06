package com.encaja.app.domain.repository

import com.encaja.app.domain.model.FamilyId
import java.time.LocalDate

/**
 * Filtro "a quién mostrar" de la pantalla Familia que se comparte con todos los usuarios de
 * la familia, por semana (clave = lunes de esa semana). Que exista un filtro para una semana
 * significa que está activado el "aplicar a todos los usuarios" de esa semana.
 */
interface FiltroFamiliaRepository {
    /** Ids (persona o unidad) ocultos para todos en esa semana, o null si no hay filtro compartido. */
    suspend fun obtener(familyId: FamilyId, lunes: LocalDate): Set<String>?

    /** Activa o actualiza el filtro compartido de esa semana. */
    suspend fun guardar(familyId: FamilyId, lunes: LocalDate, ocultos: Set<String>)

    /** Quita el filtro compartido de esa semana: cada uno vuelve a su filtro personal. */
    suspend fun quitar(familyId: FamilyId, lunes: LocalDate)
}
