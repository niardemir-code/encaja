package com.encaja.app.ui.guia

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class ActividadEdicionTest {

    private val julia = ChildId("julia")
    private val lunes = LocalDate.of(2026, 9, 21) // lunes

    @Test
    fun `crearActividades genera una por fecha con un id distinto cada vez`() {
        var contador = 0
        val fechas = listOf(lunes, lunes.plusDays(2))
        val actividades = crearActividades(
            fechas, julia, LocalTime.of(17, 0), LocalTime.of(18, 0), "  Fútbol  ", requiereDesplazamiento = true,
            generarId = { CoverageNeedId("n${contador++}") }
        )

        assertEquals(2, actividades.size)
        assertEquals("Fútbol", actividades[0].descripcion)
        assertEquals(CoverageNeedId("n0"), actividades[0].id)
        assertEquals(CoverageNeedId("n1"), actividades[1].id)
        assertEquals(lunes, actividades[0].fecha)
        assertEquals(lunes.plusDays(2), actividades[1].fecha)
    }

    @Test
    fun `al editar una sola actividad se reutiliza el mismo id`() {
        val id = CoverageNeedId("existente")
        val actividades = crearActividades(
            listOf(lunes), julia, LocalTime.of(9, 0), LocalTime.of(10, 0), "Médico", requiereDesplazamiento = false,
            generarId = { id }
        )
        assertEquals(1, actividades.size)
        assertEquals(id, actividades.first().id)
        assertFalse(actividades.first().requiereDesplazamiento)
    }

    @Test
    fun `una actividad puntual no lleva grupo de repeticion`() {
        val actividades = crearActividades(
            listOf(lunes), julia, LocalTime.of(9, 0), LocalTime.of(10, 0), "Médico", requiereDesplazamiento = false,
            generarId = { CoverageNeedId("n0") }
        )
        assertNull(actividades.first().grupoRepeticionId)
    }

    @Test
    fun `crear con repeticion asigna el mismo grupo a todas las ocurrencias`() {
        var contador = 0
        val fechas = listOf(lunes, lunes.plusDays(7), lunes.plusDays(14))
        val actividades = crearActividades(
            fechas, julia, LocalTime.of(17, 0), LocalTime.of(18, 0), "Fútbol", requiereDesplazamiento = true,
            generarId = { CoverageNeedId("n${contador++}") }
        )

        val grupos = actividades.mapNotNull { it.grupoRepeticionId }.distinct()
        assertEquals(1, grupos.size)
    }

    @Test
    fun `al editar una ocurrencia de un grupo se puede conservar su grupoRepeticionId`() {
        val id = CoverageNeedId("existente")
        val actividades = crearActividades(
            listOf(lunes), julia, LocalTime.of(9, 0), LocalTime.of(10, 0), "Médico",
            requiereDesplazamiento = false, generarId = { id }, grupoRepeticionId = "grupo-1"
        )
        assertEquals("grupo-1", actividades.first().grupoRepeticionId)
    }
}
