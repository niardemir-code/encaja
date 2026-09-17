package com.encaja.app.domain.model

/**
 * El resultado central de todo el motor: una necesidad de cobertura para
 * la que, cruzando disponibilidad y asignación, no hay nadie que pueda
 * hacerse cargo. Es lo que pinta de rojo el semáforo y la guía del día.
 */
data class Hueco(
    val need: CoverageNeed,
    val motivo: MotivoHueco
)

enum class MotivoHueco {
    /** Nadie tiene el cuidado asignado ese día (no hay patrón ni anulación). */
    SIN_ASIGNACION,
    /** Hay alguien asignado, pero esa persona no está disponible a esa hora. */
    ASIGNADO_NO_DISPONIBLE,
    /** Hay alguien asignado y disponible, pero no puede desplazarse y la tarea lo requiere. */
    ASIGNADO_SIN_DESPLAZAMIENTO
}
