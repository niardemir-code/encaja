package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Selectores de fecha, hora y días compartidos por los diálogos de disponibilidad (Familia)
// y de actividades (Guía): antes cada uno tenía su copia idéntica.

import com.encaja.app.ui.common.diaYMesCorto
import com.encaja.app.ui.common.mesYAnio
import com.encaja.app.ui.common.letraDia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.encaja.app.ui.common.formatearHora
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Caja "Desde / 09:00" con reloj a la derecha; al tocarla se abre el reloj de Material 3. */
@Composable
fun CajaHoraHoja(titulo: String, hora: LocalTime, modifier: Modifier = Modifier, onCambiar: (LocalTime) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValorHoja(titulo, formatearHora(hora), Icons.Default.Schedule, modifier) { abierto = true }
    if (abierto) {
        SelectorHoraHoja(
            titulo = titulo,
            hora = hora,
            onElegir = onCambiar,
            onCerrar = { abierto = false }
        )
    }
}

/** Caja "Desde / 29 sept" con calendario a la derecha; al tocarla se abre el calendario. */
@Composable
fun CajaFechaHoja(titulo: String, fecha: LocalDate, modifier: Modifier = Modifier, onCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValorHoja(titulo, fechaCortaHoja(fecha), Icons.Default.CalendarMonth, modifier) { abierto = true }
    if (abierto) {
        SelectorFechaHoja(
            fecha = fecha,
            onElegir = onCambiar,
            onCerrar = { abierto = false }
        )
    }
}

fun fechaCortaHoja(fecha: LocalDate): String = diaYMesCorto(fecha)

/**
 * Calendario de un mes (con flechas para cambiar de mes) en el que se marcan los días
 * que se quieran; los marcados van en color de acento y el de hoy lleva borde.
 */
@Composable
fun CalendarioMultipleHoja(
    mesInicial: YearMonth,
    seleccionadas: Set<LocalDate>,
    onAlternar: (LocalDate) -> Unit,
    /** Días que llevan un puntito (p.ej. los que ya tienen algo apuntado), estén o no marcados. */
    conPunto: Set<LocalDate> = emptySet()
) {
    var mes by remember { mutableStateOf(mesInicial) }
    val hoy = LocalDate.now()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { mes = mes.minusMonths(1) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = indigoHoja)
        }
        Text(
            mesYAnio(mes.atDay(1)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tintaHoja,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { mes = mes.plusMonths(1) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = indigoHoja)
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dia ->
            Text(
                letraDeDiaHoja(dia),
                style = MaterialTheme.typography.labelMedium,
                color = tintaSuaveHoja,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(4.dp))

    // Celdas del mes: huecos antes del día 1 (la semana empieza en lunes) y después del último.
    val primerDia = mes.atDay(1)
    val huecosInicio = primerDia.dayOfWeek.value - 1
    val celdas: List<LocalDate?> = List(huecosInicio) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    celdas.chunked(7).forEach { semana ->
        Row(modifier = Modifier.fillMaxWidth()) {
            semana.forEach { dia ->
                Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (dia != null) {
                        val marcado = dia in seleccionadas
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (marcado) acentoHoja else Color.Transparent)
                                .border(
                                    width = if (dia == hoy) 2.dp else 0.dp,
                                    color = if (dia == hoy) acentoHoja else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAlternar(dia) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dia.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (marcado) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (marcado) onAcentoHoja else tintaHoja
                            )
                            if (dia in conPunto) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (marcado) onAcentoHoja else acentoHoja)
                                )
                            }
                        }
                    }
                }
            }
            repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Una letra por día (L M X J V S D, o M T W T F S S en inglés). */
fun letraDeDiaHoja(dia: DayOfWeek): String = letraDia(dia)

/** [desde] y sus apariciones de [dia] hasta [hasta] (ambas incluidas; incluye [desde]
 * mismo si coincide con [dia]). */
fun fechasParaDiaSemanaHoja(desde: LocalDate, dia: DayOfWeek, hasta: LocalDate): List<LocalDate> {
    val primera = desde.with(TemporalAdjusters.nextOrSame(dia))
    if (hasta.isBefore(primera)) return emptyList()
    return generateSequence(primera) { it.plusWeeks(1) }.takeWhile { !it.isAfter(hasta) }.toList()
}

/**
 * Fila de siete círculos (L a D) para marcar en qué días de la semana se repite la
 * ocupación cada semana — además de, no en vez de, las fechas sueltas del calendario.
 */
@Composable
fun SelectorDiasSemanaHoja(
    seleccionados: Set<DayOfWeek>,
    onAlternar: (DayOfWeek) -> Unit,
    /** Días atenuados (activos, pero con alguna fecha quitada): mismo color, más suave. */
    atenuados: Set<DayOfWeek> = emptySet()
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dia ->
            val marcado = dia in seleccionados
            val atenuado = !marcado && dia in atenuados
            Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                marcado -> acentoHoja
                                atenuado -> acentoHoja.copy(alpha = 0.35f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (marcado || atenuado) acentoHoja else tintaSuaveHoja.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onAlternar(dia) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letraDeDiaHoja(dia),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (marcado || atenuado) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (marcado) onAcentoHoja else tintaHoja
                    )
                }
            }
        }
    }
}
