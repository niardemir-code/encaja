package com.encaja.app.ui.semana

import com.encaja.app.domain.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Datos de ejemplo de la familia Oliver Izquierdo, semana del 14 al 20
 * de septiembre de 2026. Replica el escenario que fuimos dibujando en
 * las maquetas: lunes cubierto, martes con el hueco de Etna a fútbol,
 * miércoles cubierto por Josefa. Sirve tanto para los tests como para
 * un modo demo del módulo :app antes de conectar datos reales.
 */
object DatosEjemploFamilia {

    val victor = Caregiver(CaregiverId("victor"), "Víctor Oliver", CaregiverRole.ADMIN)
    val silvia = Caregiver(CaregiverId("silvia"), "Sílvia Izquierdo", CaregiverRole.ADMIN)
    val vicente = Caregiver(CaregiverId("vicente"), "Vicente Oliver", CaregiverRole.CUIDADOR)
    val dolors = Caregiver(CaregiverId("dolors"), "Dolors Vila", CaregiverRole.CUIDADOR)
    val gregorio = Caregiver(CaregiverId("gregorio"), "Gregorio Izquierdo", CaregiverRole.CUIDADOR)
    val josefa = Caregiver(CaregiverId("josefa"), "Josefa Fernández", CaregiverRole.CUIDADOR)

    val julia = ChildId("julia")
    val etna = ChildId("etna")

    val lunes: LocalDate = LocalDate.of(2026, 9, 14)

    val caregivers = listOf(victor, silvia, vicente, dolors, gregorio, josefa)

    /** Patrón recurrente: lunes lo cubren los padres, martes y jueves la rama materna, viernes la paterna. */
    val patrones = listOf(
        PatronCuidado(DayOfWeek.MONDAY, silvia.id),
        PatronCuidado(DayOfWeek.TUESDAY, josefa.id),
        PatronCuidado(DayOfWeek.THURSDAY, josefa.id),
        PatronCuidado(DayOfWeek.FRIDAY, vicente.id)
    )

    /** El miércoles no tiene patrón fijo; esta semana lo cubre Sílvia a mano. */
    val anulaciones = mapOf(
        lunes.plusDays(2) to silvia.id
    )

    /** Víctor de turno de tarde toda la semana; Josefa con médico justo el martes por la tarde. */
    val disponibilidad = listOf(
        AvailabilityBlock(victor.id, lunes.plusDays(0), LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO),
        AvailabilityBlock(victor.id, lunes.plusDays(1), LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO),
        AvailabilityBlock(victor.id, lunes.plusDays(2), LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO),
        AvailabilityBlock(victor.id, lunes.plusDays(3), LocalTime.of(14, 0), LocalTime.of(22, 0), MotivoNoDisponibilidad.TRABAJO),
        AvailabilityBlock(josefa.id, lunes.plusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0), MotivoNoDisponibilidad.MEDICO)
    )

    val needsDeLaSemana: List<CoverageNeed> = listOf(
        // Lunes: recogida de Júlia, cubierta por Sílvia (no hay bloqueo para ella)
        CoverageNeed(CoverageNeedId("julia-lunes"), julia, lunes.plusDays(0), LocalTime.of(16, 30), LocalTime.of(17, 0), "Recoger a Júlia"),
        // Martes: fútbol de Etna, a la hora en que Josefa tiene médico -> hueco
        CoverageNeed(CoverageNeedId("etna-futbol-martes"), etna, lunes.plusDays(1), LocalTime.of(18, 30), LocalTime.of(20, 0), "Fútbol de Etna"),
        // Miércoles: recogida de Etna, sin patrón asignado ese día pero cubierta por Sílvia vía anulación puntual (ver test)
        CoverageNeed(CoverageNeedId("etna-miercoles"), etna, lunes.plusDays(2), LocalTime.of(16, 30), LocalTime.of(17, 0), "Recoger a Etna"),
        // Jueves: inglés de Júlia, cubierto por Josefa según el patrón
        CoverageNeed(CoverageNeedId("julia-jueves"), julia, lunes.plusDays(3), LocalTime.of(17, 0), LocalTime.of(18, 0), "Inglés de Júlia")
    )
}
