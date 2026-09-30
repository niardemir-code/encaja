package com.encaja.app.domain

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.DetectarConflictosDeAsignacion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class CategoriaDisponibilidadTest {

    private val victor = CaregiverId("victor")
    private val lunes = LocalDate.of(2026, 9, 21)
    private val gimnasio = CategoriaDisponibilidad(CategoriaId("gym"), "Gimnasio", "🏋️", 0xFFB8E3DA, ModoCategoria.HORAS, bloquea = true)
    private val teletrabajo = CategoriaDisponibilidad(CategoriaId("tele"), "Teletrabajo", "💻", 0xFFC9CFF5, ModoCategoria.HORAS, bloquea = false)

    @Test
    fun `sin nada guardado salen las 5 de serie en su orden`() {
        val lista = CategoriasBase.combinar(emptyList())
        assertEquals(MotivoNoDisponibilidad.values().toList(), lista.map { it.base })
    }

    @Test
    fun `una personalizacion de serie cambia todo menos el id y el motivo`() {
        val guardada = CategoriaDisponibilidad(
            CategoriasBase.idDe(MotivoNoDisponibilidad.TRABAJO), "Otro nombre", "🏭", 0xFF123456,
            ModoCategoria.DIAS, bloquea = false, base = MotivoNoDisponibilidad.TRABAJO
        )
        val trabajo = CategoriasBase.combinar(listOf(guardada)).first()
        assertEquals("Otro nombre", trabajo.nombre)
        assertEquals("🏭", trabajo.emoji)
        assertEquals(0xFF123456, trabajo.color)
        assertEquals(ModoCategoria.DIAS, trabajo.modo)
        assertFalse(trabajo.bloquea)
        assertEquals(MotivoNoDisponibilidad.TRABAJO, trabajo.base)
        assertEquals(CategoriasBase.idDe(MotivoNoDisponibilidad.TRABAJO), trabajo.id)
    }

    @Test
    fun `una de serie marcada como informativa deja de ocupar tambien en sus bloques antiguos`() {
        val trabajoInformativo = CategoriasBase.predeterminadas.first { it.base == MotivoNoDisponibilidad.TRABAJO }
            .copy(bloquea = false)
        val categorias = CategoriasBase.combinar(listOf(trabajoInformativo))
        val bloque = AvailabilityBlock(
            CaregiverId("v"), LocalDate.of(2026, 9, 15), LocalTime.of(8, 0), LocalTime.of(15, 0),
            MotivoNoDisponibilidad.TRABAJO
        )
        assertFalse(listOf(bloque).conBloqueoDeCategorias(categorias).single().bloquea)
    }

    @Test
    fun `las propias van detras de las de serie, por orden alfabetico`() {
        val lista = CategoriasBase.combinar(listOf(teletrabajo, gimnasio))
        assertEquals(listOf("Gimnasio", "Teletrabajo"), lista.drop(5).map { it.nombre })
    }

    @Test
    fun `un bloque se pinta con su categoria, o con la de serie si esta se borro`() {
        val todas = CategoriasBase.combinar(listOf(gimnasio))
        val bloque = AvailabilityBlock(victor, lunes, LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.OTRO, categoriaId = gimnasio.id)
        assertEquals(gimnasio, bloque.categoriaEn(todas))
        assertEquals("Otro", bloque.categoriaEn(CategoriasBase.combinar(emptyList())).nombre)
        val medico = bloque.copy(motivo = MotivoNoDisponibilidad.MEDICO, categoriaId = null)
        assertEquals("Médico", medico.categoriaEn(todas).nombre)
    }

    @Test
    fun `una categoria informativa no genera aviso de conflicto, una que bloquea si`() {
        val cuidador = Caregiver(victor, "Víctor", "Oliver", "Vila", CaregiverRole.ADMIN)
        val nina = Child(ChildId("nina"), "Nina")
        val necesidad = CoverageNeed(
            CoverageNeedId("n1"), nina.id, lunes, LocalTime.of(9, 0), LocalTime.of(10, 0), "Recoger",
            quienLlevaId = victor.value, quienRecogeId = victor.value
        )
        val tele = AvailabilityBlock(victor, lunes, LocalTime.of(8, 0), LocalTime.of(15, 0), MotivoNoDisponibilidad.OTRO, categoriaId = teletrabajo.id)
        val gym = tele.copy(categoriaId = gimnasio.id)
        val categorias = CategoriasBase.combinar(listOf(gimnasio, teletrabajo))

        val conTele = listOf(tele).conBloqueoDeCategorias(categorias)
        assertFalse(conTele.single().bloquea)
        assertTrue(DetectarConflictosDeAsignacion(listOf(nina), listOf(cuidador), conTele)(listOf(necesidad)).isEmpty())

        val conGym = listOf(gym).conBloqueoDeCategorias(categorias)
        val avisos = DetectarConflictosDeAsignacion(listOf(nina), listOf(cuidador), conGym)(listOf(necesidad))
        assertEquals(2, avisos.size) // conflicto tanto para "lleva" como para "recoge", ambos asignados a Víctor
    }
}
