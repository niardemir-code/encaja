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

import com.encaja.app.ui.theme.CajaFechaHoja
import com.encaja.app.ui.theme.CajaHoraHoja
import com.encaja.app.ui.theme.CalendarioMultipleHoja
import com.encaja.app.ui.theme.SelectorDiasSemanaHoja
import com.encaja.app.ui.theme.fechasParaDiaSemanaHoja
import com.encaja.app.ui.theme.SelectorHoraHoja
import com.encaja.app.ui.theme.SelectorFechaHoja
import com.encaja.app.ui.common.formatearHora
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
import com.encaja.app.ui.theme.BotonVariosDisquetesHoja
import com.encaja.app.ui.theme.CajaValorHoja
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.ConfirmarGuardadoHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
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
import com.encaja.app.ui.theme.tarjetaHoja
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
 * ocurrencia concreta.
 */
@Composable
fun DialogoActividad(
    fecha: LocalDate,
    ninos: List<Child>,
    actividad: CoverageNeed?,
    responsables: List<Responsable> = emptyList(),
    onGuardar: (needs: List<CoverageNeed>, aplicarATodaLaSerie: Boolean) -> Unit,
    onEliminar: ((aplicarATodaLaSerie: Boolean) -> Unit)?,
    // Busca todas las veces que se repite la actividad que se edita (ella incluida): según
    // devuelva más de una, se ofrece "Guardar toda la serie". Null = sin series (creando).
    buscarOcurrencias: (suspend (CoverageNeed) -> List<CoverageNeed>)? = null,
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
    // Qué se está pidiendo confirmar al guardar una actividad que pertenece a una serie:
    // false = solo esta actividad, true = la serie completa, null = nada pendiente.
    var confirmarGuardadoSerie by remember { mutableStateOf<Boolean?>(null) }
    var ocurrenciasSerie by remember { mutableStateOf<List<CoverageNeed>>(emptyList()) }
    LaunchedEffect(actividad?.id) {
        if (actividad != null && buscarOcurrencias != null) {
            ocurrenciasSerie = runCatching { buscarOcurrencias(actividad) }.getOrDefault(emptyList())
        }
    }
    val tieneSerie = ocurrenciasSerie.size > 1

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
        val fechasDelDia = fechasParaDiaSemanaHoja(fechaBase, dia, hastaRepetir).toSet()
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
        val antiguas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemanaHoja(fechaBase, dia, hastaAnterior) }.toSet()
        val nuevas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemanaHoja(fechaBase, dia, nuevaHasta) }.toSet()
        fechasRepetir = (fechasRepetir - antiguas) + nuevas
    }

    val horasValidas = fin.isAfter(inicio)
    val fechasAGuardar = fechasRepetir.sorted()
    val puedeGuardar = childId != null && descripcion.isNotBlank() && horasValidas && fechasAGuardar.isNotEmpty()

    fun guardar(aplicarATodaLaSerie: Boolean = false) {
        val id = childId ?: return
        // Solo esta: se guarda esta actividad (y las fechas extra que se hayan marcado en
        // "Repetir"), manteniendo el grupo de la serie a la que ya pertenecía. Toda la
        // serie: se envía únicamente esta ocurrencia editada como plantilla, y el editor
        // copia sus cambios (todo menos la fecha) a todas las de la serie.
        // Toda la serie: se rehace cada ocurrencia (con su id y su fecha) con lo editado, y las
        // que aún no tuvieran grupo (de antes de existir las series) pasan a compartir uno.
        if (aplicarATodaLaSerie && actividad != null && ocurrenciasSerie.size > 1) {
            val grupo = actividad.grupoRepeticionId ?: UUID.randomUUID().toString()
            val ids = ocurrenciasSerie.map { it.id }.iterator()
            onGuardar(
                crearActividades(
                    fechas = ocurrenciasSerie.map { it.fecha },
                    childId = id, inicio = inicio, fin = fin, descripcion = descripcion,
                    requiereDesplazamiento = requiereDesplazamiento,
                    generarId = { ids.next() },
                    quienLlevaId = quienLlevaId.takeIf { requiereDesplazamiento },
                    quienRecogeId = quienRecogeId.takeIf { requiereDesplazamiento },
                    grupoRepeticionId = grupo,
                    avisoLlevarMin = avisoLlevarMin, avisoRecogerMin = avisoRecogerMin, icono = iconoId,
                    creadoPorId = actividad.creadoPorId
                ),
                false
            )
            return
        }
        val fechas = fechasAGuardar
        val idsPorFecha = fechas.iterator()
        onGuardar(
            crearActividades(
                fechas = fechas,
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
                // Los avisos son independientes de "Requiere acompañamiento": se pueden
                // usar como recordatorio aunque la actividad no necesite que nadie
                // lleve o recoja al niño.
                avisoLlevarMin = avisoLlevarMin,
                avisoRecogerMin = avisoRecogerMin,
                icono = iconoId,
                creadoPorId = actividad?.creadoPorId
            ),
            false
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Hueco bajo los botones para que no queden sobre la barra de navegación de Android (3
    // botones o gesto) y no se pulse «atrás» al tocar Guardar o Borrar. La hoja no siempre
    // informa de la altura de esa barra, así que se garantizan 48 dp (lo que mide la de 3
    // botones) más un margen de 28 dp, además de los 20 dp de relleno del final.
    val margenBarraNavegacion = maxOf(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), 48.dp) + 28.dp

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
                .padding(bottom = 20.dp + margenBarraNavegacion)
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
                CajaHoraHoja("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                CajaHoraHoja("Hasta", fin, Modifier.weight(1f)) { fin = it }
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
                    detalle = "empieza a las ${formatearHora(inicio)}",
                    minutos = avisoLlevarMin,
                    onElegir = { avisoLlevarMin = it }
                )
                SeparadorHoja()
                SelectorAviso(
                    titulo = "Antes de terminar",
                    detalle = "termina a las ${formatearHora(fin)}",
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
                    CalendarioMultipleHoja(
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
                    SelectorDiasSemanaHoja(
                        seleccionados = diasSemanaRepetir,
                        onAlternar = { dia -> alternarDiaSemana(dia) }
                    )
                    if (diasSemanaRepetir.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        CajaFechaHoja("Hasta", hastaRepetir, Modifier.fillMaxWidth()) { cambiarHastaRepetir(it) }
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
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = tarjetaHoja, contentColor = tintaHoja)
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
                val fondoGuardar = if (puedeGuardar) acentoHoja else acentoHoja.copy(alpha = 0.35f)
                BotonCuadradoHoja(
                    icono = Icons.Default.Save,
                    descripcion = if (tieneSerie) "Guardar solo esta" else "Guardar",
                    fondo = fondoGuardar,
                    tinta = onAcentoHoja,
                    habilitado = puedeGuardar,
                    // Con serie, primero se pide confirmación de qué se guarda.
                    onClick = { if (tieneSerie) confirmarGuardadoSerie = false else { guardar(); onCerrar() } }
                )
                if (tieneSerie) {
                    BotonVariosDisquetesHoja(
                        descripcion = "Guardar toda la serie",
                        fondo = fondoGuardar,
                        tinta = onAcentoHoja,
                        habilitado = puedeGuardar,
                        onClick = { confirmarGuardadoSerie = true }
                    )
                }
            }
        }
    }

    confirmarGuardadoSerie?.let { serieCompleta ->
        ConfirmarGuardadoHoja(
            titulo = if (serieCompleta) "Guardar la serie completa de la actividad" else "Guardar sólo esta actividad",
            detalle = if (serieCompleta) "Los cambios se aplicarán a todas las veces que se repite esta actividad."
            else "Los cambios solo afectarán a esta actividad; el resto de la serie no se toca.",
            onConfirmar = { confirmarGuardadoSerie = null; guardar(serieCompleta); onCerrar() },
            onCancelar = { confirmarGuardadoSerie = null },
            serie = serieCompleta
        )
    }

    if (confirmarBorrado && onEliminar != null) {
        val borrar: (Boolean) -> Unit = { serie -> confirmarBorrado = false; onEliminar(serie); onCerrar() }
        AvisoHoja(
            icono = Icons.Default.Delete,
            titulo = "¿Borrar esta actividad?",
            texto = if (tieneSerie) {
                "Se repite ${ocurrenciasSerie.size} veces. Puedes borrar solo esta o toda la serie. No se puede deshacer."
            } else {
                "No se puede deshacer."
            },
            acciones = if (tieneSerie) {
                listOf(
                    AccionAvisoHoja("Borrar toda la serie (${ocurrenciasSerie.size})", EstiloAccionAviso.PELIGRO, Icons.Default.Delete) { borrar(true) },
                    AccionAvisoHoja("Borrar solo esta", EstiloAccionAviso.SUAVE, Icons.Default.Delete) { borrar(false) },
                    AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { confirmarBorrado = false }
                )
            } else {
                listOf(
                    AccionAvisoHoja("Borrar", EstiloAccionAviso.PELIGRO, Icons.Default.Delete) { borrar(false) },
                    AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { confirmarBorrado = false }
                )
            },
            onCerrar = { confirmarBorrado = false }
        )
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
        AvisoHoja(
            icono = Icons.Default.Person,
            titulo = titulo,
            texto = null,
            peligro = false,
            conX = true,
            acciones = listOf(
                AccionAvisoHoja("Cerrar", EstiloAccionAviso.BORDE) { abierto = false }
            ),
            onCerrar = { abierto = false },
            contenido = {
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
            
            }
        )
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
        AvisoHoja(
            icono = Icons.Default.Notifications,
            titulo = titulo,
            texto = null,
            peligro = false,
            conX = true,
            acciones = listOf(
                AccionAvisoHoja("Cerrar", EstiloAccionAviso.BORDE) { abierto = false }
            ),
            onCerrar = { abierto = false },
            contenido = {
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
            
            }
        )
    }
}
