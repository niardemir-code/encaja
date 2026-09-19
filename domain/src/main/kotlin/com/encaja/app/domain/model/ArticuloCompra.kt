package com.encaja.app.domain.model

/**
 * Un artículo de la lista de la compra de la familia. Se agrupan por
 * tienda en la pantalla Compra (cada tienda es como una sección aparte),
 * y cada uno se puede marcar como comprado sin borrarlo de la lista.
 */
data class ArticuloCompra(
    val id: ArticuloCompraId,
    val nombre: String,
    val tienda: String,
    val comprado: Boolean = false
)
