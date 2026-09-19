package com.encaja.app.data.firestore

import com.encaja.app.domain.model.ComidaDelDia
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ComidaDelDiaFirestoreMapperTest {

    @Test
    fun `ComidaDelDia hace el viaje completo sin perder datos`() {
        val fecha = LocalDate.of(2026, 9, 21)
        val original = ComidaDelDia(fecha, comida = "Lentejas", cena = "Tortilla")

        val documento = ComidaDelDiaFirestoreMapper.aDocumento(original)
        val reconstruido = ComidaDelDiaFirestoreMapper.desdeDocumento(fecha, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `ComidaDelDia con comida y cena nulas se reconstruye igual`() {
        val fecha = LocalDate.of(2026, 9, 22)
        val original = ComidaDelDia(fecha, comida = null, cena = null)

        val documento = ComidaDelDiaFirestoreMapper.aDocumento(original)
        val reconstruido = ComidaDelDiaFirestoreMapper.desdeDocumento(fecha, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `ComidaDelDia con documento vacio (sin campos) se lee como sin planificar`() {
        val fecha = LocalDate.of(2026, 9, 23)
        val reconstruido = ComidaDelDiaFirestoreMapper.desdeDocumento(fecha, emptyMap())

        assertEquals(ComidaDelDia(fecha, null, null), reconstruido)
    }
}
