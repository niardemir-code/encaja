package com.encaja.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * El "motivo" es una lista corta y fija: marca el comportamiento de los 5 tipos de
 * serie. Las categorías propias de la familia se guardan con motivo OTRO y su
 * [AvailabilityBlock.categoriaId] (ver CategoriaDisponibilidad). La "etiqueta" es el
 * texto libre que cada familia añade si quiere precisar ("Revisión rodilla",
 * "Viaje a Madrid"), y nunca condiciona la lógica de cálculo de huecos.
 */
enum class MotivoNoDisponibilidad { TRABAJO, MEDICO, VIAJE, VACACIONES, OTRO }

/**
 * Un tramo en el que un cuidador NO está disponible. Cubre tanto un turno
 * de trabajo como una cita médica puntual o un viaje de varios días (que
 * se guarda como un bloque de día completo por cada fecha).
 *
 * Si [horaFin] no es posterior a [horaInicio] (p.ej. 22:00 -> 06:00), el
 * bloque es un turno de noche: ocupa desde [horaInicio] hasta el final de
 * [fecha] y sigue al día siguiente hasta [horaFin].
 */
data class AvailabilityBlock(
    val caregiverId: CaregiverId,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val motivo: MotivoNoDisponibilidad,
    val etiqueta: String? = null,
    /** Categoría propia de la familia; null en los bloques de los 5 tipos de serie. */
    val categoriaId: CategoriaId? = null,
    /**
     * false si su categoría es solo informativa (no ocupa a la persona). No se guarda:
     * se calcula al leer, a partir de la categoría (ver conBloqueoDeCategorias).
     */
    val bloquea: Boolean = true
) {
    val cruzaMedianoche: Boolean
        get() = !horaFin.isAfter(horaInicio)

    val todoElDia: Boolean
        get() = horaInicio == INICIO_DIA && !horaFin.isBefore(FIN_DIA)

    /** Solapa con el tramo [inicio, fin) del mismo día [fecha]. */
    fun solapaCon(inicio: LocalTime, fin: LocalTime): Boolean =
        if (cruzaMedianoche) fin.isAfter(horaInicio)
        else horaInicio < fin && inicio < horaFin

    /**
     * Si este bloque ocupa algo del tramo [inicio, fin) de [fechaTramo] — incluido
     * el final de un turno de noche que empezó la víspera. Un bloque de una categoría
     * informativa ([bloquea] = false) no ocupa nunca.
     */
    fun ocupa(fechaTramo: LocalDate, inicio: LocalTime, fin: LocalTime): Boolean = when {
        !bloquea -> false
        fechaTramo == fecha -> solapaCon(inicio, fin)
        fechaTramo == fecha.plusDays(1) -> cruzaMedianoche && inicio.isBefore(horaFin)
        else -> false
    }

    companion object {
        val INICIO_DIA: LocalTime = LocalTime.MIN
        val FIN_DIA: LocalTime = LocalTime.of(23, 59)
    }
}
