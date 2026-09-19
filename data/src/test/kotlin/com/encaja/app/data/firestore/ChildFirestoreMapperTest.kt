package com.encaja.app.data.firestore

import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.ChildId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChildFirestoreMapperTest {

    @Test
    fun `Child hace el viaje completo sin perder datos`() {
        val original = Child(ChildId("julia"), "Júlia")

        val documento = ChildFirestoreMapper.aDocumento(original)
        val reconstruido = ChildFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `Child sin nombre en el documento no revienta, devuelve null`() {
        assertNull(ChildFirestoreMapper.desdeDocumento("julia", emptyMap()))
    }
}
