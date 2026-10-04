package com.encaja.app.ui.ajustes

import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.FamilyUnit

sealed class AjustesPantallaEstado {
    data object Cargando : AjustesPantallaEstado()
    data object SinFamilia : AjustesPantallaEstado()
    data class ConDatos(
        val ninos: List<Child>,
        val cuidadores: List<Caregiver>,
        val unidades: List<FamilyUnit>,
        /** El cuidador al que está vinculada la cuenta con la sesión abierta, si lo hay. */
        val miCaregiverId: CaregiverId? = null,
        /** Si esa cuenta es de un administrador (puede desvincular a otras). */
        val soyAdmin: Boolean = false,
        /** Cuidadores que ya tienen una cuenta vinculada. */
        val vinculados: Set<CaregiverId> = emptySet()
    ) : AjustesPantallaEstado()
}
