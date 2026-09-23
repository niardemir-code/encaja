package com.encaja.app.data.firestore

import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.MotivoNoDisponibilidad

/** Convierte entre CategoriaDisponibilidad y el mapa de campos de Firestore. Kotlin puro. */
object CategoriaFirestoreMapper {

    fun aDocumento(categoria: CategoriaDisponibilidad): Map<String, Any?> = mapOf(
        "nombre" to categoria.nombre,
        "emoji" to categoria.emoji,
        "color" to categoria.color,
        "modo" to categoria.modo.name,
        "bloquea" to categoria.bloquea,
        "base" to categoria.base?.name
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): CategoriaDisponibilidad? {
        val nombre = datos["nombre"] as? String ?: return null
        val emoji = datos["emoji"] as? String ?: return null
        // Firestore devuelve los números enteros como Long.
        val color = (datos["color"] as? Number)?.toLong() ?: return null
        val modo = (datos["modo"] as? String)?.let { runCatching { ModoCategoria.valueOf(it) }.getOrNull() } ?: return null
        val bloquea = datos["bloquea"] as? Boolean ?: true
        val base = (datos["base"] as? String)?.let { runCatching { MotivoNoDisponibilidad.valueOf(it) }.getOrNull() }
        return CategoriaDisponibilidad(CategoriaId(id), nombre, emoji, color, modo, bloquea, base)
    }
}
