package com.encaja.app.ui.menu

import com.encaja.app.domain.model.ComidaDelDia

sealed class MenuPantallaEstado {
    data object Cargando : MenuPantallaEstado()
    data object SinFamilia : MenuPantallaEstado()
    data class ConDatos(val dias: List<ComidaDelDia>) : MenuPantallaEstado()
}
