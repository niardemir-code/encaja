@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose (Material 3), no compilado en este entorno.
// Hoja inferior que se abre al tocar la casilla de un cuidador (o de una unidad
// familiar) en un día concreto de la pantalla Familia, con el diseño de la maqueta:
//  - "Actividad de este día": lo que ya hay ese día; tocar una la edita. Si solo hay
//    una, la hoja se abre ya editándola: sin esta lista ni la rejilla de categorías,
//    directamente en los horarios.
//  - "Categoría" (solo al añadir): rejilla de casillas (la elegida en color de acento
//    con un check) y "Nueva categoría" con borde discontinuo.
//  - "Horarios" (categorías por horas): todo el día, desde/hasta, guardar como horario,
//    detalle; y debajo "Repetir en otros días", un interruptor que despliega un
//    calendario mensual para marcar los días. Las categorías por días llevan "Fechas"
//    (desde/hasta) y detalle.
//  - Abajo, Cancelar, papelera (si se está editando) y disquete (guardar). Nada se
//    guarda ni se borra hasta pulsar uno de los dos.
// Las categorías se editan desde la tarjeta "Categorías" de la pantalla.

import com.encaja.app.ui.theme.BotonCuadradoHoja
import com.encaja.app.ui.theme.BotonRedondoSuaveHoja
import com.encaja.app.ui.theme.CajaFechaHoja
import com.encaja.app.ui.theme.CajaHoraHoja
import com.encaja.app.ui.theme.CalendarioMultipleHoja
import com.encaja.app.ui.theme.CampoTextoHoja
import com.encaja.app.ui.theme.FilaConInterruptorHoja
import com.encaja.app.ui.theme.IconoEnCirculoHoja
import com.encaja.app.ui.theme.NotaSeccionHoja
import com.encaja.app.ui.theme.SelectorDiasSemanaHoja
import com.encaja.app.ui.theme.SeparadorHoja
import com.encaja.app.ui.theme.TarjetaSeccionHoja
import com.encaja.app.ui.theme.TituloSeccionHoja
import com.encaja.app.ui.theme.acentoHoja
import com.encaja.app.ui.theme.bordeHoja
import com.encaja.app.ui.theme.casillaHoja
import com.encaja.app.ui.theme.fechasParaDiaSemanaHoja
import com.encaja.app.ui.theme.indigoHoja
import com.encaja.app.ui.theme.lavandaHoja
import com.encaja.app.ui.theme.onAcentoHoja
import com.encaja.app.ui.theme.tarjetaHoja
import com.encaja.app.ui.theme.tintaHoja
import com.encaja.app.ui.theme.tintaSuaveHoja
import com.encaja.app.ui.theme.SelectorHoraHoja
import com.encaja.app.ui.theme.SelectorFechaHoja
import com.encaja.app.ui.common.formatearHora
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
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
import com.encaja.app.ui.theme.BotonVariosDisquetesHoja
import com.encaja.app.ui.theme.ConfirmarGuardadoHoja
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import com.encaja.app.ui.theme.coloresInterruptorEncaja
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

private fun nombreDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " ${fecha.dayOfMonth}"

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
    bloquesDelDia: List<AvailabilityBlock>,
    turnos: List<TurnoTrabajo>,
    categorias: List<CategoriaDisponibilidad>,
    onEliminar: (AvailabilityBlock) -> Unit,
    // Guarda un tramo por horas de una categoría en varias fechas (sustituyendo lo que
    // ya hubiera de esa categoría esos días) y, si se pide, también la semana siguiente.
    onGuardarHoras: (caregiverIds: List<CaregiverId>, categoria: CategoriaDisponibilidad, fechas: List<LocalDate>, inicio: LocalTime, fin: LocalTime, duplicarSemanaSiguiente: Boolean, etiqueta: String?, grupoRepeticionId: String?) -> Unit,
    // Busca todas las veces que se repite el bloque que se está editando (él incluido).
    buscarOcurrencias: suspend (AvailabilityBlock) -> List<AvailabilityBlock>,
    // "Guardar toda la serie": aplica los cambios a todas esas ocurrencias.
    onGuardarSerie: (ocurrencias: List<AvailabilityBlock>, caregiverIds: List<CaregiverId>, categoria: CategoriaDisponibilidad, inicio: LocalTime, fin: LocalTime, etiqueta: String?, grupoRepeticionId: String?) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime, categoriaId: CategoriaId) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
    onGuardarBloques: (List<AvailabilityBlock>) -> Unit,
    onGuardarCategoria: (CategoriaDisponibilidad) -> Unit,
    onCerrar: () -> Unit
) {
    // Si el día tiene una sola ocupación, la hoja se abre directamente editándola (con
    // su categoría elegida y el formulario relleno); si tiene varias, se toca la que sea.
    val bloqueInicial = remember { bloquesDelDia.sinRepetirEntrePersonas().singleOrNull() }
    // Se guarda el id (no la categoría) para ver siempre su versión más reciente tras editarla.
    var seleccionId by remember { mutableStateOf<CategoriaId?>(bloqueInicial?.categoriaEn(categorias)?.id) }
    val seleccionada = categorias.firstOrNull { it.id == seleccionId }
    val porHoras = seleccionada?.modo == ModoCategoria.HORAS
    var creandoCategoria by remember { mutableStateOf(false) }
    // Bloque de "este día" que se está editando: al guardar se borra el original y se
    // guarda el nuevo en su lugar (solo para su persona, aunque estemos en una unidad).
    // Nada se toca hasta pulsar Guardar o Borrar: cancelar deja todo como estaba.
    var bloqueEnEdicion by remember { mutableStateOf<AvailabilityBlock?>(bloqueInicial) }
    // En una unidad familiar, lo que se guarda (también al editar y al repetir en otros
    // días) se aplica a todos sus miembros: la unidad "va junta". En una persona, solo a ella.
    val destinatarios = caregiverIds

    // Estado del formulario, elevado aquí para que Guardar (abajo del todo) lo vea. Se
    // reinicia al cambiar de categoría con el primer horario guardado de esa categoría.
    val turnosDeLaCategoria = seleccionada?.let { cat -> turnos.filter { it.esDe(cat) } }.orEmpty()
    var horasInicio by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaInicio ?: LocalTime.of(9, 0)) }
    var horasFin by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaFin ?: LocalTime.of(10, 0)) }
    var horasTodoElDia by remember(seleccionId) { mutableStateOf(false) }
    var fechasRepetir by remember(seleccionId) { mutableStateOf(setOf(fecha)) }
    var repetirActivo by remember(seleccionId) { mutableStateOf(false) }
    // Días de la semana (L-D) marcados para repetir la ocupación cada semana, además del
    // calendario manual: al marcar uno se añaden a fechasRepetir sus apariciones hasta
    // hastaRepetir; al desmarcarlo se quitan esas mismas fechas.
    var diasSemanaRepetir by remember(seleccionId) { mutableStateOf(emptySet<DayOfWeek>()) }
    // Hasta qué día llega el patrón semanal; por defecto, 3 meses.
    var hastaRepetir by remember(seleccionId) { mutableStateOf(fecha.plusMonths(3)) }

    fun alternarDiaSemana(dia: DayOfWeek) {
        val activando = dia !in diasSemanaRepetir
        diasSemanaRepetir = if (activando) diasSemanaRepetir + dia else diasSemanaRepetir - dia
        val fechasDelDia = fechasParaDiaSemanaHoja(fecha, dia, hastaRepetir).toSet()
        fechasRepetir = if (activando) {
            fechasRepetir + fechasDelDia
        } else {
            (fechasRepetir - fechasDelDia).ifEmpty { setOf(fecha) }
        }
    }

    // Al cambiar "Hasta": se recalculan las fechas que venían del patrón semanal (con el
    // rango antiguo) y se sustituyen por las del rango nuevo, sin tocar las que el
    // usuario haya marcado a mano en el calendario.
    fun cambiarHastaRepetir(nuevaHasta: LocalDate) {
        val hastaAnterior = hastaRepetir
        hastaRepetir = nuevaHasta
        if (diasSemanaRepetir.isEmpty()) return
        val antiguas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemanaHoja(fecha, dia, hastaAnterior) }.toSet()
        val nuevas = diasSemanaRepetir.flatMap { dia -> fechasParaDiaSemanaHoja(fecha, dia, nuevaHasta) }.toSet()
        fechasRepetir = (fechasRepetir - antiguas) + nuevas
    }
    var horasEtiqueta by remember(seleccionId) { mutableStateOf("") }
    var guardarComoHorario by remember(seleccionId) { mutableStateOf(false) }
    var nombreHorario by remember(seleccionId) { mutableStateOf("") }
    var rangoDesde by remember(seleccionId) { mutableStateOf(fecha) }
    var rangoHasta by remember(seleccionId) { mutableStateOf(fecha) }

    val horasFechas = fechasRepetir.sorted()
    val horasValidas = horasTodoElDia || horasInicio != horasFin
    val (inicioAGuardar, finAGuardar) =
        if (horasTodoElDia) AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA else horasInicio to horasFin
    val rangoFechas = fechasEntre(rangoDesde, rangoHasta)
    val puedeGuardarHoras = porHoras && horasFechas.isNotEmpty() && horasValidas &&
        (!guardarComoHorario || horasTodoElDia || nombreHorario.isNotBlank())
    val puedeGuardarRango = seleccionada != null && !porHoras && rangoFechas.size <= MAX_DIAS_RANGO

    // Si la ocupación que se edita pertenece a una serie (se creó junto a otros días), se
    // ofrecen dos formas de guardar: solo esta, o toda la serie.
    var ocurrenciasSerie by remember { mutableStateOf<List<AvailabilityBlock>>(emptyList()) }
    LaunchedEffect(bloqueEnEdicion) {
        val bloque = bloqueEnEdicion
        ocurrenciasSerie = if (bloque == null) emptyList()
        else runCatching { buscarOcurrencias(bloque) }.getOrDefault(emptyList())
    }
    val tieneSerie = ocurrenciasSerie.map { it.fecha }.distinct().size > 1
    // Qué se pide confirmar: false = solo esta ocupación, true = la serie completa.
    var confirmarGuardadoSerie by remember { mutableStateOf<Boolean?>(null) }

    fun guardarHoras(serieCompleta: Boolean = false) {
        val cat = seleccionada ?: return
        if (guardarComoHorario && !horasTodoElDia && nombreHorario.isNotBlank() &&
            turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin) == null
        ) {
            onCrearTurno(nombreHorario.trim(), horasInicio, horasFin, cat.id)
        }
        val original = bloqueEnEdicion
        if (serieCompleta && original != null) {
            onGuardarSerie(ocurrenciasSerie, destinatarios, cat, inicioAGuardar, finAGuardar, horasEtiqueta.trim().ifBlank { null }, original.grupoRepeticionId)
        } else {
            original?.let { onEliminar(it) }
            onGuardarHoras(
                destinatarios, cat, horasFechas, inicioAGuardar, finAGuardar, false,
                horasEtiqueta.trim().ifBlank { null }, original?.grupoRepeticionId
            )
        }
        bloqueEnEdicion = null
    }

    fun guardarRango(serieCompleta: Boolean = false) {
        val cat = seleccionada ?: return
        val original = bloqueEnEdicion
        val etiquetaRango = horasEtiqueta.trim().ifBlank { null }
        if (serieCompleta && original != null) {
            onGuardarSerie(ocurrenciasSerie, destinatarios, cat, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, etiquetaRango, original.grupoRepeticionId)
        } else {
            original?.let { onEliminar(it) }
            // Varios días de golpe forman una serie; si ya era de una, sigue en ella.
            val grupo = original?.grupoRepeticionId
                ?: if (rangoFechas.size > 1) java.util.UUID.randomUUID().toString() else null
            onGuardarBloques(destinatarios.flatMap { caregiverId ->
                rangoFechas.map { dia ->
                    bloqueDeCategoria(cat, caregiverId, dia, AvailabilityBlock.INICIO_DIA, AvailabilityBlock.FIN_DIA, etiquetaRango, grupo)
                }
            })
        }
        bloqueEnEdicion = null
    }

    // Al tocar el lápiz de un bloque: se elige su categoría y, una vez el formulario se ha
    // reiniciado con ella (los remember(seleccionId) de arriba), se rellena con sus datos.
    LaunchedEffect(bloqueEnEdicion) {
        val bloque = bloqueEnEdicion ?: return@LaunchedEffect
        horasTodoElDia = bloque.todoElDia
        if (!bloque.todoElDia) { horasInicio = bloque.horaInicio; horasFin = bloque.horaFin }
        horasEtiqueta = bloque.etiqueta.orEmpty()
        fechasRepetir = setOf(bloque.fecha)
        repetirActivo = false
        diasSemanaRepetir = emptySet()
        hastaRepetir = bloque.fecha.plusMonths(3)
        rangoDesde = bloque.fecha
        rangoHasta = bloque.fecha
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
                        append(titulo)
                        withStyle(SpanStyle(color = tintaSuaveHoja)) { append(" · ") }
                        withStyle(SpanStyle(color = acentoHoja)) { append(nombreDia(fecha)) }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tintaHoja,
                    modifier = Modifier.weight(1f)
                )
                BotonRedondoSuaveHoja(Icons.Default.Close, "Cerrar", onCerrar)
            }
            Spacer(Modifier.height(14.dp))

            // ── Actividad de este día ────────────────────────────────────────────
            // Solo cuando no se está editando: al editar se va directo a los horarios.
            if (bloquesDelDia.isNotEmpty() && bloqueEnEdicion == null) {
                TarjetaSeccionHoja {
                    Text(
                        if (bloquesDelDia.size == 1) "Actividad de este día" else "Actividades de este día (toca una para editarla)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tintaSuaveHoja
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bloquesDelDia.sortedBy { it.horaInicio }.sinRepetirEntrePersonas().forEach { bloque ->
                            val categoria = bloque.categoriaEn(categorias)
                            val editando = bloqueEnEdicion == bloque
                            // En una unidad: quién tiene esta ocupación (nada si la tienen todos).
                            val quienes = bloquesDelDia.filter { it.mismaOcupacionQue(bloque) }.map { it.caregiverId }.toSet()
                            val etiquetaQuienes = if (caregiverIds.size > 1 && !caregiverIds.all { it in quienes }) {
                                quienes.mapNotNull { nombres[it] }.joinToString(", ")
                            } else null
                            FilaBloqueDelDia(
                                categoria = categoria,
                                subtitulo = listOfNotNull(
                                    textoHorario(bloque),
                                    etiquetaQuienes,
                                    bloque.etiqueta?.takeIf { it.isNotBlank() },
                                    "no ocupa".takeIf { !categoria.bloquea }
                                ).joinToString(" · "),
                                editando = editando,
                                onEditar = {
                                    if (!editando) {
                                        seleccionId = categoria.id
                                        bloqueEnEdicion = bloque
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── Categoría ─────────────────────────────────────────────────────────
            if (bloqueEnEdicion == null) {
                TituloSeccionHoja(
                    if (caregiverIds.size > 1) "Categoría (para todos)" else "Categoría",
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
                            onClick = { seleccionId = if (seleccionId == categoria.id) null else categoria.id }
                        )
                    }
                    CasillaNuevaCategoria(modifier = Modifier.weight(1f), onClick = { creandoCategoria = true })
                }
                if (seleccionada != null) Spacer(Modifier.height(18.dp))
            } else if (seleccionada != null) {
                // Editando: solo se recuerda qué se edita, y se va directo a los horarios.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(seleccionada.color).copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(seleccionada.emoji, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Editando ${seleccionada.nombre}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tintaHoja
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Formulario ────────────────────────────────────────────────────────
            if (seleccionada != null) {
                key(seleccionada.id) {
                    if (porHoras) {
                        TituloSeccionHoja("Horarios", "Define el horario de esta actividad")
                        Spacer(Modifier.height(10.dp))
                        TarjetaSeccionHoja(padding = 0.dp) {
                            if (turnosDeLaCategoria.isNotEmpty()) {
                                HorariosGuardados(
                                    turnos = turnosDeLaCategoria,
                                    elegido = if (horasTodoElDia) null else turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin),
                                    onElegir = { horasTodoElDia = false; horasInicio = it.horaInicio; horasFin = it.horaFin },
                                    onEliminar = { onEliminarTurno(it.id) }
                                )
                                SeparadorHoja()
                            }
                            FilaConInterruptorHoja(
                                icono = Icons.Default.Schedule,
                                titulo = "Todo el día",
                                subtitulo = "Marca si dura todo el día",
                                activo = horasTodoElDia,
                                onCambiar = { horasTodoElDia = it }
                            )
                            if (!horasTodoElDia) {
                                SeparadorHoja()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CajaHoraHoja("Desde", horasInicio, Modifier.weight(1f)) { horasInicio = it }
                                    CajaHoraHoja("Hasta", horasFin, Modifier.weight(1f)) { horasFin = it }
                                }
                                if (horasInicio == horasFin) {
                                    NotaSeccionHoja("La hora de fin debe ser distinta de la de inicio.", error = true)
                                } else if (!horasFin.isAfter(horasInicio)) {
                                    NotaSeccionHoja("Turno de noche: termina al día siguiente a las ${formatearHora(horasFin)}.")
                                }
                                if (turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin) == null) {
                                    SeparadorHoja()
                                    FilaConInterruptorHoja(
                                        icono = Icons.Default.BookmarkBorder,
                                        titulo = "Guardar como horario",
                                        subtitulo = "Guarda estas horas para usarlas más adelante",
                                        activo = guardarComoHorario,
                                        onCambiar = { guardarComoHorario = it },
                                        tintaIcono = acentoHoja
                                    )
                                    if (guardarComoHorario) {
                                        CampoTextoHoja(
                                            valor = nombreHorario,
                                            onCambiar = { nombreHorario = it.take(24) },
                                            placeholder = "Nombre del horario (p.ej. Mañana)",
                                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                                        )
                                    }
                                }
                            }
                            SeparadorHoja()
                            FilaDetalle(valor = horasEtiqueta, onCambiar = { horasEtiqueta = it })
                        }

                        Spacer(Modifier.height(16.dp))
                        // Repetir en otros días: interruptor (apagado por defecto) que despliega
                        // el calendario mensual para marcar los días que se quieran.
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
                                        fechasRepetir = setOf(fecha)
                                        diasSemanaRepetir = emptySet()
                                        hastaRepetir = fecha.plusMonths(3)
                                    }
                                },
                                colors = coloresInterruptorEncaja()
                            )
                        }
                        if (repetirActivo) {
                            Spacer(Modifier.height(12.dp))
                            TarjetaSeccionHoja(padding = 10.dp) {
                                CalendarioMultipleHoja(
                                    mesInicial = YearMonth.from(fecha),
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
                                Text(
                                    "O cada semana en",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tintaSuaveHoja
                                )
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
                                    (if (horasFechas.size == 1) "1 día" else "${horasFechas.size} días") +
                                        ". Si alguno ya tenía ${seleccionada.nombre}, se sustituye.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tintaSuaveHoja
                                )
                            }
                        }
                    } else {
                        TituloSeccionHoja("Fechas", "Uno o varios días completos")
                        Spacer(Modifier.height(10.dp))
                        TarjetaSeccionHoja(padding = 0.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CajaFechaHoja("Desde", rangoDesde, Modifier.weight(1f)) { rangoDesde = it; if (rangoHasta.isBefore(it)) rangoHasta = it }
                                CajaFechaHoja("Hasta", rangoHasta, Modifier.weight(1f)) { rangoHasta = it }
                            }
                            NotaSeccionHoja(
                                if (rangoFechas.size == 1) "1 día completo" else "${rangoFechas.size} días completos",
                                error = rangoFechas.size > MAX_DIAS_RANGO
                            )
                            SeparadorHoja()
                            FilaDetalle(valor = horasEtiqueta, onCambiar = { horasEtiqueta = it })
                        }
                    }
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
                bloqueEnEdicion?.let { bloque ->
                    BotonCuadradoHoja(
                        icono = Icons.Default.Delete,
                        descripcion = "Borrar",
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        tinta = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { onEliminar(bloque); onCerrar() }
                    )
                }
                val puedeGuardar = puedeGuardarHoras || puedeGuardarRango
                val fondoGuardar = if (puedeGuardar) acentoHoja else acentoHoja.copy(alpha = 0.35f)
                BotonCuadradoHoja(
                    icono = Icons.Default.Save,
                    descripcion = if (tieneSerie) "Guardar solo esta" else "Guardar",
                    fondo = fondoGuardar,
                    tinta = onAcentoHoja,
                    habilitado = puedeGuardar,
                    // Con serie, primero se pide confirmación de qué se guarda.
                    onClick = {
                        if (tieneSerie) {
                            confirmarGuardadoSerie = false
                        } else {
                            if (porHoras) guardarHoras() else guardarRango()
                            onCerrar()
                        }
                    }
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
            titulo = if (serieCompleta) "Guardar la serie completa de la ocupación" else "Guardar sólo esta ocupación",
            detalle = if (serieCompleta) "Los cambios se aplicarán a todas las veces que se repite esta ocupación."
            else "Los cambios solo afectarán a esta ocupación; el resto de la serie no se toca.",
            onConfirmar = {
                confirmarGuardadoSerie = null
                if (porHoras) guardarHoras(serieCompleta) else guardarRango(serieCompleta)
                onCerrar()
            },
            onCancelar = { confirmarGuardadoSerie = null },
            serie = serieCompleta
        )
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

/** Un bloque ya apuntado ese día: punto de color, icono en cuadro, nombre y horario. Toda
 * la fila es clicable y abre su edición; la que se está editando lleva el borde de acento. */
@Composable
private fun FilaBloqueDelDia(
    categoria: CategoriaDisponibilidad,
    subtitulo: String,
    editando: Boolean,
    onEditar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(casillaHoja)
            .border(1.dp, if (editando) acentoHoja else bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onEditar)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
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
            Text(categoria.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = tintaSuaveHoja)
        }
        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = if (editando) acentoHoja else tintaSuaveHoja, modifier = Modifier.size(20.dp))
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
                .background(if (elegida) acentoHoja else casillaHoja)
                .border(1.dp, if (elegida) acentoHoja else bordeHoja.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
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
                color = if (elegida) onAcentoHoja else tintaHoja,
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
                    .background(acentoHoja)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = onAcentoHoja, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun CasillaNuevaCategoria(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colorBorde = tintaSuaveHoja.copy(alpha = 0.6f)
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
        Icon(Icons.Default.Add, contentDescription = null, tint = tintaHoja, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Nueva categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tintaHoja, maxLines = 2)
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
            Text("Horarios guardados", style = MaterialTheme.typography.labelLarge, color = tintaSuaveHoja, modifier = Modifier.weight(1f))
            TextButton(onClick = { editando = !editando }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(if (editando) "Hecho" else "Editar", color = acentoHoja)
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
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = acentoHoja, selectedLabelColor = onAcentoHoja)
                )
            }
        }
    }
}

/** "Detalle (opcional)" con su icono y el campo de texto debajo. */
@Composable
private fun FilaDetalle(valor: String, onCambiar: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        IconoEnCirculoHoja(Icons.Default.Description, fondo = lavandaHoja, tinta = indigoHoja)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Detalle (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
            Spacer(Modifier.height(6.dp))
            CampoTextoHoja(valor = valor, onCambiar = onCambiar, placeholder = "Añade un detalle…")
        }
    }
}

