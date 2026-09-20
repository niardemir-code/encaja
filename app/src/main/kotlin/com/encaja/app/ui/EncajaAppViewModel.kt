package com.encaja.app.ui

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.ui.familia.calcularInicialesCuidadores
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del contenedor de toda la app (EncajaApp), no de una pestaña
 * concreta — de aquí sale el avatar con las iniciales del usuario que se
 * muestra en la barra superior, junto al icono de Ajustes.
 */
@HiltViewModel
class EncajaAppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository
) : ViewModel() {

    private val _inicialesUsuario = MutableStateFlow("")
    val inicialesUsuario: StateFlow<String> = _inicialesUsuario.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = authRepository.sesionActual() ?: return@launch
            val membresia = familyMembershipRepository.obtenerMembresia(sesion.uid)

            _inicialesUsuario.value = if (membresia != null) {
                // Se calculan las iniciales de TODOS los cuidadores de la familia (no solo
                // el actual) y se usa el mismo resultado que en Familia → "con quién están
                // las niñas", para que la desambiguación (p.ej. Víctor vs. Vicente Oliver)
                // dé siempre las mismas iniciales en toda la app.
                val cuidadores = caregiverRepository.obtenerCuidadores(membresia.familyId)
                val iniciales = calcularInicialesCuidadores(cuidadores)
                iniciales[membresia.caregiverId]
                    ?: sesion.email?.trim()?.take(2)?.uppercase()
                    ?: "?"
            } else {
                sesion.email?.trim()?.take(2)?.uppercase() ?: "?"
            }
        }
    }
}
