package com.encaja.app.ui.ajustes

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
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
    private val childRepository: ChildRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<AjustesPantallaEstado>(AjustesPantallaEstado.Cargando)
    val pantalla: StateFlow<AjustesPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

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
            _pantalla.value = AjustesPantallaEstado.ConDatos(ninos)
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
}
