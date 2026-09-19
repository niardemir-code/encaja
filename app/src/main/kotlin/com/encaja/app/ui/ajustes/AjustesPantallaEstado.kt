package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.Child

sealed class AjustesPantallaEstado {
    data object Cargando : AjustesPantallaEstado()
    data object SinFamilia : AjustesPantallaEstado()
    data class ConDatos(val ninos: List<Child>) : AjustesPantallaEstado()
}
