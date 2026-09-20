package com.encaja.app.data.firestore

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId

/**
 * Convierte entre FamilyUnit y el mapa de campos de Firestore. Kotlin puro,
 * igual que CaregiverFirestoreMapper, para poder testearlo sin conexión.
 */
object FamilyUnitFirestoreMapper {

    fun aDocumento(unidad: FamilyUnit): Map<String, Any?> = mapOf(
        "codigo" to unidad.codigo,
        "nombre" to unidad.nombre,
        "miembros" to unidad.miembros.map { it.value }
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): FamilyUnit? {
        val codigo = datos["codigo"] as? String ?: return null
        val nombre = datos["nombre"] as? String ?: return null
        @Suppress("UNCHECKED_CAST")
        val miembros = (datos["miembros"] as? List<String>) ?: emptyList()

        return FamilyUnit(FamilyUnitId(id), codigo, nombre, miembros.map { CaregiverId(it) })
    }
}
