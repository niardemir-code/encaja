package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.CoverageNeedRepository

/**
 * Reglas de las series de actividades ("Repetir cada semana"), en un único sitio y sin
 * depender de Android: las usa el editor de actividades de la app y se prueban con JUnit.
 */
class ObtenerSerieDeActividad(private val coverageNeedRepository: CoverageNeedRepository) {

    /**
     * Todas las veces que se repite [need] (ella incluida), ordenadas por fecha: las de su
     * grupo si lo tiene y hay más de una; si no (actividades creadas antes de existir las
     * series, o cuyo grupo quedó con una sola), las que son idénticas — mismo niño, nombre
     * (sin distinguir mayúsculas ni espacios en los extremos) y horario — en el año anterior
     * y posterior a su fecha. Si solo sale ella misma, no hay serie.
     */
    suspend operator fun invoke(familyId: FamilyId, need: CoverageNeed): List<CoverageNeed> {
        val grupoId = need.grupoRepeticionId
        if (grupoId != null) {
            val delGrupo = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoId)
            if (delGrupo.size > 1) return delGrupo.sortedBy { it.fecha }
        }
        val nombre = need.descripcion.trim()
        val iguales = coverageNeedRepository.obtenerNeeds(familyId, need.fecha.minusYears(1), need.fecha.plusYears(1))
            .filter {
                it.childId == need.childId &&
                    it.descripcion.trim().equals(nombre, ignoreCase = true) &&
                    it.horaInicio == need.horaInicio && it.horaFin == need.horaFin &&
                    (it.grupoRepeticionId == null || it.grupoRepeticionId == grupoId)
            }
        return (if (iguales.any { it.id == need.id }) iguales else iguales + need).sortedBy { it.fecha }
    }
}

/**
 * Copia los cambios de [plantilla] (todo menos el id y la fecha) a cada una de las
 * [existentes] de la serie, conservando el id y la fecha de cada una.
 */
fun propagarASerie(plantilla: CoverageNeed, existentes: List<CoverageNeed>): List<CoverageNeed> =
    existentes.map { existente -> plantilla.copy(id = existente.id, fecha = existente.fecha) }
