package com.encaja.app.domain.model

import java.time.LocalTime

data class TurnoId(val value: String)

/**
 * Un horario guardado con nombre que define la propia familia ("Mañana 6-14",
 * "Oficina 7-15", "Revisión 10-11"...), para elegirlo de un toque al apuntar algo
 * de una categoría por horas en vez de poner las horas a mano cada vez. Si
 * [horaFin] no es posterior a [horaInicio] es un turno de noche que acaba al día
 * siguiente. [categoriaId] es la categoría a la que pertenece; null en los turnos
 * antiguos, que eran siempre de Trabajo.
 */
data class TurnoTrabajo(
    val id: TurnoId,
    val nombre: String,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val categoriaId: CategoriaId? = null
) {
    /** Si este turno es de [categoria] (los antiguos sin categoría cuentan como de Trabajo). */
    fun esDe(categoria: CategoriaDisponibilidad): Boolean =
        if (categoriaId != null) categoriaId == categoria.id
        else categoria.base == MotivoNoDisponibilidad.TRABAJO
}
