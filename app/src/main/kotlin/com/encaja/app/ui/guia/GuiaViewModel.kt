package com.encaja.app.ui.guia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
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
    private val assignmentRepository: AssignmentRepository
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
            val ninos = childRepository.obtenerNinos(membresia.familyId)
            val caregivers = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val patrones = assignmentRepository.obtenerPatrones(membresia.familyId)
            val anulaciones = assignmentRepository.obtenerAnulaciones(membresia.familyId, fecha, fecha)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(membresia.familyId, fecha, fecha)
            val needsDelDia = coverageNeedRepository.obtenerNeeds(membresia.familyId, fecha, fecha)

            val mapper = GuiaUiStateMapper(ninos, caregivers, patrones, anulaciones, disponibilidad)
            _pantalla.value = GuiaPantallaEstado.ConDatos(mapper.construir(fecha, needsDelDia))
        }
    }
}
