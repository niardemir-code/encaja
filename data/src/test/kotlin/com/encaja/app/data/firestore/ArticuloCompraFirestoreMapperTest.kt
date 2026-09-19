package com.encaja.app.data.firestore

import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArticuloCompraFirestoreMapperTest {

    @Test
    fun `ArticuloCompra hace el viaje completo sin perder datos`() {
        val original = ArticuloCompra(ArticuloCompraId("leche"), "Leche", "Mercadona", comprado = true)

        val documento = ArticuloCompraFirestoreMapper.aDocumento(original)
        val reconstruido = ArticuloCompraFirestoreMapper.desdeDocumento(original.id.value, documento)

        assertEquals(original, reconstruido)
    }

    @Test
    fun `ArticuloCompra sin campo comprado en el documento se interpreta como no comprado`() {
        val datos = mapOf("nombre" to "Pan", "tienda" to "Panadería")

        val reconstruido = ArticuloCompraFirestoreMapper.desdeDocumento("pan", datos)

        assertEquals(ArticuloCompra(ArticuloCompraId("pan"), "Pan", "Panadería", comprado = false), reconstruido)
    }

    @Test
    fun `ArticuloCompra sin nombre en el documento no revienta, devuelve null`() {
        assertNull(ArticuloCompraFirestoreMapper.desdeDocumento("leche", mapOf("tienda" to "Mercadona")))
    }

    @Test
    fun `ArticuloCompra sin tienda en el documento no revienta, devuelve null`() {
        assertNull(ArticuloCompraFirestoreMapper.desdeDocumento("leche", mapOf("nombre" to "Leche")))
    }
}
