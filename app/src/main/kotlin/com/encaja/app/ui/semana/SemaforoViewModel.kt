package com.encaja.app.ui.semana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
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
class SemaforoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<SemaforoPantallaEstado>(SemaforoPantallaEstado.Cargando)
    val pantalla: StateFlow<SemaforoPantallaEstado> = _pantalla.asStateFlow()

    /** Familia del usuario ya resuelta, para que "sembrar datos" sepa dónde escribir. */
    private var familyIdActual: FamilyId? = null

    init {
        cargar()
    }

    fun recargar() = cargar()

    /**
     * TEMPORAL — mientras no exista el sistema de invitación por código,
     * este botón vincula al usuario actual con la familia de ejemplo
     * para poder seguir probando la app de extremo a extremo.
     */
    fun vincularmeAFamiliaDeEjemplo() {
        viewModelScope.launch {
            val uid = authRepository.sesionActual()?.uid ?: return@launch
            familyMembershipRepository.vincularAFamilia(
                uid,
                FamilyMembership(FamilyId("demo-oliver-izquierdo"), DatosEjemploFamilia.victor.id)
            )
            cargar()
        }
    }

    /** TEMPORAL — igual que antes, pero ahora escribe en la familia real del usuario, no en una fija. */
    fun sembrarDatosDeEjemplo() {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            val d = DatosEjemploFamilia

            caregiverRepository.guardarCuidadores(familyId, d.caregivers)
            assignmentRepository.guardarPatrones(familyId, d.patrones)
            d.anulaciones.forEach { (fecha, caregiverId) ->
                assignmentRepository.anularParaFecha(familyId, fecha, caregiverId)
            }
            d.disponibilidad.forEach { bloque ->
                availabilityRepository.guardarBloque(familyId, bloque)
            }
            coverageNeedRepository.guardarNeeds(familyId, d.needsDeLaSemana)

            cargar()
        }
    }

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = SemaforoPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = SemaforoPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = SemaforoPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId

            val lunes = LocalDate.now().lunesDeEstaSemana()
            val domingo = lunes.plusDays(6)

            val caregivers = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val patrones = assignmentRepository.obtenerPatrones(membresia.familyId)
            val anulaciones = assignmentRepository.obtenerAnulaciones(membresia.familyId, lunes, domingo)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(membresia.familyId, lunes, domingo)
            val needs = coverageNeedRepository.obtenerNeeds(membresia.familyId, lunes, domingo)

            val mapper = SemaforoUiStateMapper(caregivers, patrones, anulaciones, disponibilidad)
            _pantalla.value = SemaforoPantallaEstado.ConDatos(mapper.construir(lunes, needs))
        }
    }
}
