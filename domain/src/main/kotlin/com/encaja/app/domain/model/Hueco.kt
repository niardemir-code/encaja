package com.encaja.app.domain.model

/**
 * El resultado central de todo el motor: una necesidad de cobertura que requiere
 * acompañamiento y a la que le falta alguien asignado (a llevarla, a recogerla, o
 * ambos) en la propia actividad. Es lo que pinta de rojo el semáforo y la guía del
 * día. El patrón semanal de Familia es solo orientativo: no interviene aquí.
 */
data class Hueco(
    val need: CoverageNeed,
    val motivo: MotivoHueco
)

enum class MotivoHueco {
    /** No hay nadie asignado a llevarla. */
    FALTA_QUIEN_LLEVA,
    /** No hay nadie asignado a recogerla. */
    FALTA_QUIEN_RECOGE,
    /** No hay nadie asignado ni a llevarla ni a recogerla. */
    SIN_ASIGNACION
}
