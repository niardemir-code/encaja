package com.encaja.app.ui.familia

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import android.content.Context
import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CaregiverRole
import com.encaja.app.ui.ajustes.generarCaregiverIdDesdeNombre
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
import com.encaja.app.domain.repository.FiltroFamiliaRepository
import com.encaja.app.domain.repository.TurnoRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val familyUnitRepository: FamilyUnitRepository,
    private val filtroFamiliaRepository: FiltroFamiliaRepository,
    @ApplicationContext contexto: Context
) : ViewModel() {

    private val prefs = contexto.getSharedPreferences(PREFS_FILTRO, Context.MODE_PRIVATE)

    private val _pantalla = MutableStateFlow<FamiliaPantallaEstado>(FamiliaPantallaEstado.Cargando)
    val pantalla: StateFlow<FamiliaPantallaEstado> = _pantalla.asStateFlow()

    private var familyIdActual: FamilyId? = null

    /** Semanas de desplazamiento respecto a la actual: 0 = esta semana, -1 = anterior, +1 = siguiente. */
    private var offsetSemanas = 0

    /** Lunes de la semana que se está mostrando ahora mismo (se actualiza en [cargarDatos]). */
    private var lunesActual: LocalDate = LocalDate.now().lunesDeEstaSemana()

    /**
     * Qué personas y unidades familiares (por el id de cada una, como texto) se ocultan de
     * la cuadrícula, por semana (clave = lunes de esa semana). Hay dos niveles:
     *  - Personal: se guarda en este móvil y se mantiene hasta que se cambie, también al
     *    cerrar la app; cada semana tiene el suyo.
     *  - Compartido ("aplicar a todos los usuarios"): se guarda en Firestore para esa
     *    semana y lo ven todos los de la familia; mientras exista, manda sobre el personal.
     */
    private var ocultosLocales: Set<String> = emptySet()
    private var ocultosCompartidos: Set<String>? = null

    private val _ocultos = MutableStateFlow<Set<String>>(emptySet())
    /** Ids (persona o unidad) ocultos en la semana que se está viendo ahora (ya con el nivel que manda). */
    val ocultos: StateFlow<Set<String>> = _ocultos.asStateFlow()

    private val _filtroCompartido = MutableStateFlow(false)
    /** True si la semana que se está viendo tiene un filtro compartido con todos los usuarios. */
    val filtroCompartido: StateFlow<Boolean> = _filtroCompartido.asStateFlow()

    init { cargar() }

    private fun claveLocal(lunes: LocalDate) = "ocultos_${familyIdActual?.value}_$lunes"

    private fun leerLocal(lunes: LocalDate): Set<String> =
        prefs.getStringSet(claveLocal(lunes), emptySet()).orEmpty().toSet()

    private fun guardarLocal(lunes: LocalDate, ids: Set<String>) {
        prefs.edit().putStringSet(claveLocal(lunes), ids).apply()
    }

    private fun publicarOcultos() {
        _ocultos.value = ocultosCompartidos ?: ocultosLocales
        _filtroCompartido.value = ocultosCompartidos != null
    }

    private fun Set<String>.alternando(id: String): Set<String> = if (id in this) this - id else this + id

    /** Muestra u oculta a una persona o unidad de la cuadrícula, para la semana actual.
     * Si esa semana tiene filtro compartido, el cambio lo ven todos los usuarios. */
    fun alternarVisibilidad(idTexto: String) {
        val lunes = lunesActual
        val familyId = familyIdActual
        val compartido = ocultosCompartidos
        if (compartido != null && familyId != null) {
            val nuevo = compartido.alternando(idTexto)
            ocultosCompartidos = nuevo
            publicarOcultos()
            lanzarSeguro { runCatching { filtroFamiliaRepository.guardar(familyId, lunes, nuevo) } }
        } else {
            val nuevo = ocultosLocales.alternando(idTexto)
            ocultosLocales = nuevo
            guardarLocal(lunes, nuevo)
            publicarOcultos()
        }
    }

    /**
     * Activa o desactiva "aplicar a todos los usuarios" para la semana actual. Al activarlo,
     * la selección que ve esta persona pasa a ser la de todos; al desactivarlo, esa selección
     * se queda como la suya personal y los demás vuelven a la propia.
     */
    fun fijarFiltroCompartido(activar: Boolean) {
        val lunes = lunesActual
        val familyId = familyIdActual ?: return
        if (activar) {
            if (ocultosCompartidos != null) return
            ocultosCompartidos = ocultosLocales
            publicarOcultos()
            val enviado = ocultosLocales
            lanzarSeguro {
                runCatching { filtroFamiliaRepository.guardar(familyId, lunes, enviado) }.onFailure {
                    // Sin conexión: se queda como estaba (solo personal).
                    if (lunesActual == lunes) { ocultosCompartidos = null; publicarOcultos() }
                }
            }
        } else {
            val actual = ocultosCompartidos ?: return
            ocultosLocales = actual
            guardarLocal(lunes, actual)
            ocultosCompartidos = null
            publicarOcultos()
            lanzarSeguro { runCatching { filtroFamiliaRepository.quitar(familyId, lunes) } }
        }
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
        lanzarSeguro {
            _pantalla.value = FamiliaPantallaEstado.Cargando
            val uid = authRepository.sesionActual()?.uid
            if (uid == null) { _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@lanzarSeguro }
            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) { familyIdActual = null; _pantalla.value = FamiliaPantallaEstado.SinFamilia; return@lanzarSeguro }
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
        lanzarSeguro {
            if (mostrarCargando) _pantalla.value = FamiliaPantallaEstado.Cargando

            val lunes = LocalDate.now().lunesDeEstaSemana().plusWeeks(offsetSemanas.toLong())
            val domingo = lunes.plusDays(6)
            lunesActual = lunes

            coroutineScope {
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(familyId) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(familyId, lunes, domingo) }
                val turnosDeferred = async { turnoRepository.obtenerTurnos(familyId) }
                val categoriasDeferred = async { categoriaRepository.obtenerCategorias(familyId) }
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(familyId) }
                val compartidoDeferred = async { runCatching { filtroFamiliaRepository.obtener(familyId, lunes) }.getOrNull() }

                val mapper = FamiliaUiStateMapper(caregiversDeferred.await(), disponibilidadDeferred.await(), unidadesDeferred.await())
                val turnos = turnosDeferred.await().sortedBy { it.horaInicio }
                val guardadas = categoriasDeferred.await()
                val categorias = CategoriasBase.combinar(guardadas)
                val eliminadas = CategoriasBase.eliminadas(guardadas)

                ocultosLocales = leerLocal(lunes)
                ocultosCompartidos = compartidoDeferred.await()
                publicarOcultos()

                _pantalla.value = FamiliaPantallaEstado.ConDatos(
                    mapper.construir(lunes, esSemanaActual = offsetSemanas == 0)
                        .copy(turnos = turnos, categorias = categorias, categoriasEliminadas = eliminadas)
                )
            }
        }
    }

    /**
     * Todas las ocupaciones de [categoria] de las personas de [caregiverIds], sea cual sea
     * su horario (desde hace un mes, que es lo que se conserva, hasta dos años adelante).
     * Es lo que enseña «Repetir o borrar en otros días» como días ya marcados.
     */
    suspend fun ocupacionesDeCategoria(
        caregiverIds: List<CaregiverId>,
        categoria: CategoriaDisponibilidad
    ): List<AvailabilityBlock> {
        val familyId = familyIdActual ?: return emptyList()
        val hoy = LocalDate.now()
        return availabilityRepository.obtenerDisponibilidad(familyId, hoy.minusMonths(1), hoy.plusYears(2))
            .filter { it.caregiverId in caregiverIds && esDeLaCategoria(it, categoria) }
            .sortedBy { it.fecha }
    }

    /**
     * Guarda una ocupación de [categoria] para cada persona de [caregiverIds] (una, o todos
     * los miembros de una unidad) en [fechasAGuardar], sustituyendo lo que ya hubiera de esa
     * misma categoría esos días (da igual con qué horario), y borra la categoría de las
     * [fechasABorrar]. Si se estaba editando una ocupación ([original]), se quita primero.
     * Todo en una sola operación, con los borrados en tandas, para que sea rápido y no
     * quede a medias. Ya no se crean «series»: cada día es independiente.
     */
    fun guardarOcupacion(
        caregiverIds: List<CaregiverId>,
        categoria: CategoriaDisponibilidad,
        fechasAGuardar: Set<LocalDate>,
        fechasABorrar: Set<LocalDate>,
        inicio: LocalTime,
        fin: LocalTime,
        etiqueta: String?,
        original: AvailabilityBlock?
    ) {
        val familyId = familyIdActual ?: return
        if (caregiverIds.isEmpty()) return
        if (fechasAGuardar.isEmpty() && fechasABorrar.isEmpty() && original == null) return

        lanzarSeguro {
            val aBorrar = mutableListOf<AvailabilityBlock>()
            if (original != null) {
                aBorrar += availabilityRepository.obtenerDisponibilidad(familyId, original.fecha, original.fecha)
                    .filter { it.caregiverId in caregiverIds && it.mismaOcupacionQue(original) }
            }
            val tocadas = fechasAGuardar + fechasABorrar
            if (tocadas.isNotEmpty()) {
                aBorrar += availabilityRepository.obtenerDisponibilidad(familyId, tocadas.min(), tocadas.max())
                    .filter { it.caregiverId in caregiverIds && it.fecha in tocadas && esDeLaCategoria(it, categoria) }
            }
            availabilityRepository.eliminarBloques(
                familyId,
                aBorrar.distinctBy { Triple(it.caregiverId, it.fecha, it.horaInicio) }
            )
            // Todos los bloques nuevos de una vez (en tandas), no uno a uno: de lunes a viernes
            // durante meses son cientos de días y esperar cada escritura tardaba como un minuto.
            val nuevos = caregiverIds.flatMap { caregiverId ->
                fechasAGuardar.sorted().map { fecha ->
                    bloqueDeCategoria(categoria, caregiverId, fecha, inicio, fin, etiqueta, null)
                }
            }
            availabilityRepository.guardarBloques(familyId, nuevos)
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Si [bloque] se apuntó con [categoria] (las de serie van por motivo, las propias por id). */
    private fun esDeLaCategoria(bloque: AvailabilityBlock, categoria: CategoriaDisponibilidad): Boolean =
        if (categoria.base != null) bloque.categoriaId == null && bloque.motivo == categoria.base
        else bloque.categoriaId == categoria.id

    /** Crea un cuidador nuevo (sin cuenta vinculada) desde la propia pantalla de Familia. */
    fun agregarCuidador(nombre: String, apellido1: String, apellido2: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        val ap1 = apellido1.trim()
        val ap2 = apellido2.trim()
        lanzarSeguro {
            // Se parte de la lista completa (también quienes están ocultos por el filtro).
            val actuales = caregiverRepository.obtenerCuidadores(familyId)
            val id = generarCaregiverIdDesdeNombre(nombreLimpio, ap1, ap2, actuales.map { it.id })
            val nuevo = Caregiver(id, nombreLimpio, ap1, ap2, CaregiverRole.CUIDADOR)
            caregiverRepository.guardarCuidadores(familyId, actuales + nuevo)
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
        lanzarSeguro {
            val existentes = availabilityRepository.obtenerDisponibilidad(familyId, bloque.fecha, bloque.fecha)
                .filter { it.caregiverId in caregiverIds && it.mismaOcupacionQue(bloque) }
            availabilityRepository.eliminarBloques(familyId, existentes)
            cargarDatos(mostrarCargando = false)
        }
    }

    /**
     * Borra TODO lo apuntado (de cualquier categoría) en las casillas dadas. Cada casilla es
     * una persona o los miembros de una unidad familiar, y un día. Una sola lectura y el
     * borrado en tandas.
     */
    fun eliminarCasillas(casillas: List<Pair<List<CaregiverId>, LocalDate>>) {
        val familyId = familyIdActual ?: return
        if (casillas.isEmpty()) return
        lanzarSeguro {
            val fechas = casillas.map { it.second }
            val existentes = availabilityRepository.obtenerDisponibilidad(familyId, fechas.min(), fechas.max())
            val aBorrar = existentes.filter { b -> casillas.any { (ids, fecha) -> b.fecha == fecha && b.caregiverId in ids } }
            availabilityRepository.eliminarBloques(familyId, aBorrar)
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Crea un horario guardado con nombre (p.ej. "Mañana 6-14") de una categoría, para toda la familia. */
    fun crearTurno(nombre: String, inicio: LocalTime, fin: LocalTime, categoriaId: CategoriaId) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        lanzarSeguro {
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
        lanzarSeguro {
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
        lanzarSeguro {
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
        lanzarSeguro {
            categoriaRepository.eliminarCategoria(familyId, categoria.id)
            cargarDatos(mostrarCargando = false)
        }
    }

    /** Borra un turno de la lista. Los días ya marcados con ese turno no cambian. */
    fun eliminarTurno(turnoId: TurnoId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            turnoRepository.eliminarTurno(familyId, turnoId)
            cargarDatos(mostrarCargando = false)
        }
    }

    private companion object {
        const val PREFS_FILTRO = "familia_filtro"
    }
}
