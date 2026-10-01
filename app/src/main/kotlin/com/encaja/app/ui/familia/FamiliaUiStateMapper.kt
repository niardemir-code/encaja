package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.FamilyUnit
import java.time.LocalDate

/**
 * Traduce disponibilidad en bruto a la cuadrícula de la semana: una fila por
 * cuidador, con sus 7 días y los bloqueos de cada uno, y una fila por unidad
 * familiar con los bloqueos de todos sus miembros juntos.
 */
class FamiliaUiStateMapper(
    private val caregivers: List<Caregiver>,
    private val disponibilidad: List<AvailabilityBlock>,
    private val unidades: List<FamilyUnit> = emptyList()
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

        val unidadesSemana = unidades.map { unidad ->
            val miembros = unidad.miembros.toSet()
            val diasDeLaUnidad = dias.map { fecha ->
                // Una ocupación apuntada desde la unidad está copiada en cada miembro: en la
                // fila de la unidad se enseña una sola vez.
                val bloqueos = disponibilidad
                    .filter { it.caregiverId in miembros && it.fecha == fecha }
                    .sortedBy { it.horaInicio }
                    .sinRepetirEntrePersonas()
                DiaDisponibilidadCuidador(fecha, bloqueos)
            }
            UnidadDisponibilidadSemana(unidad, diasDeLaUnidad)
        }

        return FamiliaUiState(lunes, esSemanaActual, cuidadores, unidadesSemana)
    }
}
