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
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CategoriaRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
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
    private val categoriaRepository: CategoriaRepository,
    private val familyUnitRepository: FamilyUnitRepository
) : ViewModel() {

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    /** Lunes de la semana que se está mostrando ahora mismo (se actualiza en [cargarDatos]). */
    private var lunesActual: LocalDate = LocalDate.now().lunesDeEstaSemana()

    /**
     * Qué personas y unidades familiares (por el id de cada una, como texto) se han
     * ocultado de la cuadrícula, por semana (clave = lunes de esa semana). Es solo una
     * preferencia de visualización — no se guarda en ningún sitio, así que se pierde al
     * salir de la pantalla — pero cada semana recuerda la suya propia mientras se
     * navega entre ellas en la misma visita.
     */
    private val ocultosPorSemana = mutableMapOf<LocalDate, MutableSet<String>>()

    private val _ocultos = MutableStateFlow<Set<String>>(emptySet())
    /** Ids (persona o unidad) ocultos en la semana que se está viendo ahora. */
    val ocultos: StateFlow<Set<String>> = _ocultos.asStateFlow()

    init { cargar() }

    /** Muestra u oculta a una persona o unidad de la cuadrícula, solo para la semana actual. */
    fun alternarVisibilidad(idTexto: String) {
        val ocultosDeEstaSemana = ocultosPorSemana.getOrPut(lunesActual) { mutableSetOf() }
        if (!ocultosDeEstaSemana.remove(idTexto)) ocultosDeEstaSemana.add(idTexto)
        _ocultos.value = ocultosDeEstaSemana.toSet()
    }

    /** Recarga completa: vuelve a comprobar sesión y familia (por si han cambiado). Se
     * usa al entrar en la pestaña; cambiar de semana o guardar algo no lo necesita. */
    fun recargar() {
        // Si ya había datos, se recarga la misma semana sin pasar por "Cargando" (no
        // vacía la pantalla ni pierde la semana elegida).
        if (familyIdActual != null && _pantalla.value is FamiliaPantallaEstado.ConDatos) cargarDatos(mostrarCargando = false)
        else cargar()
    }

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
     * ni familia (no cambian mientras se navega o se edita). Las cinco peticiones son
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
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(familyId) }

                val mapper = FamiliaUiStateMapper(caregiversDeferred.await(), disponibilidadDeferred.await(), unidadesDeferred.await())
                val turnos = turnosDeferred.await().sortedBy { it.horaInicio }
                val guardadas = categoriasDeferred.await()
                val categorias = CategoriasBase.combinar(guardadas)
                val eliminadas = CategoriasBase.eliminadas(guardadas)

                _pantalla.value = FamiliaPantallaEstado.ConDatos(
                    mapper.construir(lunes, esSemanaActual = offsetSemanas == 0)
                        .copy(turnos = turnos, categorias = categorias, categoriasEliminadas = eliminadas)
                )
            }
        }
    }

    /**
     * Guarda un tramo por horas de [categoria] para cada persona de [caregiverIds] (una,
     * o todos los miembros de una unidad familiar) en las [fechas] indicadas (y, si se
     * pide, en las mismas fechas de la semana siguiente). Si alguno de esos días ya tenía
     * algo de esa misma categoría, se sustituye — así cambiar el turno de mañana a tarde
     * no deja los dos a la vez. Vale para cualquier categoría por horas, no solo Trabajo.
     */
    fun guardarHoras(
        caregiverIds: List<CaregiverId>,
        categoria: CategoriaDisponibilidad,
        fechas: List<LocalDate>,
        inicio: LocalTime,
        fin: LocalTime,
        duplicarSemanaSiguiente: Boolean,
        etiqueta: String? = null,
        grupoRepeticionId: String? = null
    ) {
        val familyId = familyIdActual ?: return
        val todas = fechasTrabajo(fechas, duplicarSemanaSiguiente)
        if (todas.isEmpty() || caregiverIds.isEmpty()) return
        // Varios días guardados de golpe forman una serie (comparten grupo); si se está
        // editando una ocupación que ya era de una serie, sigue en ella.
        val grupo = grupoRepeticionId ?: if (todas.size > 1) java.util.UUID.randomUUID().toString() else null

        viewModelScope.launch {
            val existentes = availabilityRepository.obtenerDisponibilidad(familyId, todas.first(), todas.last())
                .filter { it.caregiverId in caregiverIds && it.fecha in todas && esDeLaCategoria(it, categoria) }
            existentes.forEach { availabilityRepository.eliminarBloque(familyId, it.caregiverId, it.fecha, it.horaInicio) }
            caregiverIds.forEach { caregiverId ->
                todas.forEach { fecha ->
                    availabilityRepository.guardarBloque(familyId, bloqueDeCategoria(categoria, caregiverId, fecha, inicio, fin, etiqueta, grupo))
                }
            }
            cargarDatos(mostrarCargando = false)
        }
    }

    /**
     * "Guardar toda la serie": aplica [categoria], el horario ([inicio]-[fin]) y la
     * [etiqueta] a todas las ocupaciones de la serie de [bloqueOriginal] (las que
     * comparten su grupo) de las personas de [caregiverIds], respetando el día de cada
     * una. Se borran y se vuelven a guardar porque la hora de inicio forma parte de la
     * clave de cada bloque.
     */
    fun guardarSerie(
        bloqueOriginal: AvailabilityBlock,
        caregiverIds: List<CaregiverId>,
        categoria: CategoriaDisponibilidad,
        inicio: java.time.LocalTime,
        fin: java.time.LocalTime,
        etiqueta: String?
    ) {
        val familyId = familyIdActual ?: return
        val grupoId = bloqueOriginal.grupoRepeticionId ?: return
        if (caregiverIds.isEmpty()) return
        viewModelScope.launch {
            val deLaSerie = availabilityRepository.obtenerBloquesDelGrupo(familyId, grupoId)
                .filter { it.caregiverId in caregiverIds }
            val fechas = deLaSerie.map { it.fecha }.distinct().ifEmpty { listOf(bloqueOriginal.fecha) }
            deLaSerie.forEach { availabilityRepository.eliminarBloque(familyId, it.caregiverId, it.fecha, it.horaInicio) }
            caregiverIds.forEach { caregiverId ->
                fechas.forEach { fecha ->
                    availabilityRepository.guardarBloque(
                        familyId, bloqueDeCategoria(categoria, caregiverId, fecha, inicio, fin, etiqueta, grupoId)
                    )
                }
            }
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Si [bloque] se apuntó con [categoria] (las de serie van por motivo, las propias por id). */
    private fun esDeLaCategoria(bloque: AvailabilityBlock, categoria: CategoriaDisponibilidad): Boolean =
        if (categoria.base != null) bloque.categoriaId == null && bloque.motivo == categoria.base
        else bloque.categoriaId == categoria.id

    /** Guarda bloques sueltos (una cita médica, los días de un viaje...). */
    fun guardarBloques(bloques: List<AvailabilityBlock>) {
        val familyId = familyIdActual ?: return
        if (bloques.isEmpty()) return
        viewModelScope.launch {
            bloques.forEach { availabilityRepository.guardarBloque(familyId, it) }
            cargarDatos(mostrarCargando = false)
        }
    }

    fun eliminarBloque(bloque: AvailabilityBlock) = eliminarBloqueDe(listOf(bloque.caregiverId), bloque)

    /**
     * Borra la ocupación de [bloque] a cada persona de [caregiverIds] que la tenga igual
     * (misma fecha, horas, categoría y detalle): desde una unidad familiar, borrar algo
     * lo quita también de todos sus miembros.
     */
    fun eliminarBloqueDe(caregiverIds: List<CaregiverId>, bloque: AvailabilityBlock) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            val existentes = availabilityRepository.obtenerDisponibilidad(familyId, bloque.fecha, bloque.fecha)
                .filter { it.caregiverId in caregiverIds && it.mismaOcupacionQue(bloque) }
            existentes.forEach { availabilityRepository.eliminarBloque(familyId, it.caregiverId, it.fecha, it.horaInicio) }
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Crea un horario guardado con nombre (p.ej. "Mañana 6-14") de una categoría, para toda la familia. */
    fun crearTurno(nombre: String, inicio: LocalTime, fin: LocalTime, categoriaId: CategoriaId) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        viewModelScope.launch {
            turnoRepository.guardarTurno(
                familyId, TurnoTrabajo(TurnoId(UUID.randomUUID().toString()), nombreLimpio, inicio, fin, categoriaId)
            )
            cargarDatos(mostrarCargando = false)
        }
    }

    /**
     * Crea o actualiza una categoría. Si es nueva (id vacío) se le da un id propio.
     * Las de serie se guardan con su mismo id fijo (y su motivo), con todo lo personalizado.
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
     * Borra una categoría. Las propias se borran del todo; las de serie se guardan marcadas
     * como eliminadas (si no, reaparecerían con sus valores de serie) y se pueden
     * recuperar. Los días que ya estaban apuntados con ella no se borran: pasan a verse
     * con el aspecto de serie de su motivo (o como "Otro" si era propia).
     */
    fun eliminarCategoria(categoria: CategoriaDisponibilidad) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            if (categoria.esBase) {
                categoriaRepository.guardarCategoria(familyId, categoria.copy(eliminada = true))
            } else {
                categoriaRepository.eliminarCategoria(familyId, categoria.id)
            }
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Vuelve a mostrar una categoría de serie que se había borrado, con sus valores de serie. */
    fun recuperarCategoria(categoria: CategoriaDisponibilidad) {
        val familyId = familyIdActual ?: return
        viewModelScope.launch {
            categoriaRepository.eliminarCategoria(familyId, categoria.id)
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
