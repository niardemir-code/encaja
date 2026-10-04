package com.encaja.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Un momento del día en el que un niño necesita a alguien: recogerlo del
 * cole, llevarlo a una extraescolar, acompañarlo al médico. El cole en sí
 * NO es una CoverageNeed porque no requiere que nadie decida nada; solo
 * lo que hay alrededor del cole (antes y después) genera necesidad real.
 */
data class CoverageNeed(
    val id: CoverageNeedId,
    val childId: ChildId,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val descripcion: String,
    val requiereDesplazamiento: Boolean = true,
    /** Id (de cuidador o de unidad familiar) asignado manualmente desde la Guía para
     * llevar al niño al empezar la actividad. Independiente del patrón semanal de
     * Familia; null si no se ha asignado nadie. */
    val quienLlevaId: String? = null,
    /** Igual que [quienLlevaId] pero para recoger al terminar. */
    val quienRecogeId: String? = null,
    /** Id común a todas las ocurrencias creadas juntas con "Repetir cada semana",
     * para poder editarlas o borrarlas como grupo; null en una actividad puntual. */
    val grupoRepeticionId: String? = null,
    /** Minutos de antelación con los que avisar (notificación en el móvil) de que hay
     * que llevar al niño (antes de [horaInicio]); null = sin aviso. */
    val avisoLlevarMin: Int? = null,
    /** Igual que [avisoLlevarMin] pero para recoger (antes de [horaFin]). */
    val avisoRecogerMin: Int? = null,
    /** Id del icono elegido (ver IconoActividad, en la capa de UI de Guía) para
     * distinguir el tipo de actividad en las listas: deportiva, educativa,
     * recreativa... null = sin elegir (se muestra un icono genérico). */
    val icono: String? = null,
    /** Id del cuidador que creó la actividad: recibe sus avisos aunque no lleve ni recoja. null
     * en actividades creadas antes de existir este dato (esas avisan a toda la familia, como
     * antes, hasta que se vuelvan a guardar). */
    val creadoPorId: String? = null
)
