package com.encaja.app.ui.semana

sealed class SemaforoPantallaEstado {
    data object Cargando : SemaforoPantallaEstado()
    data object SinFamilia : SemaforoPantallaEstado()
    data class ConDatos(val estado: SemaforoUiState) : SemaforoPantallaEstado()
}
