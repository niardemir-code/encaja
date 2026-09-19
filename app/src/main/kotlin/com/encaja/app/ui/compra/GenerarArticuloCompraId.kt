package com.encaja.app.ui.compra

import com.encaja.app.domain.model.ArticuloCompraId
import java.text.Normalizer

/**
 * Genera un id simple y legible a partir del nombre del artículo ("Leche
 * de avena" -> "leche-de-avena"), evitando colisiones con los ids que ya
 * existen en la lista ("pan" repetido pasaría a "pan-2", etc). Kotlin
 * puro, sin dependencias de Android — así se puede testear sin emulador.
 */
fun generarArticuloCompraIdDesdeNombre(nombre: String, existentes: List<ArticuloCompraId>): ArticuloCompraId {
    val base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "articulo" }

    val idsExistentes = existentes.map { it.value }.toSet()
    var candidato = base
    var sufijo = 2
    while (candidato in idsExistentes) {
        candidato = "$base-$sufijo"
        sufijo++
    }
    return ArticuloCompraId(candidato)
}
