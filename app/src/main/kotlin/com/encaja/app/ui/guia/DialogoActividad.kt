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
// Hoja inferior (ModalBottomSheet) con el mismo estilo que la de disponibilidad de
// Familia (DialogoDisponibilidad): tarjetas suaves, iconos en círculo, interruptores
// de acento... las piezas compartidas viven en ui/theme/ComponentesHoja.kt. La
// pantalla de crear y la de editar son el mismo composable, así que su aspecto es
// siempre idéntico; lo único que cambia es si aparece la papelera y el bloque de
// "esta y las siguientes" de una serie.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.CoverageNeedId
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.formatearHora
import com.encaja.app.ui.familia.millisUtcAFecha
import com.encaja.app.ui.theme.BotonCuadradoHoja
import com.encaja.app.ui.theme.BotonRedondoSuaveHoja
import com.encaja.app.ui.theme.CajaValorHoja
import com.encaja.app.ui.theme.FilaConInterruptorHoja
import com.encaja.app.ui.theme.IconoEnCirculoHoja
import com.encaja.app.ui.theme.TarjetaSeccionHoja
import com.encaja.app.ui.theme.SeparadorHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.bordeHoja
import com.encaja.app.ui.theme.casillaHoja
import com.encaja.app.ui.theme.indigoHoja
import com.encaja.app.ui.theme.lavandaHoja
import com.encaja.app.ui.theme.onAcentoHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
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
    var avisoLlevarMin by remember { mutableStateOf(actividad?.avisoLlevarMin) }
    var avisoRecogerMin by remember { mutableStateOf(actividad?.avisoRecogerMin) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var repitiendo by remember { mutableStateOf(false) }

    val fechaBase = actividad?.fecha ?: fecha

    // Repetición: se puede marcar tanto al crear como al editar una actividad
    // puntual (que todavía no pertenece a ningún grupo de repetición).
    var repetir by remember { mutableStateOf(false) }
    var diasRepeticion by remember { mutableStateOf(setOf(fechaBase.dayOfWeek)) }
    var hastaRepeticion by remember { mutableStateOf(fechaBase.plusWeeks(4)) }
    // Días sueltos añadidos a mano con el calendario, además del patrón semanal
    // (mismo sistema que "Repetir en otros días" de Familia).
    var fechasExtra by remember { mutableStateOf(emptySet<LocalDate>()) }

    // Al editar una ocurrencia de un grupo de repetición: si el cambio (o el
    // borrado) se aplica solo a ella, o también a las siguientes del grupo.
    val perteneceAGrupo = actividad?.grupoRepeticionId != null
    var aplicarATodaLaSerie by remember { mutableStateOf(false) }

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = if (!perteneceAGrupo && repetir) {
        // Al editar, fechaBase es la fecha de la actividad original: se incluye siempre,
        // marque o no el usuario su día de la semana, para no dejarla huérfana (con un
        // hueco sin cubrir de la actividad vieja) al pasar a repetirla.
        (fechasRepetidas(fechaBase, hastaRepeticion, diasRepeticion) + fechaBase + fechasExtra).distinct().sorted()
    } else {
        listOf(fechaBase)
    }
    val puedeGuardar = childId != null && descripcion.isNotBlank() && horasValidas && fechasAGuardar.isNotEmpty()

    fun guardar() {
        val id = childId ?: return
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
                grupoRepeticionId = actividad?.grupoRepeticionId,
                avisoLlevarMin = avisoLlevarMin.takeIf { requiereDesplazamiento },
                avisoRecogerMin = avisoRecogerMin.takeIf { requiereDesplazamiento }
            ),
            aplicarATodaLaSerie
        )
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
                        append(if (actividad == null) "Nueva " else "Editar ")
                        withStyle(SpanStyle(color = acentoHoja)) { append("actividad") }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tintaHoja,
                    modifier = Modifier.weight(1f)
                )
                BotonRedondoSuaveHoja(Icons.Default.Close, "Cerrar", onCerrar)
            }
            Spacer(Modifier.height(14.dp))

            if (ninos.isEmpty()) {
                Text(
                    "Antes hay que dar de alta a algún niño desde Ajustes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tintaSuaveHoja
                )
                return@Column
            }

            // ── Niño/a ────────────────────────────────────────────────────────────
            Text("Niño/a", style = MaterialTheme.typography.labelLarge, color = tintaSuaveHoja)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ninos.forEach { nino ->
                    CasillaNino(nino = nino, elegido = childId == nino.id, onClick = { childId = nino.id })
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── ¿Qué es? ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it.take(40) },
                placeholder = { Text("¿Qué es? (p.ej. Fútbol, Recoger del cole)", color = tintaSuaveHoja) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = tintaSuaveHoja) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = casillaHoja,
                    unfocusedContainerColor = casillaHoja,
                    focusedBorderColor = acentoHoja,
                    unfocusedBorderColor = bordeHoja.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))

            // ── Desde / Hasta ─────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CajaHoraActividad("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                CajaHoraActividad("Hasta", fin, Modifier.weight(1f)) { fin = it }
            }
            if (!horasValidas) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "La hora de fin debe ser posterior a la de inicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(14.dp))

            // ── Requiere acompañamiento ───────────────────────────────────────────
            TarjetaSeccionHoja(padding = 0.dp) {
                FilaConInterruptorHoja(
                    icono = Icons.AutoMirrored.Filled.DirectionsWalk,
                    titulo = "Requiere acompañamiento",
                    subtitulo = if (requiereDesplazamiento) "Alguien tiene que llevarla o recogerla."
                        else "Solo informativa: no hace falta que nadie la acompañe.",
                    activo = requiereDesplazamiento,
                    onCambiar = { requiereDesplazamiento = it }
                )
            }

            if (requiereDesplazamiento && responsables.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FilaResponsable(
                    titulo = "Quién la lleva",
                    icono = Icons.Default.Person,
                    fondoIcono = MaterialTheme.colorScheme.tertiaryContainer,
                    tintaIcono = MaterialTheme.colorScheme.onTertiaryContainer,
                    responsables = responsables,
                    elegidoId = quienLlevaId,
                    onElegir = { quienLlevaId = it }
                )
                Spacer(Modifier.height(10.dp))
                FilaResponsable(
                    titulo = "Quién la recoge",
                    icono = Icons.Default.Home,
                    fondoIcono = lavandaHoja,
                    tintaIcono = indigoHoja,
                    responsables = responsables,
                    elegidoId = quienRecogeId,
                    onElegir = { quienRecogeId = it }
                )
            }

            if (requiereDesplazamiento) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoEnCirculoHoja(Icons.Default.Notifications, fondo = MaterialTheme.colorScheme.tertiaryContainer, tinta = MaterialTheme.colorScheme.onTertiaryContainer, tamano = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Avisos en el móvil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
                }
                Spacer(Modifier.height(10.dp))
                TarjetaSeccionHoja(padding = 0.dp) {
                    SelectorAviso(
                        titulo = "Antes de llevar",
                        detalle = "empieza a las ${formatearHoraActividad(inicio)}",
                        minutos = avisoLlevarMin,
                        onElegir = { avisoLlevarMin = it }
                    )
                    SeparadorHoja()
                    SelectorAviso(
                        titulo = "Antes de recoger",
                        detalle = "termina a las ${formatearHoraActividad(fin)}",
                        minutos = avisoRecogerMin,
                        onElegir = { avisoRecogerMin = it }
                    )
                }
            }

            if (perteneceAGrupo) {
                Spacer(Modifier.height(14.dp))
                TarjetaSeccionHoja {
                    Text(
                        "Esta actividad se repite cada semana. ¿A qué aplicar los cambios?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = tintaHoja
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !aplicarATodaLaSerie,
                            onClick = { aplicarATodaLaSerie = false },
                            label = { Text("Solo este día") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = acentoHoja, selectedLabelColor = onAcentoHoja)
                        )
                        FilterChip(
                            selected = aplicarATodaLaSerie,
                            onClick = { aplicarATodaLaSerie = true },
                            label = { Text("Esta y las siguientes") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = acentoHoja, selectedLabelColor = onAcentoHoja)
                        )
                    }
                    if (aplicarATodaLaSerie) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Se aplicará a esta actividad y a todas las posteriores de la serie (sin cambiar los días en que se repite).",
                            style = MaterialTheme.typography.bodySmall,
                            color = tintaSuaveHoja
                        )
                    }
                }
            } else if (actividad == null) {
                // Solo al crear una actividad nueva: al editar una ya existente que no
                // pertenece a ningún grupo, esto se hace con el botón "Repetir" de abajo.
                Spacer(Modifier.height(14.dp))
                TarjetaSeccionHoja(padding = 0.dp) {
                    FilaConInterruptorHoja(
                        icono = Icons.Default.Repeat,
                        titulo = "Repetir cada semana",
                        subtitulo = if (repetir) "Elige los días y, si quieres, fechas sueltas" else "Solo este día",
                        activo = repetir,
                        onCambiar = { activo ->
                            repetir = activo
                            if (!activo) fechasExtra = emptySet()
                        }
                    )
                    if (repetir) {
                        SeparadorHoja()
                        Column(modifier = Modifier.padding(14.dp)) {
                            SelectorDiasRepeticion(
                                fechaBase = fechaBase,
                                diasRepeticion = diasRepeticion,
                                onDiasChange = { diasRepeticion = it },
                                hastaRepeticion = hastaRepeticion,
                                onHastaChange = { hastaRepeticion = it },
                                fechasExtra = fechasExtra,
                                onFechasExtraChange = { fechasExtra = it },
                                fechasAGuardar = fechasAGuardar
                            )
                        }
                    }
                }
            }

            if (actividad != null) {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { repitiendo = true },
                    enabled = !(perteneceAGrupo && patronSerieCargando)
                ) { Text("Repetir en otro día", color = acentoHoja) }
            }

            // ── Botones: Cancelar (texto), papelera (solo editando) y disquete ───────
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCerrar,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = casillaHoja, contentColor = tintaHoja)
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (onEliminar != null) {
                    BotonCuadradoHoja(
                        icono = Icons.Default.Delete,
                        descripcion = "Borrar",
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        tinta = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { confirmarBorrado = true }
                    )
                }
                BotonCuadradoHoja(
                    icono = Icons.Default.Save,
                    descripcion = "Guardar",
                    fondo = if (puedeGuardar) acentoHoja else acentoHoja.copy(alpha = 0.35f),
                    tinta = onAcentoHoja,
                    habilitado = puedeGuardar,
                    onClick = { guardar(); onCerrar() }
                )
            }
        }
    }

    if (confirmarBorrado && onEliminar != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text(if (aplicarATodaLaSerie) "¿Borrar esta y las siguientes?" else "¿Borrar esta actividad?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; onEliminar(aplicarATodaLaSerie); onCerrar() }) {
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
                            quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento },
                            avisoLlevarMin = avisoLlevarMin.takeIf { requiereDesplazamiento },
                            avisoRecogerMin = avisoRecogerMin.takeIf { requiereDesplazamiento }
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
                                quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento },
                                avisoLlevarMin = avisoLlevarMin.takeIf { requiereDesplazamiento },
                                avisoRecogerMin = avisoRecogerMin.takeIf { requiereDesplazamiento }
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

/* ───────────────────────────── Piezas del diseño ───────────────────────────── */

/** Paleta estable (siempre el mismo color para el mismo nombre) para los avatares de
 * los niños, ya que Child no guarda un color propio. */
private val PALETA_NINOS = listOf(
    Color(0xFF34D399), Color(0xFFA78BFA), Color(0xFFF59E0B),
    Color(0xFF60A5FA), Color(0xFFF472B6), Color(0xFF38BDF8)
)

private fun colorParaNino(nombre: String): Color {
    val indice = nombre.hashCode().let { if (it < 0) -it else it } % PALETA_NINOS.size
    return PALETA_NINOS[indice]
}

/** Casilla "Niño/a": avatar con su inicial (coloreado de forma estable por nombre) y su
 * nombre; la elegida lleva el borde y el texto en su color. */
@Composable
private fun CasillaNino(nino: Child, elegido: Boolean, onClick: () -> Unit) {
    val color = colorParaNino(nino.nombre)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (elegido) color.copy(alpha = 0.18f) else casillaHoja)
            .border(1.dp, if (elegido) color else bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(nino.nombre.take(1).uppercase(), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            nino.nombre,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (elegido) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (elegido) color else tintaHoja
        )
    }
}

/** Fila "Quién la lleva / recoge": icono en círculo, el nombre elegido (o "Nadie") y una
 * flecha; al tocarla se elige en una lista (mismo patrón que SelectorAviso). */
@Composable
private fun FilaResponsable(
    titulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    fondoIcono: Color,
    tintaIcono: Color,
    responsables: List<Responsable>,
    elegidoId: String?,
    onElegir: (String?) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val etiquetaElegida = responsables.firstOrNull { it.idTexto == elegidoId }?.etiqueta ?: "Nadie"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(casillaHoja)
            .border(1.dp, bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { abierto = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoEnCirculoHoja(icono, fondo = fondoIcono, tinta = tintaIcono, tamano = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
            Text(etiquetaElegida, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
        }
        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = tintaSuaveHoja)
    }

    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(titulo) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onElegir(null); abierto = false }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = elegidoId == null, onClick = { onElegir(null); abierto = false })
                        Text("Nadie")
                    }
                    responsables.forEach { responsable ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onElegir(responsable.idTexto); abierto = false }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = responsable.idTexto == elegidoId, onClick = { onElegir(responsable.idTexto); abierto = false })
                            Text(responsable.etiqueta)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { abierto = false }) { Text("Cerrar") } }
        )
    }
}

/** Caja "Desde / 09:00" con reloj a la derecha; al tocarla se abre el reloj de Material 3. */
@Composable
private fun CajaHoraActividad(titulo: String, hora: LocalTime, modifier: Modifier = Modifier, onCambiar: (LocalTime) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValorHoja(titulo, formatearHoraActividad(hora), Icons.Default.Schedule, modifier) { abierto = true }
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

/**
 * Diálogo aparte que abre el botón "Repetir en otro día". Tiene dos usos distintos,
 * según si la actividad que se estaba editando ya pertenece a un grupo de repetición
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
    // Días sueltos añadidos a mano con el calendario, además del patrón semanal.
    var fechasExtra by remember { mutableStateOf(emptySet<LocalDate>()) }

    // Cambiar el patrón de una serie que ya existe es distinto de copiar la actividad a
    // otro día suelto: aquí no se elige un día de destino, se compara el nuevo patrón
    // (desde la fecha de la actividad que se estaba editando) con lo que ya hay.
    val modoPatron = perteneceAGrupo && repetirCadaSemana

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = when {
        modoPatron -> (fechasRepetidas(fechaInicial, hastaRepeticion, diasRepeticion) + fechasExtra).distinct().sorted()
        repetirCadaSemana -> (fechasRepetidas(fecha, hastaRepeticion, diasRepeticion) + fecha + fechasExtra).distinct().sorted()
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
                    modifier = Modifier.fillMaxWidth().clickable {
                        repetirCadaSemana = !repetirCadaSemana
                        if (!repetirCadaSemana) fechasExtra = emptySet()
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = repetirCadaSemana, onCheckedChange = { marcado ->
                        repetirCadaSemana = marcado
                        if (!marcado) fechasExtra = emptySet()
                    })
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
                        fechaBase = fechaInicial,
                        diasRepeticion = diasRepeticion,
                        onDiasChange = { diasRepeticion = it },
                        hastaRepeticion = hastaRepeticion,
                        onHastaChange = { hastaRepeticion = it },
                        fechasExtra = fechasExtra,
                        onFechasExtraChange = { fechasExtra = it },
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
 * repetición nueva; debajo, el calendario mensual para añadir fechas sueltas. */
@Composable
private fun SelectorDiasRepeticion(
    fechaBase: LocalDate,
    diasRepeticion: Set<DayOfWeek>,
    onDiasChange: (Set<DayOfWeek>) -> Unit,
    hastaRepeticion: LocalDate,
    onHastaChange: (LocalDate) -> Unit,
    fechasExtra: Set<LocalDate>,
    onFechasExtraChange: (Set<LocalDate>) -> Unit,
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

    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    Text("O añade días concretos con el calendario", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))
    CalendarioRepeticion(
        mesInicial = YearMonth.from(fechaBase),
        seleccionadas = fechasExtra,
        onAlternar = { dia ->
            onFechasExtraChange(if (dia in fechasExtra) fechasExtra - dia else fechasExtra + dia)
        }
    )
}

/**
 * Calendario de un mes (con flechas para cambiar de mes) para añadir días sueltos, en
 * cualquier combinación, además del patrón semanal de [SelectorDiasRepeticion] — mismo
 * sistema que "Repetir en otros días" de Familia.
 */
@Composable
private fun CalendarioRepeticion(mesInicial: YearMonth, seleccionadas: Set<LocalDate>, onAlternar: (LocalDate) -> Unit) {
    var mes by remember { mutableStateOf(mesInicial) }
    val hoy = LocalDate.now()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { mes = mes.minusMonths(1) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(
            mes.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${mes.year}",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { mes = mes.plusMonths(1) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dia ->
            Text(
                dia.getDisplayName(TextStyle.NARROW, ES).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(Modifier.height(4.dp))

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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (marcado) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(
                                    width = if (dia == hoy) 2.dp else 0.dp,
                                    color = if (dia == hoy) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAlternar(dia) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dia.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (marcado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (seleccionadas.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            if (seleccionadas.size == 1) "1 día suelto añadido." else "${seleccionadas.size} días sueltos añadidos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Botón que muestra una hora y, al tocarlo, abre el reloj de Material 3 (usado dentro
 * de "Repetir en otro día", que mantiene un estilo de diálogo más simple). */
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

/** Opciones de antelación de un aviso, en minutos (null = sin aviso). */
private val OPCIONES_AVISO: List<Int?> = listOf(null, 5, 10, 15, 30, 45, 60, 90, 120)

private fun textoAviso(minutos: Int?): String = when {
    minutos == null -> "Sin aviso"
    minutos < 60 -> "$minutos min antes"
    minutos % 60 == 0 -> if (minutos == 60) "1 hora antes" else "${minutos / 60} horas antes"
    else -> "${minutos / 60} h ${minutos % 60} min antes"
}

private fun formatearHoraActividad(hora: LocalTime): String =
    "${hora.hour.toString().padStart(2, '0')}:${hora.minute.toString().padStart(2, '0')}"

/**
 * Fila "Antes de llevar · empieza a las 17:00 — Sin aviso"; al tocarla se elige la
 * antelación en una lista. El aviso llega como notificación al móvil (ver
 * ProgramadorDeAvisos).
 */
@Composable
private fun SelectorAviso(titulo: String, detalle: String, minutos: Int?, onElegir: (Int?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { abierto = true }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tintaHoja)
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
        }
        Text(
            textoAviso(minutos),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (minutos == null) tintaSuaveHoja else acentoHoja
        )
    }
    if (abierto) {
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(titulo) },
            text = {
                Column {
                    OPCIONES_AVISO.forEach { opcion ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onElegir(opcion); abierto = false },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = opcion == minutos, onClick = { onElegir(opcion); abierto = false })
                            Text(textoAviso(opcion))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { abierto = false }) { Text("Cerrar") } }
        )
    }
}
