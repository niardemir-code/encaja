package com.encaja.app.domain.usecase

import com.encaja.app.domain.model.*

/**
 * Determina qué necesidades de cobertura del día quedan sin cubrir. "Cubierta" se
 * decide por la asignación manual de la propia actividad (quién la lleva y quién la
 * recoge, elegidos en el diálogo de la Guía) — el patrón semanal de Familia es solo
 * orientativo y no interviene aquí. Una tarea que no requiere desplazamiento no
 * necesita a nadie asignado.
 */
class CalcularHuecosDelDia {
    operator fun invoke(needs: List<CoverageNeed>): List<Hueco> {
        return needs.mapNotNull { need ->
            if (!need.requiereDesplazamiento) return@mapNotNull null

            val faltaLleva = need.quienLlevaId == null
            val faltaRecoge = need.quienRecogeId == null
            when {
                faltaLleva && faltaRecoge -> Hueco(need, MotivoHueco.SIN_ASIGNACION)
                faltaLleva -> Hueco(need, MotivoHueco.FALTA_QUIEN_LLEVA)
                faltaRecoge -> Hueco(need, MotivoHueco.FALTA_QUIEN_RECOGE)
                else -> null
            }
        }
    }
}
