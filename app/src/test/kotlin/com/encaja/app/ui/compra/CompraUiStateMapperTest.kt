package com.encaja.app.ui.compra

import com.encaja.app.domain.model.ArticuloCompra
import com.encaja.app.domain.model.ArticuloCompraId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompraUiStateMapperTest {

    private val mapper = CompraUiStateMapper()

    @Test
    fun `agrupa los articulos por tienda`() {
        val articulos = listOf(
            ArticuloCompra(ArticuloCompraId("leche"), "Leche", "Mercadona"),
            ArticuloCompra(ArticuloCompraId("pan"), "Pan", "Panadería"),
            ArticuloCompra(ArticuloCompraId("huevos"), "Huevos", "Mercadona")
        )

        val estado = mapper.construir(articulos)

        assertEquals(listOf("Mercadona", "Panadería"), estado.grupos.map { it.tienda })
        assertEquals(2, estado.grupos.first { it.tienda == "Mercadona" }.articulos.size)
    }

    @Test
    fun `ordena las tiendas alfabeticamente sin distinguir mayusculas`() {
        val articulos = listOf(
            ArticuloCompra(ArticuloCompraId("a1"), "Manzanas", "zulueta"),
            ArticuloCompra(ArticuloCompraId("a2"), "Peras", "Alcampo")
        )

        val estado = mapper.construir(articulos)

        assertEquals(listOf("Alcampo", "zulueta"), estado.grupos.map { it.tienda })
    }

    @Test
    fun `dentro de cada tienda, los ya comprados quedan al final`() {
        val articulos = listOf(
            ArticuloCompra(ArticuloCompraId("a1"), "Zanahorias", "Mercadona", comprado = true),
            ArticuloCompra(ArticuloCompraId("a2"), "Arroz", "Mercadona", comprado = false)
        )

        val estado = mapper.construir(articulos)

        val nombres = estado.grupos.single().articulos.map { it.nombre }
        assertEquals(listOf("Arroz", "Zanahorias"), nombres)
    }

    @Test
    fun `una lista vacia produce cero grupos`() {
        assertTrue(mapper.construir(emptyList()).grupos.isEmpty())
    }
}
