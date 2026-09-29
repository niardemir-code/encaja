package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import java.time.LocalDate

/**
 * Traduce disponibilidad en bruto a la cuadrícula de la semana: una fila por
 * cuidador, con sus 7 días y los bloqueos de cada uno.
 */
class FamiliaUiStateMapper(
    private val caregivers: List<Caregiver>,
    private val disponibilidad: List<AvailabilityBlock>
) {
    fun construir(lunes: LocalDate, esSemanaActual: Boolean = true): FamiliaUiState {
        val dias = (0..6).map { lunes.plusDays(it.toLong()) }

        val cuidadores = caregivers.map { caregiver ->
            val diasDelCuidador = dias.map { fecha ->
                val bloqueos = disponibilidad.filter { it.caregiverId == caregiver.id && it.fecha == fecha }
                DiaDisponibilidadCuidador(fecha, bloqueos)
            }
            CuidadorDisponibilidadSemana(caregiver, diasDelCuidador)
        }

        return FamiliaUiState(lunes, esSemanaActual, cuidadores)
    }
}
