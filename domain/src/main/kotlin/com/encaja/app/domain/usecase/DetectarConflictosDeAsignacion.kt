package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*

/** A qué le toca hacerse cargo la persona asignada: llevar al niño al empezar la
 * actividad, o recogerlo al terminar. Una misma persona puede tener asignados
 * ambos roles en la misma actividad. */
enum class RolResponsable { LLEVA, RECOGE }

/**
 * Aviso (no bloqueante: no cambia si la actividad cuenta como cubierta) de que la
 * persona asignada manualmente a llevar y/o recoger a un niño tiene, justo a esa
 * hora, un bloqueo de disponibilidad registrado en Familia (turno de trabajo, cita
 * médica, viaje...). La actividad sigue "cubierta" porque hay alguien asignado;
 * esto solo avisa de que quizá no vaya a poder cumplirlo. Si a la misma persona se
 * le ha asignado llevar Y recoger, esto es un único aviso con ambos roles en
 * [roles], no dos avisos repetidos.
 */
data class AvisoConflicto(
    val need: CoverageNeed,
    val child: Child,
    val caregiver: Caregiver,
    val roles: Set<RolResponsable>,
    val bloque: AvailabilityBlock
)

/**
 * Cruza, para cada necesidad de cobertura, a quién se ha asignado manualmente (llevar
 * y/o recoger) con sus bloqueos de disponibilidad de Familia. Si el id asignado no
 * corresponde a ningún cuidador conocido (p.ej. es una unidad familiar), no se
 * comprueba nada para ese rol: los bloqueos de disponibilidad son por cuidador.
 * Cuando la misma persona está asignada a llevar y a recoger, se agrupa en un
 * único aviso en vez de generar uno por rol.
 */
class DetectarConflictosDeAsignacion(
    private val ninos: List<Child>,
    private val caregivers: List<Caregiver>,
    private val disponibilidad: List<AvailabilityBlock>
) {
    operator fun invoke(needs: List<CoverageNeed>): List<AvisoConflicto> =
        needs.flatMap { need -> conflictosDe(need) }

    private fun conflictosDe(need: CoverageNeed): List<AvisoConflicto> {
        val child = ninos.firstOrNull { it.id == need.childId } ?: return emptyList()
        val asignaciones = listOfNotNull(
            need.quienLlevaId?.let { it to RolResponsable.LLEVA },
            need.quienRecogeId?.let { it to RolResponsable.RECOGE }
        )
        return asignaciones
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapNotNull { (idTexto, roles) -> conflictoDe(need, child, idTexto, roles.toSet()) }
    }

    private fun conflictoDe(need: CoverageNeed, child: Child, idTexto: String, roles: Set<RolResponsable>): AvisoConflicto? {
        val caregiver = caregivers.firstOrNull { it.id.value == idTexto } ?: return null
        val bloque = disponibilidad
            .filter { it.caregiverId == caregiver.id }
            .firstOrNull { it.ocupa(need.fecha, need.horaInicio, need.horaFin) }
            ?: return null
        return AvisoConflicto(need, child, caregiver, roles, bloque)
    }
}
