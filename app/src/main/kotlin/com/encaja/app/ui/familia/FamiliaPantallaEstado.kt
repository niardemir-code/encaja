package com.encaja.app.ui.familia

sealed class FamiliaPantallaEstado {
    data object Cargando : FamiliaPantallaEstado()
    data object SinFamilia : FamiliaPantallaEstado()
    data class ConDatos(val estado: FamiliaUiState) : FamiliaPantallaEstado()
}
