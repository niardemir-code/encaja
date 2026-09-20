package com.encaja.app.domain.model

enum class CaregiverRole { ADMIN, CUIDADOR, SOLO_LECTURA }

/**
 * Una persona que puede hacerse cargo de un niño: padre, madre, abuelo,
 * canguro... El nombre se guarda en tres campos separados (nombre,
 * apellido1, apellido2) para poder desambiguar iniciales cuando dos
 * cuidadores comparten nombre y primer apellido (ver
 * calcularInicialesCuidadores). El "puedeDesplazarse" existe porque,
 * como vimos con Carmen tras su operación, hay cuidadores disponibles
 * pero solo para "estar con" el niño, no para recogerlo o llevarlo a
 * ningún sitio.
 */
data class Caregiver(
    val id: CaregiverId,
    val nombre: String,
    val apellido1: String,
    val apellido2: String,
    val rol: CaregiverRole,
    val puedeDesplazarse: Boolean = true
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellido1, apellido2).filter { it.isNotBlank() }.joinToString(" ")
}
