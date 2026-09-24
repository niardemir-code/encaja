package com.encaja.app.ui.guia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Línea de tiempo real (no comprimida en el ancho de pantalla): cada niño
// tiene una fila con sus actividades a su hora y duración exactas, y se
// hace scroll horizontal para ver las próximas. Una línea vertical marca
// la hora actual (solo si se está viendo el día de hoy) y el botón "Ahora"
// de la cabecera vuelve a ese punto (y al día de hoy, si se había cambiado).

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.ui.familia.Responsable
import com.encaja.app.ui.familia.formatearHora
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// La franja horaria que se representa en la guía: de 7:00 a 22:00.
private val INICIO_FRANJA: LocalTime = LocalTime.of(7, 0)
private val FIN_FRANJA: LocalTime = LocalTime.of(22, 0)
private val MINUTOS_FRANJA = java.time.Duration.between(INICIO_FRANJA, FIN_FRANJA).toMinutes().toInt()

// Escala de la línea de tiempo: cada minuto ocupa este ancho real (no se comprime
// para caber en pantalla), por eso hace falta scroll horizontal para ver todo el día.
private val ANCHO_MINUTO: Dp = 2.dp
private val ANCHO_TOTAL_FRANJA: Dp = ANCHO_MINUTO * MINUTOS_FRANJA
private val ANCHO_NOMBRE: Dp = 84.dp
private val MARGEN_AHORA: Dp = 72.dp

// Icono de quién lleva/recoge: bastante grande para que sus 2 letras se lean bien
// dentro del bloque (que mide 56.dp de alto).
private val ANCHO_ICONO_RESPONSABLE: Dp = 32.dp

private val VERDE = Color(0xFFC0DD97)
private val ROJO = Color(0xFFF7C1C1)

@Composable
fun GuiaScreen(viewModel: GuiaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    var actividadEnCreacion by remember { mutableStateOf(false) }
    var actividadEnEdicion by remember { mutableStateOf<CoverageNeed?>(null) }

    // El ViewModel sobrevive a los cambios de pestaña; se recarga al reentrar para
    // reflejar cambios hechos desde otra pestaña (p.ej. borrar una actividad).
    LaunchedEffect(Unit) { viewModel.recargar() }

    val estadoConDatos = pantalla as? GuiaPantallaEstado.ConDatos

    Scaffold(
        floatingActionButton = {
            if (estadoConDatos != null && estadoConDatos.estado.filas.isNotEmpty()) {
                FloatingActionButton(onClick = { actividadEnCreacion = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva actividad")
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
                            textAlign = TextAlign.Center
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
                        val px = with(density) { (ANCHO_MINUTO * minutosAhora - MARGEN_AHORA).toPx() }
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

                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        CabeceraDeDia(
                            fecha = fecha,
                            onDiaAnterior = { viewModel.diaAnterior() },
                            onDiaSiguiente = { viewModel.diaSiguiente() },
                            onAhora = {
                                if (esHoy) {
                                    coroutineScope.launch { scrollState.animateScrollTo(destinoAhoraPx()) }
                                } else {
                                    viewModel.hoy()
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))

                        if (estadoActual.estado.filas.isEmpty()) {
                            Text(
                                "Todavía no hay niños dados de alta en la familia.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ReglaHoras(scrollState)
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                estadoActual.estado.filas.forEach { fila ->
                                    FilaTimelineDelNino(
                                        fila = fila,
                                        scrollState = scrollState,
                                        mostrarAhora = esHoy,
                                        iniciales = estadoActual.estado.iniciales,
                                        onEditar = { actividadEnEdicion = it }
                                    )
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
                        DialogoActividad(
                            fecha = need.fecha,
                            ninos = estadoActual.estado.filas.map { it.child },
                            actividad = need,
                            responsables = estadoActual.estado.responsables,
                            onGuardar = { needs, aplicarATodaLaSerie ->
                                viewModel.guardarActividades(needs, aplicarATodaLaSerie)
                                actividadEnEdicion = null
                            },
                            onEliminar = { aplicarATodaLaSerie ->
                                viewModel.eliminarActividad(need.id, need.grupoRepeticionId, need.fecha, aplicarATodaLaSerie)
                                actividadEnEdicion = null
                            },
                            onCerrar = { actividadEnEdicion = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraDeDia(fecha: LocalDate, onDiaAnterior: () -> Unit, onDiaSiguiente: () -> Unit, onAhora: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
    val etiquetaDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDiaAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Día anterior")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(etiquetaDia, style = MaterialTheme.typography.titleMedium)
            Text(fecha.format(formatter), style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAhora) {
                Icon(Icons.Default.MyLocation, contentDescription = "Ahora")
            }
            IconButton(onClick = onDiaSiguiente) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Día siguiente")
            }
        }
    }
}

/** Regla de horas alineada con las filas de abajo: deja hueco a la izquierda para el nombre. */
@Composable
private fun ReglaHoras(scrollState: ScrollState) {
    Row(verticalAlignment = Alignment.Bottom) {
        Spacer(Modifier.width(ANCHO_NOMBRE))
        Row(modifier = Modifier.horizontalScroll(scrollState).width(ANCHO_TOTAL_FRANJA)) {
            var hora = INICIO_FRANJA
            while (hora.isBefore(FIN_FRANJA)) {
                Box(modifier = Modifier.width(ANCHO_MINUTO * 60)) {
                    Text(
                        formatearHora(hora),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                hora = hora.plusHours(1)
            }
        }
    }
}

@Composable
private fun FilaTimelineDelNino(
    fila: FilaGuia,
    scrollState: ScrollState,
    mostrarAhora: Boolean,
    iniciales: Map<CaregiverId, String>,
    onEditar: (CoverageNeed) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            fila.child.nombre,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.width(ANCHO_NOMBRE)
        )
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(ANCHO_TOTAL_FRANJA)
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (fila.bloques.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin actividades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            fila.bloques.forEach { bloque ->
                BloqueActividad(bloque = bloque, scrollState = scrollState, iniciales = iniciales, onEditar = onEditar)
            }
            if (mostrarAhora) {
                val minutosAhora = minutosDesdeInicioFranja(LocalTime.now())
                Box(
                    modifier = Modifier
                        .offset(x = ANCHO_MINUTO * minutosAhora)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

/**
 * Un bloque de actividad en la línea de tiempo. El nombre se mantiene visible mientras
 * el bloque avanza por la pantalla (se desplaza dentro del propio bloque a medida que se
 * hace scroll), siempre que quepa entero en su ancho; si no cabe, se queda al principio
 * como antes, dejando hueco a los lados para no quedar tapado por los iconos de quién
 * lleva (inicio) y quién recoge (fin) al niño, si se han elegido.
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
    val inicioPx = with(density) { (ANCHO_MINUTO * inicioMin).toPx() }
    val anchoBloquePx = with(density) { (ANCHO_MINUTO * (finMin - inicioMin)).toPx() }

    // Reserva de espacio para que el texto nunca quede debajo de los iconos. El padding
    // de inicio no mueve el punto de partida del cálculo de scroll (offset no consume
    // espacio de layout), así que hay que restar los dos lados al margen máximo.
    val paddingInicioTexto = if (bloque.quienLleva != null) ANCHO_ICONO_RESPONSABLE + 6.dp else 6.dp
    val paddingFinTexto = if (bloque.quienRecoge != null) ANCHO_ICONO_RESPONSABLE + 6.dp else 6.dp
    val paddingInicioTextoPx = with(density) { paddingInicioTexto.toPx() }
    val paddingFinTextoPx = with(density) { paddingFinTexto.toPx() }

    var labelWidthPx by remember { mutableStateOf(0f) }
    val margenMaximoPx = (anchoBloquePx - labelWidthPx - paddingInicioTextoPx - paddingFinTextoPx).coerceAtLeast(0f)
    val offsetLabelPx = (scrollState.value - inicioPx).coerceIn(0f, margenMaximoPx)
    val offsetLabelDp = with(density) { offsetLabelPx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = ANCHO_MINUTO * inicioMin)
            .width(ANCHO_MINUTO * (finMin - inicioMin))
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (bloque.cubierto) VERDE else ROJO)
            .clickable { onEditar(bloque.need) }
    ) {
        Text(
            bloque.need.descripcion,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier
                .padding(start = paddingInicioTexto, end = paddingFinTexto, top = 6.dp, bottom = 6.dp)
                .offset(x = offsetLabelDp)
                .onGloballyPositioned { coordenadas -> labelWidthPx = coordenadas.size.width.toFloat() }
        )
        // Se dibujan después del texto para quedar siempre por encima, aunque el
        // texto se desplace al hacer scroll.
        bloque.quienLleva?.let { responsable ->
            IconoResponsable(responsable, iniciales, modifier = Modifier.align(Alignment.CenterStart).padding(start = 3.dp))
        }
        bloque.quienRecoge?.let { responsable ->
            IconoResponsable(responsable, iniciales, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp))
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** Minutos transcurridos desde INICIO_FRANJA, recortados a la propia franja (7:00–22:00). */
private fun minutosDesdeInicioFranja(hora: LocalTime): Int {
    val minutos = java.time.Duration.between(INICIO_FRANJA, hora).toMinutes().toInt()
    return minutos.coerceIn(0, MINUTOS_FRANJA)
}
