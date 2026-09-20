package com.encaja.app.ui.ajustes

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CaregiverRole
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val childRepository: ChildRepository,
    private val caregiverRepository: CaregiverRepository,
    private val familyUnitRepository: FamilyUnitRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<AjustesPantallaEstado>(AjustesPantallaEstado.Cargando)
    val pantalla: StateFlow<AjustesPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Correo de la cuenta con la sesión abierta ahora mismo en este dispositivo, para que se
     * pueda distinguir a simple vista si es la cuenta esperada (por ejemplo, tras usar una
     * cuenta de prueba para comprobar el flujo de invitación). */
    val emailUsuarioActual: String?
        get() = authRepository.sesionActual()?.email

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = AjustesPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = AjustesPantallaEstado.SinFamilia
                return@launch
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = AjustesPantallaEstado.SinFamilia
                return@launch
            }
            familyIdActual = membresia.familyId

            val ninos = childRepository.obtenerNinos(membresia.familyId)
            val cuidadores = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val unidades = familyUnitRepository.obtenerUnidades(membresia.familyId)
            _pantalla.value = AjustesPantallaEstado.ConDatos(ninos, cuidadores, unidades)
        }
    }

    /** Añade un niño nuevo a la familia actual, generando su id a partir del nombre. */
    fun agregarNino(nombre: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return

        val actuales = (_pantalla.value as? AjustesPantallaEstado.ConDatos)?.ninos.orEmpty()
        val id = generarChildIdDesdeNombre(nombreLimpio, actuales.map { it.id })

        viewModelScope.launch {
            childRepository.guardarNinos(familyId, actuales + Child(id, nombreLimpio))
            cargar()
        }
    }

    fun eliminarNino(childId: ChildId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            childRepository.eliminarNino(familyId, childId)
            cargar()
        }
    }

    /** Añade un cuidador nuevo, generando su id a partir del nombre completo. Rol por defecto: CUIDADOR. */
    fun agregarCuidador(nombre: String, apellido1: String, apellido2: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        val apellido1Limpio = apellido1.trim()
        val apellido2Limpio = apellido2.trim()

        val actuales = (_pantalla.value as? AjustesPantallaEstado.ConDatos)?.cuidadores.orEmpty()
        val id = generarCaregiverIdDesdeNombre(nombreLimpio, apellido1Limpio, apellido2Limpio, actuales.map { it.id })
        val nuevo = Caregiver(id, nombreLimpio, apellido1Limpio, apellido2Limpio, CaregiverRole.CUIDADOR)

        viewModelScope.launch {
            caregiverRepository.guardarCuidadores(familyId, actuales + nuevo)
            cargar()
        }
    }

    fun eliminarCuidador(caregiverId: CaregiverId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            caregiverRepository.eliminarCuidador(familyId, caregiverId)
            cargar()
        }
    }

    /**
     * Vincula la cuenta con la sesión abierta a un cuidador ya existente de la lista,
     * sin pasar por un código de invitación. Sirve para el caso de quien está montando
     * la familia (o para recuperar el enlace si se borró y volvió a crear su propio
     * cuidador): normalmente esa vinculación se hace canjeando un código, pero ese
     * flujo solo aparece cuando la cuenta NO tiene ya una familia asignada — y quien
     * ya tiene una (aunque apunte a un cuidador borrado) nunca llega a verlo.
     */
    fun vincularmeAEsteCuidador(caregiverId: CaregiverId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            val uid = authRepository.sesionActual()?.uid ?: return@launch
            familyMembershipRepository.vincularAFamilia(uid, FamilyMembership(familyId, caregiverId))
        }
    }

    /**
     * Añade una unidad familiar nueva (un grupo, p.ej. "los abuelos maternos") a partir
     * de un código corto (para el avatar del día, p.ej. "GF"), un nombre y sus miembros.
     */
    fun agregarUnidad(codigo: String, nombre: String, miembros: List<CaregiverId>) {
        val familyId = familyIdActual ?: return
        val codigoLimpio = codigo.trim()
        val nombreLimpio = nombre.trim()
        if (codigoLimpio.isBlank() || nombreLimpio.isBlank() || miembros.isEmpty()) return

        val actuales = (_pantalla.value as? AjustesPantallaEstado.ConDatos)?.unidades.orEmpty()
        val id = generarFamilyUnitIdDesdeNombre(nombreLimpio, actuales.map { it.id })
        val nueva = FamilyUnit(id, codigoLimpio, nombreLimpio, miembros)

        viewModelScope.launch {
            familyUnitRepository.guardarUnidades(familyId, actuales + nueva)
            cargar()
        }
    }

    fun eliminarUnidad(unidadId: FamilyUnitId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            familyUnitRepository.eliminarUnidad(familyId, unidadId)
            cargar()
        }
    }
}
