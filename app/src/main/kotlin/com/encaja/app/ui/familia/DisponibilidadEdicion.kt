package com.encaja.app.ui.familia

import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriasBase
import com.encaja.app.domain.model.categoriaEn
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.TurnoTrabajo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Lógica pura (sin Compose) detrás del diálogo de disponibilidad de la
 * pantalla Familia, separada para poder testearla sin emulador.
 */

/** El turno guardado que tiene exactamente estas horas, si lo hay (para marcarlo como elegido). */
fun turnoConHoras(turnos: List<TurnoTrabajo>, inicio: LocalTime, fin: LocalTime): TurnoTrabajo? =
    turnos.firstOrNull { it.horaInicio == inicio && it.horaFin == fin }

/** Máximo de días que se aceptan en un rango (viaje/vacaciones), para no crear cientos de bloques por error. */
const val MAX_DIAS_RANGO = 90

fun etiquetaMotivo(motivo: MotivoNoDisponibilidad): String = when (motivo) {
    MotivoNoDisponibilidad.TRABAJO -> "Trabajo"
    MotivoNoDisponibilidad.MEDICO -> "Médico"
    MotivoNoDisponibilidad.VIAJE -> "Viaje"
    MotivoNoDisponibilidad.VACACIONES -> "Vacaciones"
    MotivoNoDisponibilidad.OTRO -> "Otro"
}

/** Versión corta para la casilla del día, que es estrecha. */
fun etiquetaMotivoCorta(motivo: MotivoNoDisponibilidad): String = when (motivo) {
    MotivoNoDisponibilidad.TRABAJO -> "Trab."
    MotivoNoDisponibilidad.MEDICO -> "Médico"
    MotivoNoDisponibilidad.VIAJE -> "Viaje"
    MotivoNoDisponibilidad.VACACIONES -> "Vacac."
    MotivoNoDisponibilidad.OTRO -> "Otro"
}

fun formatearHora(hora: LocalTime): String =
    "${hora.hour.toString().padStart(2, '0')}:${hora.minute.toString().padStart(2, '0')}"

/** "06:00 - 14:00", o "Todo el día". */
fun textoHorario(bloque: AvailabilityBlock): String =
    if (bloque.todoElDia) "Todo el día" else "${formatearHora(bloque.horaInicio)} - ${formatearHora(bloque.horaFin)}"

/**
 * Texto de la casilla de un día en la cuadrícula de Familia: las horas en dos
 * líneas ("06:00" / "14:00") o, si es de día completo, el tipo ("Viaje").
 * Si hay más de un bloque ese día se añade "+N" en la segunda línea.
 */
fun textoCelda(bloques: List<AvailabilityBlock>): String? {
    if (bloques.isEmpty()) return null
    val primero = bloques.sortedBy { it.horaInicio }.first()
    val extra = if (bloques.size > 1) " +${bloques.size - 1}" else ""
    return if (primero.todoElDia) {
        etiquetaMotivoCorta(primero.motivo) + extra
    } else {
        "${formatearHora(primero.horaInicio)}\n${formatearHora(primero.horaFin)}$extra"
    }
}

/**
 * Emoji a mostrar en la casilla en lugar del texto, según el primer bloque del día:
 *  - si ocupa el día entero (vacaciones, viaje...), siempre el de su categoría;
 *  - si es por horas, solo si ese emoji lo eligió la familia (categoría propia, o de
 *    serie con el emoji cambiado). Si no, null y la casilla muestra las horas con
 *    [textoCelda] — así un turno de Trabajo sigue viéndose como "06:00 / 14:00".
 * Las horas se ven siempre al tocar la casilla. El "+N" lo añade la pantalla.
 */
fun emojiCelda(bloques: List<AvailabilityBlock>, categorias: List<CategoriaDisponibilidad>): String? {
    val primero = bloques.sortedBy { it.horaInicio }.firstOrNull() ?: return null
    val categoria = primero.categoriaEn(categorias)
    if (categoria.emoji.isBlank()) return null
    return if (primero.todoElDia || CategoriasBase.tieneEmojiElegido(categoria)) categoria.emoji else null
}

/** Las fechas de la semana que empieza en [lunes] correspondientes a los [dias] marcados, en orden. */
fun fechasDeLaSemana(lunes: LocalDate, dias: Set<DayOfWeek>): List<LocalDate> =
    (0..6).map { lunes.plusDays(it.toLong()) }.filter { it.dayOfWeek in dias }

/** Las fechas a guardar como trabajo: las marcadas y, si se pide, las mismas una semana después. */
fun fechasTrabajo(fechas: List<LocalDate>, duplicarSemanaSiguiente: Boolean): List<LocalDate> =
    (if (duplicarSemanaSiguiente) fechas + fechas.map { it.plusWeeks(1) } else fechas).distinct().sorted()

/** Todas las fechas entre [desde] y [hasta], ambas incluidas (da igual el orden en que se pasen). */
fun fechasEntre(desde: LocalDate, hasta: LocalDate): List<LocalDate> {
    val (inicio, fin) = if (hasta.isBefore(desde)) hasta to desde else desde to hasta
    val dias = ChronoUnit.DAYS.between(inicio, fin)
    return (0..dias).map { inicio.plusDays(it) }
}

/** El DatePicker de Material trabaja con milisegundos en UTC; estas dos funciones hacen la conversión. */
fun fechaAMillisUtc(fecha: LocalDate): Long = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun millisUtcAFecha(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
