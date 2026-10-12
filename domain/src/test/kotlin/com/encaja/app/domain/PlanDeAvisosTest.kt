package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.planDeAvisos
import com.encaja.app.domain.usecase.sellarCreadorDe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class PlanDeAvisosTest {

    private val ana = CaregiverId("ana")
    private val luis = CaregiverId("luis")
    private val marta = CaregiverId("marta")

    private fun need(
        id: String = "n1",
        lleva: String? = null,
        recoge: String? = null,
        creador: String? = "ana"
    ) = CoverageNeed(
        id = CoverageNeedId(id),
        childId = ChildId("etna"),
        fecha = LocalDate.of(2026, 10, 7),
        horaInicio = LocalTime.of(17, 0),
        horaFin = LocalTime.of(18, 0),
        descripcion = "Natación",
        quienLlevaId = lleva,
        quienRecogeId = recoge,
        creadoPorId = creador
    )

    private val pareja = FamilyUnit(FamilyUnitId("u1"), "U1", "Luis y Marta", listOf(luis, marta))

    @Test
    fun `sin saber quien soy se programan todos los avisos`() {
        val plan = planDeAvisos(listOf(need(creador = "ana")), yo = null, unidades = emptyList())
        assertTrue(plan.single().llevar)
        assertTrue(plan.single().recoger)
    }

    @Test
    fun `el creador programa los dos avisos aunque no lleve ni recoja`() {
        val plan = planDeAvisos(listOf(need(lleva = "luis", recoge = "marta")), ana, emptyList())
        assertTrue(plan.single().llevar)
        assertTrue(plan.single().recoger)
    }

    @Test
    fun `quien solo lleva programa solo el aviso de llevar`() {
        val plan = planDeAvisos(listOf(need(lleva = "luis")), luis, emptyList())
        assertTrue(plan.single().llevar)
        assertFalse(plan.single().recoger)
    }

    @Test
    fun `un miembro de la unidad asignada recibe el aviso de recoger`() {
        val plan = planDeAvisos(listOf(need(recoge = "u1")), marta, listOf(pareja))
        assertFalse(plan.single().llevar)
        assertTrue(plan.single().recoger)
    }

    @Test
    fun `quien no tiene nada que ver no programa ningun aviso`() {
        val plan = planDeAvisos(listOf(need(lleva = "luis", recoge = "marta")), CaregiverId("otro"), emptyList())
        assertFalse(plan.single().llevar)
        assertFalse(plan.single().recoger)
    }

    @Test
    fun `el plan conserva el orden y una entrada por actividad`() {
        val plan = planDeAvisos(listOf(need(id = "a"), need(id = "b"), need(id = "c")), ana, emptyList())
        assertEquals(listOf("a", "b", "c"), plan.map { it.need.id.value })
    }

    @Test
    fun `sellar el creador solo toca las actividades sin creador`() {
        val sellada = sellarCreadorDe(listOf(need(id = "a", creador = null), need(id = "b", creador = "luis")), ana)
        assertEquals("ana", sellada[0].creadoPorId)
        assertEquals("luis", sellada[1].creadoPorId)
    }

    @Test
    fun `sin saber quien soy no se sella nada`() {
        val originales = listOf(need(creador = null))
        assertSame(originales, sellarCreadorDe(originales, null))
    }

    @Test
    fun `si todas ya tienen creador se devuelve la misma lista`() {
        val originales = listOf(need(creador = "luis"))
        assertSame(originales, sellarCreadorDe(originales, ana))
    }
}
