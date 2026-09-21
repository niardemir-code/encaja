package com.encaja.app.domain

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class AvailabilityBlockTest {

    private val victor = CaregiverId("victor")
    private val lunes = LocalDate.of(2026, 9, 21)
    private fun h(hora: Int, min: Int = 0) = LocalTime.of(hora, min)

    private val manana = AvailabilityBlock(victor, lunes, h(6), h(14), MotivoNoDisponibilidad.TRABAJO)
    private val noche = AvailabilityBlock(victor, lunes, h(22), h(6), MotivoNoDisponibilidad.TRABAJO)

    @Test
    fun `un turno de mañana ocupa las 10 y deja libres las 17`() {
        assertTrue(manana.ocupa(lunes, h(10), h(11)))
        assertFalse(manana.ocupa(lunes, h(17), h(18)))
    }

    @Test
    fun `un turno de noche ocupa la noche de su dia y la madrugada del siguiente`() {
        assertTrue(noche.cruzaMedianoche)
        assertTrue(noche.ocupa(lunes, h(23), h(23, 30)))
        assertTrue(noche.ocupa(lunes.plusDays(1), h(5), h(5, 30)))
    }

    @Test
    fun `un turno de noche no ocupa la tarde de su dia ni la tarde del siguiente`() {
        assertFalse(noche.ocupa(lunes, h(17), h(18)))
        assertFalse(noche.ocupa(lunes.plusDays(1), h(17), h(18)))
    }

    @Test
    fun `un bloque de todo el dia se reconoce y ocupa cualquier hora de ese dia`() {
        val medico = AvailabilityBlock(
            victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, MotivoNoDisponibilidad.MEDICO
        )
        assertTrue(medico.todoElDia)
        assertFalse(medico.cruzaMedianoche)
        assertTrue(medico.ocupa(lunes, h(17), h(18)))
        assertFalse(medico.ocupa(lunes.plusDays(1), h(1), h(2)))
    }
}
