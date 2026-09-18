package com.encaja.app.ui.familia

import com.encaja.app.domain.model.*
import java.time.LocalDate

class FamiliaUiStateMapper(
    private val caregivers: List<Caregiver>,
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones,
    private val disponibilidad: List<AvailabilityBlock>
) {
    fun construir(lunes: LocalDate): FamiliaUiState {
        val dias = (0..6).map { lunes.plusDays(it.toLong()) }

        val diasAsignacion = dias.map { fecha ->
            DiaAsignado(fecha, resolverAsignacion(fecha, patrones, anulaciones))
        }

        val cuidadores = caregivers.map { caregiver ->
            val diasDelCuidador = dias.map { fecha ->
                val bloqueos = disponibilidad.filter { it.caregiverId == caregiver.id && it.fecha == fecha }
                DiaDisponibilidadCuidador(fecha, bloqueos)
            }
            CuidadorDisponibilidadSemana(caregiver, diasDelCuidador)
        }

        return FamiliaUiState(diasAsignacion, cuidadores)
    }
}
