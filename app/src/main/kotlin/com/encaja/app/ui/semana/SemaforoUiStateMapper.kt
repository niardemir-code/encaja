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
    private val disponibilidad: List<AvailabilityBlock>
) {
    fun construir(lunes: LocalDate, needsDeLaSemana: List<CoverageNeed>, hoy: LocalDate = LocalDate.now()): SemaforoUiState {
        val huecos = CalcularHuecosDelDia()(needsDeLaSemana)
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
            DiaSemaforo(fecha, estado, huecosDelDia, needs = needsPorFecha[fecha].orEmpty())
        }

        // El círculo de cada día conserva su color aunque ya haya pasado (para poder
        // repasar la semana), pero las tarjetas de debajo son avisos accionables: un
        // hueco o incompatibilidad de un día ya pasado no hay nada que hacer con él,
        // así que desaparece de estas listas en cuanto la fecha queda atrás.
        val huecosVigentes = huecos.filter { it.need.fecha >= hoy }
        val avisosVigentes = avisos.filter { it.need.fecha >= hoy }

        return SemaforoUiState(dias, huecosVigentes, avisos = avisosVigentes)
    }
}
