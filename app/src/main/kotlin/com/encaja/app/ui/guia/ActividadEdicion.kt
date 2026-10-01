package com.encaja.app.ui.guia

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Lógica pura (sin Compose) detrás del diálogo para crear o editar una
 * actividad (CoverageNeed) desde la Guía del día, separada para poder
 * testearla sin emulador. Igual que DisponibilidadEdicion.kt en Familia.
 */

/** Las fechas entre [desde] y [hasta] (ambas incluidas) cuyo día de la semana está en [dias]. */
fun fechasRepetidas(desde: LocalDate, hasta: LocalDate, dias: Set<DayOfWeek>): List<LocalDate> {
    if (hasta.isBefore(desde)) return emptyList()
    return generateSequence(desde) { it.plusDays(1) }
        .takeWhile { !it.isAfter(hasta) }
        .filter { it.dayOfWeek in dias }
        .toList()
}

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
    avisoRecogerMin: Int? = null
): List<CoverageNeed> {
    val grupoId = grupoRepeticionId ?: if (fechas.size > 1) UUID.randomUUID().toString() else null
    return fechas.map { fecha ->
        CoverageNeed(
            generarId(), childId, fecha, inicio, fin, descripcion.trim(), requiereDesplazamiento,
            quienLlevaId, quienRecogeId, grupoId, avisoLlevarMin, avisoRecogerMin
        )
    }
}

/**
 * Al usar "Repetir" para cambiar el patrón semanal de una serie ya existente (marcando
 * "Repetir cada semana" desde una ocurrencia que ya pertenece a un grupo): compara el
 * nuevo patrón de días ([nuevasFechas], ya calculado desde la fecha de [plantilla] en
 * adelante) con las ocurrencias reales del grupo a partir de esa misma fecha
 * ([existentesDesdeSuFecha]). Las fechas que coinciden en ambos conjuntos se actualizan
 * conservando su id (nunca se duplican); las del nuevo patrón que no existían se crean
 * con un id nuevo (vía [generarId]); las que ya no están en el nuevo patrón se devuelven
 * para borrarlas. [plantilla] aporta los datos (niño, horario, descripción,
 * acompañamiento...) que se copian a cada fecha resultante.
 */
fun diferenciaSerie(
    plantilla: CoverageNeed,
    existentesDesdeSuFecha: List<CoverageNeed>,
    nuevasFechas: List<LocalDate>,
    generarId: () -> CoverageNeedId
): Pair<List<CoverageNeed>, List<CoverageNeed>> {
    val existentesPorFecha = existentesDesdeSuFecha.associateBy { it.fecha }
    val nuevasFechasSet = nuevasFechas.toSet()
    val aGuardar = nuevasFechas.map { fecha ->
        val id = existentesPorFecha[fecha]?.id ?: generarId()
        plantilla.copy(id = id, fecha = fecha)
    }
    val aBorrar = existentesDesdeSuFecha.filter { it.fecha !in nuevasFechasSet }
    return aGuardar to aBorrar
}
