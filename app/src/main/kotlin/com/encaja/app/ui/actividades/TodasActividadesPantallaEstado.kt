package com.encaja.app.ui.actividades

import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed

/** Las actividades (ya ordenadas por fecha y hora) de un niño, para el listado completo. */
data class GrupoActividadesNino(
    val child: Child,
    val actividades: List<CoverageNeed>
)

sealed class TodasActividadesPantallaEstado {
    data object Cargando : TodasActividadesPantallaEstado()
    data object SinFamilia : TodasActividadesPantallaEstado()
    data class ConDatos(val grupos: List<GrupoActividadesNino>) : TodasActividadesPantallaEstado()
}
