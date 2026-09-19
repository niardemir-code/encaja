package com.encaja.app.data.firestore

import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AnuncioFirestoreMapperTest {

    @Test
    fun `Anuncio hace el viaje completo sin perder datos`() {
        val original = Anuncio(
            AnuncioId("abc123"),
            "Víctor",
            "El sábado hay cumpleaños en el cole de Etna",
            LocalDateTime.of(2026, 9, 21, 18, 5)
        )

        val documento = AnuncioFirestoreMapper.aDocumento(original)
        val reconstruido = AnuncioFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `Anuncio sin texto en el documento no revienta, devuelve null`() {
        val datos = mapOf("autorNombre" to "Víctor", "publicadoEn" to LocalDateTime.now().toString())
        assertNull(AnuncioFirestoreMapper.desdeDocumento("abc123", datos))
    }

    @Test
    fun `Anuncio con fecha ilegible no revienta, devuelve null`() {
        val datos = mapOf("autorNombre" to "Víctor", "texto" to "Hola", "publicadoEn" to "no-es-una-fecha")
        assertNull(AnuncioFirestoreMapper.desdeDocumento("abc123", datos))
    }
}
