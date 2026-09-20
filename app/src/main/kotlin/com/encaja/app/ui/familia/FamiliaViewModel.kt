package com.encaja.app.ui.familia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.PatronCuidado
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    init { cargar() }
    fun recargar() = cargar()

    /** Avanza o retrocede semanas desde el chip de cabecera (-1 anterior, +1 siguiente). */
    fun cambiarSemana(delta: Int) {
        offsetSemanas += delta
        cargar()
    }

    /** Vuelve directamente a la semana actual, sin acumular desplazamientos previos. */
    fun irASemanaActual() {
        offsetSemanas = 0
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = FamiliaPantallaEstado.Cargando
            val uid = authRepository.sesionActual()?.uid
            if (uid == null) { _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@launch }
            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) { familyIdActual = null; _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@launch }
            familyIdActual = membresia.familyId

            val lunes = LocalDate.now().lunesDeEstaSemana().plusWeeks(offsetSemanas.toLong())
            val domingo = lunes.plusDays(6)
            val caregivers = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val unidades = familyUnitRepository.obtenerUnidades(membresia.familyId)
            val patrones = assignmentRepository.obtenerPatrones(membresia.familyId)
            val anulaciones = assignmentRepository.obtenerAnulaciones(membresia.familyId, lunes, domingo)
            val disponibilidad = availabilityRepository.obtenerDisponibilidad(membresia.familyId, lunes, domingo)

            val mapper = FamiliaUiStateMapper(caregivers, unidades, patrones, anulaciones, disponibilidad)
            _pantalla.value = FamiliaPantallaEstado.ConDatos(mapper.construir(lunes, esSemanaActual = offsetSemanas == 0))
        }
    }

    /**
     * Asigna (o cambia) quién es el responsable habitual de un día de la semana, de forma
     * recurrente — afecta a ese día de la semana en todas las semanas futuras, no solo a esta.
     * [idTexto] puede ser tanto el id de un cuidador como el de una unidad familiar.
     */
    fun asignarResponsableHabitual(diaSemana: DayOfWeek, idTexto: String) {
        val familyId = familyIdActual ?: return
        val patronesActuales = (_pantalla.value as? FamiliaPantallaEstado.ConDatos)
            ?.estado?.patronSemanal?.map { (dia, responsable) -> PatronCuidado(dia, CaregiverId(responsable.idTexto)) }.orEmpty()

        val actualizados = patronesActuales.filter { it.diaSemana != diaSemana } + PatronCuidado(diaSemana, CaregiverId(idTexto))

        viewModelScope.launch {
            assignmentRepository.guardarPatrones(familyId, actualizados)
            cargar()
        }
    }

    /** Quita el responsable habitual de un día de la semana; ese día queda sin patrón fijo. */
    fun quitarResponsableHabitual(diaSemana: DayOfWeek) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            assignmentRepository.eliminarPatron(familyId, diaSemana)
            cargar()
        }
    }

    /**
     * Anula el patrón habitual solo para una fecha concreta (por ejemplo, un cambio puntual
     * esta semana), sin tocar el patrón recurrente de ese día de la semana. [idTexto] puede
     * ser tanto el id de un cuidador como el de una unidad familiar.
     */
    fun anularParaEstaFecha(fecha: LocalDate, idTexto: String) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            assignmentRepository.anularParaFecha(familyId, fecha, CaregiverId(idTexto))
            cargar()
        }
    }

    /** Quita el cambio puntual de una fecha; ese día vuelve a seguir el patrón semanal. */
    fun quitarCambioPuntual(fecha: LocalDate) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            assignmentRepository.eliminarAnulacion(familyId, fecha)
            cargar()
        }
    }
}
