package com.encaja.app.ui.menu

import com.encaja.app.domain.model.ComidaDelDia

sealed class MenuPantallaEstado {
    data object Cargando : MenuPantallaEstado()
    data object SinFamilia : MenuPantallaEstado()

    /** [esSemanaActual] indica si [dias] corresponde a la semana de hoy o a otra a la que se ha navegado. */
    data class ConDatos(val dias: List<ComidaDelDia>, val esSemanaActual: Boolean = true) : MenuPantallaEstado()
}
