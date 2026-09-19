package com.encaja.app.data.firestore

import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId

object ChildFirestoreMapper {

    fun aDocumento(child: Child): Map<String, Any?> = mapOf(
        "nombre" to child.nombre
    )

    fun desdeDocumento(id: String, datos: Map<String, Any?>): Child? {
        val nombre = datos["nombre"] as? String ?: return null
        return Child(ChildId(id), nombre)
    }
}
