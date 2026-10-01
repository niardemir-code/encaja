@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.encaja.app.ui.semana

// NOTA: igual que el ViewModel, este archivo depende de Jetpack Compose
// y no ha podido compilarse en este entorno (sin acceso al repositorio
// de Google). Reproduce fielmente la maqueta de "Esta semana": círculos
// de día y tarjeta de hueco con acciones, con los tres estados: cargando,
// sin familia, con datos. El botón de invitar vive en la barra superior
// (EncajaApp.kt), no aquí.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Anuncio
import com.encaja.app.domain.model.AnuncioId
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.Child
import com.encaja.app.domain.model.CoverageNeed
import com.encaja.app.domain.model.Hueco
import com.encaja.app.domain.model.MotivoHueco
import com.encaja.app.domain.usecase.AvisoConflicto
import com.encaja.app.domain.usecase.RolResponsable
import com.encaja.app.ui.familia.etiquetaMotivo
import com.encaja.app.ui.familia.fechaAMillisUtc
import com.encaja.app.ui.familia.millisUtcAFecha
import com.encaja.app.ui.guia.DialogoActividad
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")

// Atajos a los colores del tema activo (Cálido/Nocturno) con los nombres del diseño.
private val TINTA: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val TINTA_SUAVE: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val INDIGO: Color @Composable get() = MaterialTheme.colorScheme.primary
private val LAVANDA: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val LAVANDA_TARJETA: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
private val BLANCO: Color @Composable get() = MaterialTheme.colorScheme.surface

@Composable
fun SemanaScreen(
    viewModel: SemaforoViewModel = hiltViewModel(),
    // Al tocar el círculo de un día del semáforo que ya no tiene un aviso o hueco
    // "vigente" que abrir: se salta a ese día en la Guía, sin intentar abrir ninguna
    // actividad en concreto.
    onVerDia: (LocalDate) -> Unit = {}
) {
    val pantalla by viewModel.pantalla.collectAsState()

    // Posición de la lista. Vive aquí arriba (no dentro de la rama "con datos") y en
    // rememberSaveable: así sobrevive tanto a la recarga al reentrar como al viaje a la
    // Guía y vuelta (popBackStack recompone esta pantalla desde cero y solo lo guardado
    // en el estado de la entrada de navegación se recupera).
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val coroutineScope = rememberCoroutineScope()

    // El ViewModel se conserva al cambiar de pestaña, así que sin esto se seguiría
    // viendo lo que había la última vez que se estuvo aquí — p.ej. una actividad
    // recién creada en Guía no aparecería en Semana hasta cambiar de semana y volver.
    // Al reentrar en la pestaña se vuelve a componer desde cero, así que esto se
    // ejecuta cada vez.
    LaunchedEffect(Unit) { viewModel.recargar() }

    when (val estadoActual = pantalla) {
        is SemaforoPantallaEstado.Cargando -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is SemaforoPantallaEstado.SinFamilia -> {
            var codigo by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Todavía no perteneces a ninguna familia", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase(); error = null },
                    label = { Text("Código de invitación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.canjearCodigo(codigo) { mensaje -> error = mensaje } },
                    enabled = codigo.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unirme con este código")
                }
            }
        }

        is SemaforoPantallaEstado.ConDatos -> {
            val uiState = estadoActual.estado
            val cuidadores by viewModel.cuidadores.collectAsState()
            val ninos by viewModel.ninos.collectAsState()
            val responsables by viewModel.responsables.collectAsState()
            var diaInfoAbierto by remember { mutableStateOf<DiaSemaforo?>(null) }
            // Actividad abierta en edición desde un aviso o un hueco: el mismo diálogo que
            // en la Guía, pero sin salir de esta pantalla.
            var actividadEnEdicion by remember { mutableStateOf<CoverageNeed?>(null) }

            // Índice en la lista de la tarjeta de hueco/aviso de un día, para desplazarse
            // hasta ella al tocar su círculo del semáforo. La lista es: tarjeta de la
            // semana (0), tablón (1), luego los huecos y luego los avisos.
            val primerIndiceHuecos = 2
            val primerIndiceAvisos = primerIndiceHuecos + uiState.huecosDeLaSemana.size
            fun indiceDeTarjeta(dia: DiaSemaforo): Int? = when (dia.estado) {
                EstadoDia.ROJO -> uiState.huecosDeLaSemana.indexOfFirst { it.need.fecha == dia.fecha }
                    .takeIf { it >= 0 }?.let { primerIndiceHuecos + it }
                EstadoDia.AMBAR -> uiState.avisos.indexOfFirst { it.need.fecha == dia.fecha }
                    .takeIf { it >= 0 }?.let { primerIndiceAvisos + it }
                else -> null
            }

            // Tocar en cualquier sitio fuera del campo de texto le quita el foco (y cierra el
            // teclado): si no, el cursor se quedaba parpadeando en el tablón.
            val focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            ) {
                OlasDeFondo()
                LazyColumn(
                    state = listState,
                    // imePadding: con el teclado abierto la lista se encoge en vez de quedar
                    // tapada, para que el campo del tablón siga a la vista mientras se escribe.
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TarjetaSemana(
                            dias = uiState.dias,
                            esSemanaActual = uiState.esSemanaActual,
                            onSemanaAnterior = { viewModel.cambiarSemana(-1) },
                            onSemanaSiguiente = { viewModel.cambiarSemana(1) },
                            onIrASemanaActual = { viewModel.irASemanaActual() },
                            onElegirFecha = { viewModel.irASemanaDe(it) },
                            onClickDia = { dia ->
                                when (dia.estado) {
                                    // Rojo/ámbar: baja hasta la tarjeta del hueco o aviso de ese
                                    // día, aquí mismo en Semana (desde ella ya se puede abrir la
                                    // edición). Si ya no hay tarjeta (día pasado), se va al día
                                    // en la Guía.
                                    EstadoDia.ROJO, EstadoDia.AMBAR -> {
                                        val indice = indiceDeTarjeta(dia)
                                        if (indice != null) {
                                            coroutineScope.launch { listState.animateScrollToItem(indice) }
                                        } else {
                                            onVerDia(dia.fecha)
                                        }
                                    }
                                    EstadoDia.VERDE -> diaInfoAbierto = dia
                                    EstadoDia.SIN_DATOS -> Unit
                                }
                            }
                        )
                    }

                    item {
                        TablonDeAnuncios(
                            anuncios = uiState.anuncios,
                            onPublicar = { texto -> viewModel.publicarAnuncio(texto) },
                            onEliminar = { anuncioId -> viewModel.eliminarAnuncio(anuncioId) }
                        )
                    }

                    items(uiState.huecosDeLaSemana) { hueco ->
                        TarjetaHueco(
                            hueco = hueco,
                            nombreNino = ninos.firstOrNull { it.id == hueco.need.childId }?.nombre,
                            onClick = { actividadEnEdicion = hueco.need }
                        )
                    }

                    items(uiState.avisos) { aviso ->
                        TarjetaAvisoConflicto(aviso = aviso, onClick = { actividadEnEdicion = aviso.need })
                    }
                }
            }

            actividadEnEdicion?.let { need ->
                // Igual que en la Guía: se pide el patrón real de la serie para
                // preseleccionar los días al abrir "Repetir".
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
                    ninos = ninos,
                    actividad = need,
                    responsables = responsables,
                    diasSerieActual = diasSerieActual,
                    hastaSerieActual = hastaSerieActual,
                    patronSerieCargando = patronSerieCargando,
                    onGuardar = { needs, aplicarATodaLaSerie ->
                        viewModel.guardarActividades(needs, aplicarATodaLaSerie)
                        actividadEnEdicion = null
                    },
                    onEliminar = { aplicarATodaLaSerie ->
                        viewModel.eliminarActividad(need.id, need.grupoRepeticionId, need.fecha, aplicarATodaLaSerie)
                        actividadEnEdicion = null
                    },
                    onActualizarSerie = { plantilla, nuevasFechas ->
                        viewModel.actualizarSerie(plantilla, nuevasFechas)
                        actividadEnEdicion = null
                    },
                    onCerrar = { actividadEnEdicion = null }
                )
            }

            diaInfoAbierto?.let { dia ->
                DialogoInfoDia(
                    dia = dia,
                    cuidadores = cuidadores,
                    ninos = ninos,
                    onCerrar = { diaInfoAbierto = null }
                )
            }
        }
    }
}

/** Dos olas suaves (lavanda y melocotón) en la parte baja de la pantalla, como en la
 * maqueta. Se dibujan detrás de la lista y con mucha transparencia: solo decoran. */
@Composable
private fun OlasDeFondo() {
    val lavanda = LAVANDA
    val melocoton = MaterialTheme.colorScheme.tertiaryContainer
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val ola1 = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.25f, h * 0.70f, w * 0.45f, h * 0.92f, w * 0.72f, h * 0.84f)
            cubicTo(w * 0.88f, h * 0.79f, w * 0.95f, h * 0.74f, w, h * 0.70f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        val ola2 = Path().apply {
            moveTo(0f, h * 0.90f)
            cubicTo(w * 0.30f, h * 0.82f, w * 0.55f, h * 1.0f, w * 0.80f, h * 0.92f)
            cubicTo(w * 0.90f, h * 0.89f, w * 0.96f, h * 0.86f, w, h * 0.84f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(ola1, lavanda.copy(alpha = 0.55f))
        drawPath(ola2, melocoton.copy(alpha = 0.45f))
    }
}

/**
 * Tarjeta lavanda de la cabecera: flechas para retroceder/avanzar una semana, el
 * título y el rango de fechas en el centro, y a la derecha el calendario (saltar a la
 * semana de cualquier fecha) y el "punto de disparo" para volver a la semana actual
 * (mismo icono y criterio que en Guía, Familia y Menú). Debajo, la fila de los siete
 * días con su color de semáforo y un punto bajo el día de hoy.
 */
@Composable
private fun TarjetaSemana(
    dias: List<DiaSemaforo>,
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit,
    onElegirFecha: (LocalDate) -> Unit,
    onClickDia: (DiaSemaforo) -> Unit = {}
) {
    val lunes = dias.firstOrNull()?.fecha
    var calendarioAbierto by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = LocalEncajaExtraColors.current.tarjetaSuave,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BotonRedondo(Icons.Default.ChevronLeft, "Semana anterior", onSemanaAnterior)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).clickable { calendarioAbierto = true }
                ) {
                    Text(
                        if (esSemanaActual) "Esta semana" else "Semana",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TINTA,
                        maxLines = 1
                    )
                    if (lunes != null) {
                        Text(
                            textoRangoSemana(lunes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TINTA_SUAVE,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BotonRedondo(Icons.Default.CalendarMonth, "Ir a una semana", { calendarioAbierto = true })
                Spacer(Modifier.width(6.dp))
                BotonRedondo(Icons.Default.MyLocation, "Ir a la semana actual", onIrASemanaActual, habilitado = !esSemanaActual)
                Spacer(Modifier.width(6.dp))
                BotonRedondo(Icons.Default.ChevronRight, "Semana siguiente", onSemanaSiguiente)
            }

            Spacer(Modifier.height(16.dp))
            FilaDeDias(dias, onClickDia)
        }
    }

    if (calendarioAbierto) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fechaAMillisUtc(lunes ?: LocalDate.now())
        )
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

/** "21 – 27 de septiembre", o "28 sept – 4 oct" si la semana cambia de mes: corto para
 * que quepa en una línea entre los cuatro botones de la cabecera. */
private fun textoRangoSemana(lunes: LocalDate): String {
    val domingo = lunes.plusDays(6)
    return if (lunes.month == domingo.month) {
        "${lunes.dayOfMonth} – ${domingo.dayOfMonth} de ${domingo.month.getDisplayName(TextStyle.FULL, ES)}"
    } else {
        "${lunes.dayOfMonth} ${mesCorto(lunes)} – ${domingo.dayOfMonth} ${mesCorto(domingo)}"
    }
}

private fun mesCorto(fecha: LocalDate): String =
    fecha.month.getDisplayName(TextStyle.SHORT, ES).removeSuffix(".")

/** Botón circular blanco con icono índigo, como los de la maqueta. Atenuado si no
 * está habilitado (p.ej. "ir a la semana actual" cuando ya se está en ella). */
@Composable
private fun BotonRedondo(icono: ImageVector, descripcion: String, onClick: () -> Unit, habilitado: Boolean = true) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(BLANCO)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icono,
            contentDescription = descripcion,
            tint = if (habilitado) INDIGO else INDIGO.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Botón de "invitar a alguien": ahora vive en la barra superior (a la izquierda del
 * avatar), no en la lista de Semana, así que se puede usar desde cualquier pestaña.
 * Se le pasa el mismo [SemaforoViewModel] de la pestaña Semana (obtenido en
 * EncajaApp a partir de su propia entrada del back stack), de donde saca la lista
 * de cuidadores y genera el código.
 */
@Composable
fun BotonInvitar(viewModel: SemaforoViewModel) {
    var mostrarSelector by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<String?>(null) }
    var errorInvitacion by remember { mutableStateOf<String?>(null) }
    var copiado by remember { mutableStateOf(false) }
    val cuidadores by viewModel.cuidadores.collectAsState()
    val clipboard = LocalClipboardManager.current

    BotonBarraSuperior(Icons.Default.PersonAdd, "Invitar a alguien") { mostrarSelector = true }

    if (mostrarSelector) {
        AlertDialog(
            onDismissRequest = {
                mostrarSelector = false
                codigoGenerado = null
                errorInvitacion = null
                copiado = false
            },
            title = { Text(if (codigoGenerado != null) "Código generado" else "Invitación para cuidadores") },
            text = {
                Column {
                    when {
                        codigoGenerado != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    codigoGenerado!!,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(codigoGenerado!!))
                                    copiado = true
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código")
                                }
                            }
                            if (copiado) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Copiado al portapapeles",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Compártelo con esa persona. Deja de funcionar en cuanto se use una vez.")
                        }
                        errorInvitacion != null -> {
                            Text(errorInvitacion!!, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {
                            cuidadores.forEach { caregiver ->
                                TextButton(onClick = {
                                    viewModel.generarInvitacion(
                                        caregiver.id,
                                        alConseguirlo = { codigo -> codigoGenerado = codigo },
                                        alFallar = { mensaje -> errorInvitacion = mensaje }
                                    )
                                }) {
                                    Text(caregiver.nombreCompleto)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSelector = false
                    codigoGenerado = null
                    errorInvitacion = null
                    copiado = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/** Icono dentro de un círculo lavanda muy claro, como los botones de la barra superior
 * de la maqueta (invitar, ajustes). Público porque EncajaApp lo usa para Ajustes. */
@Composable
fun BotonBarraSuperior(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

/** Los siete días: letra arriba y círculo con el número, coloreado según el semáforo
 * (lavanda = sin actividades, verde = cubierto, ámbar = aviso, rojo = hueco). El día
 * de hoy lleva además un puntito ámbar debajo. Cada círculo es clicable (salvo los
 * "sin datos", que no tienen nada que abrir): ámbar/rojo llevan al aviso o hueco de
 * ese día en la Guía, y verde abre un resumen de las actividades del día. */
@Composable
private fun FilaDeDias(dias: List<DiaSemaforo>, onClickDia: (DiaSemaforo) -> Unit = {}) {
    val hoy = LocalDate.now()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dias.forEach { dia ->
            val esHoy = dia.fecha == hoy
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TINTA_SUAVE
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorParaEstado(dia.estado))
                        .clickable(enabled = dia.estado != EstadoDia.SIN_DATOS) { onClickDia(dia) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dia.fecha.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorTextoParaEstado(dia.estado)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (esHoy) LocalEncajaExtraColors.current.ambar else Color.Transparent)
                )
            }
        }
    }
}

/** Icono en un círculo, como cabecera de cada tarjeta (megáfono, aviso, hueco). */
@Composable
private fun IconoDeTarjeta(icono: ImageVector, fondo: Color, tinta: Color) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun TablonDeAnuncios(
    anuncios: List<Anuncio>,
    onPublicar: (String) -> Unit,
    onEliminar: (AnuncioId) -> Unit
) {
    var textoNuevo by remember { mutableStateOf("") }
    val extra = LocalEncajaExtraColors.current
    // Cada vez que el texto crece (o al enfocar el campo), se pide que el campo quede a
    // la vista: si no, a medida que se escribe se va quedando escondido tras el teclado.
    val traerALaVista = remember { BringIntoViewRequester() }
    var conFoco by remember { mutableStateOf(false) }
    val focusManagerTablon = LocalFocusManager.current
    LaunchedEffect(textoNuevo, conFoco) {
        if (conFoco) traerALaVista.bringIntoView()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = LAVANDA_TARJETA
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoDeTarjeta(Icons.Default.Campaign, fondo = LAVANDA, tinta = INDIGO)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Tablón de anuncios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TINTA
                )
            }
            Spacer(Modifier.height(12.dp))

            if (anuncios.isEmpty()) {
                Text(
                    "Todavía no hay ningún anuncio.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TINTA_SUAVE
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    anuncios.forEach { anuncio ->
                        FilaAnuncio(anuncio = anuncio, onEliminar = { onEliminar(anuncio.id) })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // A todo lo ancho y sin singleLine: crece hacia abajo a medida que se escribe
            // (sin límite de líneas) en vez de desplazar el texto ya escrito, y el botón
            // de publicar va debajo, también a todo lo ancho.
            OutlinedTextField(
                value = textoNuevo,
                onValueChange = { textoNuevo = it },
                placeholder = { Text("Nuevo anuncio", color = TINTA_SUAVE) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = INDIGO) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BLANCO,
                    unfocusedContainerColor = BLANCO,
                    focusedBorderColor = INDIGO.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .bringIntoViewRequester(traerALaVista)
                    .onFocusChanged { conFoco = it.isFocused }
            )
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = textoNuevo.isNotBlank(),
                onClick = {
                    onPublicar(textoNuevo)
                    textoNuevo = ""
                    focusManagerTablon.clearFocus()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extra.acento,
                    contentColor = extra.onAcento,
                    disabledContainerColor = extra.acento.copy(alpha = 0.45f),
                    disabledContentColor = extra.onAcento
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Publicar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FilaAnuncio(anuncio: Anuncio, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BLANCO)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${anuncio.autorNombre} · ${formatearFechaAnuncio(anuncio.publicadoEn)}",
                style = MaterialTheme.typography.labelMedium,
                color = TINTA_SUAVE
            )
            Spacer(Modifier.height(2.dp))
            Text(anuncio.texto, style = MaterialTheme.typography.bodyLarge, color = TINTA)
        }
        IconButton(onClick = onEliminar) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar anuncio", tint = TINTA_SUAVE)
        }
    }
}

@Composable
private fun colorParaEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.VERDE -> LocalEncajaExtraColors.current.verdeContainer
    EstadoDia.AMBAR -> MaterialTheme.colorScheme.tertiaryContainer
    EstadoDia.ROJO -> MaterialTheme.colorScheme.errorContainer
    EstadoDia.SIN_DATOS -> LAVANDA
}

/** Color del número dentro de cada círculo del semáforo: fondo pálido del estado +
 * texto en el tono fuerte de ese mismo color; los días sin datos, índigo sobre lavanda. */
@Composable
private fun colorTextoParaEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.VERDE -> LocalEncajaExtraColors.current.onVerdeContainer
    EstadoDia.AMBAR -> MaterialTheme.colorScheme.onTertiaryContainer
    EstadoDia.ROJO -> MaterialTheme.colorScheme.onErrorContainer
    EstadoDia.SIN_DATOS -> INDIGO
}

/** "GEM · Etna": la actividad y de quién es, para saber de un vistazo a quién afecta el aviso. */
private fun tituloConNino(descripcion: String, nombreNino: String?): String =
    if (nombreNino.isNullOrBlank()) descripcion else "$descripcion · $nombreNino"

/** "Martes 29" en vez de la fecha ISO en bruto (2026-09-29). */
private fun formatearFechaHueco(fecha: LocalDate): String {
    val nombreDia = fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() }
    return "$nombreDia ${fecha.dayOfMonth}"
}

/** "18:30" en vez del LocalTime en bruto (18:30:00 o 18:30). */
private fun formatearHoraHueco(hora: LocalTime): String =
    hora.format(DateTimeFormatter.ofPattern("HH:mm"))

/**
 * Tarjeta de aviso con el estilo de la maqueta: fondo pálido, franja de color a la
 * izquierda, icono en círculo y texto a la derecha. El chip ("Sin cubrir",
 * "Importante"...) va en su propia línea, encima del título, en vez de compartir
 * fila con él — así el título y la descripción se leen enteros y nunca se cortan,
 * por largos que sean. Toda la tarjeta es clicable: abre la edición de la actividad
 * aquí mismo para poder asignar directamente quién lleva o recoge.
 */
@Composable
private fun TarjetaAviso(
    titulo: String,
    chip: String,
    texto: String,
    fondo: Color,
    franja: Color,
    tinta: Color,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = fondo
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(7.dp).fillMaxHeight().background(franja))
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconoDeTarjeta(icono, fondo = franja.copy(alpha = 0.35f), tinta = tinta)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelLarge,
                        color = tinta,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(franja.copy(alpha = 0.30f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TINTA
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(texto, style = MaterialTheme.typography.bodyLarge, color = TINTA, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun TarjetaHueco(hueco: Hueco, nombreNino: String?, onClick: () -> Unit) {
    val quien = nombreNino?.let { " a $it" } ?: ""
    val queFalta = when (hueco.motivo) {
        MotivoHueco.FALTA_QUIEN_LLEVA -> "llevar"
        MotivoHueco.FALTA_QUIEN_RECOGE -> "recoger"
        MotivoHueco.SIN_ASIGNACION -> "llevar ni recoger"
    }
    TarjetaAviso(
        titulo = tituloConNino(hueco.need.descripcion, nombreNino),
        chip = "Sin cubrir",
        texto = "${formatearFechaHueco(hueco.need.fecha)} a las ${formatearHoraHueco(hueco.need.horaInicio)}: " +
            "todavía no hay nadie asignado para $queFalta$quien.",
        fondo = LocalEncajaExtraColors.current.rosaHueco,
        franja = MaterialTheme.colorScheme.error,
        tinta = MaterialTheme.colorScheme.onErrorContainer,
        icono = Icons.Default.PriorityHigh,
        onClick = onClick
    )
}

/** "Ojo, Sílvia tiene asignado llevar y recoger a Etna el sábado en "Fútbol de
 * Etna", pero tiene asignada "Trabajo" a esa hora." Un único aviso aunque la
 * misma persona esté asignada a llevar y a recoger (no cambia el semáforo). */
private fun textoAviso(aviso: AvisoConflicto): String {
    val accion = when {
        RolResponsable.LLEVA in aviso.roles && RolResponsable.RECOGE in aviso.roles -> "llevar y recoger"
        RolResponsable.LLEVA in aviso.roles -> "llevar"
        else -> "recoger"
    }
    val dia = aviso.need.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, ES)
    val tarea = aviso.bloque.etiqueta ?: etiquetaMotivo(aviso.bloque.motivo)
    return "Ojo, ${aviso.caregiver.nombreCompleto} tiene asignado $accion a ${aviso.child.nombre} " +
        "el $dia en \"${aviso.need.descripcion}\", pero tiene asignada \"$tarea\" a esa hora."
}

@Composable
private fun TarjetaAvisoConflicto(aviso: AvisoConflicto, onClick: () -> Unit) {
    TarjetaAviso(
        titulo = tituloConNino(aviso.need.descripcion, aviso.child.nombre),
        chip = "Importante",
        texto = textoAviso(aviso),
        fondo = LocalEncajaExtraColors.current.cremaAviso,
        franja = LocalEncajaExtraColors.current.ambar,
        tinta = MaterialTheme.colorScheme.onTertiaryContainer,
        icono = Icons.Default.PriorityHigh,
        onClick = onClick
    )
}

/** Resumen de las actividades de un día verde del semáforo: al no haber ningún aviso
 * ni hueco que abrir, este diálogo es lo único a lo que puede llevar tocar su círculo. */
@Composable
private fun DialogoInfoDia(
    dia: DiaSemaforo,
    cuidadores: List<Caregiver>,
    ninos: List<Child>,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(formatearFechaHueco(dia.fecha)) },
        text = {
            if (dia.needs.isEmpty()) {
                Text("Ese día no hay ninguna actividad registrada.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    dia.needs.sortedBy { it.horaInicio }.forEach { need ->
                        Column {
                            Text(need.descripcion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val nombreNino = ninos.firstOrNull { it.id == need.childId }?.nombre
                            Text(
                                buildString {
                                    append(formatearHoraHueco(need.horaInicio))
                                    append(" – ")
                                    append(formatearHoraHueco(need.horaFin))
                                    if (nombreNino != null) append(" · $nombreNino")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (need.requiereDesplazamiento) {
                                val lleva = cuidadores.firstOrNull { it.id.value == need.quienLlevaId }?.nombreCompleto
                                val recoge = cuidadores.firstOrNull { it.id.value == need.quienRecogeId }?.nombreCompleto
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Lleva: ${lleva ?: "Sin asignar"} · Recoge: ${recoge ?: "Sin asignar"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
