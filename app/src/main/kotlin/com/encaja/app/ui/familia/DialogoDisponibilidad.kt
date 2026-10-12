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
//    detalle; las categorías por días llevan "Fechas" (desde/hasta) y detalle. Debajo,
//    en todas, "Repetir o borrar en otros días": un interruptor que despliega un calendario
//    con TODOS los días que esa persona ya tiene de esa categoría marcados; marcar añade o
//    sustituye, desmarcar borra, y los círculos L-D con su fecha de vencimiento repiten
//    (o dejan de repetir) un día de la semana.
//  - Abajo, Cancelar, papelera (si se está editando) y disquete (guardar). Nada se
//    guarda ni se borra hasta pulsar uno de los dos.
// Las categorías se editan desde la tarjeta "Categorías" de la pantalla.

import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.BotonCuadradoHoja
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.encaja.app.ui.theme.coloresInterruptorEncaja
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

private fun textoDias(n: Int): String = if (n == 1) "1 día" else "$n días"

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
    // Todas las ocupaciones de esta categoría para estas personas (sea cual sea su horario):
    // son los días que «Repetir o borrar en otros días» enseña ya marcados.
    buscarDeCategoria: suspend (CategoriaDisponibilidad) -> List<AvailabilityBlock>,
    // Guarda en [fechasAGuardar] (sustituyendo lo que hubiera de esa categoría), borra la
    // categoría de [fechasABorrar] y, si se editaba una ocupación, quita antes la original.
    onGuardar: (categoria: CategoriaDisponibilidad, fechasAGuardar: Set<LocalDate>, fechasABorrar: Set<LocalDate>, inicio: LocalTime, fin: LocalTime, etiqueta: String?, original: AvailabilityBlock?) -> Unit,
    onCrearTurno: (nombre: String, inicio: LocalTime, fin: LocalTime, categoriaId: CategoriaId) -> Unit,
    onEliminarTurno: (TurnoId) -> Unit,
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

    // Estado del formulario, elevado aquí para que Guardar (abajo del todo) lo vea. Se
    // reinicia al cambiar de categoría con el primer horario guardado de esa categoría.
    val turnosDeLaCategoria = seleccionada?.let { cat -> turnos.filter { it.esDe(cat) } }.orEmpty()
    var horasInicio by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaInicio ?: LocalTime.of(9, 0)) }
    var horasFin by remember(seleccionId) { mutableStateOf(turnosDeLaCategoria.firstOrNull()?.horaFin ?: LocalTime.of(10, 0)) }
    var horasTodoElDia by remember(seleccionId) { mutableStateOf(false) }
    // «Repetir o borrar en otros días»: al activarlo, fechasRepetir son los días MARCADOS
    // (los que ya tenían la categoría más los que se añadan); los que estaban y se desmarquen
    // se borran al guardar. Los círculos L-D no guardan nada propio: se deducen de ellas.
    var fechasRepetir by remember(seleccionId) { mutableStateOf(setOf(fecha)) }
    var repetirActivo by remember(seleccionId) { mutableStateOf(false) }
    // Si ya se ha tocado el calendario a mano, no se vuelve a rellenar solo con lo existente.
    var calendarioTocado by remember(seleccionId) { mutableStateOf(false) }
    // Hasta qué día llega el patrón semanal (fecha de vencimiento); por defecto, 3 meses.
    var hastaRepetir by remember(seleccionId) { mutableStateOf(fecha.plusMonths(3)) }
    var horasEtiqueta by remember(seleccionId) { mutableStateOf("") }
    var guardarComoHorario by remember(seleccionId) { mutableStateOf(false) }
    var nombreHorario by remember(seleccionId) { mutableStateOf("") }
    var rangoDesde by remember(seleccionId) { mutableStateOf(fecha) }
    var rangoHasta by remember(seleccionId) { mutableStateOf(fecha) }
    var creandoHorario by remember { mutableStateOf(false) }
    var confirmandoGuardado by remember { mutableStateOf(false) }

    // Los días en que estas personas ya tienen esta categoría, con cualquier horario.
    var existentes by remember(seleccionId) { mutableStateOf(emptySet<LocalDate>()) }
    LaunchedEffect(seleccionId) {
        val cat = seleccionada
        existentes = if (cat == null) emptySet()
        else runCatching { buscarDeCategoria(cat) }.getOrDefault(emptyList()).map { it.fecha }.toSet()
    }

    val rangoFechas = fechasEntre(rangoDesde, rangoHasta)
    // Qué se guarda y qué se borra al pulsar Guardar.
    val guardarFechas: Set<LocalDate> = when {
        repetirActivo -> fechasRepetir
        porHoras -> setOf(fecha)
        else -> rangoFechas.toSet()
    }
    val borrarFechas: Set<LocalDate> = if (repetirActivo) existentes - fechasRepetir else emptySet()
    val sustituidas = guardarFechas.count { it in existentes }
    val horasValidas = horasTodoElDia || horasInicio != horasFin
    val (inicioAGuardar, finAGuardar) =
        if (horasTodoElDia) AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA else horasInicio to horasFin
    val formularioValido = if (porHoras) {
        horasValidas && (!guardarComoHorario || horasTodoElDia || nombreHorario.isNotBlank())
    } else {
        repetirActivo || rangoFechas.size <= MAX_DIAS_RANGO
    }
    // Todo desmarcado y algo que borrar: se guarda sin mirar el horario del formulario.
    val soloBorrar = repetirActivo && guardarFechas.isEmpty() && borrarFechas.isNotEmpty()
    val puedeGuardar = seleccionada != null && (soloBorrar || (formularioValido && guardarFechas.isNotEmpty()))
    val necesitaConfirmar = repetirActivo && (borrarFechas.isNotEmpty() || sustituidas > 1)

    // Los círculos L-D están encendidos si TODAS las fechas de ese día de la semana, desde
    // este día hasta la fecha de vencimiento, están marcadas (al menos dos, para que sea un patrón).
    val diasSemanaActivos: Set<DayOfWeek> =
        if (repetirActivo) diasSemanaCompletos(fecha, hastaRepetir, fechasRepetir) else emptySet()
    val diasSemanaAMedias: Set<DayOfWeek> =
        if (repetirActivo) diasSemanaParciales(fecha, hastaRepetir, fechasRepetir) else emptySet()

    fun alternarDiaSemana(dia: DayOfWeek) {
        calendarioTocado = true
        val fs = fechasParaDiaSemanaHoja(fecha, dia, hastaRepetir).toSet()
        // Encendido o a medias: se quitan sus fechas; apagado: se añaden todas.
        fechasRepetir = if (dia in diasSemanaActivos || dia in diasSemanaAMedias) fechasRepetir - fs else fechasRepetir + fs
    }

    // Al cambiar el vencimiento se recalculan las fechas de los días de la semana encendidos
    // (las de más allá se quitan, las nuevas se añaden), sin tocar las marcadas a mano.
    fun cambiarHastaRepetir(nuevaHasta: LocalDate) {
        val hastaAnterior = hastaRepetir
        val activos = diasSemanaActivos
        hastaRepetir = nuevaHasta
        calendarioTocado = true
        if (activos.isEmpty()) return
        val antiguas = activos.flatMap { dia -> fechasParaDiaSemanaHoja(fecha, dia, hastaAnterior) }.toSet()
        val nuevas = activos.flatMap { dia -> fechasParaDiaSemanaHoja(fecha, dia, nuevaHasta) }.toSet()
        fechasRepetir = (fechasRepetir - antiguas) + nuevas
    }

    // Si «Repetir o borrar» se activó antes de acabar de leer los días existentes, se completan
    // cuando llegan (siempre que no se haya tocado ya el calendario).
    LaunchedEffect(existentes) {
        if (repetirActivo && !calendarioTocado) {
            fechasRepetir = existentes + (if (porHoras) setOf(fecha) else rangoFechas.toSet())
            hastaRepetir = hastaInicialRepeticion(existentes, fecha)
        }
    }

    fun guardar() {
        val cat = seleccionada ?: return
        if (porHoras && guardarComoHorario && !horasTodoElDia && nombreHorario.isNotBlank() &&
            turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin) == null
        ) {
            onCrearTurno(nombreHorario.trim(), horasInicio, horasFin, cat.id)
        }
        val (ini, fin) =
            if (porHoras) inicioAGuardar to finAGuardar else AvailabilityBlock.INICIO_DIA to AvailabilityBlock.FIN_DIA
        onGuardar(cat, guardarFechas, borrarFechas, ini, fin, horasEtiqueta.trim().ifBlank { null }, bloqueEnEdicion)
        bloqueEnEdicion = null
        onCerrar()
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
        calendarioTocado = false
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
                            HorariosGuardados(
                                turnos = turnosDeLaCategoria,
                                elegido = if (horasTodoElDia) null else turnoConHoras(turnosDeLaCategoria, horasInicio, horasFin),
                                onElegir = { horasTodoElDia = false; horasInicio = it.horaInicio; horasFin = it.horaFin },
                                onEliminar = { onEliminarTurno(it.id) },
                                onNuevo = { creandoHorario = true }
                            )
                            SeparadorHoja()
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

                    } else {
                        TituloSeccionHoja(if (repetirActivo) "Detalle" else "Fechas", if (repetirActivo) "Opcional" else "Uno o varios días completos")
                        Spacer(Modifier.height(10.dp))
                        TarjetaSeccionHoja(padding = 0.dp) {
                            // Con «Repetir o borrar» activo, las fechas se eligen en su calendario.
                            if (!repetirActivo) {
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
                            }
                            FilaDetalle(valor = horasEtiqueta, onCambiar = { horasEtiqueta = it })
                        }
                    }

                    // ── Repetir o borrar en otros días (todas las categorías) ─────────────
                    // Al activarlo se marcan en el calendario TODOS los días en que esta persona
                    // tiene ya esta categoría, con el horario que tengan: los marcados quedan (o
                    // se sustituyen por lo del formulario), los que se desmarquen se borran.
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconoEnCirculoHoja(Icons.Default.CalendarMonth, fondo = lavandaHoja, tinta = indigoHoja)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Repetir o borrar en otros días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tintaHoja)
                            Text(
                                if (repetirActivo) "Marcado = se guarda · desmarcado = se borra" else "Solo este día",
                                style = MaterialTheme.typography.bodySmall,
                                color = tintaSuaveHoja
                            )
                        }
                        Switch(
                            checked = repetirActivo,
                            onCheckedChange = { activo ->
                                repetirActivo = activo
                                calendarioTocado = false
                                if (activo) {
                                    fechasRepetir = existentes + (if (porHoras) setOf(fecha) else rangoFechas.toSet())
                                    hastaRepetir = hastaInicialRepeticion(existentes, fecha)
                                } else {
                                    fechasRepetir = setOf(fecha)
                                    hastaRepetir = fecha.plusMonths(3)
                                }
                            },
                            colors = coloresInterruptorEncaja()
                        )
                    }
                    if (repetirActivo) {
                        Spacer(Modifier.height(12.dp))
                        TarjetaSeccionHoja(padding = 10.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (existentes.isEmpty()) "${seleccionada.nombre}: ningún día apuntado para $titulo."
                                    else "${seleccionada.nombre}: ${textoDias(existentes.size)} apuntados para $titulo (ya marcados).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tintaSuaveHoja,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { fechasRepetir = emptySet(); calendarioTocado = true },
                                    enabled = fechasRepetir.isNotEmpty()
                                ) { Text("Quitar todos", color = acentoHoja, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.height(6.dp))
                            CalendarioMultipleHoja(
                                mesInicial = YearMonth.from(fecha),
                                seleccionadas = fechasRepetir,
                                onAlternar = { dia ->
                                    calendarioTocado = true
                                    fechasRepetir = if (dia in fechasRepetir) fechasRepetir - dia else fechasRepetir + dia
                                }
                            )
                            Spacer(Modifier.height(14.dp))
                            SeparadorHoja()
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "O cada semana en (lleno: todas las semanas · a medias: faltan algunas · toca para quitarlo)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tintaSuaveHoja
                            )
                            Spacer(Modifier.height(8.dp))
                            SelectorDiasSemanaHoja(
                                seleccionados = diasSemanaActivos,
                                parciales = diasSemanaAMedias,
                                onAlternar = { dia -> alternarDiaSemana(dia) }
                            )
                            Spacer(Modifier.height(10.dp))
                            CajaFechaHoja("Repetir hasta", hastaRepetir, Modifier.fillMaxWidth()) { cambiarHastaRepetir(it) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = tintaSuaveHoja, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (guardarFechas.isEmpty()) "Ningún día marcado para guardar."
                                    else "Se guardará en ${textoDias(guardarFechas.size)}" +
                                        (if (sustituidas > 0) " (${textoDias(sustituidas)} ya tenían ${seleccionada.nombre}: se sustituyen)." else "."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tintaSuaveHoja
                                )
                                if (borrarFechas.isNotEmpty()) {
                                    Text(
                                        "Se borrará ${seleccionada.nombre} de ${textoDias(borrarFechas.size)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
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
                val fondoGuardar = if (puedeGuardar) acentoHoja else acentoHoja.copy(alpha = 0.35f)
                BotonCuadradoHoja(
                    icono = Icons.Default.Save,
                    descripcion = "Guardar",
                    fondo = fondoGuardar,
                    tinta = onAcentoHoja,
                    habilitado = puedeGuardar,
                    // Si hay que borrar días o sustituir varios, antes se pide confirmación.
                    onClick = { if (necesitaConfirmar) confirmandoGuardado = true else guardar() }
                )
            }
        }
    }

    if (confirmandoGuardado) {
        val nombreCat = seleccionada?.nombre.orEmpty()
        val lineas = mutableListOf<String>()
        if (guardarFechas.isNotEmpty()) {
            lineas += "Se guardará $nombreCat en ${textoDias(guardarFechas.size)}" +
                if (sustituidas > 0) ". En ${textoDias(sustituidas)} ya lo había y se sustituye." else "."
        }
        if (borrarFechas.isNotEmpty()) lineas += "Se borrará $nombreCat de ${textoDias(borrarFechas.size)}."
        AvisoHoja(
            icono = if (soloBorrar) Icons.Default.Delete else Icons.Default.Save,
            titulo = if (soloBorrar) "¿Borrar ${textoDias(borrarFechas.size)}?" else "Aplicar los cambios",
            texto = lineas.joinToString("\n"),
            peligro = soloBorrar,
            acciones = listOf(
                AccionAvisoHoja(
                    if (soloBorrar) "Borrar" else "Confirmar",
                    if (soloBorrar) EstiloAccionAviso.PELIGRO else EstiloAccionAviso.PRIMARIO
                ) {
                    confirmandoGuardado = false
                    guardar()
                },
                AccionAvisoHoja("Volver", EstiloAccionAviso.BORDE) { confirmandoGuardado = false }
            ),
            onCerrar = { confirmandoGuardado = false }
        )
    }

    if (creandoHorario && seleccionada != null) {
        DialogoNuevoHorario(
            inicioInicial = horasInicio,
            finInicial = horasFin,
            onCrear = { nombre, ini, fin ->
                onCrearTurno(nombre, ini, fin, seleccionada.id)
                horasTodoElDia = false
                horasInicio = ini
                horasFin = fin
                creandoHorario = false
            },
            onCerrar = { creandoHorario = false }
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

/** Los horarios guardados de la categoría, como chips; «Nuevo horario» crea otro y con «Editar» se borran de un toque. */
@Composable
private fun HorariosGuardados(
    turnos: List<TurnoTrabajo>,
    elegido: TurnoTrabajo?,
    onElegir: (TurnoTrabajo) -> Unit,
    onEliminar: (TurnoTrabajo) -> Unit,
    onNuevo: () -> Unit
) {
    var editando by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Horarios guardados", style = MaterialTheme.typography.labelLarge, color = tintaSuaveHoja, modifier = Modifier.weight(1f))
            if (turnos.isNotEmpty()) {
                TextButton(onClick = { editando = !editando }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(if (editando) "Hecho" else "Editar", color = acentoHoja)
                }
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
            if (!editando) {
                AssistChip(
                    onClick = onNuevo,
                    label = { Text("Nuevo horario") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

/** Ventana para crear un horario con nombre («Tarde», «Central»...) y sus horas, sin tocar nada más. */
@Composable
private fun DialogoNuevoHorario(
    inicioInicial: LocalTime,
    finInicial: LocalTime,
    onCrear: (nombre: String, inicio: LocalTime, fin: LocalTime) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf(inicioInicial) }
    var fin by remember { mutableStateOf(finInicial) }
    AvisoHoja(
        icono = Icons.Default.Schedule,
        titulo = "Nuevo horario",
        texto = "Ponle un nombre y las horas; luego lo eliges con un toque.",
        peligro = false,
        conX = true,
        accionesEnFila = true,
        acciones = listOf(
            AccionAvisoHoja("Cancelar", EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja("Crear", EstiloAccionAviso.PRIMARIO, habilitada = nombre.isNotBlank() && inicio != fin) {
                onCrear(nombre.trim(), inicio, fin)
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            CampoTextoConIconoHoja(nombre, { nombre = it.take(24) }, "Nombre (p.ej. Tarde)", Icons.Default.Schedule)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CajaHoraHoja("Desde", inicio, Modifier.weight(1f)) { inicio = it }
                CajaHoraHoja("Hasta", fin, Modifier.weight(1f)) { fin = it }
            }
            if (inicio == fin) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "La hora de fin debe ser distinta de la de inicio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
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

