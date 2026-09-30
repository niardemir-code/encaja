package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.TurnoTrabajo
import java.time.LocalDate

/**
 * Quién puede quedar registrado como responsable de una actividad: una persona
 * individual o una unidad familiar (un grupo, p.ej. "los abuelos maternos"). Hoy
 * solo lo usa la Guía, para elegir quién lleva/recoge a cada niño — esta pantalla
 * (Familia) ya no tiene su propia asignación de "con quién están las niñas", así
 * que la clase vive aquí solo porque fue la primera en necesitarla.
 */
sealed class Responsable {
    abstract val idTexto: String
    abstract val etiqueta: String

    data class Persona(val caregiver: Caregiver) : Responsable() {
        override val idTexto get() = caregiver.id.value
        override val etiqueta get() = caregiver.nombreCompleto
    }

    data class Unidad(val unidad: FamilyUnit) : Responsable() {
        override val idTexto get() = unidad.id.value
        override val etiqueta get() = unidad.nombre
    }
}

data class DiaDisponibilidadCuidador(val fecha: LocalDate, val bloqueos: List<AvailabilityBlock>) {
    val libre: Boolean get() = bloqueos.isEmpty()
}

data class CuidadorDisponibilidadSemana(val caregiver: Caregiver, val dias: List<DiaDisponibilidadCuidador>)

/**
 * Una unidad familiar en la cuadrícula: sus 7 días llevan, juntos, los bloqueos de
 * todos sus miembros (la unidad "va junta", así que si uno está ocupado la unidad no
 * está del todo libre). Es solo de lectura: la disponibilidad se edita por persona.
 */
data class UnidadDisponibilidadSemana(val unidad: FamilyUnit, val dias: List<DiaDisponibilidadCuidador>)

data class FamiliaUiState(
    val lunes: LocalDate,
    val esSemanaActual: Boolean,
    val cuidadores: List<CuidadorDisponibilidadSemana>,
    val unidades: List<UnidadDisponibilidadSemana> = emptyList(),
    /** Turnos de trabajo con nombre que ha definido la familia, para elegirlos de un toque. */
    val turnos: List<TurnoTrabajo> = emptyList(),
    /** Todas las categorías: las de serie (con su personalización, menos las borradas) y las propias. */
    val categorias: List<CategoriaDisponibilidad> = CategoriasBase.predeterminadas,
    /** Las de serie que la familia ha borrado, por si quiere recuperarlas. */
    val categoriasEliminadas: List<CategoriaDisponibilidad> = emptyList()
)
