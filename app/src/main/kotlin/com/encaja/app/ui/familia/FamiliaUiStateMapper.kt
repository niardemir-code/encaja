package com.encaja.app.ui.familia

import com.encaja.app.domain.model.*
import java.time.LocalDate

class FamiliaUiStateMapper(
    private val caregivers: List<Caregiver>,
    private val unidades: List<FamilyUnit>,
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones,
    private val disponibilidad: List<AvailabilityBlock>
) {
    /** Busca el id guardado (de PatronCuidado/Anulaciones) primero entre los cuidadores
     * individuales y, si no aparece ahí, entre las unidades familiares. */
    private fun resolverResponsable(id: CaregiverId?): Responsable? {
        if (id == null) return null
        caregivers.find { it.id == id }?.let { return Responsable.Persona(it) }
        unidades.find { it.id.value == id.value }?.let { return Responsable.Unidad(it) }
        return null
    }

    fun construir(lunes: LocalDate, esSemanaActual: Boolean = true): FamiliaUiState {
        val dias = (0..6).map { lunes.plusDays(it.toLong()) }

        val diasAsignacion = dias.map { fecha ->
            val id = resolverAsignacion(fecha, patrones, anulaciones)
            DiaAsignado(fecha, resolverResponsable(id), esCambioPuntual = anulaciones.containsKey(fecha))
        }

        val cuidadores = caregivers.map { caregiver ->
            val diasDelCuidador = dias.map { fecha ->
                val bloqueos = disponibilidad.filter { it.caregiverId == caregiver.id && it.fecha == fecha }
                DiaDisponibilidadCuidador(fecha, bloqueos)
            }
            CuidadorDisponibilidadSemana(caregiver, diasDelCuidador)
        }

        val patronSemanal = patrones.mapNotNull { patron ->
            resolverResponsable(patron.caregiverId)?.let { patron.diaSemana to it }
        }.toMap()

        return FamiliaUiState(lunes, esSemanaActual, diasAsignacion, cuidadores, unidades, patronSemanal)
    }
}
