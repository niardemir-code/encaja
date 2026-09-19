package com.encaja.app.data.firestore

import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId

object ArticuloCompraFirestoreMapper {

    fun aDocumento(articulo: ArticuloCompra): Map<String, Any?> = mapOf(
        "nombre" to articulo.nombre,
        "tienda" to articulo.tienda,
        "comprado" to articulo.comprado
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): ArticuloCompra? {
        val nombre = datos["nombre"] as? String ?: return null
        val tienda = datos["tienda"] as? String ?: return null
        val comprado = datos["comprado"] as? Boolean ?: false
        return ArticuloCompra(ArticuloCompraId(id), nombre, tienda, comprado)
    }
}
