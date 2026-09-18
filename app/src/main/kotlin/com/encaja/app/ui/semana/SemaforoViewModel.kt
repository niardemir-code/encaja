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

    /**
     * TEMPORAL — botón de desarrollo. Escribe los datos de ejemplo de
     * DatosEjemploFamilia en Firestore de verdad, para no tener que
     * crearlos a mano en la consola web. Se borrará cuando exista una
     * forma real de dar de alta cuidadores y necesidades desde la app.
     */
    fun sembrarDatosDeEjemplo() {
        viewModelScope.launch {
            val d = DatosEjemploFamilia

            caregiverRepository.guardarCuidadores(FAMILY_ID_PROVISIONAL, d.caregivers)
            assignmentRepository.guardarPatrones(FAMILY_ID_PROVISIONAL, d.patrones)
            d.anulaciones.forEach { (fecha, caregiverId) ->
                assignmentRepository.anularParaFecha(FAMILY_ID_PROVISIONAL, fecha, caregiverId)
            }
            d.disponibilidad.forEach { bloque ->
                availabilityRepository.guardarBloque(FAMILY_ID_PROVISIONAL, bloque)
            }
            coverageNeedRepository.guardarNeeds(FAMILY_ID_PROVISIONAL, d.needsDeLaSemana)

            cargar()
        }
    }

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