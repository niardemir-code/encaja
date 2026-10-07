package com.encaja.app.ui.guia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.ResultadoMembresia
import com.encaja.app.domain.repository.FamilyUnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GuiaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val childRepository: ChildRepository,
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val editor: EditorDeActividades
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
        lanzarSeguro {
            _pantalla.value = GuiaPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@lanzarSeguro
            }

            val familyId = when (val resultado = familyMembershipRepository.consultarMembresia(uid)) {
                is ResultadoMembresia.Tiene -> resultado.membresia.familyId
                is ResultadoMembresia.NoTiene -> null
                is ResultadoMembresia.Error -> familyIdActual ?: run {
                    _pantalla.value = GuiaPantallaEstado.ErrorDeConexion
                    return@lanzarSeguro
                }
            }
            if (familyId == null) {
                familyIdActual = null
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@lanzarSeguro
            }
            familyIdActual = familyId
            cargarDia()
        }
    }

    /** Recarga solo los datos del día actual: cambiar de día, o guardar/borrar una
     * actividad, no necesita volver a comprobar sesión ni familia. */
    private fun cargarDia() {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            val fecha = fechaActual

            // Las seis lecturas son independientes entre sí, así que se lanzan todas a la
            // vez en vez de esperarlas una detrás de otra: cambiar de día tarda lo que
            // tarda la más lenta, no la suma de las seis.
            coroutineScope {
                val ninosDeferred = async { childRepository.obtenerNinos(familyId) }
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(familyId) }
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(familyId) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(familyId, fecha, fecha) }
                val needsDeferred = async { coverageNeedRepository.obtenerNeeds(familyId, fecha, fecha) }

                val mapper = GuiaUiStateMapper(
                    ninosDeferred.await(),
                    caregiversDeferred.await(),
                    unidadesDeferred.await(),
                    disponibilidadDeferred.await()
                )
                _pantalla.value = GuiaPantallaEstado.ConDatos(mapper.construir(fecha, needsDeferred.await()))
            }
        }
    }

    /** Ver [EditorDeActividades.guardar]. */
    fun guardarActividades(needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.guardar(familyId, needs, aplicarATodaLaSerie)
            cargarDia()
        }
    }

    /** Ver [EditorDeActividades.eliminar]. */
    fun eliminarActividad(id: CoverageNeedId, grupoRepeticionId: String?, fecha: LocalDate, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.eliminar(familyId, id, grupoRepeticionId, fecha, aplicarATodaLaSerie)
            cargarDia()
        }
    }

    /** Ver [EditorDeActividades.ocurrenciasDeSerie]. */
    suspend fun ocurrenciasDeSerie(need: CoverageNeed): List<CoverageNeed> {
        val familyId = familyIdActual ?: return listOf(need)
        return editor.ocurrenciasDeSerie(familyId, need)
    }

    /** Ver [EditorDeActividades.patronDeSerie]. */
    suspend fun patronDeSerie(grupoRepeticionId: String, desde: LocalDate): Pair<Set<DayOfWeek>, LocalDate?> {
        val familyId = familyIdActual ?: return emptySet<DayOfWeek>() to null
        return editor.patronDeSerie(familyId, grupoRepeticionId, desde)
    }

    /** Ver [EditorDeActividades.actualizarSerie]. */
    fun actualizarSerie(plantilla: CoverageNeed, nuevasFechas: List<LocalDate>) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.actualizarSerie(familyId, plantilla, nuevasFechas)
            cargarDia()
        }
    }
}
