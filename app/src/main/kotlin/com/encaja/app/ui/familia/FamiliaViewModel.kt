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
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CategoriaRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.TurnoRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val caregiverRepository: CaregiverRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val turnoRepository: TurnoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    /** Lunes de la semana que se está mostrando ahora mismo (se actualiza en [cargarDatos]). */
    private var lunesActual: LocalDate = LocalDate.now().lunesDeEstaSemana()

    /**
     * Qué cuidadores se han ocultado de la cuadrícula, por semana (clave = lunes de esa
     * semana). Es solo una preferencia de visualización — no se guarda en ningún sitio,
     * así que se pierde al salir de la pantalla — pero cada semana recuerda la suya
     * propia mientras se navega entre ellas en la misma visita.
     */
    private val ocultosPorSemana = mutableMapOf<LocalDate, MutableSet<CaregiverId>>()

    private val _ocultos = MutableStateFlow<Set<CaregiverId>>(emptySet())
    /** Cuidadores ocultos en la semana que se está viendo ahora. */
    val ocultos: StateFlow<Set<CaregiverId>> = _ocultos.asStateFlow()

    init { cargar() }

    /** Muestra u oculta a un cuidador de la cuadrícula, solo para la semana actual. */
    fun alternarVisibilidad(caregiverId: CaregiverId) {
        val ocultosDeEstaSemana = ocultosPorSemana.getOrPut(lunesActual) { mutableSetOf() }
        if (!ocultosDeEstaSemana.remove(caregiverId)) ocultosDeEstaSemana.add(caregiverId)
        _ocultos.value = ocultosDeEstaSemana.toSet()
    }

    /** Recarga completa: vuelve a comprobar sesión y familia (por si han cambiado). Se
     * usa al entrar en la pestaña; cambiar de semana o guardar algo no lo necesita. */
    fun recargar() = cargar()

    /**
     * Avanza o retrocede semanas desde la cabecera (-1 anterior, +1 siguiente).
     * Sin pantalla de "Cargando" de por medio: la semana anterior se queda visible
     * hasta que llega la nueva, en vez de dejar la pantalla en blanco un momento.
     */
    fun cambiarSemana(delta: Int) {
        offsetSemanas += delta
        cargarDatos(mostrarCargando = false)
    }

    /** Vuelve directamente a la semana actual, sin acumular desplazamientos previos. */
    fun irASemanaActual() {
        offsetSemanas = 0
        cargarDatos(mostrarCargando = false)
    }

    /** Salta directamente a la semana que contiene [fecha], elegida en el calendario
     * (icono junto a las flechas de la cabecera), sin acumular desplazamientos previos. */
    fun irASemanaDe(fecha: LocalDate) {
        val lunesHoy = LocalDate.now().lunesDeEstaSemana()
        offsetSemanas = ChronoUnit.WEEKS.between(lunesHoy, fecha.lunesDeEstaSemana()).toInt()
        cargarDatos(mostrarCargando = false)
    }

    /** Primera carga (o recarga forzada): valida sesión y familia — lo único que de
     * verdad puede tardar un poco — y solo entonces pide los datos de la semana. */
    private fun cargar() {
        viewModelScope.launch {
            _pantalla.value = FamiliaPantallaEstado.Cargando
            val uid = authRepository.sesionActual()?.uid
            if (uid == null) { _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@launch }
            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) { familyIdActual = null; _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@launch }
            familyIdActual = membresia.familyId
            cargarDatos(mostrarCargando = false)
        }
    }

    /**
     * Recarga solo los datos de la semana con el desplazamiento actual: cambiar de
     * semana, o guardar algo desde un diálogo, no necesita volver a comprobar sesión
     * ni familia (no cambian mientras se navega o se edita). Las cuatro peticiones son
     * independientes entre sí, así que se lanzan todas a la vez con [async] en vez de
     * esperarlas una detrás de otra: avanzar de semana tarda lo que tarda la más
     * lenta, no la suma de las cuatro.
     * [mostrarCargando] = false evita pasar por la pantalla de "Cargando", para que
     * la lista no vuelva arriba al guardar desde un diálogo.
     */
    private fun cargarDatos(mostrarCargando: Boolean) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            if (mostrarCargando) _pantalla.value = FamiliaPantallaEstado.Cargando

            val lunes = LocalDate.now().lunesDeEstaSemana().plusWeeks(offsetSemanas.toLong())
            val domingo = lunes.plusDays(6)
            lunesActual = lunes
            _ocultos.value = ocultosPorSemana[lunes].orEmpty()

            coroutineScope {
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(familyId) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(familyId, lunes, domingo) }
                val turnosDeferred = async { turnoRepository.obtenerTurnos(familyId) }
                val categoriasDeferred = async { categoriaRepository.obtenerCategorias(familyId) }

                val mapper = FamiliaUiStateMapper(caregiversDeferred.await(), disponibilidadDeferred.await())
                val turnos = turnosDeferred.await().sortedBy { it.horaInicio }
                val categorias = CategoriasBase.combinar(categoriasDeferred.await())

                _pantalla.value = FamiliaPantallaEstado.ConDatos(
                    mapper.construir(lunes, esSemanaActual = offsetSemanas == 0).copy(turnos = turnos, categorias = categorias)
                )
            }
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
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Guarda bloques sueltos (una cita médica, los días de un viaje...). */
    fun guardarBloques(bloques: List<AvailabilityBlock>) {
        val familyId = familyIdActual ?: return
        if (bloques.isEmpty()) return
        viewModelScope.launch {
            bloques.forEach { availabilityRepository.guardarBloque(familyId, it) }
            cargarDatos(mostrarCargando = false)
        }
    }

    fun eliminarBloque(bloque: AvailabilityBlock) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            availabilityRepository.eliminarBloque(familyId, bloque.caregiverId, bloque.fecha, bloque.horaInicio)
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Crea un turno de trabajo con nombre (p.ej. "Mañana 6-14") para toda la familia. */
    fun crearTurno(nombre: String, inicio: LocalTime, fin: LocalTime) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        viewModelScope.launch {
            turnoRepository.guardarTurno(familyId, TurnoTrabajo(TurnoId(UUID.randomUUID().toString()), nombreLimpio, inicio, fin))
            cargarDatos(mostrarCargando = false)
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
            cargarDatos(mostrarCargando = false)
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
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Borra un turno de la lista. Los días ya marcados con ese turno no cambian. */
    fun eliminarTurno(turnoId: TurnoId) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            turnoRepository.eliminarTurno(familyId, turnoId)
            cargarDatos(mostrarCargando = false)
        }
    }
}
