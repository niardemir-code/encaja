package com.encaja.app.domain.model

enum class CaregiverRole { ADMIN, CUIDADOR, SOLO_LECTURA }

/**
 * Una persona que puede hacerse cargo de un niño: padre, madre, abuelo,
 * canguro... El "puedeDesplazarse" existe porque, como vimos con Carmen
 * tras su operación, hay cuidadores disponibles pero solo para "estar con"
 * el niño, no para recogerlo o llevarlo a ningún sitio.
 */
data class Caregiver(
    val id: CaregiverId,
    val nombre: String,
    val rol: CaregiverRole,
    val puedeDesplazarse: Boolean = true
)
