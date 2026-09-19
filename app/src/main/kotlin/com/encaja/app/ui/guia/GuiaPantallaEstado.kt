package com.encaja.app.ui.guia

sealed class GuiaPantallaEstado {
    data object Cargando : GuiaPantallaEstado()
    data object SinFamilia : GuiaPantallaEstado()
    data class ConDatos(val estado: GuiaUiState) : GuiaPantallaEstado()
}
