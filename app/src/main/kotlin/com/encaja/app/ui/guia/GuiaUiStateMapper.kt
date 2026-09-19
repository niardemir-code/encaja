package com.encaja.app.ui.guia

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.CalcularHuecosDelDia
import java.time.LocalDate

/**
 * Traduce el resultado del dominio a la Guía del día: una fila por niño,
 * con sus bloques de ese día coloreados según si están cubiertos o son
 * un hueco. Kotlin puro, igual que SemaforoUiStateMapper.
 */
class GuiaUiStateMapper(
    private val ninos: List<Child>,
    private val caregivers: List<Caregiver>,
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones,
    private val disponibilidad: List<AvailabilityBlock>
) {
    fun construir(fecha: LocalDate, needsDelDia: List<CoverageNeed>): GuiaUiState {
        val calcularHuecos = CalcularHuecosDelDia(caregivers, patrones, anulaciones, disponibilidad)
        val idsConHueco = calcularHuecos(needsDelDia).map { it.need.id }.toSet()
        val needsPorNino = needsDelDia.groupBy { it.childId }

        val filas = ninos.map { nino ->
            val bloques = needsPorNino[nino.id].orEmpty()
                .sortedBy { it.horaInicio }
                .map { need ->
                    val asignadoId = resolverAsignacion(need.fecha, patrones, anulaciones)
                    val nombreAsignado = caregivers.firstOrNull { it.id == asignadoId }?.nombre
                    BloqueGuia(need, nombreAsignado, cubierto = need.id !in idsConHueco)
                }
            FilaGuia(nino, bloques)
        }

        return GuiaUiState(fecha, filas)
    }
}
