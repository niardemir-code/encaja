package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.FamilyUnitId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerarFamilyUnitIdTest {

    @Test
    fun `genera un id en minusculas y sin acentos a partir del nombre`() {
        val id = generarFamilyUnitIdDesdeNombre("Gregorio y Josefa", emptyList())
        assertEquals("gregorio-y-josefa", id.value)
    }

    @Test
    fun `si el id ya existe, añade un sufijo numerico`() {
        val id = generarFamilyUnitIdDesdeNombre(
            "Gregorio y Josefa", listOf(FamilyUnitId("gregorio-y-josefa"))
        )
        assertEquals("gregorio-y-josefa-2", id.value)
    }
}
