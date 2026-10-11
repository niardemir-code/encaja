package com.encaja.app.ui.guia

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Lógica pura (sin Compose) detrás del diálogo para crear o editar una
 * actividad (CoverageNeed) desde la Guía del día, separada para poder
 * testearla sin emulador. Igual que DisponibilidadEdicion.kt en Familia.
 */

/**
 * Una actividad por cada fecha de [fechas], todas iguales salvo el día. [generarId]
 * se llama una vez por fecha (para poder pasar un id fijo al editar una sola, o uno
 * nuevo por cada ocurrencia al crear con repetición). Si hay más de una fecha (se
 * creó con repetición), todas comparten un mismo [CoverageNeed.grupoRepeticionId]
 * generado aquí, para poder editarlas o borrarlas juntas más adelante; una sola
 * fecha no lleva grupo (actividad puntual), salvo que se pase [grupoRepeticionId]
 * explícitamente (al editar una ocurrencia que ya pertenecía a un grupo).
 */
fun crearActividades(
    fechas: List<LocalDate>,
    childId: ChildId,
    inicio: LocalTime,
    fin: LocalTime,
    descripcion: String,
    requiereDesplazamiento: Boolean,
    generarId: () -> CoverageNeedId,
    quienLlevaId: String? = null,
    quienRecogeId: String? = null,
    grupoRepeticionId: String? = null,
    avisoLlevarMin: Int? = null,
    avisoRecogerMin: Int? = null,
    icono: String? = null,
    creadoPorId: String? = null
): List<CoverageNeed> {
    val grupoId = grupoRepeticionId ?: if (fechas.size > 1) UUID.randomUUID().toString() else null
    return fechas.map { fecha ->
        CoverageNeed(
            generarId(), childId, fecha, inicio, fin, descripcion.trim(), requiereDesplazamiento,
            quienLlevaId, quienRecogeId, grupoId, avisoLlevarMin, avisoRecogerMin, icono, creadoPorId
        )
    }
}
