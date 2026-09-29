package com.encaja.app.ui.semana

import com.encaja.app.domain.model.*
import com.encaja.app.domain.usecase.CalcularHuecosDelDia
import com.encaja.app.domain.usecase.DetectarConflictosDeAsignacion
import java.time.LocalDate

/**
 * Traduce el resultado de los casos de uso de dominio al estado que pinta
 * la pantalla del Semáforo. Es la única pieza que conoce a la vez el
 * dominio y la forma de la UI — el ViewModel real (en Android) se limita
 * a llamar a esto y exponer el resultado en un StateFlow.
 */
class SemaforoUiStateMapper(
    private val ninos: List<Child>,
    private val caregivers: List<Caregiver>,
    private val patrones: List<PatronCuidado>,
    private val anulaciones: Anulaciones,
    private val disponibilidad: List<AvailabilityBlock>
) {
    fun construir(lunes: LocalDate, needsDeLaSemana: List<CoverageNeed>): SemaforoUiState {
        val calcularHuecos = CalcularHuecosDelDia(caregivers, patrones, anulaciones, disponibilidad)
        val huecos = calcularHuecos(needsDeLaSemana)
        val huecosPorFecha = huecos.groupBy { it.need.fecha }
        val needsPorFecha = needsDeLaSemana.groupBy { it.fecha }

        // No bloqueantes (la tarea sigue "cubierta"): quien lleva/recoge manualmente
        // en Guía tiene, justo a esa hora, un bloqueo de disponibilidad en Familia.
        val avisos = DetectarConflictosDeAsignacion(ninos, caregivers, disponibilidad)(needsDeLaSemana)
        val avisosPorFecha = avisos.groupBy { it.need.fecha }

        val dias = (0..6).map { offset ->
            val fecha = lunes.plusDays(offset.toLong())
            val huecosDelDia = huecosPorFecha[fecha].orEmpty()
            val estado = when {
                huecosDelDia.isNotEmpty() -> EstadoDia.ROJO
                !avisosPorFecha[fecha].isNullOrEmpty() -> EstadoDia.AMBAR
                needsPorFecha[fecha].isNullOrEmpty() -> EstadoDia.SIN_DATOS
                else -> EstadoDia.VERDE
            }
            DiaSemaforo(fecha, estado, huecosDelDia)
        }

        return SemaforoUiState(dias, huecos, avisos = avisos)
    }
}
