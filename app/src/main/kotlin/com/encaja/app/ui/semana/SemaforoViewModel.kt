package com.encaja.app.ui.semana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AnuncioRepository
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.InviteRepository
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
    private val inviteRepository: InviteRepository,
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository,
    private val anuncioRepository: AnuncioRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<SemaforoPantallaEstado>(SemaforoPantallaEstado.Cargando)
    val pantalla: StateFlow<SemaforoPantallaEstado> = _pantalla.asStateFlow()

    private val _cuidadores = MutableStateFlow<List<Caregiver>>(emptyList())
    val cuidadores: StateFlow<List<Caregiver>> = _cuidadores.asStateFlow()

    /** Familia del usuario ya resuelta, para que "invitar" sepa dónde escribir. */
    private var familyIdActual: FamilyId? = null

    /** Nombre del cuidador actual, para firmar los anuncios que publique. */
    private var nombreCuidadorActual: String = "Alguien de la familia"

    init {
        cargar()
    }

    fun recargar() = cargar()

    /** Introduce un código de invitación y, si es válido, vincula al usuario a esa familia. */
    fun canjearCodigo(codigo: String, alFallar: (String) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.sesionActual()?.uid ?: return@launch
            inviteRepository.canjearInvitacion(codigo).fold(
                onSuccess = { membership ->
                    familyMembershipRepository.vincularAFamilia(uid, membership)
                    cargar()
                },
                onFailure = { error -> alFallar(error.message ?: "Código no válido") }
            )
        }
    }

    /** Genera un código de invitación para un cuidador concreto de la familia actual. */
    fun generarInvitacion(caregiverId: CaregiverId, alConseguirlo: (String) -> Unit, alFallar: (String) -> Unit) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            inviteRepository.generarInvitacion(familyId, caregiverId).fold(
                onSuccess = { codigo -> alConseguirlo(codigo) },
                onFailure = { error -> alFallar(error.message ?: "No se pudo generar el código") }
            )
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
            _cuidadores.value = caregivers
            nombreCuidadorActual = caregivers.firstOrNull { it.id == membresia.caregiverId }?.nombreCompleto
                ?: "Alguien de la familia"

            val patrones = assignmentRepository.obtenerPatrones(membresia.familyId)
            val anulaciones = assignmentRepository.obtenerAnulaciones(membresia.familyId, lunes, domingo)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(membresia.familyId, lunes, domingo)
            val needs = coverageNeedRepository.obtenerNeeds(membresia.familyId, lunes, domingo)
            val anuncios = anuncioRepository.obtenerAnuncios(membresia.familyId)

            val mapper = SemaforoUiStateMapper(caregivers, patrones, anulaciones, disponibilidad)
            _pantalla.value = SemaforoPantallaEstado.ConDatos(mapper.construir(lunes, needs).copy(anuncios = anuncios))
        }
    }

    /** Publica un anuncio nuevo en el tablón, firmado con el nombre del cuidador actual. */
    fun publicarAnuncio(texto: String) {
        val familyId = familyIdActual ?: return
        val textoLimpio = texto.trim()
        if (textoLimpio.isBlank()) return

        viewModelScope.launch {
            anuncioRepository.publicarAnuncio(familyId, nombreCuidadorActual, textoLimpio)
            cargar()
        }
    }

    /** Cualquier miembro de la familia puede borrar un anuncio del tablón. */
    fun eliminarAnuncio(anuncioId: AnuncioId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            anuncioRepository.eliminarAnuncio(familyId, anuncioId)
            cargar()
        }
    }
}
