package com.encaja.app.data.firestore

import com.encaja.app.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class CategoriaFirestoreMapperTest {

    @Test
    fun `una categoria propia hace el viaje completo sin perder datos`() {
        val original = CategoriaDisponibilidad(CategoriaId("gym"), "Gimnasio", "🏋️", 0xFFB8E3DA, ModoCategoria.HORAS, bloquea = false)
        assertEquals(original, CategoriaFirestoreMapper.desdeDocumento("gym", CategoriaFirestoreMapper.aDocumento(original)))
    }

    @Test
    fun `la personalizacion de una categoria de serie conserva su base`() {
        val original = CategoriasBase.predeterminadas.first().copy(emoji = "🏭", color = 0xFF123456)
        val reconstruida = CategoriaFirestoreMapper.desdeDocumento(original.id.value, CategoriaFirestoreMapper.aDocumento(original))
        assertEquals(original, reconstruida)
        assertEquals(MotivoNoDisponibilidad.TRABAJO, reconstruida?.base)
    }

    @Test
    fun `el color llega bien aunque Firestore lo devuelva como otro tipo numerico`() {
        val documento = CategoriaFirestoreMapper.aDocumento(CategoriasBase.predeterminadas.last()) + ("color" to 4292927712.0)
        assertEquals(4292927712L, CategoriaFirestoreMapper.desdeDocumento("x", documento)?.color)
    }

    @Test
    fun `documento con modo desconocido no revienta, devuelve null`() {
        val corrupto = mapOf("nombre" to "X", "emoji" to "📌", "color" to 1L, "modo" to "SEMANAS")
        assertNull(CategoriaFirestoreMapper.desdeDocumento("x", corrupto))
    }

    @Test
    fun `un bloque de una categoria propia conserva su categoriaId`() {
        val original = AvailabilityBlock(
            CaregiverId("victor"), LocalDate.of(2026, 9, 21), LocalTime.of(18, 0), LocalTime.of(19, 0),
            MotivoNoDisponibilidad.OTRO, categoriaId = CategoriaId("gym")
        )
        assertEquals(original, AvailabilityBlockFirestoreMapper.desdeDocumento(AvailabilityBlockFirestoreMapper.aDocumento(original)))
    }
}
