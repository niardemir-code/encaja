package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.FamilyUnit

sealed class AjustesPantallaEstado {
    data object Cargando : AjustesPantallaEstado()
    data object SinFamilia : AjustesPantallaEstado()
    data class ConDatos(
        val ninos: List<Child>,
        val cuidadores: List<Caregiver>,
        val unidades: List<FamilyUnit>
    ) : AjustesPantallaEstado()
}
