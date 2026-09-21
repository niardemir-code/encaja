@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Diálogo que se abre al tocar la casilla de un cuidador en un día concreto de la
// pantalla Familia. Arriba lista lo que ya hay ese día (con papelera); debajo se
// elige el tipo a añadir (Trabajo, Médico, Viaje, Vacaciones, Otro) y aparece el
// formulario de ese tipo. Las horas se eligen con el reloj de Material 3 y las
// fechas con su calendario, nada se escribe a mano.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

/** Color de fondo de la casilla según el tipo; se reutiliza en la leyenda. */
fun colorMotivo(motivo: MotivoNoDisponibilidad): Color = when (motivo) {
    MotivoNoDisponibilidad.TRABAJO -> Color(0xFFF7C1C1)
    MotivoNoDisponibilidad.MEDICO -> Color(0xFFB5D4F4)
    MotivoNoDisponibilidad.VIAJE -> Color(0xFFFAC775)
    MotivoNoDisponibilidad.VACACIONES -> Color(0xFFDDD3F7)
    MotivoNoDisponibilidad.OTRO -> Color(0xFFE0E0E0)
}

private fun nombreDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${fecha.dayOfMonth}"

private fun fechaCorta(fecha: LocalDate): String =
    "${fecha.dayOfMonth} ${fecha.month.getDisplayName(TextStyle.SHORT, ES)}"

@Composable
fun DialogoDisponibilidad(
    caregiver: Caregiver,
    fecha: LocalDate,
    lunes: LocalDate,
    bloquesDelDia: List<AvailabilityBlock>,
    turnos: List<TurnoTrabajo>,
    onEliminar: (AvailabilityBlock) -> Unit,
    onGuardarTrabajo: (fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, duplicarSemanaSiguiente: Boolean, nombreTurno: String?) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onGuardarBloques: (List<AvailabilityBlock>) -> Unit,
    onCerrar: () -> Unit
) {
    var tipo by remember { mutableStateOf<MotivoNoDisponibilidad?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("${caregiver.nombre} · ${nombreDia(fecha)}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (bloquesDelDia.isNotEmpty()) {
                    Text("Este día", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    bloquesDelDia.sortedBy { it.horaInicio }.forEach { bloque ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(12.dp).clip(CircleShape).background(colorMotivo(bloque.motivo))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${etiquetaMotivo(bloque.motivo)} · ${textoHorario(bloque)}")
                                bloque.etiqueta?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onEliminar(bloque) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar", modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }

                Text("Añadir", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MotivoNoDisponibilidad.values().forEach { opcion ->
                        FilterChip(
                            selected = tipo == opcion,
                            onClick = { tipo = if (tipo == opcion) null else opcion },
                            label = { Text(etiquetaMotivo(opcion)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                val seleccionado = tipo
                if (seleccionado != null) when (seleccionado) {
                    MotivoNoDisponibilidad.TRABAJO -> FormularioTrabajo(
                        fecha = fecha,
                        lunes = lunes,
                        turnos = turnos,
                        onCrearTurno = onCrearTurno,
                        onEliminarTurno = onEliminarTurno,
                        onGuardar = { fechas, inicio, fin, duplicar, nombreTurno ->
                            onGuardarTrabajo(fechas, inicio, fin, duplicar, nombreTurno)
                            onCerrar()
                        }
                    )

                    MotivoNoDisponibilidad.MEDICO, MotivoNoDisponibilidad.OTRO -> FormularioHoras(
                        motivo = seleccionado,
                        onGuardar = { inicio, fin, etiqueta ->
                            onGuardarBloques(listOf(AvailabilityBlock(caregiver.id, fecha, inicio, fin, seleccionado, etiqueta)))
                            onCerrar()
                        }
                    )

                    MotivoNoDisponibilidad.VIAJE, MotivoNoDisponibilidad.VACACIONES -> FormularioRango(
                        fecha = fecha,
                        motivo = seleccionado,
                        onGuardar = { fechas, etiqueta ->
                            onGuardarBloques(fechas.map { dia ->
                                AvailabilityBlock(
                                    caregiver.id, dia, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, seleccionado, etiqueta
                                )
                            })
                            onCerrar()
                        }
                    )

                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun FormularioTrabajo(
    fecha: LocalDate,
    lunes: LocalDate,
    turnos: List<TurnoTrabajo>,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onGuardar: (fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, duplicar: Boolean, nombreTurno: String?) -> Unit
) {
    var inicio by remember { mutableStateOf(turnos.firstOrNull()?.horaInicio ?: LocalTime.of(8, 0)) }
    var fin by remember { mutableStateOf(turnos.firstOrNull()?.horaFin ?: LocalTime.of(15, 0)) }
    var dias by remember { mutableStateOf(setOf(fecha.dayOfWeek)) }
    var editandoTurnos by remember { mutableStateOf(false) }
    var nuevoTurnoAbierto by remember { mutableStateOf(false) }
    val turnoElegido = turnoConHoras(turnos, inicio, fin)
    // Si se borran todos los turnos estando en modo edición, se sale solo de ese modo.
    val enEdicion = editandoTurnos && turnos.isNotEmpty()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Tus turnos", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (turnos.isNotEmpty()) {
            TextButton(onClick = { editandoTurnos = !enEdicion }) {
                Text(if (enEdicion) "Hecho" else "Editar")
            }
        }
    }
    if (turnos.isEmpty()) {
        Text(
            "Aún no hay turnos guardados. Crea uno (p.ej. \"Mañana 6-14\") para elegirlo de un toque.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        turnos.forEach { turno ->
            FilterChip(
                selected = !enEdicion && turno == turnoElegido,
                onClick = {
                    if (enEdicion) onEliminarTurno(turno.id)
                    else { inicio = turno.horaInicio; fin = turno.horaFin }
                },
                label = { Text("${turno.nombre} · ${formatearHora(turno.horaInicio)}-${formatearHora(turno.horaFin)}") },
                trailingIcon = if (enEdicion) {
                    { Icon(Icons.Default.Close, contentDescription = "Borrar turno", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        if (!enEdicion) {
            AssistChip(
                onClick = { nuevoTurnoAbierto = true },
                label = { Text("Nuevo turno") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("Horas de este día", style = MaterialTheme.typography.labelMedium)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonHora("Desde", inicio, Modifier.weight(1f)) { inicio = it }
        BotonHora("Hasta", fin, Modifier.weight(1f)) { fin = it }
    }
    if (!fin.isAfter(inicio)) {
        Text(
            "Turno de noche: termina al día siguiente a las ${formatearHora(fin)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (turnoElegido == null) {
        TextButton(onClick = { nuevoTurnoAbierto = true }) { Text("Guardar estas horas como turno") }
    }

    if (nuevoTurnoAbierto) {
        DialogoNuevoTurno(
            inicioInicial = inicio,
            finInicial = fin,
            onCrear = { nombre, i, f ->
                onCrearTurno(nombre, i, f)
                inicio = i; fin = f
                nuevoTurnoAbierto = false
            },
            onCerrar = { nuevoTurnoAbierto = false }
        )
    }

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dias = if (dias.size == 7) setOf(fecha.dayOfWeek) else DayOfWeek.values().toSet() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = dias.size == 7,
            onCheckedChange = { marcado -> dias = if (marcado) DayOfWeek.values().toSet() else setOf(fecha.dayOfWeek) }
        )
        Text("Toda la semana (lunes a domingo)")
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.values().forEach { dia ->
            val marcado = dia in dias
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (marcado) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { dias = if (marcado) dias - dia else dias + dia },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dia.getDisplayName(TextStyle.NARROW, ES).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (marcado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    val fechas = fechasDeLaSemana(lunes, dias)
    Button(
        onClick = { onGuardar(fechas, inicio, fin, false, turnoElegido?.nombre) },
        enabled = fechas.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Guardar") }
    OutlinedButton(
        onClick = { onGuardar(fechas, inicio, fin, true, turnoElegido?.nombre) },
        enabled = fechas.isNotEmpty(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Guardar y duplicar a la semana siguiente") }
}

@Composable
private fun FormularioHoras(
    motivo: MotivoNoDisponibilidad,
    onGuardar: (inicio: LocalTime, fin: LocalTime, etiqueta: String?) -> Unit
) {
    var todoElDia by remember { mutableStateOf(false) }
    var inicio by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var fin by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var etiqueta by remember { mutableStateOf("") }
    val horasValidas = todoElDia || fin.isAfter(inicio)

    Row(
        modifier = Modifier.fillMaxWidth().clickable { todoElDia = !todoElDia },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todoElDia, onCheckedChange = { todoElDia = it })
        Text("Todo el día")
    }
    if (!todoElDia) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonHora("Desde", inicio, Modifier.weight(1f)) { inicio = it }
            BotonHora("Hasta", fin, Modifier.weight(1f)) { fin = it }
        }
        if (!horasValidas) {
            Text(
                "La hora de fin debe ser posterior a la de inicio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = etiqueta,
        onValueChange = { etiqueta = it },
        label = { Text(if (motivo == MotivoNoDisponibilidad.OTRO) "¿Qué es?" else "Detalle (opcional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            val (i, f) = if (todoElDia) AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA else inicio to fin
            onGuardar(i, f, etiqueta.trim().ifBlank { null })
        },
        enabled = horasValidas,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Guardar") }
}

@Composable
private fun FormularioRango(
    fecha: LocalDate,
    motivo: MotivoNoDisponibilidad,
    onGuardar: (fechas: List<LocalDate>, etiqueta: String?) -> Unit
) {
    var desde by remember { mutableStateOf(fecha) }
    var hasta by remember { mutableStateOf(fecha) }
    var etiqueta by remember { mutableStateOf("") }
    val fechas = fechasEntre(desde, hasta)
    val demasiadosDias = fechas.size > MAX_DIAS_RANGO

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonFecha("Desde", desde, Modifier.weight(1f)) { desde = it; if (hasta.isBefore(it)) hasta = it }
        BotonFecha("Hasta", hasta, Modifier.weight(1f)) { hasta = it }
    }
    Text(
        if (fechas.size == 1) "1 día completo" else "${fechas.size} días completos",
        style = MaterialTheme.typography.bodySmall,
        color = if (demasiadosDias) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = etiqueta,
        onValueChange = { etiqueta = it },
        label = { Text(if (motivo == MotivoNoDisponibilidad.VIAJE) "Destino (opcional)" else "Detalle (opcional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onGuardar(fechas, etiqueta.trim().ifBlank { null }) },
        enabled = !demasiadosDias,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Guardar") }
}

/** Crear un turno con nombre y horas, que queda guardado para toda la familia. */
@Composable
private fun DialogoNuevoTurno(
    inicioInicial: LocalTime,
    finInicial: LocalTime,
    onCrear: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf(inicioInicial) }
    var fin by remember { mutableStateOf(finInicial) }
    val valido = nombre.isNotBlank() && inicio != fin

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo turno") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (p.ej. Mañana, Oficina)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonHora("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                    BotonHora("Hasta", fin, Modifier.weight(1f)) { fin = it }
                }
                if (!fin.isAfter(inicio) && inicio != fin) {
                    Text(
                        "Turno de noche: termina al día siguiente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCrear(nombre.trim(), inicio, fin) }, enabled = valido) { Text("Guardar turno") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

/** Botón que muestra una hora y, al tocarlo, abre el reloj de Material 3 para cambiarla. */
@Composable
private fun BotonHora(titulo: String, hora: LocalTime, modifier: Modifier = Modifier, onCambiar: (LocalTime) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = modifier) {
        Text("$titulo ${formatearHora(hora)}")
    }
    if (abierto) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(titulo) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onCambiar(LocalTime.of(estado.hour, estado.minute))
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        )
    }
}

/** Botón que muestra una fecha y, al tocarlo, abre el calendario de Material 3. */
@Composable
private fun BotonFecha(titulo: String, fecha: LocalDate, modifier: Modifier = Modifier, onCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = modifier) {
        Text("$titulo ${fechaCorta(fecha)}")
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtc(fecha))
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambiar(millisUtcAFecha(it)) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}
