package com.encaja.app.ui.guia

import com.encaja.app.domain.model.ChildId
import com.encaja.app.domain.model.CoverageNeedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ActividadEdicionTest {

    private val julia = ChildId("julia")
    private val lunes = LocalDate.of(2026, 9, 21) // lunes

    @Test
    fun `una actividad puntual es solo esa fecha`() {
        val fechas = fechasRepetidas(lunes, lunes, setOf(lunes.dayOfWeek))
        assertEquals(listOf(lunes), fechas)
    }

    @Test
    fun `la repeticion incluye solo los dias marcados dentro del rango`() {
        val fechas = fechasRepetidas(lunes, lunes.plusWeeks(2), setOf(DayOfWeek.TUESDAY))
        // martes de esta semana y de la siguiente (el rango de 2 semanas termina el propio lunes): 2 fechas
        assertEquals(2, fechas.size)
        assertTrue(fechas.all { it.dayOfWeek == DayOfWeek.TUESDAY })
        assertEquals(lunes.plusDays(1), fechas.first())
    }

    @Test
    fun `si el rango es anterior a la fecha de inicio no hay fechas`() {
        assertEquals(emptyList<LocalDate>(), fechasRepetidas(lunes, lunes.minusDays(1), setOf(lunes.dayOfWeek)))
    }

    @Test
    fun `crearActividades genera una por fecha con un id distinto cada vez`() {
        var contador = 0
        val fechas = listOf(lunes, lunes.plusDays(2))
        val actividades = crearActividades(
            fechas, julia, LocalTime.of(17, 0), LocalTime.of(18, 0), "  Fútbol  ", requiereDesplazamiento = true
        ) { CoverageNeedId("n${contador++}") }

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
            listOf(lunes), julia, LocalTime.of(9, 0), LocalTime.of(10, 0), "Médico", requiereDesplazamiento = false
        ) { id }
        assertEquals(1, actividades.size)
        assertEquals(id, actividades.first().id)
        assertFalse(actividades.first().requiereDesplazamiento)
    }

    @Test
    fun `una actividad puntual no lleva grupo de repeticion`() {
        val actividades = crearActividades(
            listOf(lunes), julia, LocalTime.of(9, 0), LocalTime.of(10, 0), "Médico", requiereDesplazamiento = false
        ) { CoverageNeedId("n0") }
        assertNull(actividades.first().grupoRepeticionId)
    }

    @Test
    fun `crear con repeticion asigna el mismo grupo a todas las ocurrencias`() {
        var contador = 0
        val fechas = listOf(lunes, lunes.plusDays(7), lunes.plusDays(14))
        val actividades = crearActividades(
            fechas, julia, LocalTime.of(17, 0), LocalTime.of(18, 0), "Fútbol", requiereDesplazamiento = true
        ) { CoverageNeedId("n${contador++}") }

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

    private val martes = lunes.plusDays(1)
    private val miercoles = lunes.plusDays(2)
    private val jueves = lunes.plusDays(3)
    private val viernes = lunes.plusDays(4)

    private fun plantillaGrupo(fecha: LocalDate, id: String) = com.encaja.app.domain.model.CoverageNeed(
        CoverageNeedId(id), julia, fecha, LocalTime.of(17, 0), LocalTime.of(18, 0), "Fútbol",
        requiereDesplazamiento = true, grupoRepeticionId = "grupo-1"
    )

    @Test
    fun `si el patron no cambia diferenciaSerie no crea ni borra nada`() {
        val existentes = listOf(plantillaGrupo(lunes, "n-lunes"), plantillaGrupo(martes, "n-martes"))
        val (aGuardar, aBorrar) = diferenciaSerie(
            plantilla = plantillaGrupo(lunes, "n-lunes"),
            existentesDesdeSuFecha = existentes,
            nuevasFechas = listOf(lunes, martes)
        ) { CoverageNeedId("nuevo") }

        assertEquals(setOf("n-lunes", "n-martes"), aGuardar.map { it.id.value }.toSet())
        assertTrue(aBorrar.isEmpty())
    }

    @Test
    fun `cambiar el patron crea lo nuevo, conserva lo que coincide y borra lo que sobra`() {
        // Serie lunes, martes, jueves -> nuevo patrón martes, miércoles, viernes:
        // martes se conserva con su mismo id, miércoles y viernes se crean, lunes y
        // jueves se borran (a partir de la fecha de la plantilla, que es el lunes).
        val existentes = listOf(
            plantillaGrupo(lunes, "n-lunes"),
            plantillaGrupo(martes, "n-martes"),
            plantillaGrupo(jueves, "n-jueves")
        )
        var contador = 0
        val (aGuardar, aBorrar) = diferenciaSerie(
            plantilla = plantillaGrupo(lunes, "n-lunes"),
            existentesDesdeSuFecha = existentes,
            nuevasFechas = listOf(martes, miercoles, viernes)
        ) { CoverageNeedId("nuevo-${contador++}") }

        assertEquals(3, aGuardar.size)
        assertEquals(CoverageNeedId("n-martes"), aGuardar.first { it.fecha == martes }.id)
        assertTrue(aGuardar.first { it.fecha == miercoles }.id != CoverageNeedId("n-martes"))
        assertTrue(aGuardar.none { it.id == CoverageNeedId("n-lunes") || it.id == CoverageNeedId("n-jueves") })

        assertEquals(setOf("n-lunes", "n-jueves"), aBorrar.map { it.id.value }.toSet())
    }

    @Test
    fun `quitar un dia del patron lo deja solo en la lista de borrar`() {
        val existentes = listOf(plantillaGrupo(lunes, "n-lunes"), plantillaGrupo(martes, "n-martes"))
        val (aGuardar, aBorrar) = diferenciaSerie(
            plantilla = plantillaGrupo(lunes, "n-lunes"),
            existentesDesdeSuFecha = existentes,
            nuevasFechas = listOf(martes)
        ) { CoverageNeedId("nuevo") }

        assertEquals(listOf(CoverageNeedId("n-martes")), aGuardar.map { it.id })
        assertEquals(listOf(CoverageNeedId("n-lunes")), aBorrar.map { it.id })
    }
}
