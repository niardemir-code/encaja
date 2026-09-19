package com.encaja.app.ui.guia

import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import java.time.LocalDate

/**
 * Un bloque de actividad de un niño en la Guía del día (una franja horaria
 * con una descripción, coloreada según si está cubierta o es un hueco).
 */
data class BloqueGuia(
    val need: CoverageNeed,
    val cuidadorAsignado: String?,
    val cubierto: Boolean
)

/** La fila de un niño en la Guía: su nombre y los bloques de ese día, en orden. */
data class FilaGuia(
    val child: Child,
    val bloques: List<BloqueGuia>
)

data class GuiaUiState(
    val fecha: LocalDate,
    val filas: List<FilaGuia>
)
