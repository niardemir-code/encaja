@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.encaja.app.ui.theme

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Selectores de fecha y de hora con el mismo diseño que los avisos (AvisoHoja): tarjeta
// grande con icono en círculo, título y botones en píldora. Sustituyen a DatePickerDialog
// y al AlertDialog con TimePicker de Material por defecto.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private fun fechaAMillisUtcHoja(fecha: LocalDate): Long =
    fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisUtcAFechaHoja(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Calendario en una tarjeta de aviso; [confirmar] es el texto del botón ("Ir", "Aceptar"). */
@Composable
fun SelectorFechaHoja(
    fecha: LocalDate,
    onElegir: (LocalDate) -> Unit,
    onCerrar: () -> Unit,
    titulo: String = "Elige una fecha",
    confirmar: String = "Aceptar"
) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtcHoja(fecha))
    AvisoHoja(
        icono = Icons.Default.CalendarMonth,
        titulo = titulo,
        texto = null,
        peligro = false,
        conX = true,
        accionesEnFila = true,
        ancho = 380.dp,
        acciones = listOf(
            AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja(confirmar, EstiloAccionAviso.PRIMARIO, habilitada = estado.selectedDateMillis != null) {
                estado.selectedDateMillis?.let { onElegir(millisUtcAFechaHoja(it)) }
                onCerrar()
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            DatePicker(
                state = estado,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedDayContainerColor = acentoHoja,
                    selectedDayContentColor = onAcentoHoja,
                    todayContentColor = acentoHoja,
                    todayDateBorderColor = acentoHoja,
                    selectedYearContainerColor = acentoHoja,
                    selectedYearContentColor = onAcentoHoja
                )
            )
        }
    )
}

/** Reloj de 24 horas en una tarjeta de aviso. */
@Composable
fun SelectorHoraHoja(
    titulo: String,
    hora: LocalTime,
    onElegir: (LocalTime) -> Unit,
    onCerrar: () -> Unit
) {
    val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
    AvisoHoja(
        icono = Icons.Default.Schedule,
        titulo = titulo,
        texto = null,
        peligro = false,
        conX = true,
        accionesEnFila = true,
        ancho = 360.dp,
        acciones = listOf(
            AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja("Aceptar", EstiloAccionAviso.PRIMARIO) {
                onElegir(LocalTime.of(estado.hour, estado.minute))
                onCerrar()
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = estado,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = lavandaHoja.copy(alpha = 0.5f),
                        selectorColor = acentoHoja,
                        clockDialSelectedContentColor = onAcentoHoja,
                        timeSelectorSelectedContainerColor = lavandaHoja,
                        timeSelectorSelectedContentColor = indigoHoja
                    )
                )
            }
        }
    )
}
