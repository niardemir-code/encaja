package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.actividadesAntiguas
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class LimpiarActividadesAntiguasTest {

    private val etna = ChildId("etna")
    private val hoy = LocalDate.of(2026, 9, 30)

    private fun need(id: String, fecha: LocalDate) = CoverageNeed(
        CoverageNeedId(id), etna, fecha, LocalTime.of(9, 0), LocalTime.of(10, 0), "Actividad"
    )

    @Test
    fun `una actividad de hace mas de un mes se marca para borrar`() {
        val antigua = need("antigua", hoy.minusMonths(1).minusDays(1))
        assertEquals(listOf(antigua.id), actividadesAntiguas(listOf(antigua), hoy))
    }

    @Test
    fun `una actividad de exactamente un mes atras todavia no se borra`() {
        val limite = need("limite", hoy.minusMonths(1))
        assertTrue(actividadesAntiguas(listOf(limite), hoy).isEmpty())
    }

    @Test
    fun `una actividad reciente o futura no se toca`() {
        val reciente = need("reciente", hoy.minusDays(3))
        val futura = need("futura", hoy.plusDays(3))
        assertTrue(actividadesAntiguas(listOf(reciente, futura), hoy).isEmpty())
    }

    @Test
    fun `varias antiguas se devuelven todas`() {
        val a = need("a", hoy.minusMonths(2))
        val b = need("b", hoy.minusMonths(3))
        val vigente = need("vigente", hoy)
        assertEquals(setOf(a.id, b.id), actividadesAntiguas(listOf(a, b, vigente), hoy).toSet())
    }
}
