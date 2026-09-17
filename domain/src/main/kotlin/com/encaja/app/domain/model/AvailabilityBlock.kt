package com.encaja.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * El "motivo" es una lista corta y fija a propósito: así el icono y el
 * color de cada tipo son siempre los mismos en toda la app. La "etiqueta"
 * es el texto libre que cada familia añade si quiere precisar
 * ("Revisión rodilla"), y nunca condiciona la lógica de cálculo de huecos.
 */
enum class MotivoNoDisponibilidad { TRABAJO, MEDICO, VACACIONES, OTRO }

/**
 * Un tramo en el que un cuidador NO está disponible. Cubre tanto un turno
 * de trabajo recurrente como una cita médica puntual o un periodo largo
 * de baja: el modelo es el mismo, solo cambia cuánto dura.
 */
data class AvailabilityBlock(
    val caregiverId: CaregiverId,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val motivo: MotivoNoDisponibilidad,
    val etiqueta: String? = null
) {
    fun solapaCon(inicio: LocalTime, fin: LocalTime): Boolean =
        horaInicio < fin && inicio < horaFin
}
