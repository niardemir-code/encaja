package com.encaja.app.domain.repository

import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.FamilyId

/**
 * Categorías guardadas por la familia: las propias y las personalizaciones (emoji y
 * color) de las 5 de serie. Para la lista completa que se muestra, ver CategoriasBase.combinar.
 */
interface CategoriaRepository {
    suspend fun obtenerCategorias(familyId: FamilyId): List<CategoriaDisponibilidad>
    suspend fun guardarCategoria(familyId: FamilyId, categoria: CategoriaDisponibilidad)
    suspend fun eliminarCategoria(familyId: FamilyId, categoriaId: CategoriaId)
}
