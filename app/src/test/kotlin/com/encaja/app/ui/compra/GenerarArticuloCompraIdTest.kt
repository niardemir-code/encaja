package com.encaja.app.ui.compra

import com.encaja.app.domain.model.ArticuloCompraId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerarArticuloCompraIdTest {

    @Test
    fun `quita acentos y pasa a minusculas`() {
        assertEquals(ArticuloCompraId("cafe"), generarArticuloCompraIdDesdeNombre("Café", emptyList()))
    }

    @Test
    fun `nombre con espacios se convierte en guiones`() {
        assertEquals(
            ArticuloCompraId("leche-de-avena"),
            generarArticuloCompraIdDesdeNombre("Leche de avena", emptyList())
        )
    }

    @Test
    fun `si el id ya existe, anade un sufijo numerico`() {
        val id = generarArticuloCompraIdDesdeNombre("Pan", listOf(ArticuloCompraId("pan")))
        assertEquals(ArticuloCompraId("pan-2"), id)
    }

    @Test
    fun `si el id y el sufijo -2 ya existen, sigue probando`() {
        val id = generarArticuloCompraIdDesdeNombre(
            "Pan",
            listOf(ArticuloCompraId("pan"), ArticuloCompraId("pan-2"))
        )
        assertEquals(ArticuloCompraId("pan-3"), id)
    }

    @Test
    fun `un nombre en blanco no revienta`() {
        assertEquals(ArticuloCompraId("articulo"), generarArticuloCompraIdDesdeNombre("   ", emptyList()))
    }
}
