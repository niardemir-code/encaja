@file:OptIn(ExperimentalMaterial3Api::class)

package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Diálogo para crear o editar una actividad (CoverageNeed) de un niño desde la
// Guía del día: de quién es, qué es, a qué horas y si se repite en otros días.
// Hoja inferior (ModalBottomSheet) con el mismo estilo que la de disponibilidad de
// Familia (DialogoDisponibilidad) — las piezas compartidas viven en
// ui/theme/ComponentesHoja.kt. La pantalla de crear y la de editar son el mismo
// composable, así que su aspecto es siempre idéntico; lo único que cambia es si
// aparece la papelera.
// "Repetir en otros días" usa exactamente el mismo sistema que en Familia: un
// interruptor que despliega un calendario mensual (para marcar fechas sueltas a
// mano) y, debajo, un selector de días de la semana (L-D) que, al marcarlos,
// rellena automáticamente las próximas semanas de ese día en el propio
// calendario — ambos alimentan el mismo conjunto de fechas. Al guardar, cada
// fecha se convierte en una actividad independiente (la fecha original conserva
// su id si se está editando; el resto son nuevas), sin ningún concepto de
// "serie": no hay que elegir si el cambio o el borrado afecta solo a esta
// ocurrencia o también a las siguientes.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

private val ES = Locale("es")

/**
 * [actividad] null = crear una nueva (con opción de repetición); si no, se edita esa
 * ocurrencia concreta. [diasSerieActual], [hastaSerieActual], [patronSerieCargando] y
 * [onActualizarSerie] ya no los usa este diálogo (la repetición dejó de tener el
 * concepto de "serie"), pero se mantienen en la firma porque Guía y Semana todavía
 * los pasan; el día en que dejen de hacerlo, se pueden quitar de aquí también.
 */
@Composable
fun DialogoActividad(
    fecha: LocalDate,
    ninos: List<Child>,
    actividad: CoverageNeed?,
    responsables: List<Responsable> = emptyList(),
    diasSerieActual: Set<DayOfWeek>? = null,
    hastaSerieActual: LocalDate? = null,
    patronSerieCargando: Boolean = false,
    onGuardar: (needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) -> Unit,
    onEliminar: ((aplicarATodaLaSerie: Boolean) -> Unit)?,
    onActualizarSerie: ((plantilla: CoverageNeed, nuevasFechas: List<LocalDate>) -> Unit)? = null,
    onCerrar: () -> Unit
) {
    var childId by remember { mutableStateOf(actividad?.childId ?: ninos.firstOrNull()?.id) }
    var descripcion by remember { mutableStateOf(actividad?.descripcion ?: "") }
    var iconoId by remember { mutableStateOf(actividad?.icono) }
    var inicio by remember { mutableStateOf(actividad?.horaInicio ?: LocalTime.of(17, 0)) }
    var fin by remember { mutableStateOf(actividad?.horaFin ?: LocalTime.of(18, 0)) }
    var requiereDesplazamiento by remember { mutableStateOf(actividad?.requiereDesplazamiento ?: true) }
    var quienLlevaId by remember { mutableStateOf(actividad?.quienLlevaId) }
    var quienRecogeId by remember { mutableStateOf(actividad?.quienRecogeId) }
    var avisoLlevarMin by remember { mutableStateOf(actividad?.avisoLlevarMin) }
    var avisoRecogerMin by remember { mutableStateOf(actividad?.avisoRecogerMin) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val fechaBase = actividad?.fecha ?: fecha

    // Repetir en otros días: mismo sistema que en Familia (DialogoDisponibilidad) — un
    // calendario mensual para marcar fechas sueltas a mano, más un selector de días de
    // la semana que rellena automáticamente las próximas semanas de ese día.
    var fechasRepetir by remember { mutableStateOf(setOf(fechaBase)) }
    var repetirActivo by remember { mutableStateOf(false) }
    var diasSemanaRepetir by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    // Hasta qué día llega el patrón semanal; por defecto, 3 meses.
    var hastaRepetir by remember { mutableStateOf(fechaBase.plusMonths(3)) }

    fun alternarDiaSemana(dia: DayOfWeek) {
        val activando = dia !in diasSemanaRepetir
        diasSemanaRepetir = if (activando) diasSemanaRepetir + dia else diasSemanaRepetir - dia
        val fechasDelDia = fechasParaDiaSemana(fechaBase, dia, hastaRepetir).toSet()
        fechasRepetir = if (activando) {
            fechasRepetir + fechasDelDia
        } else {
            (fechasRepetir - fechasDelDia).ifEmpty { setOf(fechaBase) }
        }
    }

    // Al cambiar "Hasta": se recalculan las fechas que venían del patrón semanal (con el
    // rango antiguo) y se sustituyen por las del rango nuevo, sin tocar las que el
    // usuario haya marcado a mano en el calendario.
    fun cambiarHastaRepetir(nuevaHasta: LocalDate) {
        val hastaAnterior = hastaRepetir
        hastaRepetir = nuevaHasta
        if (diasSemanaRepetir.isEmpty()) return
        val antiguas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemana(fechaBase, dia, hastaAnterior) }.toSet()
        val nuevas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemana(fechaBase, dia, nuevaHasta) }.toSet()
        fechasRepetir = (fechasRepetir - antiguas) + nuevas
    }

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = fechasRepetir.sorted()
    val puedeGuardar = childId != null && descripcion.isNotBlank() && horasValidas && fechasAGuardar.isNotEmpty()

    fun guardar() {
        val id = childId ?: return
        // La fecha original (al editar) conserva su id; el resto de fechas del
        // conjunto de repetición son actividades nuevas e independientes — no hay
        // ningún grupo que las una.
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
                // Los avisos son independientes de "Requiere acompañamiento": se pueden
                // usar como recordatorio aunque la actividad no necesite que nadie
                // lleve o recoja al niño.
                avisoLlevarMin = avisoLlevarMin,
                avisoRecogerMin = avisoRecogerMin,
                icono = iconoId
            ),
            false
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        // La propia hoja se corta justo debajo de la barra de estado (hora, batería,
        // conexiones...) en vez de extenderse por detrás: si no, su fondo tapa lo que
        // hubiera ahí antes y los iconos del sistema (su color lo decide el tema
        // general del teléfono, no esta pantalla) pueden quedar ilegibles sobre él.
        modifier = Modifier.statusBarsPadding(),
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

            // ── Icono ─────────────────────────────────────────────────────────────
            Text("Icono", style = MaterialTheme.typography.labelLarge, color = tintaSuaveHoja)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IconoActividad.entries.toList()) { opcion ->
                    CasillaIconoActividad(
                        opcion = opcion,
                        elegido = iconoId == opcion.id,
                        onClick = { iconoId = if (iconoId == opcion.id) null else opcion.id }
                    )
                }
            }
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

            // Los avisos son independientes de "Requiere acompañamiento": sirven como
            // recordatorio del inicio/fin de la actividad aunque no haga falta que
            // nadie la lleve o la recoja.
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoEnCirculoHoja(Icons.Default.Notifications, fondo = MaterialTheme.colorScheme.tertiaryContainer, tinta = MaterialTheme.colorScheme.onTertiaryContainer, tamano = 34.dp)
                Spacer(Modifier.width(10.dp))
                Text("Avisos en el móvil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
            }
            Spacer(Modifier.height(10.dp))
            TarjetaSeccionHoja(padding = 0.dp) {
                SelectorAviso(
                    titulo = "Antes de empezar",
                    detalle = "empieza a las ${formatearHoraActividad(inicio)}",
                    minutos = avisoLlevarMin,
                    onElegir = { avisoLlevarMin = it }
                )
                SeparadorHoja()
                SelectorAviso(
                    titulo = "Antes de terminar",
                    detalle = "termina a las ${formatearHoraActividad(fin)}",
                    minutos = avisoRecogerMin,
                    onElegir = { avisoRecogerMin = it }
                )
            }

            // ── Repetir en otros días ─────────────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconoEnCirculoHoja(Icons.Default.CalendarMonth, fondo = lavandaHoja, tinta = indigoHoja)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Repetir en otros días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
                    Text(
                        if (repetirActivo) "Toca los días del calendario" else "Solo este día",
                        style = MaterialTheme.typography.bodySmall,
                        color = tintaSuaveHoja
                    )
                }
                Switch(
                    checked = repetirActivo,
                    onCheckedChange = { activo ->
                        repetirActivo = activo
                        if (!activo) {
                            fechasRepetir = setOf(fechaBase)
                            diasSemanaRepetir = emptySet()
                            hastaRepetir = fechaBase.plusMonths(3)
                        }
                    },
                    colors = com.encaja.app.ui.theme.coloresInterruptorEncaja()
                )
            }
            if (repetirActivo) {
                Spacer(Modifier.height(12.dp))
                TarjetaSeccionHoja(padding = 10.dp) {
                    CalendarioMultipleActividad(
                        mesInicial = YearMonth.from(fechaBase),
                        seleccionadas = fechasRepetir,
                        onAlternar = { dia ->
                            fechasRepetir = if (dia in fechasRepetir) {
                                if (fechasRepetir.size > 1) fechasRepetir - dia else fechasRepetir
                            } else {
                                fechasRepetir + dia
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    SeparadorHoja()
                    Spacer(Modifier.height(10.dp))
                    Text("O cada semana en", style = MaterialTheme.typography.bodyMedium, color = tintaSuaveHoja)
                    Spacer(Modifier.height(8.dp))
                    SelectorDiasSemanaActividad(
                        seleccionados = diasSemanaRepetir,
                        onAlternar = { dia -> alternarDiaSemana(dia) }
                    )
                    if (diasSemanaRepetir.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        CajaFechaActividad("Hasta", hastaRepetir, Modifier.fillMaxWidth()) { cambiarHastaRepetir(it) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = tintaSuaveHoja, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (fechasAGuardar.size == 1) "1 día" else "${fechasAGuardar.size} días") +
                            ". Si alguno ya tenía ${descripcion.trim().ifBlank { "esta actividad" }}, se sustituye.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tintaSuaveHoja
                    )
                }
            }

            // ── Botones: Cancelar (texto), papelera (solo editando) y disquete ───────
            Spacer(Modifier.height(20.dp))
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
            title = { Text("¿Borrar esta actividad?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrado = false; onEliminar(false); onCerrar() }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }
}

/* ───────────────────────────── Piezas del diseño ───────────────────────────── */

/** [desde] y sus apariciones de [dia] hasta [hasta] (ambas incluidas; incluye [desde]
 * mismo si coincide con [dia]) — igual que en Familia. */
private fun fechasParaDiaSemana(desde: LocalDate, dia: DayOfWeek, hasta: LocalDate): List<LocalDate> {
    val primera = desde.with(TemporalAdjusters.nextOrSame(dia))
    if (hasta.isBefore(primera)) return emptyList()
    return generateSequence(primera) { it.plusWeeks(1) }.takeWhile { !it.isAfter(hasta) }.toList()
}

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

/** Casilla redonda de una opción del catálogo de iconos; la elegida queda resaltada
 * en el color de acento. Tocar la ya elegida la deselecciona (vuelve al genérico). */
@Composable
private fun CasillaIconoActividad(opcion: IconoActividad, elegido: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (elegido) acentoHoja else casillaHoja)
            .border(1.dp, if (elegido) acentoHoja else bordeHoja.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            opcion.icono,
            contentDescription = opcion.etiqueta,
            tint = if (elegido) onAcentoHoja else tintaSuaveHoja,
            modifier = Modifier.size(24.dp)
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

/** Caja "Hasta / 2 ene" con calendario a la derecha; al tocarla se abre el calendario
 * de Material 3. Usada para el límite del patrón semanal de "Repetir en otros días". */
@Composable
private fun CajaFechaActividad(titulo: String, fecha: LocalDate, modifier: Modifier = Modifier, onCambiar: (LocalDate) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    CajaValorHoja(titulo, fechaCortaActividad(fecha), Icons.Default.CalendarMonth, modifier) { abierto = true }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtcActividad(fecha))
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambiar(millisUtcAFechaActividad(it)) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

private fun fechaCortaActividad(fecha: LocalDate): String =
    "${fecha.dayOfMonth} ${fecha.month.getDisplayName(TextStyle.SHORT, ES).removeSuffix(".")}"

private fun fechaAMillisUtcActividad(fecha: LocalDate): Long =
    fecha.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisUtcAFechaActividad(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()

/**
 * Calendario de un mes (con flechas para cambiar de mes) en el que se marcan los días
 * que se quieran; los marcados van en color de acento y el de hoy lleva borde. Igual
 * que CalendarioMultiple de Familia (DialogoDisponibilidad), duplicado aquí para no
 * acoplar los dos paquetes de UI.
 */
@Composable
private fun CalendarioMultipleActividad(mesInicial: YearMonth, seleccionadas: Set<LocalDate>, onAlternar: (LocalDate) -> Unit) {
    var mes by remember { mutableStateOf(mesInicial) }
    val hoy = LocalDate.now()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { mes = mes.minusMonths(1) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = indigoHoja)
        }
        Text(
            mes.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${mes.year}",
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
                letraDeDiaActividad(dia),
                style = MaterialTheme.typography.labelMedium,
                color = tintaSuaveHoja,
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
                        }
                    }
                }
            }
            repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** L M X J V S D (con X para el miércoles, para no repetir la M). */
private fun letraDeDiaActividad(dia: DayOfWeek): String = when (dia) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

/**
 * Fila de siete círculos (L a D) para marcar en qué días de la semana se repite la
 * actividad cada semana — además de, no en vez de, las fechas sueltas del calendario.
 * Igual que SelectorDiasSemana de Familia.
 */
@Composable
private fun SelectorDiasSemanaActividad(seleccionados: Set<DayOfWeek>, onAlternar: (DayOfWeek) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dia ->
            val marcado = dia in seleccionados
            Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (marcado) acentoHoja else Color.Transparent)
                        .border(width = 1.dp, color = if (marcado) acentoHoja else tintaSuaveHoja.copy(alpha = 0.35f), shape = CircleShape)
                        .clickable { onAlternar(dia) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letraDeDiaActividad(dia),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (marcado) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (marcado) onAcentoHoja else tintaHoja
                    )
                }
            }
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
