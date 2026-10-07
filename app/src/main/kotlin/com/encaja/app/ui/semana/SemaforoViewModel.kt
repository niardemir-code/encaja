package com.encaja.app.ui.semana

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.model.FamilyMembership
import com.encaja.app.domain.repository.AnuncioRepository
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.ResultadoMembresia
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.encaja.app.domain.repository.InviteRepository
import com.encaja.app.domain.usecase.lunesDeEstaSemana
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.guia.EditorDeActividades
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SemaforoViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val inviteRepository: InviteRepository,
    private val childRepository: ChildRepository,
    private val caregiverRepository: CaregiverRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val anuncioRepository: AnuncioRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val editor: EditorDeActividades,
    private val cambiosDeMembresia: com.encaja.app.ui.CambiosDeMembresia
) : ViewModel() {

    private val _pantalla = MutableStateFlow<SemaforoPantallaEstado>(SemaforoPantallaEstado.Cargando)
    val pantalla: StateFlow<SemaforoPantallaEstado> = _pantalla.asStateFlow()

    private val _cuidadores = MutableStateFlow<List<Caregiver>>(emptyList())
    val cuidadores: StateFlow<List<Caregiver>> = _cuidadores.asStateFlow()

    /** Para poder mostrar el nombre del niño y quién lleva/recoge en el diálogo de
     * información de un día verde del semáforo. */
    private val _ninos = MutableStateFlow<List<Child>>(emptyList())
    val ninos: StateFlow<List<Child>> = _ninos.asStateFlow()

    /** Personas y unidades familiares, para elegir quién lleva/recoge al editar una
     * actividad directamente desde un aviso de esta pantalla. */
    private val _responsables = MutableStateFlow<List<Responsable>>(emptyList())
    val responsables: StateFlow<List<Responsable>> = _responsables.asStateFlow()

    /** Familia del usuario ya resuelta, para que "invitar" sepa dónde escribir. Se
     * comprueba una sola vez (en [cargar]): cambiar de semana no vuelve a comprobar
     * sesión ni familia, porque no cambian mientras se navega. */
    private var familyIdActual: FamilyId? = null

    /** Nombre del cuidador actual, para firmar los anuncios que publique. */
    private var nombreCuidadorActual: String = "Alguien de la familia"

    /** Lunes de la semana que se está mostrando. */
    private var lunesActual: LocalDate = LocalDate.now().lunesDeEstaSemana()

    init {
        cargar()
        // Si la cuenta pierde (o gana) su familia mientras la app está abierta, se vuelve a
        // comprobar para mostrar la pantalla que corresponda.
        lanzarSeguro(avisar = false) {
            cambiosDeMembresia.eventos.collect { cargar(mostrarCargando = false) }
        }
    }

    /** Recarga completa: vuelve a comprobar sesión y familia (por si han cambiado) y
     * recarga la semana que se estuviera viendo. Se usa al entrar en la pestaña y tras
     * publicar/borrar un anuncio o canjear un código. */
    fun recargar() = cargar(mostrarCargando = _pantalla.value !is SemaforoPantallaEstado.ConDatos)

    /** Avanza o retrocede semanas desde la cabecera (-1 anterior, +1 siguiente). No hace
     * falta volver a comprobar sesión ni familia: solo cambian los datos de la semana. */
    fun cambiarSemana(delta: Int) {
        lunesActual = lunesActual.plusWeeks(delta.toLong())
        cargarSemana(mostrarCargando = false)
    }

    /** Vuelve directamente a la semana actual, igual que en Familia y Menú. */
    fun irASemanaActual() {
        lunesActual = LocalDate.now().lunesDeEstaSemana()
        cargarSemana(mostrarCargando = false)
    }

    /** Salta directamente a la semana que contiene [fecha], elegida en el calendario. */
    fun irASemanaDe(fecha: LocalDate) {
        lunesActual = fecha.lunesDeEstaSemana()
        cargarSemana(mostrarCargando = false)
    }

    /** Introduce un código de invitación y, si es válido, vincula al usuario a esa familia. */
    fun canjearCodigo(codigo: String, alFallar: (String) -> Unit) {
        lanzarSeguro {
            val uid = authRepository.sesionActual()?.uid ?: return@lanzarSeguro
            inviteRepository.canjearInvitacion(codigo).fold(
                onSuccess = { membership ->
                    familyMembershipRepository.vincularAFamilia(uid, membership)
                    cambiosDeMembresia.avisar()
                    cargar()
                },
                onFailure = { error -> alFallar(error.message ?: "Código no válido") }
            )
        }
    }

    /**
     * Crea una familia nueva desde cero para quien empieza sin invitación: la persona queda
     * como primer cuidador (administrador) y su cuenta se vincula a él.
     */
    fun crearFamilia(nombre: String, apellido1: String, apellido2: String, alFallar: (String) -> Unit) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return
        lanzarSeguro {
            val uid = authRepository.sesionActual()?.uid ?: return@lanzarSeguro
            // Antes de crear nada, comprobar que esta cuenta de verdad no tiene ya una familia:
            // crear una nueva sobrescribiría el vínculo existente (users/{uid}).
            when (familyMembershipRepository.consultarMembresia(uid)) {
                is ResultadoMembresia.Tiene -> { cargar(); return@lanzarSeguro }
                ResultadoMembresia.Error -> {
                    alFallar("No se pudo comprobar tu cuenta. Revisa la conexión e inténtalo de nuevo")
                    return@lanzarSeguro
                }
                ResultadoMembresia.NoTiene -> Unit
            }
            try {
                val familyId = FamilyId(java.util.UUID.randomUUID().toString())
                val caregiverId = com.encaja.app.ui.ajustes.generarCaregiverIdDesdeNombre(
                    nombreLimpio, apellido1.trim(), apellido2.trim(), emptyList()
                )
                val yo = Caregiver(
                    caregiverId, nombreLimpio, apellido1.trim(), apellido2.trim(),
                    com.encaja.app.domain.model.CaregiverRole.ADMIN
                )
                // Primero el vínculo (users/{uid}) y después la ficha: las reglas de Firestore
                // solo dejan escribir en una familia a quien ya pertenece a ella.
                familyMembershipRepository.vincularAFamilia(uid, FamilyMembership(familyId, caregiverId))
                caregiverRepository.guardarCuidadores(familyId, listOf(yo))
                cambiosDeMembresia.avisar()
                cargar()
            } catch (e: Exception) {
                alFallar("No se pudo crear la familia, inténtalo de nuevo")
            }
        }
    }

    /** Genera un código de invitación para un cuidador concreto de la familia actual. */
    fun generarInvitacion(caregiverId: CaregiverId, alConseguirlo: (String) -> Unit, alFallar: (String) -> Unit) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            inviteRepository.generarInvitacion(familyId, caregiverId).fold(
                onSuccess = { codigo -> alConseguirlo(codigo) },
                onFailure = { error -> alFallar(error.message ?: "No se pudo generar el código") }
            )
        }
    }

    /** Primera carga (o recarga forzada): valida sesión y familia — lo único que de
     * verdad puede tardar un poco — y solo entonces pide los datos de la semana. */
    private fun cargar(mostrarCargando: Boolean = true) {
        lanzarSeguro {
            // Si ya había datos en pantalla (recarga al reentrar en la pestaña), se dejan
            // visibles hasta que lleguen los nuevos: pasar por "Cargando" vaciaba la lista y
            // con ella se perdía la posición del scroll.
            if (mostrarCargando) _pantalla.value = SemaforoPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                _pantalla.value = SemaforoPantallaEstado.SinFamilia
                return@lanzarSeguro
            }

            val membresia = when (val resultado = familyMembershipRepository.consultarMembresia(uid)) {
                is ResultadoMembresia.Tiene -> resultado.membresia
                ResultadoMembresia.NoTiene -> {
                    familyIdActual = null
                    _pantalla.value = SemaforoPantallaEstado.SinFamilia
                    return@lanzarSeguro
                }
                ResultadoMembresia.Error -> {
                    // Un fallo de red NO es "sin familia": si ya había datos en pantalla se
                    // dejan como están; si no, se ofrece reintentar en vez de "Crear mi familia".
                    if (_pantalla.value !is SemaforoPantallaEstado.ConDatos) {
                        _pantalla.value = SemaforoPantallaEstado.ErrorDeConexion
                    }
                    return@lanzarSeguro
                }
            }
            familyIdActual = membresia.familyId

            cargarSemana(mostrarCargando = false, caregiverIdPropio = membresia.caregiverId)
        }
    }

    /**
     * Recarga solo los datos de [lunesActual]. Las peticiones de la semana son
     * independientes entre sí (ninguna necesita el resultado de otra), así que se
     * lanzan todas a la vez con [async] en vez de esperarlas una detrás de otra: avanzar
     * de semana tarda lo que tarda la más lenta, no la suma de todas.
     * [mostrarCargando] = false deja la semana anterior visible mientras llega la
     * nueva, en vez de pasar por una pantalla en blanco a cada cambio.
     */
    private fun cargarSemana(mostrarCargando: Boolean, caregiverIdPropio: CaregiverId? = null) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            if (mostrarCargando) _pantalla.value = SemaforoPantallaEstado.Cargando

            val lunes = lunesActual
            val domingo = lunes.plusDays(6)

            coroutineScope {
                val caregiversDeferred = async { caregiverRepository.obtenerCuidadores(familyId) }
                val ninosDeferred = async { childRepository.obtenerNinos(familyId) }
                val disponibilidadDeferred = async { availabilityRepository.obtenerDisponibilidad(familyId, lunes, domingo) }
                val needsDeferred = async { coverageNeedRepository.obtenerNeeds(familyId, lunes, domingo) }
                val anunciosDeferred = async { anuncioRepository.obtenerAnuncios(familyId) }
                val unidadesDeferred = async { familyUnitRepository.obtenerUnidades(familyId) }

                val caregivers = caregiversDeferred.await()
                _cuidadores.value = caregivers
                _ninos.value = ninosDeferred.await()
                _responsables.value = caregivers.map { Responsable.Persona(it) } +
                    unidadesDeferred.await().map { Responsable.Unidad(it) }
                if (caregiverIdPropio != null) {
                    nombreCuidadorActual = caregivers.firstOrNull { it.id == caregiverIdPropio }?.nombreCompleto
                        ?: nombreCuidadorActual
                }

                val mapper = SemaforoUiStateMapper(ninosDeferred.await(), caregivers, disponibilidadDeferred.await())
                _pantalla.value = SemaforoPantallaEstado.ConDatos(
                    mapper.construir(lunes, needsDeferred.await()).copy(
                        anuncios = anunciosDeferred.await(),
                        esSemanaActual = lunes == LocalDate.now().lunesDeEstaSemana()
                    )
                )
            }
        }
    }

    /**
     * Refresca solo la lista de cuidadores y de responsables (personas + unidades), sin tocar la
     * semana en pantalla. Se llama al abrir «Invitar a alguien» para que un cuidador recién
     * creado en Ajustes aparezca sin tener que cerrar y abrir la app.
     */
    fun recargarCuidadores() {
        val familyId = familyIdActual ?: return
        lanzarSeguro(avisar = false) {
            val caregivers = caregiverRepository.obtenerCuidadores(familyId)
            val unidades = familyUnitRepository.obtenerUnidades(familyId)
            _cuidadores.value = caregivers
            _responsables.value = caregivers.map { Responsable.Persona(it) } +
                unidades.map { Responsable.Unidad(it) }
        }
    }

    /** Publica un anuncio nuevo en el tablón, firmado con el nombre del cuidador actual. */
    fun publicarAnuncio(texto: String) {
        val textoLimpio = texto.trim()
        if (textoLimpio.isBlank()) return
        val familyId = familyIdActual ?: return

        lanzarSeguro {
            anuncioRepository.publicarAnuncio(familyId, nombreCuidadorActual, textoLimpio)
            cargarSemana(mostrarCargando = false)
        }
    }

    /** Cualquier miembro de la familia puede borrar un anuncio del tablón. */
    fun eliminarAnuncio(anuncioId: AnuncioId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            anuncioRepository.eliminarAnuncio(familyId, anuncioId)
            cargarSemana(mostrarCargando = false)
        }
    }

    /* ── Edición de una actividad desde un aviso o hueco (mismo diálogo que en la Guía) ── */

    fun guardarActividades(needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.guardar(familyId, needs, aplicarATodaLaSerie)
            cargarSemana(mostrarCargando = false)
        }
    }

    fun eliminarActividad(id: CoverageNeedId, grupoRepeticionId: String?, fecha: LocalDate, aplicarATodaLaSerie: Boolean) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.eliminar(familyId, id, grupoRepeticionId, fecha, aplicarATodaLaSerie)
            cargarSemana(mostrarCargando = false)
        }
    }

    /** Ver [EditorDeActividades.ocurrenciasDeSerie]. */
    suspend fun ocurrenciasDeSerie(need: CoverageNeed): List<CoverageNeed> {
        val familyId = familyIdActual ?: return listOf(need)
        return editor.ocurrenciasDeSerie(familyId, need)
    }

    suspend fun patronDeSerie(grupoRepeticionId: String, desde: LocalDate): Pair<Set<DayOfWeek>, LocalDate?> {
        val familyId = familyIdActual ?: return emptySet<DayOfWeek>() to null
        return editor.patronDeSerie(familyId, grupoRepeticionId, desde)
    }

    fun actualizarSerie(plantilla: CoverageNeed, nuevasFechas: List<LocalDate>) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            editor.actualizarSerie(familyId, plantilla, nuevasFechas)
            cargarSemana(mostrarCargando = false)
        }
    }
}
