package com.encaja.app.data.firestore

import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.FamilyUnitId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FamilyUnitFirestoreMapperTest {

    @Test
    fun `FamilyUnit hace el viaje completo sin perder datos`() {
        val original = FamilyUnit(
            FamilyUnitId("abuelos-maternos"), "GF", "Gregorio y Josefa",
            listOf(CaregiverId("gregorio"), CaregiverId("josefa"))
        )

        val documento = FamilyUnitFirestoreMapper.aDocumento(original)
        val reconstruido = FamilyUnitFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `documento sin miembros se reconstruye con la lista vacia`() {
        val documento = mapOf("codigo" to "GF", "nombre" to "Gregorio y Josefa")
        val reconstruido = FamilyUnitFirestoreMapper.desdeDocumento("abuelos-maternos", documento)

        assertEquals(emptyList<CaregiverId>(), reconstruido?.miembros)
    }

    @Test
    fun `documento sin nombre no revienta, devuelve null`() {
        val documentoCorrupto = mapOf("codigo" to "GF")
        assertNull(FamilyUnitFirestoreMapper.desdeDocumento("x", documentoCorrupto))
    }
}
