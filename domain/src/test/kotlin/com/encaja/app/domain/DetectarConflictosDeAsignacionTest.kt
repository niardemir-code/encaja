package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.DetectarConflictosDeAsignacion
import com.encaja.app.domain.usecase.RolResponsable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class DetectarConflictosDeAsignacionTest {

    private val dia = LocalDate.of(2026, 10, 7)
    private val etna = Child(ChildId("etna"), "Etna")
    private val ana = Caregiver(CaregiverId("ana"), "Ana", "", "", CaregiverRole.ADMIN)
    private val luis = Caregiver(CaregiverId("luis"), "Luis", "", "", CaregiverRole.CUIDADOR)

    private fun need(inicio: Int, fin: Int, lleva: String? = null, recoge: String? = null, nino: String = "etna") =
        CoverageNeed(
            CoverageNeedId("n-$inicio"), ChildId(nino), dia,
            LocalTime.of(inicio, 0), LocalTime.of(fin, 0), "Actividad",
            quienLlevaId = lleva, quienRecogeId = recoge
        )

    private fun bloque(quien: Caregiver, inicio: Int, fin: Int, bloquea: Boolean = true, fecha: LocalDate = dia) =
        AvailabilityBlock(
            quien.id, fecha, LocalTime.of(inicio, 0), LocalTime.of(fin, 0),
            MotivoNoDisponibilidad.TRABAJO, bloquea = bloquea
        )

    private fun detector(vararg bloques: AvailabilityBlock) =
        DetectarConflictosDeAsignacion(listOf(etna), listOf(ana, luis), bloques.toList())

    @Test
    fun `sin bloqueos no hay avisos`() {
        assertTrue(detector()(listOf(need(17, 18, lleva = "ana"))).isEmpty())
    }

    @Test
    fun `el asignado con un bloqueo a esa hora genera un aviso`() {
        val avisos = detector(bloque(ana, 16, 19))(listOf(need(17, 18, lleva = "ana")))
        assertEquals(1, avisos.size)
        assertEquals(ana, avisos[0].caregiver)
        assertEquals(setOf(RolResponsable.LLEVA), avisos[0].roles)
    }

    @Test
    fun `llevar y recoger la misma persona es un unico aviso con ambos roles`() {
        val avisos = detector(bloque(ana, 16, 19))(listOf(need(17, 18, lleva = "ana", recoge = "ana")))
        assertEquals(1, avisos.size)
        assertEquals(setOf(RolResponsable.LLEVA, RolResponsable.RECOGE), avisos[0].roles)
    }

    @Test
    fun `dos personas distintas con bloqueo generan dos avisos`() {
        val avisos = detector(bloque(ana, 16, 19), bloque(luis, 16, 19))(
            listOf(need(17, 18, lleva = "ana", recoge = "luis"))
        )
        assertEquals(2, avisos.size)
    }

    @Test
    fun `un bloqueo de otra persona no cuenta`() {
        assertTrue(detector(bloque(luis, 16, 19))(listOf(need(17, 18, lleva = "ana"))).isEmpty())
    }

    @Test
    fun `un bloqueo que solo toca el borde no solapa`() {
        // El bloqueo acaba justo cuando empieza la actividad.
        assertTrue(detector(bloque(ana, 15, 17))(listOf(need(17, 18, lleva = "ana"))).isEmpty())
    }

    @Test
    fun `un bloque que no bloquea se ignora`() {
        assertTrue(detector(bloque(ana, 16, 19, bloquea = false))(listOf(need(17, 18, lleva = "ana"))).isEmpty())
    }

    @Test
    fun `un bloqueo de otro dia no cuenta`() {
        val ayer = dia.minusDays(1)
        assertTrue(detector(bloque(ana, 16, 19, fecha = ayer))(listOf(need(17, 18, lleva = "ana"))).isEmpty())
    }

    @Test
    fun `un turno de noche que cruza la medianoche afecta a primera hora del dia siguiente`() {
        val turnoNoche = AvailabilityBlock(
            ana.id, dia.minusDays(1), LocalTime.of(22, 0), LocalTime.of(6, 0), MotivoNoDisponibilidad.TRABAJO
        )
        val avisos = detector(turnoNoche)(listOf(need(5, 6, lleva = "ana")))
        assertEquals(1, avisos.size)
    }

    @Test
    fun `un id asignado que no es un cuidador conocido no se comprueba`() {
        // Por ejemplo, una unidad familiar: los bloqueos son por cuidador.
        assertTrue(detector(bloque(ana, 16, 19))(listOf(need(17, 18, lleva = "u1"))).isEmpty())
    }

    @Test
    fun `una actividad de un nino desconocido se ignora`() {
        assertTrue(detector(bloque(ana, 16, 19))(listOf(need(17, 18, lleva = "ana", nino = "otro"))).isEmpty())
    }

    @Test
    fun `una actividad sin asignar no genera avisos`() {
        assertTrue(detector(bloque(ana, 16, 19))(listOf(need(17, 18))).isEmpty())
    }
}
