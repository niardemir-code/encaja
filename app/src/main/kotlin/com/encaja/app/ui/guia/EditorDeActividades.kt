package com.encaja.app.ui.guia

import com.encaja.app.avisos.ProgramadorDeAvisos
import com.encaja.app.avisos.SincronizadorDeAvisos
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Las operaciones de edición de una actividad (guardar, borrar, cambiar el patrón de
 * una serie...) que necesita el DialogoActividad, fuera de cualquier ViewModel: así el
 * mismo diálogo puede abrirse tanto desde la Guía como desde un aviso de Semana, y
 * las reglas de las series viven en un único sitio. Cada ViewModel solo pone el
 * familyId y recarga lo suyo después.
 */
class EditorDeActividades @Inject constructor(
    private val coverageNeedRepository: CoverageNeedRepository,
    private val childRepository: ChildRepository,
    private val avisos: ProgramadorDeAvisos,
    private val sincronizador: SincronizadorDeAvisos
) {
    /** Programa en este móvil los avisos que le tocan de [needs] (creador, quien lleva, quien recoge). */
    private suspend fun programarAvisos(familyId: FamilyId, needs: List<CoverageNeed>) {
        if (needs.none { it.avisoLlevarMin != null || it.avisoRecogerMin != null }) {
            needs.forEach { avisos.cancelar(it.id) }
            return
        }
        sincronizador.programarMisAvisos(familyId, needs)
    }

    /**
     * Guarda una o varias actividades (varias si se crearon con "Repetir cada semana").
     * Si [aplicarATodaLaSerie] es true, [needs] trae una única ocurrencia editada que ya
     * pertenece a un grupo: sus cambios (todo menos la fecha) se copian a ella y a todas
     * las ocurrencias del grupo (anteriores y posteriores), conservando el id y la fecha
     * de cada una.
     */
    suspend fun guardar(familyId: FamilyId, necesidades: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) {
        if (necesidades.isEmpty()) return
        // Las actividades nuevas quedan firmadas por quien las crea (recibirá sus avisos).
        val needs = sincronizador.sellarCreador(necesidades)
        if (aplicarATodaLaSerie) {
            val plantilla = needs.first()
            val grupoId = plantilla.grupoRepeticionId
            val deLaSerie = if (grupoId != null) {
                coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoId)
                    .ifEmpty { listOf(plantilla) }
            } else {
                listOf(plantilla)
            }
            val actualizadas = deLaSerie.map { existente -> plantilla.copy(id = existente.id, fecha = existente.fecha) }
            coverageNeedRepository.guardarNeeds(familyId, actualizadas)
            programarAvisos(familyId, actualizadas)
        } else {
            coverageNeedRepository.guardarNeeds(familyId, needs)
            programarAvisos(familyId, needs)
        }
    }

    /**
     * Todas las veces que se repite [need] (ella incluida): las de su grupo si lo tiene y
     * hay más de una; si no (actividades creadas antes de existir las series, o cuyo grupo
     * quedó con una sola), las que son idénticas — mismo niño, nombre y horario — en el año
     * anterior y posterior a su fecha. Si solo sale ella misma, no hay serie.
     */
    suspend fun ocurrenciasDeSerie(familyId: FamilyId, need: CoverageNeed): List<CoverageNeed> {
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

    /**
     * Borra una actividad. Si [aplicarATodaLaSerie] es true y pertenece a un grupo
     * ([grupoRepeticionId]), borra también todas las ocurrencias posteriores (desde
     * [fecha] en adelante) de esa misma serie.
     */
    suspend fun eliminar(
        familyId: FamilyId,
        id: CoverageNeedId,
        grupoRepeticionId: String?,
        fecha: LocalDate,
        aplicarATodaLaSerie: Boolean
    ) {
        if (aplicarATodaLaSerie && grupoRepeticionId != null) {
            val idsABorrar = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoRepeticionId)
                .filter { !it.fecha.isBefore(fecha) }
                .map { it.id }
            coverageNeedRepository.eliminarNeeds(familyId, idsABorrar)
            idsABorrar.forEach { avisos.cancelar(it) }
        } else {
            coverageNeedRepository.eliminarNeed(familyId, id)
            avisos.cancelar(id)
        }
    }

    /**
     * El patrón real (días de la semana y última fecha) con el que se repite hoy la
     * serie [grupoRepeticionId], mirando solo las ocurrencias a partir de [desde] — para
     * preseleccionar el selector de días al abrir "Repetir" sobre una serie ya existente.
     */
    suspend fun patronDeSerie(familyId: FamilyId, grupoRepeticionId: String, desde: LocalDate): Pair<Set<DayOfWeek>, LocalDate?> {
        val ocurrencias = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoRepeticionId)
            .filter { !it.fecha.isBefore(desde) }
        val dias = ocurrencias.map { it.fecha.dayOfWeek }.toSet()
        val hasta = ocurrencias.maxByOrNull { it.fecha }?.fecha
        return dias to hasta
    }

    /**
     * Cambia el patrón semanal de la serie de [plantilla] a partir de su fecha: las
     * ocurrencias de [nuevasFechas] que ya existían conservan su id, las nuevas se crean
     * y las que ya no encajan en el patrón se borran (ver [diferenciaSerie]).
     */
    suspend fun actualizarSerie(familyId: FamilyId, plantillaOriginal: CoverageNeed, nuevasFechas: List<LocalDate>) {
        val plantilla = sincronizador.sellarCreador(listOf(plantillaOriginal)).first()
        val grupoId = plantilla.grupoRepeticionId ?: return
        val existentesDesdeSuFecha = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoId)
            .filter { !it.fecha.isBefore(plantilla.fecha) }
        val (aGuardar, aBorrar) = diferenciaSerie(plantilla, existentesDesdeSuFecha, nuevasFechas) {
            CoverageNeedId(UUID.randomUUID().toString())
        }
        if (aGuardar.isNotEmpty()) coverageNeedRepository.guardarNeeds(familyId, aGuardar)
        if (aBorrar.isNotEmpty()) coverageNeedRepository.eliminarNeeds(familyId, aBorrar.map { it.id })
        programarAvisos(familyId, aGuardar)
        aBorrar.forEach { avisos.cancelar(it.id) }
    }
}
