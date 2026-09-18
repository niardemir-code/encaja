package com.encaja.app.ui.semana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * FamilyId fijo temporal: todavía no hay pantalla de login ni selección
 * de familia, así que de momento todo el mundo ve la misma. Cuando eso
 * exista, este valor vendrá de la sesión del usuario, no de una
 * constante.
 */
private val FAMILY_ID_PROVISIONAL = FamilyId("demo-oliver-izquierdo")

@HiltViewModel
class SemaforoViewModel @Inject constructor(
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SemaforoUiState(emptyList(), emptyList(), emptyList()))
    val uiState: StateFlow<SemaforoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            val lunes = LocalDate.now().lunesDeEstaSemana()
            val domingo = lunes.plusDays(6)

            val caregivers = caregiverRepository.obtenerCuidadores(FAMILY_ID_PROVISIONAL)
            val patrones = assignmentRepository.obtenerPatrones(FAMILY_ID_PROVISIONAL)
            val anulaciones = assignmentRepository.obtenerAnulaciones(FAMILY_ID_PROVISIONAL, lunes, domingo)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(FAMILY_ID_PROVISIONAL, lunes, domingo)
            val needs = coverageNeedRepository.obtenerNeeds(FAMILY_ID_PROVISIONAL, lunes, domingo)

            val mapper = SemaforoUiStateMapper(caregivers, patrones, anulaciones, disponibilidad)
            _uiState.value = mapper.construir(lunes, needs)
        }
    }
}