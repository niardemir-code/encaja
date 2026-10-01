package com.encaja.app.ui

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.avisos.ProgramadorDeAvisos
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.usecase.actividadesAntiguas
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
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val childRepository: ChildRepository,
    private val avisos: ProgramadorDeAvisos
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

            if (membresia != null) {
                limpiarActividadesAntiguas(membresia.familyId)
                limpiarOcupacionesAntiguas(membresia.familyId)
                reprogramarAvisos(membresia.familyId)
            }
        }
    }

    /**
     * Borra de golpe, una vez por cada arranque de la app, las actividades con más
     * de un mes de antigüedad — para no acumular datos que ya no hace falta
     * conservar. Se hace aquí (no en cada pestaña) precisamente porque este
     * ViewModel vive todo lo que vive la app, así que solo se ejecuta una vez por
     * sesión en vez de cada vez que se entra en Semana o en Guía.
     */
    private suspend fun limpiarActividadesAntiguas(familyId: FamilyId) {
        val todas = coverageNeedRepository.obtenerTodosLosNeeds(familyId)
        val aBorrar = actividadesAntiguas(todas)
        if (aBorrar.isNotEmpty()) coverageNeedRepository.eliminarNeeds(familyId, aBorrar)
    }

    /**
     * Vuelve a programar en este móvil los avisos de las actividades de los próximos
     * días (las alarmas se pierden al reiniciar el teléfono o al reinstalar, y las que
     * cree otro miembro de la familia solo llegan aquí a través de los datos).
     */
    private suspend fun reprogramarAvisos(familyId: FamilyId) {
        val hoy = java.time.LocalDate.now()
        val proximas = coverageNeedRepository.obtenerNeeds(familyId, hoy, hoy.plusDays(60))
            .filter { it.avisoLlevarMin != null || it.avisoRecogerMin != null }
        if (proximas.isEmpty()) return
        val nombres = childRepository.obtenerNinos(familyId).associate { it.id to it.nombre }
        proximas.forEach { avisos.programar(it, nombres[it.childId]) }
    }

    /**
     * Igual que con las actividades: las ocupaciones de Familia (trabajo, médico,
     * viajes…) de hace más de un mes se borran para no ocupar espacio. Se piden desde
     * un par de años atrás, que de sobra cubre lo que pueda quedar.
     */
    private suspend fun limpiarOcupacionesAntiguas(familyId: FamilyId) {
        val hoy = java.time.LocalDate.now()
        val limite = hoy.minusMonths(1)
        val antiguas = availabilityRepository.obtenerDisponibilidad(familyId, hoy.minusYears(2), limite.minusDays(1))
            .filter { it.fecha.isBefore(limite) }
        antiguas.forEach { availabilityRepository.eliminarBloque(familyId, it.caregiverId, it.fecha, it.horaInicio) }
    }
}
