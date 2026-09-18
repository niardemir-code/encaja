package com.encaja.app.ui.familia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = FamiliaPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = FamiliaPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                _pantalla.value = FamiliaPantallaEstado.SinFamilia
                return@launch
            }

            val lunes = LocalDate.now().lunesDeEstaSemana()
            val domingo = lunes.plusDays(6)

            val caregivers = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val patrones = assignmentRepository.obtenerPatrones(membresia.familyId)
            val anulaciones = assignmentRepository.obtenerAnulaciones(membresia.familyId, lunes, domingo)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(membresia.familyId, lunes, domingo)

            val mapper = FamiliaUiStateMapper(caregivers, patrones, anulaciones, disponibilidad)
            _pantalla.value = FamiliaPantallaEstado.ConDatos(mapper.construir(lunes))
        }
    }
}
