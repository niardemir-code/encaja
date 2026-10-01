@file:OptIn(ExperimentalMaterial3Api::class)

package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Diálogo para crear o editar una actividad (CoverageNeed) de un niño desde la
// Guía del día: de quién es, qué es, a qué horas y si se repite en más días de
// la semana hasta una fecha (p.ej. "fútbol todos los martes hasta final de
// curso"). Si la actividad todavía no pertenece a ningún grupo de repetición,
// la casilla "Repetir cada semana" la crea como serie nueva. Si ya pertenece a
// una, "Esta y las siguientes" copia los demás cambios (niño, descripción,
// horario, acompañamiento) a ella y a las ocurrencias posteriores del grupo,
// sin tocar los días en que se repite la serie — para eso está el botón
// "Repetir": abre un diálogo aparte donde se elige un día y hora (y,
// opcionalmente, que se repita cada semana en cualquier combinación de días)
// para dar de alta una actividad o serie independiente con los mismos datos,
// sin partir de cero.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import com.encaja.app.ui.theme.coloresInterruptorEncaja
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.formatearHora
import com.encaja.app.ui.familia.millisUtcAFecha
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private val ES = Locale("es")

/**
 * [actividad] null = crear una nueva (con opción de repetición); si no, se edita esa
 * ocurrencia concreta. Si pertenece a un grupo de repetición (se creó con "Repetir
 * cada semana"), se puede elegir aplicar el cambio o el borrado solo a ella o
 * también a las siguientes ocurrencias del grupo — [onGuardar] y [onEliminar] llevan
 * ese booleano como segundo parámetro; "esta y las siguientes" copia los demás
 * cambios (niño, descripción, horario, acompañamiento) sin tocar los días en que se
 * repite la serie. El botón "Repetir" (solo al editar) abre un diálogo aparte para
 * dar de alta, con los datos actuales, una actividad o serie independiente en otro
 * día/hora y, si se quiere, cada semana en los días que se elijan.
 */
@Composable
fun DialogoActividad(
    fecha: LocalDate,
    ninos: List<Child>,
    actividad: CoverageNeed?,
    responsables: List<Responsable> = emptyList(),
    // Patrón real (días de la semana y última fecha) con el que se repite hoy la serie
    // de [actividad], si ya pertenece a un grupo; se usa para preseleccionar el
    // selector de días al abrir "Repetir". Null mientras no se conoce todavía (se
    // consulta de forma asíncrona) o si la actividad no pertenece a ningún grupo.
    diasSerieActual: Set<DayOfWeek>? = null,
    hastaSerieActual: LocalDate? = null,
    // true mientras se está pidiendo el patrón real de la serie (patronDeSerie) y
    // [actividad] pertenece a un grupo: mientras tanto se deshabilita "Repetir" para no
    // dejar que el usuario elija días sobre el patrón provisional y que luego, al
    // llegar el real, sus cambios queden pisados sin que se dé cuenta.
    patronSerieCargando: Boolean = false,
    onGuardar: (needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) -> Unit,
    onEliminar: ((aplicarATodaLaSerie: Boolean) -> Unit)?,
    // Cambia el patrón semanal de la serie a la que pertenece [actividad] (se llama
    // desde "Repetir" al marcar "Repetir cada semana" sobre una ocurrencia que ya
    // pertenece a un grupo): crea lo que no existía, conserva lo que coincide y borra
    // lo que ya no encaje en el nuevo patrón, sin duplicar nada.
    onActualizarSerie: ((plantilla: CoverageNeed, nuevasFechas: List<LocalDate>) -> Unit)? = null,
    onCerrar: () -> Unit
) {
    var childId by remember { mutableStateOf(actividad?.childId ?: ninos.firstOrNull()?.id) }
    var descripcion by remember { mutableStateOf(actividad?.descripcion ?: "") }
    var inicio by remember { mutableStateOf(actividad?.horaInicio ?: LocalTime.of(17, 0)) }
    var fin by remember { mutableStateOf(actividad?.horaFin ?: LocalTime.of(18, 0)) }
    var requiereDesplazamiento by remember { mutableStateOf(actividad?.requiereDesplazamiento ?: true) }
    var quienLlevaId by remember { mutableStateOf(actividad?.quienLlevaId) }
    var quienRecogeId by remember { mutableStateOf(actividad?.quienRecogeId) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var repitiendo by remember { mutableStateOf(false) }

    val fechaBase = actividad?.fecha ?: fecha

    // Repetición: se puede marcar tanto al crear como al editar una actividad
    // puntual (que todavía no pertenece a ningún grupo de repetición).
    var repetir by remember { mutableStateOf(false) }
    var diasRepeticion by remember { mutableStateOf(setOf(fechaBase.dayOfWeek)) }
    var hastaRepeticion by remember { mutableStateOf(fechaBase.plusWeeks(4)) }

    // Al editar una ocurrencia de un grupo de repetición: si el cambio (o el
    // borrado) se aplica solo a ella, o también a las siguientes del grupo.
    val perteneceAGrupo = actividad?.grupoRepeticionId != null
    var aplicarATodaLaSerie by remember { mutableStateOf(false) }

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = if (!perteneceAGrupo && repetir) {
        // Al editar, fechaBase es la fecha de la actividad original: se incluye siempre,
        // marque o no el usuario su día de la semana, para no dejarla huérfana (con un
        // hueco sin cubrir de la actividad vieja) al pasar a repetirla.
        (fechasRepetidas(fechaBase, hastaRepeticion, diasRepeticion) + fechaBase).distinct().sorted()
    } else {
        listOf(fechaBase)
    }
    val puedeGuardar = childId != null && descripcion.isNotBlank() && horasValidas && fechasAGuardar.isNotEmpty()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (actividad == null) "Nueva actividad" else "Editar actividad") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (ninos.isEmpty()) {
                    Text(
                        "Antes hay que dar de alta a algún niño desde Ajustes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@Column
                }

                Text("Niño/a", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ninos.forEach { nino ->
                        FilterChip(
                            selected = childId == nino.id,
                            onClick = { childId = nino.id },
                            label = { Text(nino.nombre) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it.take(40) },
                    label = { Text("¿Qué es? (p.ej. Fútbol, Recoger del cole)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonHoraActividad("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                    BotonHoraActividad("Hasta", fin, Modifier.weight(1f)) { fin = it }
                }
                if (!horasValidas) {
                    Text(
                        "La hora de fin debe ser posterior a la de inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { requiereDesplazamiento = !requiereDesplazamiento },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Requiere acompañamiento")
                        Text(
                            if (requiereDesplazamiento) "Alguien tiene que llevarla o recogerla."
                            else "Solo informativa: no hace falta que nadie la acompañe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = requiereDesplazamiento, onCheckedChange = { requiereDesplazamiento = it }, colors = coloresInterruptorEncaja())
                }

                if (requiereDesplazamiento && responsables.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SelectorResponsable(
                        titulo = "Quién la lleva",
                        responsables = responsables,
                        elegidoId = quienLlevaId,
                        onElegir = { quienLlevaId = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    SelectorResponsable(
                        titulo = "Quién la recoge",
                        responsables = responsables,
                        elegidoId = quienRecogeId,
                        onElegir = { quienRecogeId = it }
                    )
                }

                if (perteneceAGrupo) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Esta actividad se repite cada semana. ¿A qué aplicar los cambios?", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !aplicarATodaLaSerie,
                            onClick = { aplicarATodaLaSerie = false },
                            label = { Text("Solo este día") }
                        )
                        FilterChip(
                            selected = aplicarATodaLaSerie,
                            onClick = { aplicarATodaLaSerie = true },
                            label = { Text("Esta y las siguientes") }
                        )
                    }
                    if (aplicarATodaLaSerie) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Se aplicará a esta actividad y a todas las posteriores de la serie (sin cambiar los días en que se repite).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (actividad == null) {
                    // Solo al crear una actividad nueva: al editar una ya existente que no
                    // pertenece a ningún grupo, esto se hace con el botón "Repetir" de abajo.
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { repetir = !repetir },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = repetir, onCheckedChange = { repetir = it })
                        Text("Repetir cada semana")
                    }
                    if (repetir) {
                        SelectorDiasRepeticion(
                            diasRepeticion = diasRepeticion,
                            onDiasChange = { diasRepeticion = it },
                            hastaRepeticion = hastaRepeticion,
                            onHastaChange = { hastaRepeticion = it },
                            fechasAGuardar = fechasAGuardar
                        )
                    }
                }

                if (onEliminar != null || actividad != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (actividad != null) {
                            TextButton(
                                onClick = { repitiendo = true },
                                enabled = !(perteneceAGrupo && patronSerieCargando)
                            ) { Text("Repetir") }
                        }
                        if (onEliminar != null) {
                            TextButton(onClick = { confirmarBorrado = true }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(6.dp))
                                Text("Borrar actividad", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = childId
                    if (id != null) {
                        // Si al editar una actividad puntual se marca "Repetir cada semana",
                        // fechasAGuardar pasa a tener varias fechas: la ocurrencia que coincide
                        // con la fecha original conserva su id, y el resto son nuevas.
                        val idsPorFecha = fechasAGuardar.iterator()
                        onGuardar(
                            crearActividades(
                                fechas = fechasAGuardar,
                                childId = id,
                                inicio = inicio,
                                fin = fin,
                                descripcion = descripcion,
                                requiereDesplazamiento = requiereDesplazamiento,
                                generarId = {
                                    val fechaActual = idsPorFecha.next()
                                    if (actividad != null && fechaActual == actividad.fecha) actividad.id
                                    else CoverageNeedId(UUID.randomUUID().toString())
                                },
                                quienLlevaId = quienLlevaId.takeIf { requiereDesplazamiento },
                                quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento },
                                grupoRepeticionId = actividad?.grupoRepeticionId
                            ),
                            aplicarATodaLaSerie
                        )
                    }
                },
                enabled = puedeGuardar
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )

    if (confirmarBorrado && onEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(if (aplicarATodaLaSerie) "¿Borrar esta y las siguientes?" else "¿Borrar esta actividad?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; onEliminar(aplicarATodaLaSerie) }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }

    if (repitiendo) {
        val id = childId
        if (id != null) {
            DialogoRepetirActividad(
                fechaInicial = fechaBase,
                inicioInicial = inicio,
                finInicial = fin,
                perteneceAGrupo = perteneceAGrupo,
                diasSerieActual = diasSerieActual,
                hastaSerieActual = hastaSerieActual,
                onConfirmar = { fechas, i, f, modoPatron ->
                    if (modoPatron && actividad != null && onActualizarSerie != null) {
                        // Cambio del patrón semanal de la serie: se compara con las
                        // ocurrencias reales a partir de esta fecha (lo hace
                        // actualizarSerie), así que aquí no hace falta generar ids a
                        // mano ni preocuparse por duplicados.
                        val plantilla = actividad.copy(
                            descripcion = descripcion.trim(),
                            horaInicio = i,
                            horaFin = f,
                            requiereDesplazamiento = requiereDesplazamiento,
                            quienLlevaId = quienLlevaId.takeIf { requiereDesplazamiento },
                            quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento }
                        )
                        onActualizarSerie(plantilla, fechas)
                    } else {
                        // Copia independiente a otro día/hora (no toca la serie original).
                        // Si el patrón elegido incluye la propia fecha de la actividad que
                        // se estaba editando, esa fecha no es una ocurrencia nueva: es la
                        // misma actividad de siempre, así que se reutiliza su id (queda
                        // absorbida) en vez de crear una duplicada ese día.
                        val idsPorFecha = fechas.iterator()
                        onGuardar(
                            crearActividades(
                                fechas = fechas,
                                childId = id,
                                inicio = i,
                                fin = f,
                                descripcion = descripcion,
                                requiereDesplazamiento = requiereDesplazamiento,
                                generarId = {
                                    val fechaActual = idsPorFecha.next()
                                    if (actividad != null && fechaActual == actividad.fecha) actividad.id
                                    else CoverageNeedId(UUID.randomUUID().toString())
                                },
                                quienLlevaId = quienLlevaId.takeIf { requiereDesplazamiento },
                                quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento }
                            ),
                            false
                        )
                    }
                    repitiendo = false
                },
                onCerrar = { repitiendo = false }
            )
        }
    }
}

/**
 * Diálogo aparte que abre el botón "Repetir". Tiene dos usos distintos, según si la
 * actividad que se estaba editando ya pertenece a un grupo de repetición
 * ([perteneceAGrupo]):
 * - Copiar a otro día/hora: se elige un día y hora nuevos (por defecto, los de la
 *   actividad original) y, opcionalmente, que se repita cada semana en cualquier
 *   combinación de días hasta una fecha. Da de alta una actividad o serie
 *   independiente con esos datos; no toca la actividad original ni su serie.
 * - Si [perteneceAGrupo] es true y se marca "Repetir cada semana": en vez de elegir un
 *   día concreto, se cambia el propio patrón semanal de la serie a partir de la fecha
 *   de la actividad que se estaba editando (preseleccionado con [diasSerieActual] y
 *   [hastaSerieActual], el patrón real de la serie); los días que no coincidan se crean
 *   o se borran, y los que coincidan se conservan sin duplicarse.
 * [onConfirmar] recibe las fechas resultantes, la hora y si el modo elegido fue el de
 * cambiar el patrón de la serie (para que quien llama sepa si debe usar
 * onActualizarSerie o crear una copia independiente).
 */
@Composable
private fun DialogoRepetirActividad(
    fechaInicial: LocalDate,
    inicioInicial: LocalTime,
    finInicial: LocalTime,
    perteneceAGrupo: Boolean,
    diasSerieActual: Set<DayOfWeek>?,
    hastaSerieActual: LocalDate?,
    onConfirmar: (fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, modoPatron: Boolean) -> Unit,
    onCerrar: () -> Unit
) {
    var fecha by remember { mutableStateOf(fechaInicial) }
    var inicio by remember { mutableStateOf(inicioInicial) }
    var fin by remember { mutableStateOf(finInicial) }
    var repetirCadaSemana by remember { mutableStateOf(false) }
    var diasRepeticion by remember(diasSerieActual) {
        mutableStateOf(diasSerieActual ?: setOf(fechaInicial.dayOfWeek))
    }
    var hastaRepeticion by remember(hastaSerieActual) {
        mutableStateOf(hastaSerieActual ?: fechaInicial.plusWeeks(4))
    }

    // Cambiar el patrón de una serie que ya existe es distinto de copiar la actividad a
    // otro día suelto: aquí no se elige un día de destino, se compara el nuevo patrón
    // (desde la fecha de la actividad que se estaba editando) con lo que ya hay.
    val modoPatron = perteneceAGrupo && repetirCadaSemana

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = when {
        modoPatron -> fechasRepetidas(fechaInicial, hastaRepeticion, diasRepeticion)
        repetirCadaSemana -> (fechasRepetidas(fecha, hastaRepeticion, diasRepeticion) + fecha).distinct().sorted()
        else -> listOf(fecha)
    }
    val puedeConfirmar = horasValidas && fechasAGuardar.isNotEmpty()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Repetir actividad") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Día y hora", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                if (modoPatron) {
                    Text(
                        "Se aplicará a partir del ${fechaInicial.dayOfMonth} " +
                            "${fechaInicial.month.getDisplayName(TextStyle.SHORT, ES)} ${fechaInicial.year}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    BotonFechaActividad("Día", fecha, Modifier.fillMaxWidth()) { fecha = it }
                    Spacer(Modifier.height(8.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonHoraActividad("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                    BotonHoraActividad("Hasta", fin, Modifier.weight(1f)) { fin = it }
                }
                if (!horasValidas) {
                    Text(
                        "La hora de fin debe ser posterior a la de inicio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { repetirCadaSemana = !repetirCadaSemana },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = repetirCadaSemana, onCheckedChange = { repetirCadaSemana = it })
                    Text("Repetir cada semana")
                }
                if (perteneceAGrupo && repetirCadaSemana) {
                    Text(
                        "Cambia los días en que se repite esta serie a partir de esa fecha: se " +
                            "crean los días marcados que no existían y se borran los que se " +
                            "desmarquen, sin duplicar los que ya coincidían.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (repetirCadaSemana) {
                    SelectorDiasRepeticion(
                        diasRepeticion = diasRepeticion,
                        onDiasChange = { diasRepeticion = it },
                        hastaRepeticion = hastaRepeticion,
                        onHastaChange = { hastaRepeticion = it },
                        fechasAGuardar = fechasAGuardar
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(fechasAGuardar, inicio, fin, modoPatron) },
                enabled = puedeConfirmar
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

/** Círculos de día de la semana (los 7) + fecha de fin, para elegir el patrón de una
 * repetición nueva. */
@Composable
private fun SelectorDiasRepeticion(
    diasRepeticion: Set<DayOfWeek>,
    onDiasChange: (Set<DayOfWeek>) -> Unit,
    hastaRepeticion: LocalDate,
    onHastaChange: (LocalDate) -> Unit,
    fechasAGuardar: List<LocalDate>
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.values().forEach { dia ->
            val marcado = dia in diasRepeticion
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (marcado) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable {
                        onDiasChange(if (marcado) diasRepeticion - dia else diasRepeticion + dia)
                    },
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
    Spacer(Modifier.height(8.dp))
    BotonFechaActividad("Hasta", hastaRepeticion, Modifier.fillMaxWidth()) { onHastaChange(it) }
    Spacer(Modifier.height(4.dp))
    Text(
        when {
            fechasAGuardar.isEmpty() -> "Ningún día de la semana elegida cae en ese rango."
            fechasAGuardar.size == 1 -> "1 actividad en total."
            else -> "${fechasAGuardar.size} actividades en total."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Menú desplegable para elegir quién (persona o subgrupo familiar) lleva o recoge al niño. */
@Composable
private fun SelectorResponsable(
    titulo: String,
    responsables: List<Responsable>,
    elegidoId: String?,
    onElegir: (String?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val etiquetaElegida = responsables.firstOrNull { it.idTexto == elegidoId }?.etiqueta ?: "Nadie"

    Text(titulo, style = MaterialTheme.typography.labelMedium)
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = etiquetaElegida,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text("Nadie") },
                onClick = { onElegir(null); expandido = false }
            )
            responsables.forEach { responsable ->
                DropdownMenuItem(
                    text = { Text(responsable.etiqueta) },
                    onClick = { onElegir(responsable.idTexto); expandido = false }
                )
            }
        }
    }
}

/** Botón que muestra una hora y, al tocarlo, abre el reloj de Material 3 para cambiarla. */
@Composable
private fun BotonHoraActividad(titulo: String, hora: LocalTime, modifier: Modifier = Modifier, onCambiar: (LocalTime) -> Unit) {
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
private fun BotonFechaActividad(titulo: String, fecha: LocalDate, modifier: Modifier = Modifier, onCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = modifier) {
        Text("$titulo ${fecha.dayOfMonth} ${fecha.month.getDisplayName(TextStyle.SHORT, ES)} ${fecha.year}")
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
