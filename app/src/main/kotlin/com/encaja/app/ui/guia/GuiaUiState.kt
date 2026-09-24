package com.encaja.app.ui.guia

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.ui.familia.Responsable
import java.time.LocalDate

/**
 * Un bloque de actividad de un niño en la Guía del día (una franja horaria
 * con una descripción, coloreada según si está cubierta o es un hueco).
 * [quienLleva]/[quienRecoge] son quién se encarga de llevar al niño al
 * empezar la actividad y de recogerlo al terminar, si se han elegido.
 */
data class BloqueGuia(
    val need: CoverageNeed,
    val cubierto: Boolean,
    val quienLleva: Responsable? = null,
    val quienRecoge: Responsable? = null
)

/** La fila de un niño en la Guía: su nombre y los bloques de ese día, en orden. */
data class FilaGuia(
    val child: Child,
    val bloques: List<BloqueGuia>
)

data class GuiaUiState(
    val fecha: LocalDate,
    val filas: List<FilaGuia>,
    /** Todas las personas y unidades familiares, para elegir quién lleva/recoge al editar una actividad. */
    val responsables: List<Responsable> = emptyList(),
    /** Iniciales (2 letras, desambiguadas) de cada cuidador, para el icono de quién lleva/recoge. */
    val iniciales: Map<CaregiverId, String> = emptyMap()
)
