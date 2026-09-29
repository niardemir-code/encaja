package com.encaja.app.ui.guia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val assignmentRepository: AssignmentRepository,
    private val familyUnitRepository: FamilyUnitRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<GuiaPantallaEstado>(GuiaPantallaEstado.Cargando)
    val pantalla: StateFlow<GuiaPantallaEstado> = _pantalla.asStateFlow()

    private var fechaActual: LocalDate = LocalDate.now()

    init {
        cargar()
    }

    fun diaAnterior() {
        fechaActual = fechaActual.minusDays(1)
        cargar()
    }

    fun diaSiguiente() {
        fechaActual = fechaActual.plusDays(1)
        cargar()
    }

    fun hoy() {
        fechaActual = LocalDate.now()
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = GuiaPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                _pantalla.value = GuiaPantallaEstado.SinFamilia
                return@launch
            }

            val fecha = fechaActual

            // Las seis lecturas son independientes entre sí, así que se lanzan todas a la
            // vez en vez de esperarlas una detrás de otra: cambiar de día tarda lo que
            // tarda la más lenta, no la suma de las seis.
            coroutineScope {
                val ninosDeferred = async { childRepository.obtenerNinos(membresia.familyId) }
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(membresia.familyId) }
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(membresia.familyId) }
                val patronesDeferred = async { assignmentRepository.obtenerPatrones(membresia.familyId) }
                val anulacionesDeferred = async { assignmentRepository.obtenerAnulaciones(membresia.familyId, fecha, fecha) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(membresia.familyId, fecha, fecha) }
                val needsDeferred = async { coverageNeedRepository.obtenerNeeds(membresia.familyId, fecha, fecha) }

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
}
