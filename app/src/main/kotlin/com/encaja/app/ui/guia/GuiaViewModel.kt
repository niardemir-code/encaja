package com.encaja.app.ui.guia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GuiaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val childRepository: ChildRepository,
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository,
    private val familyUnitRepository: FamilyUnitRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<GuiaPantallaEstado>(GuiaPantallaEstado.Cargando)
    val pantalla: StateFlow<GuiaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null
    private var fechaActual: LocalDate = LocalDate.now()

    init {
        cargar()
    }

    /** Recarga completa: vuelve a comprobar sesión y familia (por si han cambiado). Se
     * usa al reentrar en la pestaña, para reflejar cambios hechos desde otra pantalla. */
    fun recargar() = cargar()

    fun diaAnterior() {
        fechaActual = fechaActual.minusDays(1)
        cargarDia()
    }

    fun diaSiguiente() {
        fechaActual = fechaActual.plusDays(1)
        cargarDia()
    }

    fun hoy() {
        fechaActual = LocalDate.now()
        cargarDia()
    }

    /** Salta directamente al día elegido en el calendario. */
    fun irADia(fecha: LocalDate) {
        fechaActual = fecha
        cargarDia()
    }

    /** Primera carga (o recarga forzada): valida sesión y familia y solo entonces
     * pide los datos del día. */
    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = GuiaPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId
            cargarDia()
        }
    }

    /** Recarga solo los datos del día actual: cambiar de día, o guardar/borrar una
     * actividad, no necesita volver a comprobar sesión ni familia. */
    private fun cargarDia() {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            val fecha = fechaActual

            // Las seis lecturas son independientes entre sí, así que se lanzan todas a la
            // vez en vez de esperarlas una detrás de otra: cambiar de día tarda lo que
            // tarda la más lenta, no la suma de las seis.
            coroutineScope {
                val ninosDeferred = async { childRepository.obtenerNinos(familyId) }
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(familyId) }
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(familyId) }
                val patronesDeferred = async { assignmentRepository.obtenerPatrones(familyId) }
                val anulacionesDeferred = async { assignmentRepository.obtenerAnulaciones(familyId, fecha, fecha) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(familyId, fecha, fecha) }
                val needsDeferred = async { coverageNeedRepository.obtenerNeeds(familyId, fecha, fecha) }

                val mapper = GuiaUiStateMapper(
                    ninosDeferred.await(),
                    caregiversDeferred.await(),
                    unidadesDeferred.await(),
                    patronesDeferred.await(),
                    anulacionesDeferred.await(),
                    disponibilidadDeferred.await()
                )
                _pantalla.value = GuiaPantallaEstado.ConDatos(mapper.construir(fecha, needsDeferred.await()))
            }
        }
    }

    /**
     * Guarda una o varias actividades (varias si se crearon con "Repetir cada semana").
     * Si [aplicarATodaLaSerie] es true, [needs] trae una única ocurrencia editada que ya
     * pertenece a un grupo: sus cambios (todo menos la fecha) se copian a ella y a todas
     * las ocurrencias posteriores del grupo, conservando el id y la fecha de cada una.
     */
    fun guardarActividades(needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        if (needs.isEmpty()) return
        viewModelScope.launch {
            if (aplicarATodaLaSerie) {
                val plantilla = needs.first()
                val grupoId = plantilla.grupoRepeticionId
                val posteriores = if (grupoId != null) {
                    coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoId)
                        .filter { !it.fecha.isBefore(plantilla.fecha) }
                } else {
                    listOf(plantilla)
                }
                val actualizadas = posteriores.map { existente -> plantilla.copy(id = existente.id, fecha = existente.fecha) }
                coverageNeedRepository.guardarNeeds(familyId, actualizadas)
            } else {
                coverageNeedRepository.guardarNeeds(familyId, needs)
            }
            cargarDia()
        }
    }

    /**
     * Borra una actividad. Si [aplicarATodaLaSerie] es true y pertenece a un grupo
     * ([grupoRepeticionId]), borra también todas las ocurrencias posteriores (desde
     * [fecha] en adelante) de esa misma serie.
     */
    fun eliminarActividad(id: CoverageNeedId, grupoRepeticionId: String?, fecha: LocalDate, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            if (aplicarATodaLaSerie && grupoRepeticionId != null) {
                val idsABorrar = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoRepeticionId)
                    .filter { !it.fecha.isBefore(fecha) }
                    .map { it.id }
                coverageNeedRepository.eliminarNeeds(familyId, idsABorrar)
            } else {
                coverageNeedRepository.eliminarNeed(familyId, id)
            }
            cargarDia()
        }
    }

    /**
     * El patrón real (días de la semana y última fecha) con el que se repite hoy la
     * serie [grupoRepeticionId], mirando solo las ocurrencias a partir de [desde] — para
     * preseleccionar el selector de días al abrir "Repetir" sobre una serie ya existente.
     */
    suspend fun patronDeSerie(grupoRepeticionId: String, desde: LocalDate): Pair<Set<DayOfWeek>, LocalDate?> {
        val familyId = familyIdActual ?: return emptySet<DayOfWeek>() to null
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
    fun actualizarSerie(plantilla: CoverageNeed, nuevasFechas: List<LocalDate>) {
        val familyId = familyIdActual ?: return
        val grupoId = plantilla.grupoRepeticionId ?: return
        viewModelScope.launch {
            val existentesDesdeSuFecha = coverageNeedRepository.obtenerNeedsDelGrupo(familyId, grupoId)
                .filter { !it.fecha.isBefore(plantilla.fecha) }
            val (aGuardar, aBorrar) = diferenciaSerie(plantilla, existentesDesdeSuFecha, nuevasFechas) {
                CoverageNeedId(UUID.randomUUID().toString())
            }
            if (aGuardar.isNotEmpty()) coverageNeedRepository.guardarNeeds(familyId, aGuardar)
            if (aBorrar.isNotEmpty()) coverageNeedRepository.eliminarNeeds(familyId, aBorrar.map { it.id })
            cargarDia()
        }
    }
}
