@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Diálogo que se abre al tocar la casilla de un cuidador en un día concreto de la
// pantalla Familia. Arriba lista lo que ya hay ese día (con papelera); debajo se
// elige la categoría a añadir (las 5 de serie y las propias de la familia, que se
// crean y editan aquí mismo con DialogoCategoria) y aparece su formulario. Las
// horas se eligen con el reloj de Material 3 y las fechas con su calendario.

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
import androidx.compose.material.icons.filled.Edit
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
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.categoriaEn
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

private val DIAS_LABORABLES = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)

/** Texto de ayuda del campo libre de cada categoría. */
private fun etiquetaCampo(categoria: CategoriaDisponibilidad): String = when (categoria.base) {
    MotivoNoDisponibilidad.OTRO -> "¿Qué es?"
    MotivoNoDisponibilidad.VIAJE -> "Destino (opcional)"
    else -> "Detalle (opcional)"
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
    categorias: List<CategoriaDisponibilidad>,
    onEliminar: (AvailabilityBlock) -> Unit,
    onGuardarTrabajo: (fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, duplicarSemanaSiguiente: Boolean, nombreTurno: String?) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onGuardarBloques: (List<AvailabilityBlock>) -> Unit,
    onGuardarCategoria: (CategoriaDisponibilidad) -> Unit,
    onEliminarCategoria: (CategoriaId) -> Unit,
    onCerrar: () -> Unit
) {
    // Se guarda el id (no la categoría) para ver siempre su versión más reciente tras editarla.
    var seleccionId by remember { mutableStateOf<CategoriaId?>(null) }
    val seleccionada = categorias.firstOrNull { it.id == seleccionId }
    val esTrabajo = seleccionada?.base == MotivoNoDisponibilidad.TRABAJO

    var editandoCategorias by remember { mutableStateOf(false) }
    var categoriaEnEdicion by remember { mutableStateOf<CategoriaDisponibilidad?>(null) }
    var creandoCategoria by remember { mutableStateOf(false) }

    // Estado del formulario de Trabajo, elevado aquí (en vez de dentro de FormularioTrabajo)
    // para poder mostrar "Guardar" fuera de la zona con scroll, al mismo nivel que "Cerrar".
    var trabajoInicio by remember { mutableStateOf(turnos.firstOrNull()?.horaInicio ?: LocalTime.of(8, 0)) }
    var trabajoFin by remember { mutableStateOf(turnos.firstOrNull()?.horaFin ?: LocalTime.of(15, 0)) }
    var trabajoDias by remember { mutableStateOf(setOf(fecha.dayOfWeek)) }
    var avisoSemanaCopiada by remember { mutableStateOf(false) }

    val trabajoFechas = fechasDeLaSemana(lunes, trabajoDias)
    val trabajoTurnoElegido = turnoConHoras(turnos, trabajoInicio, trabajoFin)

    LaunchedEffect(avisoSemanaCopiada) {
        if (avisoSemanaCopiada) {
            delay(2000)
            avisoSemanaCopiada = false
        }
    }

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
                        val categoria = bloque.categoriaEn(categorias)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(categoria.color)))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${categoria.emoji} ${categoria.nombre} · ${textoHorario(bloque)}")
                                val detalle = listOfNotNull(
                                    bloque.etiqueta?.takeIf { it.isNotBlank() },
                                    "no ocupa".takeIf { !categoria.bloquea }
                                ).joinToString(" · ")
                                if (detalle.isNotEmpty()) {
                                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Añadir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { editandoCategorias = !editandoCategorias }) {
                        Text(if (editandoCategorias) "Hecho" else "Editar")
                    }
                }
                if (editandoCategorias) {
                    Text(
                        "Toca una categoría para cambiar su icono, color o nombre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categorias.forEach { categoria ->
                        FilterChip(
                            selected = !editandoCategorias && categoria.id == seleccionId,
                            onClick = {
                                if (editandoCategorias) categoriaEnEdicion = categoria
                                else seleccionId = if (seleccionId == categoria.id) null else categoria.id
                            },
                            label = { Text("${categoria.emoji} ${categoria.nombre}") },
                            trailingIcon = if (editandoCategorias) {
                                { Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(categoria.color))
                        )
                    }
                    AssistChip(
                        onClick = { creandoCategoria = true },
                        label = { Text("Nueva categoría") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (seleccionada != null && !editandoCategorias) {
                    // key: al cambiar de categoría, el formulario empieza de cero.
                    key(seleccionada.id) {
                        when {
                            esTrabajo -> FormularioTrabajo(
                                fecha = fecha,
                                turnos = turnos,
                                inicio = trabajoInicio,
                                onInicioCambiado = { trabajoInicio = it },
                                fin = trabajoFin,
                                onFinCambiado = { trabajoFin = it },
                                dias = trabajoDias,
                                onDiasCambiado = { trabajoDias = it },
                                onCrearTurno = onCrearTurno,
                                onEliminarTurno = onEliminarTurno,
                                onDuplicar = {
                                    onGuardarTrabajo(trabajoFechas, trabajoInicio, trabajoFin, true, trabajoTurnoElegido?.nombre)
                                    avisoSemanaCopiada = true
                                },
                                puedeDuplicar = trabajoFechas.isNotEmpty(),
                                avisoSemanaCopiada = avisoSemanaCopiada
                            )

                            seleccionada.modo == ModoCategoria.HORAS -> FormularioHoras(
                                etiquetaCampo = etiquetaCampo(seleccionada),
                                onGuardar = { inicio, fin, etiqueta ->
                                    onGuardarBloques(listOf(bloqueDeCategoria(seleccionada, caregiver.id, fecha, inicio, fin, etiqueta)))
                                    onCerrar()
                                }
                            )

                            else -> FormularioRango(
                                fecha = fecha,
                                etiquetaCampo = etiquetaCampo(seleccionada),
                                onGuardar = { fechas, etiqueta ->
                                    onGuardarBloques(fechas.map { dia ->
                                        bloqueDeCategoria(
                                            seleccionada, caregiver.id, dia, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, etiqueta
                                        )
                                    })
                                    onCerrar()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (esTrabajo && !editandoCategorias) {
                TextButton(
                    onClick = {
                        onGuardarTrabajo(trabajoFechas, trabajoInicio, trabajoFin, false, trabajoTurnoElegido?.nombre)
                        onCerrar()
                    },
                    enabled = trabajoFechas.isNotEmpty()
                ) { Text("Guardar") }
            } else {
                TextButton(onClick = onCerrar) { Text("Cerrar") }
            }
        },
        dismissButton = if (esTrabajo && !editandoCategorias) {
            { TextButton(onClick = onCerrar) { Text("Cerrar") } }
        } else null
    )

    if (creandoCategoria) {
        DialogoCategoria(
            inicial = null,
            categorias = categorias,
            onGuardar = { nueva ->
                onGuardarCategoria(nueva)
                creandoCategoria = false
                editandoCategorias = false
                seleccionId = nueva.id // queda elegida en cuanto se recarga la lista
            },
            onEliminar = null,
            onCerrar = { creandoCategoria = false }
        )
    }

    categoriaEnEdicion?.let { categoria ->
        DialogoCategoria(
            inicial = categoria,
            categorias = categorias,
            onGuardar = { editada ->
                onGuardarCategoria(editada)
                categoriaEnEdicion = null
            },
            onEliminar = if (categoria.esBase) null else {
                {
                    onEliminarCategoria(categoria.id)
                    if (seleccionId == categoria.id) seleccionId = null
                    categoriaEnEdicion = null
                }
            },
            onCerrar = { categoriaEnEdicion = null }
        )
    }
}

@Composable
private fun FormularioTrabajo(
    fecha: LocalDate,
    turnos: List<TurnoTrabajo>,
    inicio: LocalTime,
    onInicioCambiado: (LocalTime) -> Unit,
    fin: LocalTime,
    onFinCambiado: (LocalTime) -> Unit,
    dias: Set<DayOfWeek>,
    onDiasCambiado: (Set<DayOfWeek>) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onDuplicar: () -> Unit,
    puedeDuplicar: Boolean,
    avisoSemanaCopiada: Boolean
) {
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
                    else { onInicioCambiado(turno.horaInicio); onFinCambiado(turno.horaFin) }
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
        BotonHora("Desde", inicio, Modifier.weight(1f)) { onInicioCambiado(it) }
        BotonHora("Hasta", fin, Modifier.weight(1f)) { onFinCambiado(it) }
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
                onInicioCambiado(i); onFinCambiado(f)
                nuevoTurnoAbierto = false
            },
            onCerrar = { nuevoTurnoAbierto = false }
        )
    }

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDiasCambiado(if (dias == DIAS_LABORABLES) setOf(fecha.dayOfWeek) else DIAS_LABORABLES) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = dias == DIAS_LABORABLES,
            onCheckedChange = { marcado -> onDiasCambiado(if (marcado) DIAS_LABORABLES else setOf(fecha.dayOfWeek)) }
        )
        Text("De lunes a viernes")
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
                    .clickable { onDiasCambiado(if (marcado) dias - dia else dias + dia) },
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

    Spacer(Modifier.height(12.dp))
    // En columna (no en fila): con los dos textos en horizontal, en un diálogo estrecho
    // el aviso se salía por el borde derecho y quedaba cortado.
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onDuplicar, enabled = puedeDuplicar) { Text("Duplicar a la semana siguiente") }
        if (avisoSemanaCopiada) {
            Text(
                "Semana copiada ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun FormularioHoras(
    etiquetaCampo: String,
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
        label = { Text(etiquetaCampo) },
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
    etiquetaCampo: String,
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
        label = { Text(etiquetaCampo) },
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
