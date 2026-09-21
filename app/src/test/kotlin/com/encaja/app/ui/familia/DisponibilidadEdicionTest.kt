package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DisponibilidadEdicionTest {

    private val victor = CaregiverId("victor")
    private val lunes = LocalDate.of(2026, 9, 21)

    @Test
    fun `la casilla muestra las horas en dos lineas`() {
        val bloque = AvailabilityBlock(victor, lunes, LocalTime.of(6, 0), LocalTime.of(14, 0), MotivoNoDisponibilidad.TRABAJO)
        assertEquals("06:00\n14:00", textoCelda(listOf(bloque)))
    }

    @Test
    fun `un bloque de dia completo muestra el tipo, y varios bloques añaden el contador`() {
        val viaje = AvailabilityBlock(victor, lunes, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, MotivoNoDisponibilidad.VIAJE)
        val otro = AvailabilityBlock(victor, lunes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.OTRO)
        assertEquals("Viaje +1", textoCelda(listOf(otro, viaje)))
        assertNull(textoCelda(emptyList()))
    }

    @Test
    fun `los dias marcados se convierten en las fechas de esa semana`() {
        val fechas = fechasDeLaSemana(lunes, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(listOf(lunes, lunes.plusDays(2)), fechas)
    }

    @Test
    fun `duplicar a la semana siguiente añade las mismas fechas una semana despues`() {
        val fechas = fechasTrabajo(listOf(lunes, lunes.plusDays(1)), duplicarSemanaSiguiente = true)
        assertEquals(listOf(lunes, lunes.plusDays(1), lunes.plusDays(7), lunes.plusDays(8)), fechas)
    }

    @Test
    fun `un rango de fechas incluye ambos extremos y acepta el orden invertido`() {
        assertEquals(3, fechasEntre(lunes, lunes.plusDays(2)).size)
        assertEquals(fechasEntre(lunes, lunes.plusDays(2)), fechasEntre(lunes.plusDays(2), lunes))
        assertEquals(listOf(lunes), fechasEntre(lunes, lunes))
    }

    @Test
    fun `la conversion a milisegundos del selector de fecha es reversible`() {
        assertEquals(lunes, millisUtcAFecha(fechaAMillisUtc(lunes)))
    }

    @Test
    fun `se reconoce el turno guardado que coincide con las horas elegidas`() {
        val manana = TurnoTrabajo(TurnoId("m"), "Mañana", LocalTime.of(6, 0), LocalTime.of(14, 0))
        val oficina = TurnoTrabajo(TurnoId("o"), "Oficina", LocalTime.of(7, 0), LocalTime.of(15, 0))
        assertEquals(oficina, turnoConHoras(listOf(manana, oficina), LocalTime.of(7, 0), LocalTime.of(15, 0)))
        assertNull(turnoConHoras(listOf(manana, oficina), LocalTime.of(7, 0), LocalTime.of(16, 0)))
    }
}
