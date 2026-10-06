package com.encaja.app.ui.semana

sealed class SemaforoPantallaEstado {
    data object Cargando : SemaforoPantallaEstado()
    data object SinFamilia : SemaforoPantallaEstado()

    /** No se pudo comprobar la familia (p. ej. sin conexión): se ofrece reintentar, no crear una. */
    data object ErrorDeConexion : SemaforoPantallaEstado()
    data class ConDatos(val estado: SemaforoUiState) : SemaforoPantallaEstado()
}
