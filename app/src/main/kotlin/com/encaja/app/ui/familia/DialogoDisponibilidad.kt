@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Hoja inferior que se abre al tocar la casilla de un cuidador (o de una unidad
// familiar) en un día concreto de la pantalla Familia, con el diseño de la maqueta:
//  - "Actividad de este día": lo que ya hay ese día, con lápiz (editar) y papelera.
//  - "Categoría": rejilla de casillas (la elegida en color de acento con un check) y
//    "Nueva categoría" con borde discontinuo.
//  - "Horarios" (categorías por horas): todo el día, desde/hasta, guardar como horario,
//    detalle; y debajo "Repetir en otros días" con los días de la semana y "Duplicar a
//    la semana siguiente". Las categorías por días llevan "Fechas" (desde/hasta) y detalle.
//  - Abajo, Cancelar y Guardar.
// Las categorías se editan desde la tarjeta "Categorías" de la pantalla.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.encaja.app.domain.model.AvailabilityBlock
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.CategoriaId
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.categoriaEn
import com.encaja.app.domain.model.TurnoId
import com.encaja.app.domain.model.TurnoTrabajo
import com.encaja.app.ui.theme.LocalEncajaExtraColors
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

// Atajos a los colores del tema activo con los nombres del diseño.
private val TINTA: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TINTA_SUAVE: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val TARJETA: Color @Composable get() = LocalEncajaExtraColors.current.tarjetaSuave
private val CASILLA: Color @Composable get() = MaterialTheme.colorScheme.surface
private val BORDE: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val ACENTO: Color @Composable get() = LocalEncajaExtraColors.current.acento
private val ON_ACENTO: Color @Composable get() = LocalEncajaExtraColors.current.onAcento
private val LAVANDA: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val INDIGO: Color @Composable get() = MaterialTheme.colorScheme.primary

private fun nombreDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${fecha.dayOfMonth}"

private fun fechaCorta(fecha: LocalDate): String =
    "${fecha.dayOfMonth} ${fecha.month.getDisplayName(TextStyle.SHORT, ES).removeSuffix(".")}"

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
    // Bloque de "este día" que se está editando: al guardar se borra el original y se
    // guarda el nuevo en su lugar (solo para su persona, aunque estemos en una unidad).
    var bloqueEnEdicion by remember { mutableStateOf<AvailabilityBlock?>(null) }
    val destinatarios = bloqueEnEdicion?.let { listOf(it.caregiverId) } ?: caregiverIds

    // Estado del formulario, elevado aquí para que Guardar (abajo del todo) lo vea. Se
    // reinicia al cambiar de categoría con el primer horario guardado de esa categoría.
    val turnosDeLaCategoria = seleccionada?.let { cat -> turnos.filter { it.esDe(cat) } }.orEmpty()
    var horasInicio by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaInicio ?: LocalTime.of(9, 0)) }
    var horasFin by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaFin ?: LocalTime.of(10, 0)) }
    var horasTodoElDia by remember(seleccionId) { mutableStateOf(false) }
    var horasDias by remember(seleccionId) { mutableStateOf(setOf(fecha.dayOfWeek)) }
    var horasEtiqueta by remember(seleccionId) { mutableStateOf("") }
    var guardarComoHorario by remember(seleccionId) { mutableStateOf(false) }
    var nombreHorario by remember(seleccionId) { mutableStateOf("") }
    var rangoDesde by remember(seleccionId) { mutableStateOf(fecha) }
    var rangoHasta by remember(seleccionId) { mutableStateOf(fecha) }
    var avisoSemanaCopiada by remember { mutableStateOf(false) }

    val horasFechas = fechasDeLaSemana(lunes, horasDias)
    val horasValidas = horasTodoElDia || horasInicio != horasFin
    val (inicioAGuardar, finAGuardar) =
        if (horasTodoElDia) AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA else horasInicio to horasFin
    val rangoFechas = fechasEntre(rangoDesde, rangoHasta)
    val puedeGuardarHoras = porHoras && horasFechas.isNotEmpty() && horasValidas &&
        (!guardarComoHorario || horasTodoElDia || nombreHorario.isNotBlank())
    val puedeGuardarRango = seleccionada != null && !porHoras && rangoFechas.size <= MAX_DIAS_RANGO

    fun guardarHoras(duplicar: Boolean) {
        val cat = seleccionada ?: return
        if (guardarComoHorario && !horasTodoElDia && nombreHorario.isNotBlank() &&
            turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin) == null
        ) {
            onCrearTurno(nombreHorario.trim(), horasInicio, horasFin, cat.id)
        }
        bloqueEnEdicion?.let { onEliminar(it) }
        onGuardarHoras(destinatarios, cat, horasFechas, inicioAGuardar, finAGuardar, duplicar, horasEtiqueta.trim().ifBlank { null })
        bloqueEnEdicion = null
    }

    fun guardarRango() {
        val cat = seleccionada ?: return
        bloqueEnEdicion?.let { onEliminar(it) }
        onGuardarBloques(destinatarios.flatMap { caregiverId ->
            rangoFechas.map { dia ->
                bloqueDeCategoria(cat, caregiverId, dia, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, horasEtiqueta.trim().ifBlank { null })
            }
        })
        bloqueEnEdicion = null
    }

    // Al tocar el lápiz de un bloque: se elige su categoría y, una vez el formulario se ha
    // reiniciado con ella (los remember(seleccionId) de arriba), se rellena con sus datos.
    LaunchedEffect(bloqueEnEdicion) {
        val bloque = bloqueEnEdicion ?: return@LaunchedEffect
        horasTodoElDia = bloque.todoElDia
        if (!bloque.todoElDia) { horasInicio = bloque.horaInicio; horasFin = bloque.horaFin }
        horasEtiqueta = bloque.etiqueta.orEmpty()
        horasDias = setOf(fecha.dayOfWeek)
        rangoDesde = bloque.fecha
        rangoHasta = bloque.fecha
    }

    LaunchedEffect(avisoSemanaCopiada) {
        if (avisoSemanaCopiada) {
            delay(2000)
            avisoSemanaCopiada = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ── Título ────────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append(titulo)
                        withStyle(SpanStyle(color = TINTA_SUAVE)) { append(" · ") }
                        withStyle(SpanStyle(color = ACENTO)) { append(nombreDia(fecha)) }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TINTA,
                    modifier = Modifier.weight(1f)
                )
                BotonRedondoSuave(Icons.Default.Close, "Cerrar", onCerrar)
            }
            Spacer(Modifier.height(14.dp))

            // ── Actividad de este día ────────────────────────────────────────────
            if (bloquesDelDia.isNotEmpty()) {
                TarjetaSeccion {
                    Text(
                        if (bloquesDelDia.size == 1) "Actividad de este día" else "Actividades de este día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TINTA_SUAVE
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bloquesDelDia.sortedWith(compareBy({ it.caregiverId.value }, { it.horaInicio })).forEach { bloque ->
                            val categoria = bloque.categoriaEn(categorias)
                            val editando = bloqueEnEdicion == bloque
                            FilaBloqueDelDia(
                                categoria = categoria,
                                subtitulo = listOfNotNull(
                                    textoHorario(bloque),
                                    nombres[bloque.caregiverId].takeIf { caregiverIds.size > 1 },
                                    bloque.etiqueta?.takeIf { it.isNotBlank() },
                                    "no ocupa".takeIf { !categoria.bloquea }
                                ).joinToString(" · "),
                                editando = editando,
                                onEditar = {
                                    if (editando) {
                                        bloqueEnEdicion = null
                                    } else {
                                        seleccionId = categoria.id
                                        bloqueEnEdicion = bloque
                                    }
                                },
                                onEliminar = { if (editando) bloqueEnEdicion = null; onEliminar(bloque) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── Categoría ─────────────────────────────────────────────────────────
            TituloSeccion(
                if (bloqueEnEdicion == null && caregiverIds.size > 1) "Categoría (para todos)" else "Categoría",
                "Selecciona una categoría para esta actividad"
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categorias.forEach { categoria ->
                    CasillaCategoria(
                        categoria = categoria,
                        elegida = categoria.id == seleccionId,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            bloqueEnEdicion = null
                            seleccionId = if (seleccionId == categoria.id) null else categoria.id
                        }
                    )
                }
                CasillaNuevaCategoria(modifier = Modifier.weight(1f), onClick = { creandoCategoria = true })
            }

            // ── Formulario ────────────────────────────────────────────────────────
            if (seleccionada != null) {
                Spacer(Modifier.height(18.dp))
                key(seleccionada.id) {
                    if (porHoras) {
                        TituloSeccion("Horarios", "Define el horario de esta actividad")
                        Spacer(Modifier.height(10.dp))
                        TarjetaSeccion(padding = 0.dp) {
                            if (turnosDeLaCategoria.isNotEmpty()) {
                                HorariosGuardados(
                                    turnos = turnosDeLaCategoria,
                                    elegido = if (horasTodoElDia) null else turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin),
                                    onElegir = { horasTodoElDia = false; horasInicio = it.horaInicio; horasFin = it.horaFin },
                                    onEliminar = { onEliminarTurno(it.id) }
                                )
                                Separador()
                            }
                            FilaConInterruptor(
                                icono = Icons.Default.Schedule,
                                titulo = "Todo el día",
                                subtitulo = "Marca si dura todo el día",
                                activo = horasTodoElDia,
                                onCambiar = { horasTodoElDia = it }
                            )
                            if (!horasTodoElDia) {
                                Separador()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CajaHora("Desde", horasInicio, Modifier.weight(1f)) { horasInicio = it }
                                    CajaHora("Hasta", horasFin, Modifier.weight(1f)) { horasFin = it }
                                }
                                if (horasInicio == horasFin) {
                                    NotaSeccion("La hora de fin debe ser distinta de la de inicio.", error = true)
                                } else if (!horasFin.isAfter(horasInicio)) {
                                    NotaSeccion("Turno de noche: termina al día siguiente a las ${formatearHora(horasFin)}.")
                                }
                                if (turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin) == null) {
                                    Separador()
                                    FilaConInterruptor(
                                        icono = Icons.Default.BookmarkBorder,
                                        titulo = "Guardar como horario",
                                        subtitulo = "Guarda estas horas para usarlas más adelante",
                                        activo = guardarComoHorario,
                                        onCambiar = { guardarComoHorario = it },
                                        tintaIcono = ACENTO
                                    )
                                    if (guardarComoHorario) {
                                        CampoTexto(
                                            valor = nombreHorario,
                                            onCambiar = { nombreHorario = it.take(24) },
                                            placeholder = "Nombre del horario (p.ej. Mañana)",
                                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                                        )
                                    }
                                }
                            }
                            Separador()
                            FilaDetalle(valor = horasEtiqueta, onCambiar = { horasEtiqueta = it })
                        }

                        Spacer(Modifier.height(16.dp))
                        // Repetir en otros días
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconoEnCirculo(Icons.Default.CalendarMonth, fondo = LAVANDA, tinta = INDIGO)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Repetir en otros días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TINTA)
                                Text("Selecciona los días de esta semana", style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
                            }
                            Switch(
                                checked = horasDias == DIAS_LABORABLES,
                                onCheckedChange = { marcado -> horasDias = if (marcado) DIAS_LABORABLES else setOf(fecha.dayOfWeek) },
                                colors = coloresInterruptor()
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("De lunes a viernes", style = MaterialTheme.typography.bodySmall, color = TINTA)
                        }
                        Spacer(Modifier.height(12.dp))
                        SelectorDias(dias = horasDias, onCambiar = { horasDias = it })
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TINTA_SUAVE, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Si alguno de esos días ya tenía ${seleccionada.nombre}, se sustituye.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TINTA_SUAVE
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        FilaDuplicar(
                            habilitado = puedeGuardarHoras,
                            copiada = avisoSemanaCopiada,
                            onClick = {
                                guardarHoras(duplicar = true)
                                avisoSemanaCopiada = true
                            }
                        )
                    } else {
                        TituloSeccion("Fechas", "Uno o varios días completos")
                        Spacer(Modifier.height(10.dp))
                        TarjetaSeccion(padding = 0.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CajaFecha("Desde", rangoDesde, Modifier.weight(1f)) { rangoDesde = it; if (rangoHasta.isBefore(it)) rangoHasta = it }
                                CajaFecha("Hasta", rangoHasta, Modifier.weight(1f)) { rangoHasta = it }
                            }
                            NotaSeccion(
                                if (rangoFechas.size == 1) "1 día completo" else "${rangoFechas.size} días completos",
                                error = rangoFechas.size > MAX_DIAS_RANGO
                            )
                            Separador()
                            FilaDetalle(valor = horasEtiqueta, onCambiar = { horasEtiqueta = it })
                        }
                    }
                }
            }

            // ── Botones ───────────────────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCerrar,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = TARJETA, contentColor = TINTA)
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        if (porHoras) guardarHoras(duplicar = false) else guardarRango()
                        onCerrar()
                    },
                    enabled = puedeGuardarHoras || puedeGuardarRango,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ACENTO,
                        contentColor = ON_ACENTO,
                        disabledContainerColor = ACENTO.copy(alpha = 0.35f),
                        disabledContentColor = ON_ACENTO
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

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

/* ───────────────────────────── Piezas del diseño ───────────────────────────── */

@Composable
private fun TituloSeccion(titulo: String, subtitulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TINTA)
    Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = TINTA_SUAVE)
}

/** Tarjeta suave con borde tenue, base de "este día", "Horarios" y "Fechas". */
@Composable
private fun TarjetaSeccion(padding: Dp = 14.dp, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TARJETA)
            .border(1.dp, BORDE.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(padding),
        content = contenido
    )
}

@Composable
private fun Separador() {
    HorizontalDivider(color = BORDE.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
private fun NotaSeccion(texto: String, error: Boolean = false) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else TINTA_SUAVE,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
    )
}

@Composable
private fun BotonRedondoSuave(icono: ImageVector, descripcion: String, onClick: () -> Unit, tinta: Color = TINTA) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(TARJETA)
            .border(1.dp, BORDE.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = tinta, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun IconoEnCirculo(icono: ImageVector, fondo: Color, tinta: Color, tamano: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(tamano / 2))
    }
}

@Composable
private fun coloresInterruptor() = SwitchDefaults.colors(
    checkedThumbColor = ON_ACENTO,
    checkedTrackColor = ACENTO,
    checkedBorderColor = Color.Transparent
)

/** Un bloque ya apuntado ese día: punto de color, icono en cuadro, nombre y horario, lápiz y papelera. */
@Composable
private fun FilaBloqueDelDia(
    categoria: CategoriaDisponibilidad,
    subtitulo: String,
    editando: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CASILLA)
            .border(1.dp, if (editando) ACENTO else BORDE.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(categoria.color)))
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(categoria.color).copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(categoria.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(categoria.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TINTA)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
        }
        BotonRedondoSuave(Icons.Default.Edit, if (editando) "Dejar de editar" else "Editar", onEditar, tinta = if (editando) ACENTO else TINTA)
        Spacer(Modifier.width(6.dp))
        BotonRedondoSuave(Icons.Default.Delete, "Quitar", onEliminar, tinta = MaterialTheme.colorScheme.error)
    }
}

/** Casilla de categoría en la rejilla: emoji y nombre; la elegida, en color de acento con un check. */
@Composable
private fun CasillaCategoria(categoria: CategoriaDisponibilidad, elegida: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (elegida) ACENTO else CASILLA)
                .border(1.dp, if (elegida) ACENTO else BORDE.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(categoria.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                categoria.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (elegida) ON_ACENTO else TINTA,
                maxLines = 2
            )
        }
        if (elegida) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ACENTO)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ON_ACENTO, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun CasillaNuevaCategoria(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colorBorde = TINTA_SUAVE.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .drawBehind {
                val radio = 16.dp.toPx()
                drawRoundRect(
                    color = colorBorde,
                    cornerRadius = CornerRadius(radio, radio),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = TINTA, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Nueva categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TINTA, maxLines = 2)
    }
}

/** Los horarios guardados de la categoría, como chips; con "Editar" se borran de un toque. */
@Composable
private fun HorariosGuardados(
    turnos: List<TurnoTrabajo>,
    elegido: TurnoTrabajo?,
    onElegir: (TurnoTrabajo) -> Unit,
    onEliminar: (TurnoTrabajo) -> Unit
) {
    var editando by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Horarios guardados", style = MaterialTheme.typography.labelLarge, color = TINTA_SUAVE, modifier = Modifier.weight(1f))
            TextButton(onClick = { editando = !editando }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if (editando) "Hecho" else "Editar", color = ACENTO)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            turnos.forEach { turno ->
                FilterChip(
                    selected = !editando && turno == elegido,
                    onClick = { if (editando) onEliminar(turno) else onElegir(turno) },
                    label = { Text("${turno.nombre} · ${formatearHora(turno.horaInicio)}-${formatearHora(turno.horaFin)}") },
                    trailingIcon = if (editando) {
                        { Icon(Icons.Default.Close, contentDescription = "Borrar horario", modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ACENTO, selectedLabelColor = ON_ACENTO)
                )
            }
        }
    }
}

/** Fila con icono en círculo, título, subtítulo e interruptor a la derecha. */
@Composable
private fun FilaConInterruptor(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    onCambiar: (Boolean) -> Unit,
    tintaIcono: Color = INDIGO
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCambiar(!activo) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoEnCirculo(icono, fondo = tintaIcono.copy(alpha = 0.18f), tinta = tintaIcono)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TINTA)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
        }
        Switch(checked = activo, onCheckedChange = onCambiar, colors = coloresInterruptor())
    }
}

/** "Detalle (opcional)" con su icono y el campo de texto debajo. */
@Composable
private fun FilaDetalle(valor: String, onCambiar: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        IconoEnCirculo(Icons.Default.Description, fondo = LAVANDA, tinta = INDIGO)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Detalle (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TINTA)
            Spacer(Modifier.height(6.dp))
            CampoTexto(valor = valor, onCambiar = onCambiar, placeholder = "Añade un detalle…")
        }
    }
}

@Composable
private fun CampoTexto(valor: String, onCambiar: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambiar,
        placeholder = { Text(placeholder, color = TINTA_SUAVE) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CASILLA,
            unfocusedContainerColor = CASILLA,
            focusedBorderColor = ACENTO,
            unfocusedBorderColor = BORDE.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Caja "Desde / 09:00" con reloj a la derecha; al tocarla se abre el reloj de Material 3. */
@Composable
private fun CajaHora(titulo: String, hora: LocalTime, modifier: Modifier = Modifier, onCambiar: (LocalTime) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValor(titulo, formatearHora(hora), Icons.Default.Schedule, modifier) { abierto = true }
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

/** Caja "Desde / 29 sept" con calendario a la derecha; al tocarla se abre el calendario. */
@Composable
private fun CajaFecha(titulo: String, fecha: LocalDate, modifier: Modifier = Modifier, onCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValor(titulo, fechaCorta(fecha), Icons.Default.CalendarMonth, modifier) { abierto = true }
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

@Composable
private fun CajaValor(titulo: String, valor: String, icono: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CASILLA)
            .border(1.dp, BORDE.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TINTA, maxLines = 1)
        }
        IconoEnCirculo(icono, fondo = LAVANDA, tinta = INDIGO, tamano = 34.dp)
    }
}

/** Los siete días en círculos; los marcados, en color de acento. */
@Composable
private fun SelectorDias(dias: Set<DayOfWeek>, onCambiar: (Set<DayOfWeek>) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.values().forEach { dia ->
            val marcado = dia in dias
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (marcado) ACENTO else CASILLA)
                    .border(1.dp, if (marcado) ACENTO else BORDE.copy(alpha = 0.6f), CircleShape)
                    .clickable { onCambiar(if (marcado) dias - dia else dias + dia) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letraDeDia(dia),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (marcado) ON_ACENTO else TINTA
                )
            }
        }
    }
}

/** L M X J V S D (con X para el miércoles, como en la maqueta, para no repetir la M). */
private fun letraDeDia(dia: DayOfWeek): String = when (dia) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

/** Fila "Duplicar a la semana siguiente" en tono de acento, con flecha. */
@Composable
private fun FilaDuplicar(habilitado: Boolean, copiada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ACENTO.copy(alpha = 0.16f))
            .border(1.dp, ACENTO.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoEnCirculo(Icons.Default.ContentCopy, fondo = ACENTO.copy(alpha = 0.25f), tinta = ACENTO, tamano = 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            if (copiada) "Semana copiada ✓" else "Duplicar a la semana siguiente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (habilitado) ACENTO else ACENTO.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ACENTO)
    }
}
