package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.ChildId
import java.text.Normalizer

/**
 * Genera un id simple y legible a partir del nombre escrito ("Júlia" ->
 * "julia"), evitando colisiones con los ids que ya existen en la familia
 * ("etna" repetido pasaría a "etna-2", etc). Kotlin puro, sin dependencias
 * de Android — así se puede testear sin emulador.
 */
fun generarChildIdDesdeNombre(nombre: String, existentes: List<ChildId>): ChildId {
    val base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "nino" }

    val idsExistentes = existentes.map { it.value }.toSet()
    var candidato = base
    var sufijo = 2
    while (candidato in idsExistentes) {
        candidato = "$base-$sufijo"
        sufijo++
    }
    return ChildId(candidato)
}
