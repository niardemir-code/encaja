package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import java.time.LocalDate

/**
 * Ids de las actividades (CoverageNeed) con más de un mes de antigüedad, para
 * borrarlas de golpe y no acumular datos que ya no hace falta conservar. Se
 * considera "antigua" la que ya ha pasado hace más de un mes desde [hoy] — las de
 * este mismo mes, aunque ya hayan pasado, no se tocan.
 */
fun actividadesAntiguas(needs: List<CoverageNeed>, hoy: LocalDate = LocalDate.now()): List<CoverageNeedId> {
    val limite = hoy.minusMonths(1)
    return needs.filter { it.fecha.isBefore(limite) }.map { it.id }
}
