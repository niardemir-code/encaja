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
 * Escenario de referencia: familia Oliver Izquierdo, martes 15 de septiembre. Etna
 * tiene fútbol a las 18:30. Lo que decide si queda cubierta es a quién se ha
 * asignado manualmente en la propia actividad (llevar/recoger) — el patrón semanal
 * de Familia (aquí solo usado para el reparto) es meramente orientativo.
 */
class CalcularHuecosDelDiaTest {

    private val victor = Caregiver(CaregiverId("victor"), "Víctor", "Oliver", "Vila", CaregiverRole.ADMIN)
    private val silvia = Caregiver(CaregiverId("silvia"), "Sílvia", "Izquierdo", "Camps", CaregiverRole.ADMIN)
    private val josefa = Caregiver(CaregiverId("josefa"), "Josefa", "Fernández", "Ruiz", CaregiverRole.CUIDADOR)
    private val etna = ChildId("etna")
    private val martes = LocalDate.of(2026, 9, 15)

    private lateinit var futbol: CoverageNeed

    @BeforeEach
    fun setUp() {
        futbol = CoverageNeed(
            CoverageNeedId("futbol-etna-15sep"), etna, martes,
            LocalTime.of(18, 30), LocalTime.of(20, 0), "Fútbol", requiereDesplazamiento = true
        )
    }

    @Test
    fun `con alguien asignado a llevar y a recoger no hay hueco`() {
        val cubierta = futbol.copy(quienLlevaId = silvia.id.value, quienRecogeId = silvia.id.value)
        assertTrue(CalcularHuecosDelDia()(listOf(cubierta)).isEmpty())
    }

    @Test
    fun `sin nadie asignado genera hueco por SIN_ASIGNACION`() {
        val huecos = CalcularHuecosDelDia()(listOf(futbol))

        assertEquals(1, huecos.size)
        assertEquals(MotivoHueco.SIN_ASIGNACION, huecos.first().motivo)
    }

    @Test
    fun `con solo quien recoge asignado falta quien la lleve`() {
        val need = futbol.copy(quienRecogeId = josefa.id.value)
        val huecos = CalcularHuecosDelDia()(listOf(need))

        assertEquals(1, huecos.size)
        assertEquals(MotivoHueco.FALTA_QUIEN_LLEVA, huecos.first().motivo)
    }

    @Test
    fun `con solo quien lleva asignado falta quien la recoja`() {
        val need = futbol.copy(quienLlevaId = silvia.id.value)
        val huecos = CalcularHuecosDelDia()(listOf(need))

        assertEquals(1, huecos.size)
        assertEquals(MotivoHueco.FALTA_QUIEN_RECOGE, huecos.first().motivo)
    }

    @Test
    fun `una tarea que no requiere desplazamiento no genera hueco aunque no haya nadie asignado`() {
        val estarConEtna = futbol.copy(requiereDesplazamiento = false)
        assertTrue(CalcularHuecosDelDia()(listOf(estarConEtna)).isEmpty())
    }

    @Test
    fun `una anulacion manual gana al patron recurrente`() {
        val patrones = listOf(PatronCuidado(DayOfWeek.TUESDAY, josefa.id))
        val anulacion = mapOf(martes to silvia.id)
        val asignado = resolverAsignacion(martes, patrones, anulacion)

        assertEquals(silvia.id, asignado)
    }

    @Test
    fun `proponer cuidadores excluye a quien esta ocupado y ordena por reparto`() {
        val disponibilidad = listOf(
            AvailabilityBlock(victor.id, martes, LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO),
            AvailabilityBlock(josefa.id, martes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO)
        )
        val reparto = mapOf(josefa.id to 2, silvia.id to 4)

        val propuestas = ProponerCuidadores(
            listOf(victor, silvia, josefa), disponibilidad, reparto
        )(Hueco(futbol, MotivoHueco.SIN_ASIGNACION))

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
        val reparto = CalcularRepartoSemanal(patronesSemana, emptyMap())(martes.lunesDeEstaSemana())

        assertEquals(2, reparto[victor.id])
        assertEquals(2, reparto[silvia.id])
        assertEquals(1, reparto[josefa.id])
    }

    @Test
    fun `un conflicto de disponibilidad no cambia si esta cubierta, solo genera aviso`() {
        val need = futbol.copy(quienLlevaId = silvia.id.value, quienRecogeId = josefa.id.value)
        val disponibilidad = listOf(
            AvailabilityBlock(josefa.id, martes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO)
        )

        assertTrue(CalcularHuecosDelDia()(listOf(need)).isEmpty())

        val avisos = DetectarConflictosDeAsignacion(
            listOf(Child(etna, "Etna")), listOf(victor, silvia, josefa), disponibilidad
        )(listOf(need))

        assertEquals(1, avisos.size)
        assertEquals(setOf(RolResponsable.RECOGE), avisos.first().roles)
        assertEquals(josefa.id, avisos.first().caregiver.id)
    }

    @Test
    fun `si la misma persona lleva y recoge y tiene un bloqueo, sale un solo aviso con ambos roles`() {
        val need = futbol.copy(quienLlevaId = josefa.id.value, quienRecogeId = josefa.id.value)
        val disponibilidad = listOf(
            AvailabilityBlock(josefa.id, martes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO)
        )

        val avisos = DetectarConflictosDeAsignacion(
            listOf(Child(etna, "Etna")), listOf(victor, silvia, josefa), disponibilidad
        )(listOf(need))

        assertEquals(1, avisos.size)
        assertEquals(setOf(RolResponsable.LLEVA, RolResponsable.RECOGE), avisos.first().roles)
    }
}
