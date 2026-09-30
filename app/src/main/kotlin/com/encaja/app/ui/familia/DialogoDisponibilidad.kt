@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Diálogo que se abre al tocar la casilla de un cuidador en un día concreto de la
// pantalla Familia. Arriba lista lo que ya hay ese día (con papelera); debajo se
// elige la categoría a añadir (las de serie y las propias de la familia, todas con
// el mismo formulario según vayan por horas o por días) y aparece su formulario.
// Las horas se eligen con el reloj de Material 3 y las fechas con su calendario.
// Las categorías se editan desde la tarjeta "Categorías" de la pantalla.

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
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.categoriaEn
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

/** Texto de ayuda del campo libre: el mismo para todas las categorías. */
@Suppress("UNUSED_PARAMETER")
private fun etiquetaCampo(categoria: CategoriaDisponibilidad): String = "Detalle (opcional)"

private fun nombreDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${fecha.dayOfMonth}"

private fun fechaCorta(fecha: LocalDate): String =
    "${fecha.dayOfMonth} ${fecha.month.getDisplayName(TextStyle.SHORT, ES)}"

/**
 * [titulo] es el nombre de la persona o de la unidad familiar. [caregiverIds] son las
 * personas a las que se aplica lo que se añada: una sola en una persona, o todos los
 * miembros en una unidad familiar. [nombres] sirve para indicar de quién es cada bloque
 * cuando hay más de una persona (unidad).
 */
@Composable
fun DialogoDisponibilidad(
    titulo: String,
    caregiverIds: List<CaregiverId>,
    nombres: Map<CaregiverId, String>,
    fecha: LocalDate,
    lunes: LocalDate,
    bloquesDelDia: List<AvailabilityBlock>,
    turnos: List<TurnoTrabajo>,
    categorias: List<CategoriaDisponibilidad>,
    onEliminar: (AvailabilityBlock) -> Unit,
    // Guarda un tramo por horas de una categoría en varias fechas (sustituyendo lo que
    // ya hubiera de esa categoría esos días) y, si se pide, también la semana siguiente.
    onGuardarHoras: (caregiverIds: List<CaregiverId>, categoria: CategoriaDisponibilidad, fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, duplicarSemanaSiguiente: Boolean, etiqueta: String?) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime, categoriaId: CategoriaId) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onGuardarBloques: (List<AvailabilityBlock>) -> Unit,
    onGuardarCategoria: (CategoriaDisponibilidad) -> Unit,
    onCerrar: () -> Unit
) {
    // Se guarda el id (no la categoría) para ver siempre su versión más reciente tras editarla.
    var seleccionId by remember { mutableStateOf<CategoriaId?>(null) }
    val seleccionada = categorias.firstOrNull { it.id == seleccionId }
    val porHoras = seleccionada?.modo == ModoCategoria.HORAS
    var creandoCategoria by remember { mutableStateOf(false) }
    // Bloque de "Este día" que se está editando: al guardar se borra el original y se
    // guarda el nuevo en su lugar (solo para su persona, aunque estemos en una unidad).
    var bloqueEnEdicion by remember { mutableStateOf<AvailabilityBlock?>(null) }
    val destinatarios = bloqueEnEdicion?.let { listOf(it.caregiverId) } ?: caregiverIds

    // Estado del formulario por horas, elevado aquí (en vez de dentro de FormularioHoras)
    // para poder mostrar "Guardar" fuera de la zona con scroll, al mismo nivel que "Cerrar".
    // Se reinicia al cambiar de categoría (key más abajo) con el primer horario guardado
    // de esa categoría, si lo hay.
    val turnosDeLaCategoria = seleccionada?.let { cat -> turnos.filter { it.esDe(cat) } }.orEmpty()
    var horasInicio by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaInicio ?: LocalTime.of(9, 0)) }
    var horasFin by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaFin ?: LocalTime.of(10, 0)) }
    var horasTodoElDia by remember(seleccionId) { mutableStateOf(false) }
    var horasDias by remember(seleccionId) { mutableStateOf(setOf(fecha.dayOfWeek)) }
    var horasEtiqueta by remember(seleccionId) { mutableStateOf("") }
    var avisoSemanaCopiada by remember { mutableStateOf(false) }

    val horasFechas = fechasDeLaSemana(lunes, horasDias)
    val horasValidas = horasTodoElDia || horasInicio != horasFin
    val (inicioAGuardar, finAGuardar) =
        if (horasTodoElDia) AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA else horasInicio to horasFin
    fun guardarHoras(duplicar: Boolean) {
        val cat = seleccionada ?: return
        bloqueEnEdicion?.let { onEliminar(it) }
        onGuardarHoras(destinatarios, cat, horasFechas, inicioAGuardar, finAGuardar, duplicar, horasEtiqueta.trim().ifBlank { null })
        bloqueEnEdicion = null
    }

    // Al tocar "editar" en un bloque: se elige su categoría y, una vez el formulario se ha
    // reiniciado con ella (los remember(seleccionId) de arriba), se rellena con sus datos.
    LaunchedEffect(bloqueEnEdicion) {
        val bloque = bloqueEnEdicion ?: return@LaunchedEffect
        horasTodoElDia = bloque.todoElDia
        if (!bloque.todoElDia) { horasInicio = bloque.horaInicio; horasFin = bloque.horaFin }
        horasEtiqueta = bloque.etiqueta.orEmpty()
        horasDias = setOf(fecha.dayOfWeek)
    }

    LaunchedEffect(avisoSemanaCopiada) {
        if (avisoSemanaCopiada) {
            delay(2000)
            avisoSemanaCopiada = false
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("$titulo · ${nombreDia(fecha)}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (bloquesDelDia.isNotEmpty()) {
                    Text("Este día", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    bloquesDelDia.sortedWith(compareBy({ it.caregiverId.value }, { it.horaInicio })).forEach { bloque ->
                        val categoria = bloque.categoriaEn(categorias)
                        val editando = bloqueEnEdicion == bloque
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (editando) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(categoria.color)))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${categoria.emoji} ${categoria.nombre} · ${textoHorario(bloque)}")
                                val detalle = listOfNotNull(
                                    nombres[bloque.caregiverId].takeIf { caregiverIds.size > 1 },
                                    bloque.etiqueta?.takeIf { it.isNotBlank() },
                                    "no ocupa".takeIf { !categoria.bloquea }
                                ).joinToString(" · ")
                                if (detalle.isNotEmpty()) {
                                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            // Editar: carga el bloque en el formulario de abajo; al guardar se sustituye.
                            IconButton(onClick = {
                                if (editando) {
                                    bloqueEnEdicion = null
                                } else {
                                    seleccionId = categoria.id
                                    bloqueEnEdicion = bloque
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = if (editando) "Dejar de editar" else "Editar", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { if (editando) bloqueEnEdicion = null; onEliminar(bloque) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar", modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    if (bloqueEnEdicion != null) "Editar" else if (caregiverIds.size > 1) "Añadir a todos" else "Añadir",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categorias.forEach { categoria ->
                        FilterChip(
                            selected = categoria.id == seleccionId,
                            onClick = {
                                bloqueEnEdicion = null
                                seleccionId = if (seleccionId == categoria.id) null else categoria.id
                            },
                            label = { Text("${categoria.emoji} ${categoria.nombre}") },
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

                if (seleccionada != null) {
                    // key: al cambiar de categoría, el formulario empieza de cero.
                    key(seleccionada.id) {
                        if (porHoras) {
                            FormularioHoras(
                                categoria = seleccionada,
                                fecha = fecha,
                                turnos = turnosDeLaCategoria,
                                inicio = horasInicio,
                                onInicioCambiado = { horasInicio = it },
                                fin = horasFin,
                                onFinCambiado = { horasFin = it },
                                todoElDia = horasTodoElDia,
                                onTodoElDiaCambiado = { horasTodoElDia = it },
                                etiqueta = horasEtiqueta,
                                onEtiquetaCambiada = { horasEtiqueta = it },
                                dias = horasDias,
                                onDiasCambiado = { horasDias = it },
                                onCrearTurno = { nombre, i, f -> onCrearTurno(nombre, i, f, seleccionada.id) },
                                onEliminarTurno = onEliminarTurno,
                                onDuplicar = {
                                    guardarHoras(duplicar = true)
                                    avisoSemanaCopiada = true
                                },
                                puedeDuplicar = horasFechas.isNotEmpty() && horasValidas,
                                avisoSemanaCopiada = avisoSemanaCopiada
                            )
                        } else {
                            FormularioRango(
                                fecha = fecha,
                                etiquetaCampo = etiquetaCampo(seleccionada),
                                etiquetaInicial = bloqueEnEdicion?.etiqueta.orEmpty(),
                                onGuardar = { fechas, etiqueta ->
                                    bloqueEnEdicion?.let { onEliminar(it) }
                                    onGuardarBloques(destinatarios.flatMap { caregiverId ->
                                        fechas.map { dia ->
                                            bloqueDeCategoria(
                                                seleccionada, caregiverId, dia, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, etiqueta
                                            )
                                        }
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
            if (porHoras) {
                TextButton(
                    onClick = {
                        guardarHoras(duplicar = false)
                        onCerrar()
                    },
                    enabled = horasFechas.isNotEmpty() && horasValidas
                ) { Text("Guardar") }
            } else {
                TextButton(onClick = onCerrar) { Text("Cerrar") }
            }
        },
        dismissButton = if (porHoras) {
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
                seleccionId = nueva.id // queda elegida en cuanto se recarga la lista
            },
            onEliminar = null,
            onCerrar = { creandoCategoria = false }
        )
    }
}

/**
 * Formulario estándar de cualquier categoría por horas: los horarios guardados de esa
 * categoría (para elegir de un toque, crear nuevos o borrarlos), las horas de este día
 * o "todo el día", un detalle opcional, los días de la semana en que se aplica y la
 * opción de duplicarlo a la semana siguiente.
 */
@Composable
private fun FormularioHoras(
    categoria: CategoriaDisponibilidad,
    fecha: LocalDate,
    turnos: List<TurnoTrabajo>,
    inicio: LocalTime,
    onInicioCambiado: (LocalTime) -> Unit,
    fin: LocalTime,
    onFinCambiado: (LocalTime) -> Unit,
    todoElDia: Boolean,
    onTodoElDiaCambiado: (Boolean) -> Unit,
    etiqueta: String,
    onEtiquetaCambiada: (String) -> Unit,
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
    val turnoElegido = if (todoElDia) null else turnoConHoras(turnos, inicio, fin)
    // Si se borran todos los turnos estando en modo edición, se sale solo de ese modo.
    val enEdicion = editandoTurnos && turnos.isNotEmpty()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Horarios guardados", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (turnos.isNotEmpty()) {
            TextButton(onClick = { editandoTurnos = !enEdicion }) {
                Text(if (enEdicion) "Hecho" else "Editar")
            }
        }
    }
    if (turnos.isEmpty()) {
        Text(
            "Aún no hay horarios guardados de ${categoria.nombre}. Crea uno (p.ej. \"Mañana 6-14\") para elegirlo de un toque.",
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
                    else { onTodoElDiaCambiado(false); onInicioCambiado(turno.horaInicio); onFinCambiado(turno.horaFin) }
                },
                label = { Text("${turno.nombre} · ${formatearHora(turno.horaInicio)}-${formatearHora(turno.horaFin)}") },
                trailingIcon = if (enEdicion) {
                    { Icon(Icons.Default.Close, contentDescription = "Borrar horario", modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        if (!enEdicion) {
            AssistChip(
                onClick = { nuevoTurnoAbierto = true },
                label = { Text("Nuevo horario") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("Horas de este día", style = MaterialTheme.typography.labelMedium)
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onTodoElDiaCambiado(!todoElDia) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todoElDia, onCheckedChange = { onTodoElDiaCambiado(it) })
        Text("Todo el día")
    }
    if (!todoElDia) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonHora("Desde", inicio, Modifier.weight(1f)) { onInicioCambiado(it) }
            BotonHora("Hasta", fin, Modifier.weight(1f)) { onFinCambiado(it) }
        }
        if (inicio == fin) {
            Text(
                "La hora de fin debe ser distinta de la de inicio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!fin.isAfter(inicio)) {
            Text(
                "Turno de noche: termina al día siguiente a las ${formatearHora(fin)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (turnoElegido == null && inicio != fin) {
            TextButton(onClick = { nuevoTurnoAbierto = true }) { Text("Guardar estas horas como horario") }
        }
    }

    if (nuevoTurnoAbierto) {
        DialogoNuevoTurno(
            inicioInicial = inicio,
            finInicial = fin,
            onCrear = { nombre, i, f ->
                onCrearTurno(nombre, i, f)
                onTodoElDiaCambiado(false); onInicioCambiado(i); onFinCambiado(f)
                nuevoTurnoAbierto = false
            },
            onCerrar = { nuevoTurnoAbierto = false }
        )
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = etiqueta,
        onValueChange = onEtiquetaCambiada,
        label = { Text(etiquetaCampo(categoria)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))
    Text("Días de esta semana", style = MaterialTheme.typography.labelMedium)
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
    Text(
        "Si alguno de esos días ya tenía ${categoria.nombre}, se sustituye.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

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
private fun FormularioRango(
    fecha: LocalDate,
    etiquetaCampo: String,
    etiquetaInicial: String = "",
    onGuardar: (fechas: List<LocalDate>, etiqueta: String?) -> Unit
) {
    var desde by remember { mutableStateOf(fecha) }
    var hasta by remember { mutableStateOf(fecha) }
    var etiqueta by remember(etiquetaInicial) { mutableStateOf(etiquetaInicial) }
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

/** Crear un horario con nombre y horas, que queda guardado para toda la familia. */
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
        title = { Text("Nuevo horario") },
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
            TextButton(onClick = { onCrear(nombre.trim(), inicio, fin) }, enabled = valido) { Text("Guardar horario") }
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
