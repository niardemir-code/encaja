package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.TurnoTrabajo
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Quién puede quedar registrado como responsable de un día: una persona
 * individual o una unidad familiar (un grupo, p.ej. "los abuelos maternos").
 * Se guarda en Firestore/Room igual en ambos casos (como un simple id de
 * texto en PatronCuidado/Anulaciones) — esta clase solo existe en la capa
 * de pantalla, para poder pintar cada caso de forma distinta y elegir entre
 * ambos en los diálogos de asignación.
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

/** Quién tiene asignado el cuidado ese día, y si es por patrón semanal o por un cambio puntual. */
data class DiaAsignado(
    val fecha: LocalDate,
    val responsable: Responsable?,
    val esCambioPuntual: Boolean
)

data class DiaDisponibilidadCuidador(val fecha: LocalDate, val bloqueos: List<AvailabilityBlock>) {
    val libre: Boolean get() = bloqueos.isEmpty()
}

data class CuidadorDisponibilidadSemana(val caregiver: Caregiver, val dias: List<DiaDisponibilidadCuidador>)

data class FamiliaUiState(
    val lunes: LocalDate,
    val esSemanaActual: Boolean,
    val diasAsignacion: List<DiaAsignado>,
    val cuidadores: List<CuidadorDisponibilidadSemana>,
    val unidades: List<FamilyUnit>,
    /** El patrón recurrente: quién es el responsable habitual cada día de la semana (L-D).
     * Un día sin entrada en el mapa significa que no tiene responsable fijo asignado. */
    val patronSemanal: Map<DayOfWeek, Responsable>,
    /** Turnos de trabajo con nombre que ha definido la familia, para elegirlos de un toque. */
    val turnos: List<TurnoTrabajo> = emptyList()
) {
    /** Todas las opciones asignables a un día: primero las personas, luego las unidades. */
    val opcionesAsignables: List<Responsable>
        get() = cuidadores.map { Responsable.Persona(it.caregiver) } + unidades.map { Responsable.Unidad(it) }
}
