package com.encaja.app.ui.guia

import com.encaja.app.avisos.ProgramadorDeAvisos
import com.encaja.app.avisos.SincronizadorDeAvisos
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.usecase.ObtenerSerieDeActividad
import com.encaja.app.domain.usecase.propagarASerie
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
    private val serieDeActividad = ObtenerSerieDeActividad(coverageNeedRepository)

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
            val actualizadas = propagarASerie(plantilla, deLaSerie)
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
    suspend fun ocurrenciasDeSerie(familyId: FamilyId, need: CoverageNeed): List<CoverageNeed> =
        serieDeActividad(familyId, need)

    /**
     * Borra una actividad. Si [serieCompleta] es true, borra todas las veces que se
     * repite (las de su grupo, o las idénticas si es una serie anterior a los grupos;
     * ver [ocurrenciasDeSerie]), antes y después de esta; si no, solo [need].
     */
    suspend fun eliminar(familyId: FamilyId, need: CoverageNeed, serieCompleta: Boolean) {
        val ids = if (serieCompleta) {
            (ocurrenciasDeSerie(familyId, need).map { it.id } + need.id).distinct()
        } else {
            listOf(need.id)
        }
        if (ids.size > 1) coverageNeedRepository.eliminarNeeds(familyId, ids)
        else coverageNeedRepository.eliminarNeed(familyId, ids.first())
        ids.forEach { avisos.cancelar(it) }
    }
}
