package com.encaja.app.ui

// NOTA: depende de Hilt/ViewModel (androidx.lifecycle), no compilado en este entorno.

import androidx.lifecycle.ViewModel
import com.encaja.app.ui.common.lanzarSeguro
import com.encaja.app.avisos.ProgramadorDeAvisos
import com.encaja.app.avisos.SincronizadorDeAvisos
import com.encaja.app.domain.repository.DispositivoRepository
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.AvailabilityRepository
import com.encaja.app.domain.repository.CaregiverRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.ResultadoMembresia
import com.encaja.app.domain.usecase.actividadesAntiguas
import com.encaja.app.ui.familia.calcularInicialesCuidadores
import android.content.Context
import com.encaja.app.ui.common.BajaDeDispositivo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val avisos: ProgramadorDeAvisos,
    private val sincronizador: SincronizadorDeAvisos,
    private val dispositivos: DispositivoRepository,
    private val cambiosDeMembresia: CambiosDeMembresia,
    @ApplicationContext private val contexto: Context
) : ViewModel() {

    private val _inicialesUsuario = MutableStateFlow("")
    val inicialesUsuario: StateFlow<String> = _inicialesUsuario.asStateFlow()

    /** Se emite cuando esta cuenta deja de pertenecer a su familia con la app abierta. */
    val membresiaPerdida = cambiosDeMembresia.perdidas

    private var cancelarEscucha: (() -> Unit)? = null
    private var teniaMembresia = false
    private var ultimaFamilia: FamilyId? = null

    init {
        lanzarSeguro(avisar = false) { iniciar() }
        // Al vincularse la cuenta a una familia (código de invitación, crear familia o
        // "Vincularme"), se repite todo esto sin esperar a reiniciar la app.
        lanzarSeguro(avisar = false) {
            cambiosDeMembresia.eventos.collect {
                iniciar()
                vigilarMembresia()
            }
        }
        vigilarMembresia()
    }

    /**
     * Escucha en tiempo real si esta cuenta sigue vinculada a su familia. Si un administrador la
     * desvincula (o elimina a su cuidador) mientras la app está abierta, se avisa para llevar al
     * usuario a la pantalla de sin familia en vez de dejarle seguir editando.
     */
    private fun vigilarMembresia() {
        cancelarEscucha?.invoke()
        cancelarEscucha = null
        teniaMembresia = false
        val uid = authRepository.sesionActual()?.uid ?: return
        cancelarEscucha = familyMembershipRepository.escucharMembresia(uid) { resultado ->
            when (resultado) {
                is ResultadoMembresia.Tiene -> {
                    teniaMembresia = true
                    ultimaFamilia = resultado.membresia.familyId
                    BajaDeDispositivo.recordarMembresia(contexto, uid)
                }
                // También si la baja ocurrió con la app cerrada: se recordó en un uso anterior.
                ResultadoMembresia.NoTiene -> if (teniaMembresia || BajaDeDispositivo.tuvoMembresia(contexto, uid)) {
                    teniaMembresia = false
                    cambiosDeMembresia.avisarPerdida()
                    cambiosDeMembresia.avisar() // las pantallas vuelven a comprobar su familia
                }
                ResultadoMembresia.Error -> Unit
            }
        }
    }

    /**
     * Tras avisar al usuario de que su cuenta ha sido dada de baja: cancela los avisos
     * programados y borra todos los datos de la app en este dispositivo. Cierra la app.
     */
    fun borrarDatosDelDispositivo() {
        lanzarSeguro(avisar = false) {
            runCatching {
                val familyId = ultimaFamilia
                if (familyId != null) {
                    val hoy = java.time.LocalDate.now()
                    // Sin permiso en el servidor, esta lectura sale de la copia local.
                    coverageNeedRepository.obtenerNeeds(familyId, hoy.minusDays(1), hoy.plusDays(120))
                        .forEach { avisos.cancelar(it.id) }
                }
            }
            BajaDeDispositivo.borrarTodo(contexto)
        }
    }

    override fun onCleared() {
        cancelarEscucha?.invoke()
        super.onCleared()
    }

    private suspend fun iniciar() {
        run {
            val sesion = authRepository.sesionActual() ?: return
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
            // Registra este móvil para recibir las notificaciones push de la familia.
            dispositivos.registrarDispositivoActual()
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
        sincronizador.reprogramar(familyId)
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
