package com.encaja.app.ui.ocupaciones

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.FamilyUnit

/**
 * Una ocupación del listado: la misma ocupación (fecha, horas, categoría y detalle)
 * puede estar en varias personas a la vez — es lo que pasa al apuntarla desde una
 * unidad familiar — y aquí se enseña una sola vez, con [bloques] (uno por persona),
 * [quien] (el nombre de la unidad si coincide con sus miembros, o los nombres) y
 * [idsFiltro] (los ids de persona y de unidad por los que se puede filtrar).
 */
data class Ocupacion(
    val clave: String,
    val bloques: List<AvailabilityBlock>,
    val categoria: CategoriaDisponibilidad,
    val quien: String,
    val idsFiltro: Set<String>
) {
    val representante: AvailabilityBlock get() = bloques.first()
}

/** Una opción del filtro: "Todos", una persona o una unidad familiar. */
data class OpcionFiltro(val idTexto: String?, val etiqueta: String)

sealed class OcupacionesPantallaEstado {
    data object Cargando : OcupacionesPantallaEstado()
    data object SinFamilia : OcupacionesPantallaEstado()
    data class ConDatos(
        val ocupaciones: List<Ocupacion>,
        val cuidadores: List<Caregiver>,
        val unidades: List<FamilyUnit>
    ) : OcupacionesPantallaEstado()
}
