package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.ChildId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerarChildIdTest {

    @Test
    fun `quita acentos y pasa a minusculas`() {
        assertEquals(ChildId("julia"), generarChildIdDesdeNombre("Júlia", emptyList()))
    }

    @Test
    fun `nombre con espacios se convierte en guiones`() {
        assertEquals(ChildId("joan-marc"), generarChildIdDesdeNombre("Joan Marc", emptyList()))
    }

    @Test
    fun `si el id ya existe, anade un sufijo numerico`() {
        val id = generarChildIdDesdeNombre("Etna", listOf(ChildId("etna")))
        assertEquals(ChildId("etna-2"), id)
    }

    @Test
    fun `si el id y el sufijo -2 ya existen, sigue probando`() {
        val id = generarChildIdDesdeNombre("Etna", listOf(ChildId("etna"), ChildId("etna-2")))
        assertEquals(ChildId("etna-3"), id)
    }

    @Test
    fun `un nombre en blanco no revienta`() {
        assertEquals(ChildId("nino"), generarChildIdDesdeNombre("   ", emptyList()))
    }
}
