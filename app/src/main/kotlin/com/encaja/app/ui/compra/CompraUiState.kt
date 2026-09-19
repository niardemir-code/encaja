package com.encaja.app.ui.compra

import com.encaja.app.domain.model.ArticuloCompra

/** Un grupo de artículos que pertenecen a la misma tienda, ya ordenados para pintar. */
data class GrupoTienda(
    val tienda: String,
    val articulos: List<ArticuloCompra>
)

data class CompraUiState(
    val grupos: List<GrupoTienda>
)
