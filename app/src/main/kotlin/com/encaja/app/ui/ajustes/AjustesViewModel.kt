package com.encaja.app.ui.ajustes

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
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
    private val familyUnitRepository: FamilyUnitRepository,
    private val cambiosDeMembresia: com.encaja.app.ui.CambiosDeMembresia,
    private val cuentaRepository: com.encaja.app.domain.repository.CuentaRepository,
    private val coverageNeedRepository: com.encaja.app.domain.repository.CoverageNeedRepository,
    private val assignmentRepository: com.encaja.app.domain.repository.AssignmentRepository
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
        lanzarSeguro {
            _pantalla.value = AjustesPantallaEstado.Cargando

            val uid = authRepository.sesionActual()?.uid
            if (uid == null) {
                familyIdActual = null
                _pantalla.value = AjustesPantallaEstado.SinFamilia
                return@lanzarSeguro
            }

            val membresia = familyMembershipRepository.obtenerMembresia(uid)
            if (membresia == null) {
                familyIdActual = null
                _pantalla.value = AjustesPantallaEstado.SinFamilia
                return@lanzarSeguro
            }
            familyIdActual = membresia.familyId

            val ninos = childRepository.obtenerNinos(membresia.familyId)
            val cuidadores = caregiverRepository.obtenerCuidadores(membresia.familyId)
            val unidades = familyUnitRepository.obtenerUnidades(membresia.familyId)
            val vinculados = cuentaRepository.cuidadoresVinculados(membresia.familyId)
            val yo = cuidadores.firstOrNull { it.id == membresia.caregiverId }
            _pantalla.value = AjustesPantallaEstado.ConDatos(
                ninos, cuidadores, unidades,
                miCaregiverId = membresia.caregiverId,
                soyAdmin = yo?.rol == CaregiverRole.ADMIN,
                vinculados = vinculados
            )
        }
    }

    /**
     * Borra la cuenta con la sesión abierta (la desvincula de la familia y elimina el acceso).
     * Si sale bien se llama a [alTerminar] (con un código de invitación si era la última cuenta
     * de la familia); si no, a [alFallar]. El cuidador y su historial no se tocan.
     */
    fun borrarMiCuenta(alTerminar: (codigoParaVolver: String?) -> Unit, alFallar: (String) -> Unit) {
        lanzarSeguro {
            cuentaRepository.borrarMiCuenta().fold(
                onSuccess = { codigo -> alTerminar(codigo) },
                onFailure = { alFallar("No se pudo borrar la cuenta. Comprueba la conexión e inténtalo de nuevo.") }
            )
        }
    }

    /** (Administradores) quita la cuenta vinculada a un cuidador; el cuidador sigue en la familia. */
    fun desvincularCuenta(caregiverId: CaregiverId, alFallar: (String) -> Unit) {
        lanzarSeguro {
            cuentaRepository.desvincularCuenta(caregiverId).fold(
                onSuccess = { cargar() },
                onFailure = { alFallar("No se pudo desvincular la cuenta. Inténtalo de nuevo.") }
            )
        }
    }

    /** Añade un niño nuevo a la familia actual, generando su id a partir del nombre. */
    fun agregarNino(nombre: String) {
        val familyId = familyIdActual ?: return
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return

        val actuales = (_pantalla.value as? AjustesPantallaEstado.ConDatos)?.ninos.orEmpty()
        val id = generarChildIdDesdeNombre(nombreLimpio, actuales.map { it.id })

        lanzarSeguro {
            childRepository.guardarNinos(familyId, actuales + Child(id, nombreLimpio))
            cargar()
        }
    }

    fun eliminarNino(childId: ChildId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
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

        lanzarSeguro {
            caregiverRepository.guardarCuidadores(familyId, actuales + nuevo)
            cargar()
        }
    }

    fun eliminarCuidador(caregiverId: CaregiverId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            // 1) Si tenía una cuenta vinculada, se desvincula primero: así deja de tener acceso
            //    a la familia aunque conserve su sesión abierta en otro dispositivo. Si no la
            //    tenía (o quien borra no es administrador) la llamada falla y se sigue igual.
            cuentaRepository.desvincularCuenta(caregiverId)

            // 2) Las unidades familiares en las que figuraba se eliminan por completo.
            val unidadesAfectadas = familyUnitRepository.obtenerUnidades(familyId)
                .filter { caregiverId in it.miembros }
            unidadesAfectadas.forEach { familyUnitRepository.eliminarUnidad(familyId, it.id) }

            // 3) Las asignaciones a esa persona (y a esas unidades) quedan en blanco.
            limpiarAsignaciones(
                familyId,
                setOf(caregiverId.value) + unidadesAfectadas.map { it.id.value },
                setOf(caregiverId)
            )

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
        lanzarSeguro {
            val uid = authRepository.sesionActual()?.uid ?: return@lanzarSeguro
            familyMembershipRepository.vincularAFamilia(uid, FamilyMembership(familyId, caregiverId))
            cambiosDeMembresia.avisar()
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

        lanzarSeguro {
            familyUnitRepository.guardarUnidades(familyId, actuales + nueva)
            cargar()
        }
    }

    fun eliminarUnidad(unidadId: FamilyUnitId) {
        val familyId = familyIdActual ?: return
        lanzarSeguro {
            familyUnitRepository.eliminarUnidad(familyId, unidadId)
            limpiarAsignaciones(familyId, setOf(unidadId.value), emptySet())
            cargar()
        }
    }

    /**
     * Deja en blanco las asignaciones que apuntaban a [idsAsignados] (cuidadores y/o unidades ya
     * eliminados): quien lleva / quien recoge de las actividades y, para los cuidadores en
     * [cuidadores], el patrón semanal y las anulaciones por fecha de Familia.
     */
    private suspend fun limpiarAsignaciones(
        familyId: FamilyId,
        idsAsignados: Set<String>,
        cuidadores: Set<CaregiverId>
    ) {
        val afectadas = coverageNeedRepository.obtenerTodosLosNeeds(familyId).filter {
            it.quienLlevaId in idsAsignados || it.quienRecogeId in idsAsignados
        }
        if (afectadas.isNotEmpty()) {
            coverageNeedRepository.guardarNeeds(
                familyId,
                afectadas.map {
                    it.copy(
                        quienLlevaId = it.quienLlevaId.takeUnless { id -> id in idsAsignados },
                        quienRecogeId = it.quienRecogeId.takeUnless { id -> id in idsAsignados }
                    )
                }
            )
        }
        if (cuidadores.isEmpty()) return

        assignmentRepository.obtenerPatrones(familyId)
            .filter { it.caregiverId in cuidadores }
            .forEach { assignmentRepository.eliminarPatron(familyId, it.diaSemana) }

        val hoy = java.time.LocalDate.now()
        assignmentRepository.obtenerAnulaciones(familyId, hoy.minusMonths(1), hoy.plusYears(2))
            .filterValues { it in cuidadores }
            .keys
            .forEach { assignmentRepository.eliminarAnulacion(familyId, it) }
    }
}
