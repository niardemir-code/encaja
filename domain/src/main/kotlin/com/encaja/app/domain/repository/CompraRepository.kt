package com.encaja.app.domain.repository

import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import com.encaja.app.domain.model.FamilyId

interface CompraRepository {
    suspend fun obtenerArticulos(familyId: FamilyId): List<ArticuloCompra>

    /** Crea el artículo si es nuevo, o sobrescribe el existente (p. ej. al marcarlo comprado). */
    suspend fun guardarArticulo(familyId: FamilyId, articulo: ArticuloCompra)

    suspend fun eliminarArticulo(familyId: FamilyId, articuloId: ArticuloCompraId)
}
