package com.encaja.app.ui.familia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.PatronCuidado
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import com.encaja.app.domain.repository.AssignmentRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CategoriaRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.encaja.app.domain.repository.TurnoRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val assignmentRepository: AssignmentRepository,
    private val turnoRepository: TurnoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    init { cargar() }
    fun recargar() = cargar()

    /**
     * Avanza o retrocede semanas desde la cabecera (-1 anterior, +1 siguiente).
     * Sin pantalla de "Cargando" de por medio: la semana anterior se queda visible
     * hasta que llega la nueva, en vez de dejar la pantalla en blanco un momento.
     */
    fun cambiarSemana(delta: Int) {
        offsetSemanas += delta
        cargar(mostrarCargando = false)
    }

    /** Vuelve directamente a la semana actual, sin acumular desplazamientos previos. */
    fun irASemanaActual() {
        offsetSemanas = 0
        cargar(mostrarCargando = false)
    }

    /**
     * [mostrarCargando] = false recarga sin pasar por la pantalla de "Cargando", para que
     * la lista no vuelva arriba al guardar desde un diálogo (mismo motivo que en Menú).
     */
    private fun cargar(mostrarCargando: Boolean = true) {
        viewModelScope.launch {
            if (mostrarCargando) _pantalla.value = FamiliaPantallaEstado.Cargando
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
            val turnos = turnoRepository.obtenerTurnos(membresia.familyId).sortedBy { it.horaInicio }
            val categorias = CategoriasBase.combinar(categoriaRepository.obtenerCategorias(membresia.familyId))

            val mapper = FamiliaUiStateMapper(caregivers, unidades, patrones, anulaciones, disponibilidad)
            _pantalla.value = FamiliaPantallaEstado.ConDatos(
                mapper.construir(lunes, esSemanaActual = offsetSemanas == 0).copy(turnos = turnos, categorias = categorias)
            )
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

    /**
     * Guarda un turno de trabajo en las [fechas] indicadas (y, si se pide, en las mismas
     * fechas de la semana siguiente). Si alguno de esos días ya tenía un turno de trabajo,
     * se sustituye — así cambiar de mañana a tarde no deja los dos turnos a la vez.
     */
    fun guardarTrabajo(
        caregiverId: CaregiverId,
        fechas: List<LocalDate>,
        inicio: LocalTime,
        fin: LocalTime,
        duplicarSemanaSiguiente: Boolean,
        nombreTurno: String? = null
    ) {
        val familyId = familyIdActual ?: return
        val todas = fechasTrabajo(fechas, duplicarSemanaSiguiente)
        if (todas.isEmpty()) return

        viewModelScope.launch {
            val existentes = availabilityRepository.obtenerDisponibilidad(familyId, todas.first(), todas.last())
                .filter { it.caregiverId == caregiverId && it.motivo == MotivoNoDisponibilidad.TRABAJO && it.fecha in todas }
            existentes.forEach { availabilityRepository.eliminarBloque(familyId, it.caregiverId, it.fecha, it.horaInicio) }
            todas.forEach { fecha ->
                availabilityRepository.guardarBloque(
                    familyId, AvailabilityBlock(caregiverId, fecha, inicio, fin, MotivoNoDisponibilidad.TRABAJO, nombreTurno)
                )
            }
            cargar(mostrarCargando = false)
        }
    }

    /** Guarda bloques sueltos (una cita médica, los días de un viaje...). */
    fun guardarBloques(bloques: List<AvailabilityBlock>) {
        val familyId = familyIdActual ?: return
        if (bloques.isEmpty()) return
        viewModelScope.launch {
            bloques.forEach { availabilityRepository.guardarBloque(familyId, it) }
            cargar(mostrarCargando = false)
        }
    }

    fun eliminarBloque(bloque: AvailabilityBlock) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            availabilityRepository.eliminarBloque(familyId, bloque.caregiverId, bloque.fecha, bloque.horaInicio)
            cargar(mostrarCargando = false)
        }
    }

    /** Crea un turno de trabajo con nombre (p.ej. "Mañana 6-14") para toda la familia. */
    fun crearTurno(nombre: String, inicio: LocalTime, fin: LocalTime) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        viewModelScope.launch {
            turnoRepository.guardarTurno(familyId, TurnoTrabajo(TurnoId(UUID.randomUUID().toString()), nombreLimpio, inicio, fin))
            cargar(mostrarCargando = false)
        }
    }

    /**
     * Crea o actualiza una categoría. Si es nueva (id vacío) se le da un id propio.
     * Las de serie se guardan con su mismo id fijo: solo cuentan su emoji y su color.
     */
    fun guardarCategoria(categoria: CategoriaDisponibilidad) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = categoria.nombre.trim()
        if (nombreLimpio.isBlank()) return
        val conId = if (categoria.id.value.isBlank()) categoria.copy(id = CategoriaId(UUID.randomUUID().toString())) else categoria
        viewModelScope.launch {
            categoriaRepository.guardarCategoria(familyId, conId.copy(nombre = nombreLimpio))
            cargar(mostrarCargando = false)
        }
    }

    /**
     * Borra una categoría propia. Los días que ya estaban apuntados con ella no se borran:
     * pasan a verse como "Otro" (y a ocupar a la persona), que es con lo que se guardaron.
     */
    fun eliminarCategoria(categoriaId: CategoriaId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            categoriaRepository.eliminarCategoria(familyId, categoriaId)
            cargar(mostrarCargando = false)
        }
    }

    /** Borra un turno de la lista. Los días ya marcados con ese turno no cambian. */
    fun eliminarTurno(turnoId: TurnoId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            turnoRepository.eliminarTurno(familyId, turnoId)
            cargar(mostrarCargando = false)
        }
    }
}
