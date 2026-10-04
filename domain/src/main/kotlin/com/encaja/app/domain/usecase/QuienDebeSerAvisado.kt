package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyUnit

enum class TipoAviso { LLEVAR, RECOGER }

/**
 * ¿Debe recibir [miId] el aviso de [tipo] de esta actividad? Sí si:
 *  - la creó él, o
 *  - es la persona asignada a llevar (aviso de llevar) o a recoger (aviso de recoger), o
 *    forma parte de la unidad familiar asignada a eso.
 * Las actividades antiguas, sin creador conocido, avisan a todos como hasta ahora.
 */
fun debeAvisarA(
    need: CoverageNeed,
    tipo: TipoAviso,
    miId: CaregiverId,
    unidades: List<FamilyUnit>
): Boolean {
    val creador = need.creadoPorId ?: return true
    if (creador == miId.value) return true
    val asignado = if (tipo == TipoAviso.LLEVAR) need.quienLlevaId else need.quienRecogeId
    if (asignado == null) return false
    if (asignado == miId.value) return true
    return unidades.any { it.id.value == asignado && miId in it.miembros }
}
