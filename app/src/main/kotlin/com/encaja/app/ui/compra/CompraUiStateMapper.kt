package com.encaja.app.ui.compra

import com.encaja.app.domain.model.ArticuloCompra

/**
 * Agrupa la lista plana de artículos por tienda (para pintar una sección
 * por tienda) y, dentro de cada tienda, deja primero los que faltan por
 * comprar y al final los ya comprados. Kotlin puro: sin Compose ni
 * Android, así que se puede testear sin emulador.
 */
class CompraUiStateMapper {

    fun construir(articulos: List<ArticuloCompra>): CompraUiState {
        val grupos = articulos
            .groupBy { it.tienda }
            .toSortedMap(compareBy { it.lowercase() })
            .map { (tienda, articulosDeTienda) ->
                GrupoTienda(
                    tienda = tienda,
                    articulos = articulosDeTienda.sortedWith(compareBy({ it.comprado }, { it.nombre.lowercase() }))
                )
            }
        return CompraUiState(grupos)
    }
}
