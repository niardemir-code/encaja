package com.encaja.app.avisos

// NOTA: depende de Android/Hilt, no compilado en este entorno.

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyId
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.ChildRepository
import com.encaja.app.domain.repository.CoverageNeedRepository
import com.encaja.app.domain.repository.FamilyMembershipRepository
import com.encaja.app.domain.repository.FamilyUnitRepository
import com.encaja.app.domain.usecase.TipoAviso
import com.encaja.app.domain.usecase.debeAvisarA
import kotlinx.coroutines.sync.Mutex
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decide qué avisos de actividades programa ESTE móvil: los de las actividades que creó su
 * dueño y, además, el de llevar si le toca llevar y el de recoger si le toca recoger (él
 * o una unidad familiar de la que forma parte). Lo usan el guardado de actividades, el
 * arranque de la app y los avisos push que llegan cuando otro miembro cambia algo.
 */
@Singleton
class SincronizadorDeAvisos @Inject constructor(
    private val authRepository: AuthRepository,
    private val membershipRepository: FamilyMembershipRepository,
    private val coverageNeedRepository: CoverageNeedRepository,
    private val childRepository: ChildRepository,
    private val familyUnitRepository: FamilyUnitRepository,
    private val programador: ProgramadorDeAvisos
) {
    private val enMarcha = Mutex()
    @Volatile private var pendiente = false

    /** El cuidador de la cuenta abierta en este móvil, o null si no hay sesión o familia. */
    suspend fun miCaregiverId(): CaregiverId? {
        val uid = authRepository.sesionActual()?.uid ?: return null
        return membershipRepository.obtenerMembresia(uid)?.caregiverId
    }

    /** Pone a las actividades nuevas (sin creador) el cuidador de este móvil como creador. */
    suspend fun sellarCreador(needs: List<CoverageNeed>): List<CoverageNeed> {
        if (needs.none { it.creadoPorId == null }) return needs
        val yo = miCaregiverId()?.value ?: return needs
        return needs.map { if (it.creadoPorId == null) it.copy(creadoPorId = yo) else it }
    }

    /** Programa en este móvil los avisos que le tocan de [needs] y quita los que no. */
    suspend fun programarMisAvisos(familyId: FamilyId, needs: List<CoverageNeed>) {
        if (needs.isEmpty()) return
        val yo = miCaregiverId()
        val unidades = familyUnitRepository.obtenerUnidades(familyId)
        val nombres = childRepository.obtenerNinos(familyId).associate { it.id to it.nombre }
        needs.forEach { need ->
            val llevar = yo == null || debeAvisarA(need, TipoAviso.LLEVAR, yo, unidades)
            val recoger = yo == null || debeAvisarA(need, TipoAviso.RECOGER, yo, unidades)
            programador.programar(need, nombres[need.childId], llevar, recoger)
        }
    }

    /**
     * Vuelve a programar los avisos de los próximos 60 días (las alarmas se pierden al
     * reiniciar o reinstalar, y los cambios de otros miembros solo llegan a través de los
     * datos). Si ya hay una en marcha, anota que hace falta otra al terminar y sale.
     */
    suspend fun reprogramar(familyId: FamilyId) {
        if (!enMarcha.tryLock()) {
            pendiente = true
            return
        }
        try {
            do {
                pendiente = false
                val hoy = LocalDate.now()
                val proximas = coverageNeedRepository.obtenerNeeds(familyId, hoy, hoy.plusDays(60))
                programarMisAvisos(familyId, proximas)
            } while (pendiente)
        } finally {
            enMarcha.unlock()
        }
    }

    /** Igual que [reprogramar] pero averiguando la familia de la cuenta abierta. */
    suspend fun reprogramar() {
        val uid = authRepository.sesionActual()?.uid ?: return
        val familyId = membershipRepository.obtenerMembresia(uid)?.familyId ?: return
        reprogramar(familyId)
    }
}
