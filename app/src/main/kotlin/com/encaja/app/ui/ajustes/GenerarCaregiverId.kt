package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.CaregiverId
import java.text.Normalizer

/**
 * Genera un id legible para un cuidador nuevo a partir de su nombre
 * completo (p.ej. "Víctor Oliver Vila" -> "victor-oliver-vila"),
 * evitando colisiones con los ids ya existentes añadiendo un sufijo
 * numérico ("-2", "-3"...) si hiciera falta. Kotlin puro, sin
 * dependencias de Android, para poder testearlo aislado.
 */
fun generarCaregiverIdDesdeNombre(
    nombre: String,
    apellido1: String,
    apellido2: String,
    existentes: List<CaregiverId>
): CaregiverId {
    val base = Normalizer.normalize(listOf(nombre, apellido1, apellido2).joinToString(" "), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "cuidador" }

    val idsExistentes = existentes.map { it.value }.toSet()
    var candidato = base
    var sufijo = 2
    while (candidato in idsExistentes) {
        candidato = "$base-$sufijo"
        sufijo++
    }
    return CaregiverId(candidato)
}
