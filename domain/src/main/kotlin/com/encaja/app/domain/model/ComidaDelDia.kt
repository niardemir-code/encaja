package com.encaja.app.domain.model

import java.time.LocalDate

/**
 * El menú (comida y cena) de un día concreto para toda la familia.
 * comida/cena a null significa "sin planificar todavía".
 */
data class ComidaDelDia(
    val fecha: LocalDate,
    val comida: String?,
    val cena: String?
)
