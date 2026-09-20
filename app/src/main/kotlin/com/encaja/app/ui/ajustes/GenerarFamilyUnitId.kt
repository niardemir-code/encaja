package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.FamilyUnitId
import java.text.Normalizer

/**
 * Genera un id legible para una unidad familiar nueva a partir de su
 * nombre (p.ej. "Gregorio y Josefa" -> "gregorio-y-josefa"), evitando
 * colisiones con los ids ya existentes igual que generarCaregiverIdDesdeNombre.
 */
fun generarFamilyUnitIdDesdeNombre(nombre: String, existentes: List<FamilyUnitId>): FamilyUnitId {
    val base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "unidad" }

    val idsExistentes = existentes.map { it.value }.toSet()
    var candidato = base
    var sufijo = 2
    while (candidato in idsExistentes) {
        candidato = "$base-$sufijo"
        sufijo++
    }
    return FamilyUnitId(candidato)
}
