package com.encaja.app.ui.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime

class FormatoHoraTest {

    @Test
    fun `rellena con ceros las horas y minutos de un solo digito`() {
        assertEquals("06:05", formatearHora(LocalTime.of(6, 5)))
    }

    @Test
    fun `usa 24 horas`() {
        assertEquals("18:30", formatearHora(LocalTime.of(18, 30)))
        assertEquals("00:00", formatearHora(LocalTime.MIDNIGHT))
    }

    @Test
    fun `ignora los segundos`() {
        assertEquals("18:30", formatearHora(LocalTime.of(18, 30, 45)))
    }
}
