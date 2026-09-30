@file:OptIn(ExperimentalMaterial3Api::class)

package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Rediseño con el mismo criterio que la pantalla Semana (fuente Nunito, tarjetas
// redondeadas suaves, botones circulares): una tarjeta con la cabecera del día; la
// línea de tiempo real de borde a borde de la pantalla (no comprimida; cada niño tiene
// un carril con sus actividades a su hora y duración exactas, con scroll horizontal, y
// su nombre encima), una pastilla roja con la hora actual y su línea vertical (solo si
// se está viendo hoy), y debajo la lista "Actividades del día", una tarjeta por niño.
// El botón "Ahora" (punto de disparo) vuelve a la hora actual (y al día de hoy).

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.formatearHora
import com.encaja.app.ui.familia.millisUtcAFecha
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ES = Locale("es")

// La franja horaria que se representa en la guía: de 7:00 a 22:00.
private val INICIO_FRANJA: LocalTime = LocalTime.of(7, 0)
private val FIN_FRANJA: LocalTime = LocalTime.of(22, 0)
private val MINUTOS_FRANJA = java.time.Duration.between(INICIO_FRANJA, FIN_FRANJA).toMinutes().toInt()

// Escala de la línea de tiempo: cada minuto ocupa este ancho real (no se comprime
// para caber en pantalla), por eso hace falta scroll horizontal para ver todo el día.
private val ANCHO_MINUTO: Dp = 2.dp
// Media hora de margen a cada lado del lienzo, para que la etiqueta de la primera y la
// última hora (centradas en su marca) no queden cortadas.
private val MARGEN_LIENZO: Dp = ANCHO_MINUTO * 30
private val ANCHO_LIENZO: Dp = ANCHO_MINUTO * MINUTOS_FRANJA + MARGEN_LIENZO * 2
private val MARGEN_AHORA: Dp = 96.dp

// Alturas fijas de la regla, del nombre sobre cada carril y de los carriles.
private val ALTO_REGLA: Dp = 46.dp
private val ALTO_NOMBRE: Dp = 22.dp
private val ALTO_FILA: Dp = 64.dp
private val ESPACIO_FILAS: Dp = 14.dp
private val ALTO_PASTILLA_AHORA: Dp = 22.dp
private val ANCHO_PASTILLA_AHORA: Dp = 56.dp

// Icono de quién lleva/recoge en el bloque: bastante grande para que sus 2 letras se
// lean bien dentro del bloque.
private val ANCHO_ICONO_RESPONSABLE: Dp = 26.dp

// Atajos a los colores del tema activo (Cálido/Nocturno) con los nombres del diseño.
private val TINTA: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TINTA_SUAVE: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val INDIGO: Color @Composable get() = MaterialTheme.colorScheme.primary
private val LAVANDA: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val BLANCO: Color @Composable get() = MaterialTheme.colorScheme.surface
private val VERDE: Color @Composable get() = LocalEncajaExtraColors.current.verdeContainer
private val ON_VERDE: Color @Composable get() = LocalEncajaExtraColors.current.onVerdeContainer
private val ROJO: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
private val ON_ROJO: Color @Composable get() = MaterialTheme.colorScheme.onErrorContainer

/** Posición horizontal (dentro del lienzo) de un instante, contando el margen izquierdo. */
private fun xDeMinutos(minutos: Int): Dp = MARGEN_LIENZO + ANCHO_MINUTO * minutos

@Composable
fun GuiaScreen(
    viewModel: GuiaViewModel = hiltViewModel(),
    // Al llegar desde un aviso de la pantalla Semana (falta cubrir / incompatibilidad):
    // salta directamente a ese día y abre su edición, para asignar sin tener que
    // buscarla a mano. Ambos quedan en null en la entrada normal por la pestaña.
    fechaInicial: LocalDate? = null,
    necesidadIdInicial: String? = null,
    // Al cerrar esa edición (guardar, borrar o cancelar) hay que volver a Semana en
    // vez de quedarse en Guía, ya que el usuario nunca pidió venir aquí a mirar el
    // día entero: solo quería resolver ese aviso concreto.
    onVolverDespuesDeAsignar: () -> Unit = {}
) {
    val pantalla by viewModel.pantalla.collectAsState()
    var actividadEnCreacion by remember { mutableStateOf(false) }
    var actividadEnEdicion by remember { mutableStateOf<CoverageNeed?>(null) }
    var necesidadPendiente by remember(necesidadIdInicial) { mutableStateOf(necesidadIdInicial) }
    // True mientras la actividad en edición es la que se abrió sola al llegar desde un
    // aviso (no una que el usuario haya abierto a mano tocándola en la propia Guía).
    var edicionVieneDeAviso by remember(necesidadIdInicial) { mutableStateOf(false) }

    fun cerrarEdicion() {
        actividadEnEdicion = null
        if (edicionVieneDeAviso) {
            edicionVieneDeAviso = false
            onVolverDespuesDeAsignar()
        }
    }

    // El ViewModel sobrevive a los cambios de pestaña; se recarga al reentrar para
    // reflejar cambios hechos desde otra pestaña (p.ej. borrar una actividad). Si se
    // llega con una fecha concreta (desde un aviso), se salta a ese día en vez de la
    // recarga normal.
    LaunchedEffect(fechaInicial) {
        if (fechaInicial != null) viewModel.irADia(fechaInicial) else viewModel.recargar()
    }

    // En cuanto los datos de ese día están cargados, busca la actividad pendiente y
    // abre su diálogo de edición. Se limpia tras abrirlo para no volver a hacerlo si
    // el usuario lo cierra y se queda en la pantalla.
    LaunchedEffect(pantalla, necesidadPendiente) {
        val pendiente = necesidadPendiente ?: return@LaunchedEffect
        val estado = (pantalla as? GuiaPantallaEstado.ConDatos)?.estado ?: return@LaunchedEffect
        if (fechaInicial != null && estado.fecha != fechaInicial) return@LaunchedEffect
        val need = estado.filas.flatMap { it.bloques }.map { it.need }.firstOrNull { it.id.value == pendiente }
        if (need != null) {
            actividadEnEdicion = need
            edicionVieneDeAviso = true
            necesidadPendiente = null
        }
    }

    val estadoConDatos = pantalla as? GuiaPantallaEstado.ConDatos
    val extra = LocalEncajaExtraColors.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (estadoConDatos != null && estadoConDatos.estado.filas.isNotEmpty()) {
                // Cuadrado redondeado en el color de acento, como en la maqueta.
                FloatingActionButton(
                    onClick = { actividadEnCreacion = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = extra.acento,
                    contentColor = extra.onAcento
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva actividad", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { paddingScaffold ->
        Box(modifier = Modifier.padding(paddingScaffold)) {
            when (val estadoActual = pantalla) {
                is GuiaPantallaEstado.Cargando -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is GuiaPantallaEstado.SinFamilia -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                            textAlign = TextAlign.Center,
                            color = TINTA
                        )
                    }
                }

                is GuiaPantallaEstado.ConDatos -> {
                    val fecha = estadoActual.estado.fecha
                    val esHoy = fecha == LocalDate.now()
                    val scrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()
                    val density = LocalDensity.current

                    fun destinoAhoraPx(): Int {
                        val minutosAhora = minutosDesdeInicioFranja(LocalTime.now())
                        val px = with(density) { (xDeMinutos(minutosAhora) - MARGEN_AHORA).toPx() }
                        return px.coerceIn(0f, scrollState.maxValue.toFloat()).roundToInt()
                    }

                    // Al entrar o cambiar de día: si es hoy, centra la línea de "ahora"
                    // (esperando a que el layout tenga ya el ancho real; ScrollState.maxValue
                    // empieza en Int.MAX_VALUE de sentinela hasta que se mide el contenido);
                    // si no es hoy, al principio del día.
                    LaunchedEffect(fecha) {
                        snapshotFlow { scrollState.maxValue }.first { it != Int.MAX_VALUE }
                        scrollState.scrollTo(if (esHoy) destinoAhoraPx() else 0)
                    }

                    // Sin margen lateral global: la cabecera del día y la lista de abajo
                    // llevan el suyo, y la línea de tiempo va de borde a borde de la pantalla.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 12.dp, bottom = 96.dp)
                    ) {
                        TarjetaDia(
                            fecha = fecha,
                            onDiaAnterior = { viewModel.diaAnterior() },
                            onDiaSiguiente = { viewModel.diaSiguiente() },
                            onElegirFecha = { viewModel.irADia(it) },
                            onAhora = {
                                if (esHoy) {
                                    coroutineScope.launch { scrollState.animateScrollTo(destinoAhoraPx()) }
                                } else {
                                    viewModel.hoy()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        if (estadoActual.estado.filas.isEmpty()) {
                            Text(
                                "Todavía no hay niños dados de alta en la familia.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TINTA_SUAVE,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Spacer(Modifier.height(10.dp))
                            LineaDeTiempo(
                                filas = estadoActual.estado.filas,
                                scrollState = scrollState,
                                mostrarAhora = esHoy,
                                iniciales = estadoActual.estado.iniciales,
                                onEditar = { actividadEnEdicion = it }
                            )

                            Spacer(Modifier.height(20.dp))
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CabeceraActividadesDelDia(onAnadir = { actividadEnCreacion = true })
                                estadoActual.estado.filas.forEach { fila ->
                                    TarjetaActividadesDelNino(fila = fila, onEditar = { actividadEnEdicion = it })
                                }
                            }
                        }
                    }

                    if (actividadEnCreacion) {
                        DialogoActividad(
                            fecha = fecha,
                            ninos = estadoActual.estado.filas.map { it.child },
                            actividad = null,
                            responsables = estadoActual.estado.responsables,
                            onGuardar = { needs, aplicarATodaLaSerie ->
                                viewModel.guardarActividades(needs, aplicarATodaLaSerie)
                                actividadEnCreacion = false
                            },
                            onEliminar = null,
                            onCerrar = { actividadEnCreacion = false }
                        )
                    }

                    actividadEnEdicion?.let { need ->
                        // Patrón real de la serie (días de la semana y última fecha), para
                        // preseleccionar el selector de días al abrir "Repetir" en vez de
                        // partir de uno vacío. Se pide de nuevo cada vez que cambia la
                        // actividad en edición.
                        var diasSerieActual by remember(need.id) { mutableStateOf<Set<DayOfWeek>?>(null) }
                        var hastaSerieActual by remember(need.id) { mutableStateOf<LocalDate?>(null) }
                        var patronSerieCargando by remember(need.id) { mutableStateOf(need.grupoRepeticionId != null) }
                        LaunchedEffect(need.id) {
                            val grupoId = need.grupoRepeticionId
                            if (grupoId != null) {
                                val (dias, hasta) = viewModel.patronDeSerie(grupoId, need.fecha)
                                diasSerieActual = dias
                                hastaSerieActual = hasta
                                patronSerieCargando = false
                            }
                        }
                        DialogoActividad(
                            fecha = need.fecha,
                            ninos = estadoActual.estado.filas.map { it.child },
                            actividad = need,
                            responsables = estadoActual.estado.responsables,
                            diasSerieActual = diasSerieActual,
                            hastaSerieActual = hastaSerieActual,
                            patronSerieCargando = patronSerieCargando,
                            onGuardar = { needs, aplicarATodaLaSerie ->
                                viewModel.guardarActividades(needs, aplicarATodaLaSerie)
                                cerrarEdicion()
                            },
                            onEliminar = { aplicarATodaLaSerie ->
                                viewModel.eliminarActividad(need.id, need.grupoRepeticionId, need.fecha, aplicarATodaLaSerie)
                                cerrarEdicion()
                            },
                            onActualizarSerie = { plantilla, nuevasFechas ->
                                viewModel.actualizarSerie(plantilla, nuevasFechas)
                                cerrarEdicion()
                            },
                            onCerrar = { cerrarEdicion() }
                        )
                    }
                }
            }
        }
    }
}

/* ───────────────────────────── Cabecera y tarjeta del día ───────────────────────────── */

/**
 * Tarjeta suave con la cabecera del día: flechas, nombre del día y fecha en el centro,
 * calendario y "punto de disparo" a la derecha — mismos botones circulares que en
 * Semana. La línea de tiempo va fuera, a todo el ancho de la pantalla.
 */
@Composable
private fun TarjetaDia(
    fecha: LocalDate,
    onDiaAnterior: () -> Unit,
    onDiaSiguiente: () -> Unit,
    onElegirFecha: (LocalDate) -> Unit,
    onAhora: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", ES)
    val etiquetaDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }
    var calendarioAbierto by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = LocalEncajaExtraColors.current.tarjetaSuave,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BotonRedondo(Icons.Default.ChevronLeft, "Día anterior", onDiaAnterior)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { calendarioAbierto = true }
                ) {
                    Text(
                        etiquetaDia,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TINTA,
                        maxLines = 1
                    )
                    Text(
                        fecha.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = TINTA_SUAVE,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BotonRedondo(Icons.Default.CalendarMonth, "Ir a una fecha", { calendarioAbierto = true })
                Spacer(Modifier.width(6.dp))
                BotonRedondo(Icons.Default.MyLocation, "Ahora", onAhora)
                Spacer(Modifier.width(6.dp))
                BotonRedondo(Icons.Default.ChevronRight, "Día siguiente", onDiaSiguiente)
            }
        }
    }

    if (calendarioAbierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtc(fecha))
        DatePickerDialog(
            onDismissRequest = { calendarioAbierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onElegirFecha(millisUtcAFecha(it)) }
                    calendarioAbierto = false
                }) { Text("Ir") }
            },
            dismissButton = { TextButton(onClick = { calendarioAbierto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Botón circular con icono índigo, igual que los de la cabecera de Semana. */
@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(BLANCO)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = INDIGO, modifier = Modifier.size(22.dp))
    }
}

/* ───────────────────────────── Línea de tiempo ───────────────────────────── */

/**
 * La línea de tiempo entera, de borde a borde de la pantalla y con scroll horizontal:
 * la regla de horas, la cuadrícula, un carril por niño (con su nombre justo encima,
 * pegado al borde izquierdo de lo que se ve, para que no estorbe pero se lea siempre)
 * y (si es hoy) la pastilla con la hora actual y su línea roja atravesándolo todo.
 */
@Composable
private fun LineaDeTiempo(
    filas: List<FilaGuia>,
    scrollState: ScrollState,
    mostrarAhora: Boolean,
    iniciales: Map<CaregiverId, String>,
    onEditar: (CoverageNeed) -> Unit
) {
    val density = LocalDensity.current
    var anchoVisiblePx by remember { mutableStateOf(0) }
    val colorLinea = TINTA_SUAVE
    val colorAhora = MaterialTheme.colorScheme.error
    val alturaReglaPx = with(density) { ALTO_REGLA.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalEncajaExtraColors.current.tarjetaSuave)
            .padding(vertical = 8.dp)
            .onSizeChanged { anchoVisiblePx = it.width }
            .horizontalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .width(ANCHO_LIENZO)
                .drawBehind {
                    // Cuadrícula: una línea por hora (más visible) y una por cuarto (muy tenue),
                    // desde debajo de la regla hasta el final del último carril.
                    var minuto = 0
                    while (minuto <= MINUTOS_FRANJA) {
                        val x = xDeMinutos(minuto).toPx()
                        val esHora = minuto % 60 == 0
                        drawLine(
                            color = colorLinea.copy(alpha = if (esHora) 0.28f else 0.10f),
                            start = Offset(x, alturaReglaPx - 6.dp.toPx()),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        minuto += 15
                    }
                }
        ) {
            Column {
                ReglaHoras()
                filas.forEachIndexed { indice, fila ->
                    if (indice > 0) Spacer(Modifier.height(ESPACIO_FILAS))
                    NombreDeCarril(fila.child.nombre, scrollState)
                    FilaTimelineDelNino(
                        fila = fila,
                        scrollState = scrollState,
                        iniciales = iniciales,
                        anchoVisiblePx = anchoVisiblePx,
                        onEditar = onEditar
                    )
                }
            }

            if (mostrarAhora) {
                val xAhora = xDeMinutos(minutosDesdeInicioFranja(LocalTime.now()))
                // Línea vertical desde la pastilla hasta el final de todos los carriles. Va
                // dentro de una caja matchParentSize porque estamos bajo un scroll
                // vertical (altura sin límite): fillMaxHeight a secas mediría 0.
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .offset(x = xAhora - 1.dp)
                            .padding(top = ALTO_PASTILLA_AHORA - 2.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(colorAhora.copy(alpha = 0.85f))
                    )
                }
                // Pastilla con la hora, centrada sobre la línea.
                Box(
                    modifier = Modifier
                        .offset(x = xAhora - ANCHO_PASTILLA_AHORA / 2)
                        .size(width = ANCHO_PASTILLA_AHORA, height = ALTO_PASTILLA_AHORA)
                        .clip(RoundedCornerShape(50))
                        .background(colorAhora),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatearHora(LocalTime.now()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

/** Nombre del niño encima de su carril. Se desplaza con el scroll para quedarse siempre
 * pegado al borde izquierdo de la parte visible, sin ocupar sitio en el carril. */
@Composable
private fun NombreDeCarril(nombre: String, scrollState: ScrollState) {
    Box(modifier = Modifier.width(ANCHO_LIENZO).height(ALTO_NOMBRE)) {
        Text(
            nombre,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = TINTA,
            maxLines = 1,
            modifier = Modifier
                .offset { IntOffset(scrollState.value, 0) }
                .padding(start = 16.dp)
        )
    }
}

/** Regla de horas: cada etiqueta centrada sobre su marca de la cuadrícula, pegada abajo
 * (arriba queda el hueco para la pastilla de "ahora"). */
@Composable
private fun ReglaHoras() {
    val anchoHora = ANCHO_MINUTO * 60
    Box(modifier = Modifier.width(ANCHO_LIENZO).height(ALTO_REGLA)) {
        var hora = INICIO_FRANJA
        while (!hora.isAfter(FIN_FRANJA)) {
            val minutos = minutosDesdeInicioFranja(hora)
            Box(
                modifier = Modifier
                    .offset(x = xDeMinutos(minutos) - anchoHora / 2)
                    .width(anchoHora)
                    .fillMaxHeight()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    formatearHora(hora),
                    style = MaterialTheme.typography.labelMedium,
                    color = TINTA_SUAVE
                )
            }
            if (hora == FIN_FRANJA) break
            hora = hora.plusHours(1)
        }
    }
}

@Composable
private fun FilaTimelineDelNino(
    fila: FilaGuia,
    scrollState: ScrollState,
    iniciales: Map<CaregiverId, String>,
    anchoVisiblePx: Int,
    onEditar: (CoverageNeed) -> Unit
) {
    Box(modifier = Modifier.width(ANCHO_LIENZO).height(ALTO_FILA)) {
        if (fila.bloques.isEmpty()) {
            FilaSinActividades(scrollState, anchoVisiblePx)
        }
        fila.bloques.forEach { bloque ->
            BloqueActividad(bloque = bloque, scrollState = scrollState, iniciales = iniciales, onEditar = onEditar)
        }
    }
}

/**
 * Caja de borde discontinuo a lo largo de todo el día con "Sin actividades" en el centro
 * de la parte visible: como el lienzo mide varias pantallas de ancho, el texto se
 * desplaza con el scroll para quedar siempre a la vista.
 */
@Composable
private fun FilaSinActividades(scrollState: ScrollState, anchoVisiblePx: Int) {
    val colorBorde = TINTA_SUAVE.copy(alpha = 0.45f)
    var anchoTextoPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val anchoLienzoPx = with(density) { ANCHO_LIENZO.toPx() }
    val xTexto = (scrollState.value + anchoVisiblePx / 2f - anchoTextoPx / 2f)
        .coerceIn(0f, (anchoLienzoPx - anchoTextoPx).coerceAtLeast(0f))
        .roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MARGEN_LIENZO, vertical = 4.dp)
            .bordeDiscontinuo(colorBorde, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            "Sin actividades",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TINTA_SUAVE,
            modifier = Modifier
                .offset { IntOffset(xTexto, 0) }
                .onSizeChanged { anchoTextoPx = it.width }
        )
    }
}

/** Borde de trazo discontinuo (Compose no lo trae de serie) con la forma redondeada dada. */
private fun Modifier.bordeDiscontinuo(color: Color, forma: RoundedCornerShape): Modifier = this.drawBehind {
    val radio = 16.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radio, radio),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    )
}.clip(forma)

/**
 * Un bloque de actividad en la línea de tiempo: icono en círculo, nombre y horario. El
 * contenido se mantiene visible mientras el bloque avanza por la pantalla (se desplaza
 * dentro del propio bloque a medida que se hace scroll), siempre que quepa entero en su
 * ancho; si no cabe, se queda al principio, dejando hueco a los lados para no quedar
 * tapado por los iconos de quién lleva (inicio) y quién recoge (fin), si se han elegido.
 */
@Composable
private fun BloqueActividad(
    bloque: BloqueGuia,
    scrollState: ScrollState,
    iniciales: Map<CaregiverId, String>,
    onEditar: (CoverageNeed) -> Unit
) {
    val density = LocalDensity.current
    val inicioMin = minutosDesdeInicioFranja(bloque.need.horaInicio)
    val finMin = minutosDesdeInicioFranja(bloque.need.horaFin).coerceAtLeast(inicioMin + 20)
    val inicioPx = with(density) { xDeMinutos(inicioMin).toPx() }
    val anchoBloquePx = with(density) { (ANCHO_MINUTO * (finMin - inicioMin)).toPx() }

    val fondo = if (bloque.cubierto) VERDE else ROJO
    val tinta = if (bloque.cubierto) ON_VERDE else ON_ROJO

    // Reserva de espacio para que el contenido nunca quede debajo de los iconos. El
    // padding de inicio no mueve el punto de partida del cálculo de scroll (offset no
    // consume espacio de layout), así que hay que restar los dos lados al margen máximo.
    val paddingInicio = if (bloque.quienLleva != null) ANCHO_ICONO_RESPONSABLE + 8.dp else 8.dp
    val paddingFin = if (bloque.quienRecoge != null) ANCHO_ICONO_RESPONSABLE + 8.dp else 8.dp
    val paddingInicioPx = with(density) { paddingInicio.toPx() }
    val paddingFinPx = with(density) { paddingFin.toPx() }

    var anchoContenidoPx by remember { mutableStateOf(0f) }
    val margenMaximoPx = (anchoBloquePx - anchoContenidoPx - paddingInicioPx - paddingFinPx).coerceAtLeast(0f)
    val offsetContenidoPx = (scrollState.value - inicioPx).coerceIn(0f, margenMaximoPx)
    val offsetContenidoDp = with(density) { offsetContenidoPx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = xDeMinutos(inicioMin))
            .width(ANCHO_MINUTO * (finMin - inicioMin))
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .border(1.dp, tinta.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onEditar(bloque.need) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = paddingInicio, end = paddingFin)
                .offset(x = offsetContenidoDp)
                .onGloballyPositioned { coordenadas -> anchoContenidoPx = coordenadas.size.width.toFloat() }
        ) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(tinta.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = tinta, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    bloque.need.descripcion,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tinta,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    "${formatearHora(bloque.need.horaInicio)} – ${formatearHora(bloque.need.horaFin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tinta,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        // Se dibujan después del contenido para quedar siempre por encima, aunque el
        // contenido se desplace al hacer scroll.
        bloque.quienLleva?.let { responsable ->
            IconoResponsable(responsable, iniciales, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
        }
        bloque.quienRecoge?.let { responsable ->
            IconoResponsable(responsable, iniciales, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
        }
    }
}

/** Círculo con las iniciales (2 letras, persona o unidad familiar) de quien lleva/recoge. */
@Composable
private fun IconoResponsable(responsable: Responsable, iniciales: Map<CaregiverId, String>, modifier: Modifier = Modifier) {
    val texto = when (responsable) {
        is Responsable.Persona -> iniciales[responsable.caregiver.id] ?: responsable.caregiver.nombre.take(2).uppercase()
        is Responsable.Unidad -> responsable.unidad.codigo.take(2).uppercase()
    }
    Box(
        modifier = modifier
            .size(ANCHO_ICONO_RESPONSABLE)
            .clip(CircleShape)
            .background(BLANCO.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TINTA, maxLines = 1, softWrap = false)
    }
}

/* ───────────────────────────── Actividades del día ───────────────────────────── */

/** Icono de lista, título y, a la derecha, un botón redondo "+" en color de acento
 * (en vez de "+ Añadir" con texto, que le comía sitio al título). */
@Composable
private fun CabeceraActividadesDelDia(onAnadir: () -> Unit) {
    val extra = LocalEncajaExtraColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.FormatListBulleted,
            contentDescription = null,
            tint = TINTA_SUAVE,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Actividades del día",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TINTA,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(extra.acento.copy(alpha = 0.18f))
                .clickable(onClick = onAnadir),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva actividad", tint = extra.acento, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Tarjeta de un niño con sus actividades del día en formato de lista (hora, nombre y
 * quién lleva/recoge): mismo contenido que los bloques de arriba pero más fácil de
 * repasar entero sin scroll horizontal. Tocar una actividad la abre para editarla.
 */
@Composable
private fun TarjetaActividadesDelNino(fila: FilaGuia, onEditar: (CoverageNeed) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = LocalEncajaExtraColors.current.tarjetaSuave,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fila.child.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TINTA
                )
            }
            Spacer(Modifier.height(12.dp))

            if (fila.bloques.isEmpty()) {
                SinActividadesEnLista()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.bloques.sortedBy { it.need.horaInicio }.forEach { bloque ->
                        FilaActividadEnLista(bloque = bloque, onClick = { onEditar(bloque.need) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaActividadEnLista(bloque: BloqueGuia, onClick: () -> Unit) {
    val franja = if (bloque.cubierto) ON_VERDE else MaterialTheme.colorScheme.error
    val tintaIcono = if (bloque.cubierto) ON_VERDE else ON_ROJO
    val fondoIcono = if (bloque.cubierto) VERDE else ROJO

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BLANCO)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min)
    ) {
        Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(franja))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(fondoIcono),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = tintaIcono, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${formatearHora(bloque.need.horaInicio)} – ${formatearHora(bloque.need.horaFin)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TINTA
                    )
                    Text(bloque.need.descripcion, style = MaterialTheme.typography.bodyMedium, color = TINTA_SUAVE)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TINTA_SUAVE)
            }

            val lleva = bloque.quienLleva?.etiqueta
            val recoge = bloque.quienRecoge?.etiqueta
            if (lleva != null || recoge != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = TINTA_SUAVE,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        lleva?.let { LineaResponsable("Lleva:", it) }
                        recoge?.let { LineaResponsable("Recoge:", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineaResponsable(etiqueta: String, nombre: String) {
    Row {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
        Spacer(Modifier.width(4.dp))
        Text(nombre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TINTA)
    }
}

/** Caja de borde discontinuo con icono de calendario y explicación, para un niño sin actividades. */
@Composable
private fun SinActividadesEnLista() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bordeDiscontinuo(TINTA_SUAVE.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(BLANCO),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TINTA_SUAVE, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Sin actividades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = TINTA)
            Text(
                "No hay ninguna actividad programada para este día.",
                style = MaterialTheme.typography.bodySmall,
                color = TINTA_SUAVE
            )
        }
    }
}

/** Minutos transcurridos desde INICIO_FRANJA, recortados a la propia franja (7:00–22:00). */
private fun minutosDesdeInicioFranja(hora: LocalTime): Int {
    val minutos = java.time.Duration.between(INICIO_FRANJA, hora).toMinutes().toInt()
    return minutos.coerceIn(0, MINUTOS_FRANJA)
}
