package com.encaja.app.ui.guia

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.CalcularHuecosDelDia
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.familia.calcularInicialesCuidadores
import java.time.LocalDate

/**
 * Traduce el resultado del dominio a la Guía del día: una fila por niño,
 * con sus bloques de ese día coloreados según si están cubiertos o son
 * un hueco. Kotlin puro, igual que SemaforoUiStateMapper.
 */
class GuiaUiStateMapper(
    private val ninos: List<Child>,
    private val caregivers: List<Caregiver>,
    private val unidades: List<FamilyUnit>,
    private val disponibilidad: List<AvailabilityBlock>
) {
    private val responsables: List<Responsable> =
        caregivers.map { Responsable.Persona(it) } + unidades.map { Responsable.Unidad(it) }
    private val iniciales = calcularInicialesCuidadores(caregivers)

    private fun resolverResponsable(idTexto: String?): Responsable? {
        if (idTexto == null) return null
        return responsables.firstOrNull { it.idTexto == idTexto }
    }

    fun construir(fecha: LocalDate, needsDelDia: List<CoverageNeed>): GuiaUiState {
        val calcularHuecos = CalcularHuecosDelDia(caregivers, unidades, disponibilidad)
        val idsConHueco = calcularHuecos(needsDelDia).map { it.need.id }.toSet()
        val needsPorNino = needsDelDia.groupBy { it.childId }

        val filas = ninos.map { nino ->
            val bloques = needsPorNino[nino.id].orEmpty()
                .sortedBy { it.horaInicio }
                .map { need ->
                    BloqueGuia(
                        need = need,
                        cubierto = need.id !in idsConHueco,
                        quienLleva = resolverResponsable(need.quienLlevaId),
                        quienRecoge = resolverResponsable(need.quienRecogeId)
                    )
                }
            FilaGuia(nino, bloques)
        }

        return GuiaUiState(fecha, filas, responsables, iniciales)
    }
}
