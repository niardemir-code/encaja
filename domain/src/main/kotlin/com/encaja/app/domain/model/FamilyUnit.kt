package com.encaja.app.domain.model

data class FamilyUnitId(val value: String)

/**
 * Un grupo con nombre formado por varios cuidadores que casi siempre van
 * juntos (p.ej. los abuelos maternos). Se puede asignar a un día igual que
 * un cuidador individual — en ese caso, todos sus [miembros] quedan
 * "con las niñas" ese día.
 *
 * [codigo] es lo que se ve en el avatar del día (unas pocas letras, p.ej.
 * "GF" para Gregorio y Josefa): se pinta con un estilo distinto al de una
 * persona individual (letra en negrita sobre un fondo rayado) para que se
 * distingan a simple vista en la fila de la semana.
 */
data class FamilyUnit(
    val id: FamilyUnitId,
    val codigo: String,
    val nombre: String,
    val miembros: List<CaregiverId>
)
