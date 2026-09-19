package com.encaja.app.ui.compra

sealed class CompraPantallaEstado {
    data object Cargando : CompraPantallaEstado()
    data object SinFamilia : CompraPantallaEstado()
    data class ConDatos(val estado: CompraUiState) : CompraPantallaEstado()
}
