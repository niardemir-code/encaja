package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Escenario de referencia: familia Oliver Izquierdo, martes 15 de
 * septiembre. Víctor Oliver de turno de tarde, Josefa Fernández (abuela
 * materna) asignada ese día según el patrón, Etna con fútbol a las 18:30.
 */
class CalcularHuecosDelDiaTest {

    private val victor = Caregiver(CaregiverId("victor"), "Víctor Oliver", CaregiverRole.ADMIN)
    private val silvia = Caregiver(CaregiverId("silvia"), "Sílvia Izquierdo", CaregiverRole.ADMIN)
    private val josefa = Caregiver(CaregiverId("josefa"), "Josefa Fernández", CaregiverRole.CUIDADOR)
    private val etna = ChildId("etna")
    private val martes = LocalDate.of(2026, 9, 15)

    private lateinit var futbol: CoverageNeed
    private lateinit var disponibilidadBase: List<AvailabilityBlock>
    private lateinit var patrones: List<PatronCuidado>

    @BeforeEach
    fun setUp() {
        futbol = CoverageNeed(
            CoverageNeedId("futbol-etna-15sep"), etna, martes,
            LocalTime.of(18, 30), LocalTime.of(20, 0), "Fútbol", requiereDesplazamiento = true
        )
        disponibilidadBase = listOf(
            AvailabilityBlock(victor.id, martes, LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO)
        )
        patrones = listOf(PatronCuidado(DayOfWeek.TUESDAY, josefa.id))
    }

    @Test
    fun `cuidador asignado y disponible no genera hueco`() {
        val huecos = CalcularHuecosDelDia(patrones, emptyList(), disponibilidadBase)(listOf(futbol))
        assertTrue(huecos.isEmpty())
    }

    @Test
    fun `cuidador asignado pero ocupado genera hueco por ASIGNADO_NO_DISPONIBLE`() {
        val disponibilidad = disponibilidadBase + AvailabilityBlock(
            josefa.id, martes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO
        )
        val huecos = CalcularHuecosDelDia(patrones, emptyList(), disponibilidad)(listOf(futbol))

        assertEquals(1, huecos.size)
        assertEquals(MotivoHueco.ASIGNADO_NO_DISPONIBLE, huecos.first().motivo)
    }

    @Test
    fun `sin patron ni anulacion genera hueco por SIN_ASIGNACION`() {
        val huecos = CalcularHuecosDelDia(emptyList(), emptyList(), disponibilidadBase)(listOf(futbol))

        assertEquals(1, huecos.size)
        assertEquals(MotivoHueco.SIN_ASIGNACION, huecos.first().motivo)
    }

    @Test
    fun `una anulacion manual gana al patron recurrente`() {
        val anulacion = listOf(AnulacionAsignacion(martes, silvia.id))
        val asignado = resolverAsignacion(martes, patrones, anulacion)

        assertEquals(silvia.id, asignado)
    }

    @Test
    fun `proponer cuidadores excluye a quien esta ocupado y ordena por reparto`() {
        val disponibilidad = disponibilidadBase + AvailabilityBlock(
            josefa.id, martes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO
        )
        val reparto = mapOf(josefa.id to 2, silvia.id to 4)

        val propuestas = ProponerCuidadores(
            listOf(victor, silvia, josefa), disponibilidad, reparto
        )(Hueco(futbol, MotivoHueco.ASIGNADO_NO_DISPONIBLE))

        assertEquals(listOf(silvia.id), propuestas.map { it.id })
    }

    @Test
    fun `reparto semanal cuenta los tramos de cada cuidador`() {
        val patronesSemana = listOf(
            PatronCuidado(DayOfWeek.MONDAY, victor.id),
            PatronCuidado(DayOfWeek.TUESDAY, josefa.id),
            PatronCuidado(DayOfWeek.WEDNESDAY, silvia.id),
            PatronCuidado(DayOfWeek.THURSDAY, silvia.id),
            PatronCuidado(DayOfWeek.FRIDAY, victor.id)
        )
        val reparto = CalcularRepartoSemanal(patronesSemana, emptyList())(martes.lunesDeEstaSemana())

        assertEquals(2, reparto[victor.id])
        assertEquals(2, reparto[silvia.id])
        assertEquals(1, reparto[josefa.id])
    }
}
