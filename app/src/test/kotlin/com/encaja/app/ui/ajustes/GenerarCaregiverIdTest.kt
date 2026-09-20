package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.CaregiverId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerarCaregiverIdTest {

    @Test
    fun `genera un id en minusculas y sin acentos a partir del nombre completo`() {
        val id = generarCaregiverIdDesdeNombre("Víctor", "Oliver", "Vila", emptyList())
        assertEquals("victor-oliver-vila", id.value)
    }

    @Test
    fun `si el id ya existe, añade un sufijo numerico`() {
        val id = generarCaregiverIdDesdeNombre(
            "Víctor", "Oliver", "Vila", listOf(CaregiverId("victor-oliver-vila"))
        )
        assertEquals("victor-oliver-vila-2", id.value)
    }

    @Test
    fun `sufijo numerico sigue creciendo mientras haya colision`() {
        val id = generarCaregiverIdDesdeNombre(
            "Ana", "Ruiz", "Alba",
            listOf(CaregiverId("ana-ruiz-alba"), CaregiverId("ana-ruiz-alba-2"))
        )
        assertEquals("ana-ruiz-alba-3", id.value)
    }
}
