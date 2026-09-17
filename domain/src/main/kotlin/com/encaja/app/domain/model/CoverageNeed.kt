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
    val requiereDesplazamiento: Boolean = true
)
