package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.FamilyUnit

/** Qué avisos de una actividad debe programar este móvil. */
data class AvisosDeActividad(val need: CoverageNeed, val llevar: Boolean, val recoger: Boolean)

/**
 * Para cada actividad, si este móvil ([yo]) debe programar el aviso de llevar y el de
 * recoger. Si no se sabe quién es [yo] (sin sesión o sin familia), se programan todos,
 * como cuando los avisos no distinguían personas.
 */
fun planDeAvisos(
    needs: List<CoverageNeed>,
    yo: CaregiverId?,
    unidades: List<FamilyUnit>
): List<AvisosDeActividad> = needs.map { need ->
    AvisosDeActividad(
        need = need,
        llevar = yo == null || debeAvisarA(need, TipoAviso.LLEVAR, yo, unidades),
        recoger = yo == null || debeAvisarA(need, TipoAviso.RECOGER, yo, unidades)
    )
}

/**
 * Pone a las actividades nuevas (sin creador) a [yo] como creador. Las que ya tienen
 * creador no se tocan; sin [yo] no se cambia nada.
 */
fun sellarCreadorDe(needs: List<CoverageNeed>, yo: CaregiverId?): List<CoverageNeed> {
    if (yo == null || needs.none { it.creadoPorId == null }) return needs
    return needs.map { if (it.creadoPorId == null) it.copy(creadoPorId = yo.value) else it }
}
