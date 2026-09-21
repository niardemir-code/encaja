package com.encaja.app.domain.model

import java.time.LocalTime

data class TurnoId(val value: String)

/**
 * Un turno de trabajo con nombre que define la propia familia ("Mañana 6-14",
 * "Oficina 7-15"...), para elegirlo de un toque al marcar el trabajo de un día
 * en vez de poner las horas a mano cada vez. Si [horaFin] no es posterior a
 * [horaInicio] es un turno de noche que acaba al día siguiente.
 */
data class TurnoTrabajo(
    val id: TurnoId,
    val nombre: String,
    val horaInicio: LocalTime,
    val horaFin: LocalTime
)
