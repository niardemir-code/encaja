package com.encaja.app.ui.semana

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FormatoAnuncioTest {

    @Test
    fun `formatea fecha y hora en un formato compacto`() {
        val fecha = LocalDateTime.of(2026, 9, 21, 18, 5)
        assertEquals("21 sept, 18:05", formatearFechaAnuncio(fecha))
    }

    @Test
    fun `los minutos de un solo digito llevan cero delante`() {
        val fecha = LocalDateTime.of(2026, 1, 5, 9, 5)
        assertEquals("5 ene, 09:05", formatearFechaAnuncio(fecha))
    }
}
