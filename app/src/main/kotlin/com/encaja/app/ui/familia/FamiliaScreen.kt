@file:OptIn(ExperimentalLayoutApi::class)

package com.encaja.app.ui.familia

// NOTA: depende de Jetpack Compose y Hilt, no compilado en este entorno.
// Pantalla Familia con el diseño de tarjetas: cabecera fija (título, lema y
// selector de semana) y, debajo, con scroll, tres tarjetas blancas:
//  - "Con quién están las niñas": una pastilla por día; al tocarla se abre el
//    diálogo para elegir responsable (solo esa fecha o todos los [día]).
//  - "Disponibilidad": explicación y leyenda de colores.
//  - Cuadrícula: por persona, avatar y nombre arriba y sus 7 casillas debajo; cada
//    casilla abre DialogoDisponibilidad para añadir trabajo, médico, viajes...
// Los colores del diseño están aquí como constantes para no tocar el tema
// del resto de pantallas.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.categoriaEn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val VERDE = Color(0xFFC0DD97)
private val FONDO_UNIDAD = Color(0xFFDDD3F7)

// Paleta del diseño de la pantalla Familia.
private val FONDO_PANTALLA = Color(0xFFF7F6FC)
private val TINTA = Color(0xFF1C1A4A)          // títulos y textos principales
private val TINTA_SUAVE = Color(0xFF5B5972)    // subtítulos
private val INDIGO = Color(0xFF2F2A8F)         // iconos, iniciales, "Esta semana"
private val LAVANDA = Color(0xFFEAE7FB)        // pastillas, botones redondos, fondo de iconos
private val LAVANDA_CLARA = Color(0xFFF3F1FD)  // cabecera de la tarjeta "Con quién"
private val GRIS_VACIO = Color(0xFFEEEEF3)     // día sin responsable
private val FONDO_FILA = Color(0xFFFAF9FE)     // filas de la cuadrícula

/** Fondo y color de letra del avatar de cada persona, por orden de la lista. */
private val COLORES_AVATAR = listOf(
    Color(0xFFE4E2FA) to Color(0xFF3B3597),
    Color(0xFFD6F1E3) to Color(0xFF1E6B45),
    Color(0xFFE6DEFA) to Color(0xFF4A3B9A),
    Color(0xFFFBDDE4) to Color(0xFF9A2A45),
    Color(0xFFDCEAFA) to Color(0xFF1E5A8C),
    Color(0xFFFCE4D6) to Color(0xFF9A3D1C)
)

// Medidas de la cuadrícula: la semana entera cabe siempre en el ancho de la pantalla.
private val ALTO_CABECERA = 44.dp
private val ALTO_CELDA = 50.dp
private val ESPACIO_CELDAS = 4.dp
private val PADDING_BLOQUE = 8.dp

private val ES = Locale("es")

@Composable
fun FamiliaScreen(viewModel: FamiliaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(FONDO_PANTALLA)) {
        when (val estadoActual = pantalla) {
            is FamiliaPantallaEstado.Cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = INDIGO)
                }
            }

            is FamiliaPantallaEstado.SinFamilia -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Vincúlate a una familia desde la pestaña Semana para ver esta pantalla.",
                        textAlign = TextAlign.Center,
                        color = TINTA
                    )
                }
            }

            is FamiliaPantallaEstado.ConDatos -> ContenidoFamilia(estadoActual.estado, viewModel)
        }
    }
}

@Composable
private fun ContenidoFamilia(estado: FamiliaUiState, viewModel: FamiliaViewModel) {
    val iniciales = remember(estado.cuidadores) {
        calcularInicialesCuidadores(estado.cuidadores.map { it.caregiver })
    }
    var celdaEnEdicion by remember { mutableStateOf<Pair<CaregiverId, LocalDate>?>(null) }

    // Se busca en el estado actual (no en una copia guardada al abrir), así al borrar
    // un bloque el diálogo se actualiza solo tras la recarga.
    celdaEnEdicion?.let { (caregiverId, fecha) ->
        val cuidadorSemana = estado.cuidadores.firstOrNull { it.caregiver.id == caregiverId }
        if (cuidadorSemana == null) {
            celdaEnEdicion = null
        } else {
            DialogoDisponibilidad(
                caregiver = cuidadorSemana.caregiver,
                fecha = fecha,
                lunes = estado.lunes,
                bloquesDelDia = cuidadorSemana.dias.firstOrNull { it.fecha == fecha }?.bloqueos.orEmpty(),
                onEliminar = { viewModel.eliminarBloque(it) },
                turnos = estado.turnos,
                categorias = estado.categorias,
                onGuardarTrabajo = { fechas, inicio, fin, duplicar, nombreTurno ->
                    viewModel.guardarTrabajo(caregiverId, fechas, inicio, fin, duplicar, nombreTurno)
                },
                onCrearTurno = { nombre, inicio, fin -> viewModel.crearTurno(nombre, inicio, fin) },
                onEliminarTurno = { viewModel.eliminarTurno(it) },
                onGuardarBloques = { viewModel.guardarBloques(it) },
                onGuardarCategoria = { viewModel.guardarCategoria(it) },
                onEliminarCategoria = { viewModel.eliminarCategoria(it) },
                onCerrar = { celdaEnEdicion = null }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera fija: fuera del LazyColumn para que no se mueva con el scroll.
        CabeceraFamilia(
            lunes = estado.lunes,
            esSemanaActual = estado.esSemanaActual,
            onSemanaAnterior = { viewModel.cambiarSemana(-1) },
            onSemanaSiguiente = { viewModel.cambiarSemana(1) },
            onIrASemanaActual = { viewModel.irASemanaActual() },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Tarjeta {
                    CabeceraTarjeta(
                        icono = Icons.Default.Groups,
                        titulo = "Con quién están las niñas",
                        subtitulo = "Toca un día para elegir quién es responsable",
                        modifier = Modifier.background(LAVANDA_CLARA).padding(14.dp)
                    )
                    FilaDeAsignacion(
                        dias = estado.diasAsignacion,
                        opciones = estado.opcionesAsignables,
                        iniciales = iniciales,
                        patronSemanal = estado.patronSemanal,
                        onAsignarHabitual = { dia, idTexto -> viewModel.asignarResponsableHabitual(dia, idTexto) },
                        onQuitarHabitual = { dia -> viewModel.quitarResponsableHabitual(dia) },
                        onAnularFecha = { fecha, idTexto -> viewModel.anularParaEstaFecha(fecha, idTexto) },
                        onQuitarCambioPuntual = { fecha -> viewModel.quitarCambioPuntual(fecha) },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            item {
                Tarjeta {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CabeceraTarjeta(
                            icono = Icons.Default.CalendarMonth,
                            titulo = "Disponibilidad",
                            subtitulo = "Toca la casilla de una persona en un día para añadir trabajo, médico, viajes…"
                        )
                        Spacer(Modifier.height(12.dp))
                        Leyenda(estado.categorias)
                    }
                }
            }

            item {
                TarjetaCuadricula(
                    lunes = estado.lunes,
                    cuidadores = estado.cuidadores,
                    categorias = estado.categorias,
                    iniciales = iniciales,
                    onClickDia = { caregiverId, fecha -> celdaEnEdicion = caregiverId to fecha }
                )
            }
        }
    }
}

@Composable
private fun CabeceraFamilia(
    lunes: LocalDate,
    esSemanaActual: Boolean,
    onSemanaAnterior: () -> Unit,
    onSemanaSiguiente: () -> Unit,
    onIrASemanaActual: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Familia",
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TINTA
        )
        Text("Coordinados, todo encaja", style = MaterialTheme.typography.bodyLarge, color = TINTA_SUAVE)
        Spacer(Modifier.height(12.dp))

        Tarjeta {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonCircular(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Semana anterior", onSemanaAnterior)
                Text(
                    textoRangoSemana(lunes),
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TINTA,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                // Siempre visible con el mismo estilo; solo responde si no estás ya en esta semana.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(LAVANDA)
                        .clickable(enabled = !esSemanaActual, onClick = onIrASemanaActual)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Esta semana",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = INDIGO
                    )
                }
                Spacer(Modifier.width(6.dp))
                BotonCircular(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente", onSemanaSiguiente)
            }
        }
    }
}

/** "21 - 27 de septiembre", o "28 sept - 4 oct" si la semana cambia de mes. */
private fun textoRangoSemana(lunes: LocalDate): String {
    val domingo = lunes.plusDays(6)
    return if (lunes.month == domingo.month) {
        "${lunes.dayOfMonth} - ${domingo.dayOfMonth} de ${domingo.month.getDisplayName(TextStyle.FULL, ES)}"
    } else {
        "${lunes.dayOfMonth} ${mesCorto(lunes)} - ${domingo.dayOfMonth} ${mesCorto(domingo)}"
    }
}

private fun mesCorto(fecha: LocalDate): String =
    fecha.month.getDisplayName(TextStyle.SHORT, ES).removeSuffix(".")

/** Primera letra del día abreviado en español: L M M J V S D. */
private fun letraDia(fecha: LocalDate): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).take(1).uppercase()

/** Tarjeta blanca con esquinas redondeadas y sombra suave, base de todos los bloques. */
@Composable
private fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(content = contenido)
    }
}

/** Icono en un cuadrado lavanda + título y subtítulo, como encabezado de una tarjeta. */
@Composable
private fun CabeceraTarjeta(icono: ImageVector, titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(LAVANDA),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = INDIGO, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TINTA)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TINTA_SUAVE)
        }
    }
}

@Composable
private fun BotonCircular(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(LAVANDA).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = INDIGO)
    }
}

@Composable
private fun FilaDeAsignacion(
    dias: List<DiaAsignado>,
    opciones: List<Responsable>,
    iniciales: Map<CaregiverId, String>,
    patronSemanal: Map<java.time.DayOfWeek, Responsable>,
    onAsignarHabitual: (java.time.DayOfWeek, String) -> Unit,
    onQuitarHabitual: (java.time.DayOfWeek) -> Unit,
    onAnularFecha: (LocalDate, String) -> Unit,
    onQuitarCambioPuntual: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var diaEnEdicion by remember { mutableStateOf<DiaAsignado?>(null) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dias.forEach { dia ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { diaEnEdicion = dia },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(letraDia(dia.fecha), style = MaterialTheme.typography.labelMedium, color = TINTA_SUAVE)
                Spacer(Modifier.height(6.dp))
                PildoraDia(responsable = dia.responsable, iniciales = iniciales)
            }
        }
    }

    val diaActual = diaEnEdicion
    if (diaActual != null) {
        DialogoAsignarDia(
            dia = diaActual,
            opciones = opciones,
            iniciales = iniciales,
            hayResponsableHabitual = patronSemanal[diaActual.fecha.dayOfWeek] != null,
            onAsignarHabitual = { idTexto -> onAsignarHabitual(diaActual.fecha.dayOfWeek, idTexto); diaEnEdicion = null },
            onQuitarHabitual = { onQuitarHabitual(diaActual.fecha.dayOfWeek); diaEnEdicion = null },
            onAnularFecha = { idTexto -> onAnularFecha(diaActual.fecha, idTexto); diaEnEdicion = null },
            onQuitarCambioPuntual = { onQuitarCambioPuntual(diaActual.fecha); diaEnEdicion = null },
            onCerrar = { diaEnEdicion = null }
        )
    }
}

/** Pastilla de un día: iniciales sobre lavanda (persona), código sobre rayado (unidad) o "–" en gris. */
@Composable
private fun PildoraDia(responsable: Responsable?, iniciales: Map<CaregiverId, String>) {
    val base = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
    val estiloTexto = MaterialTheme.typography.titleSmall
    when (responsable) {
        null -> Box(base.background(GRIS_VACIO), contentAlignment = Alignment.Center) {
            Text("–", style = estiloTexto, color = TINTA_SUAVE)
        }

        is Responsable.Persona -> Box(base.background(LAVANDA), contentAlignment = Alignment.Center) {
            Text(
                iniciales[responsable.caregiver.id] ?: "",
                style = estiloTexto,
                fontWeight = FontWeight.ExtraBold,
                color = INDIGO
            )
        }

        is Responsable.Unidad -> Box(base.fondoRayado(FONDO_UNIDAD), contentAlignment = Alignment.Center) {
            Text(
                responsable.unidad.codigo.take(2).uppercase(),
                style = estiloTexto,
                fontWeight = FontWeight.ExtraBold,
                color = INDIGO
            )
        }
    }
}

/**
 * Una única lista de opciones (personas y unidades familiares). La casilla "Todos los
 * [día]" decide qué pasa al tocar una opción: marcada, la deja como responsable habitual
 * de ese día de la semana (todas las semanas); sin marcar, solo cambia esta fecha
 * concreta. Así se evita tener dos listas duplicadas una debajo de la otra.
 */
@Composable
private fun DialogoAsignarDia(
    dia: DiaAsignado,
    opciones: List<Responsable>,
    iniciales: Map<CaregiverId, String>,
    hayResponsableHabitual: Boolean,
    onAsignarHabitual: (String) -> Unit,
    onQuitarHabitual: () -> Unit,
    onAnularFecha: (String) -> Unit,
    onQuitarCambioPuntual: () -> Unit,
    onCerrar: () -> Unit
) {
    val etiquetaDia = dia.fecha.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }
    var todosLosDias by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("$etiquetaDia ${dia.fecha.dayOfMonth}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { todosLosDias = !todosLosDias }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = todosLosDias, onCheckedChange = { todosLosDias = it })
                    Text("Todos los $etiquetaDia (responsable habitual)")
                }
                Text(
                    if (todosLosDias) {
                        "Se aplicará todas las semanas, hasta que lo cambies."
                    } else {
                        "Solo cambia este día. El patrón habitual no se toca."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                opciones.forEach { opcion ->
                    FilaOpcion(opcion, iniciales) {
                        if (todosLosDias) onAsignarHabitual(opcion.idTexto) else onAnularFecha(opcion.idTexto)
                    }
                }

                if (dia.esCambioPuntual || hayResponsableHabitual) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                }
                if (dia.esCambioPuntual) {
                    TextButton(onClick = onQuitarCambioPuntual) {
                        Text("Quitar cambio puntual de este día", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (hayResponsableHabitual) {
                    TextButton(onClick = onQuitarHabitual) {
                        Text("Quitar responsable habitual de los $etiquetaDia", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        }
    )
}

@Composable
private fun FilaOpcion(opcion: Responsable, iniciales: Map<CaregiverId, String>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarResponsable(responsable = opcion, iniciales = iniciales, tamano = 24.dp)
        Spacer(Modifier.width(10.dp))
        Text(opcion.etiqueta)
    }
}

/** Avatar de un día o de una fila de opción: iniciales (desambiguadas con
 * calcularInicialesCuidadores, igual que en el resto de la app) sobre fondo liso
 * para una persona, código en negrita sobre fondo rayado en diagonal para una
 * unidad familiar. */
@Composable
private fun AvatarResponsable(
    responsable: Responsable?,
    iniciales: Map<CaregiverId, String>,
    tamano: androidx.compose.ui.unit.Dp
) {
    when (responsable) {
        null -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) { Text("–", style = MaterialTheme.typography.labelSmall) }

        is Responsable.Persona -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text(iniciales[responsable.caregiver.id] ?: "", style = MaterialTheme.typography.labelSmall) }

        is Responsable.Unidad -> Box(
            modifier = Modifier.size(tamano).clip(RoundedCornerShape(8.dp)).fondoRayado(FONDO_UNIDAD),
            contentAlignment = Alignment.Center
        ) {
            Text(
                responsable.unidad.codigo.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Fondo rayado en diagonal, para distinguir a simple vista una unidad familiar de una persona. */
private fun Modifier.fondoRayado(colorFondo: Color): Modifier = this.drawBehind {
    drawRect(colorFondo)
    val espaciado = 6.dp.toPx()
    val grosor = 2.dp.toPx()
    val diagonal = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
    val colorRaya = Color.White.copy(alpha = 0.55f)
    rotate(degrees = 45f, pivot = center) {
        var x = -diagonal
        while (x < diagonal) {
            drawLine(
                color = colorRaya,
                start = Offset(x, -diagonal),
                end = Offset(x, diagonal),
                strokeWidth = grosor
            )
            x += espaciado
        }
    }
}

/**
 * Tarjeta con la cuadrícula de disponibilidad. Arriba, la cabecera con los 7 días;
 * debajo, un bloque por persona: su avatar y nombre en una línea y, justo debajo,
 * sus 7 casillas ocupando todo el ancho, alineadas con la cabecera. Así la semana
 * entera (lunes a domingo) se ve siempre sin desplazarse.
 */
@Composable
private fun TarjetaCuadricula(
    lunes: LocalDate,
    cuidadores: List<CuidadorDisponibilidadSemana>,
    categorias: List<CategoriaDisponibilidad>,
    iniciales: Map<CaregiverId, String>,
    onClickDia: (CaregiverId, LocalDate) -> Unit
) {
    val fechas = (0..6).map { lunes.plusDays(it.toLong()) }
    val hoy = LocalDate.now()

    Tarjeta {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Mismo padding lateral que el interior de cada bloque, para que los días cuadren.
            Row(
                modifier = Modifier.fillMaxWidth().height(ALTO_CABECERA).padding(horizontal = PADDING_BLOQUE),
                horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)
            ) {
                fechas.forEach { fecha ->
                    CabeceraDia(fecha, esHoy = fecha == hoy, modifier = Modifier.weight(1f))
                }
            }

            if (cuidadores.isEmpty()) {
                Text(
                    "Añade personas en Ajustes para ver aquí su disponibilidad.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TINTA_SUAVE
                )
            }

            cuidadores.forEachIndexed { indice, cuidadorSemana ->
                BloqueCuidador(
                    cuidadorSemana = cuidadorSemana,
                    categorias = categorias,
                    iniciales = iniciales[cuidadorSemana.caregiver.id] ?: "",
                    colores = COLORES_AVATAR[indice % COLORES_AVATAR.size],
                    onClickDia = { fecha -> onClickDia(cuidadorSemana.caregiver.id, fecha) }
                )
            }
        }
    }
}

/** Letra y número del día; el de hoy va resaltado en lavanda. */
@Composable
private fun CabeceraDia(fecha: LocalDate, esHoy: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (esHoy) LAVANDA else Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(letraDia(fecha), style = MaterialTheme.typography.labelMedium, color = if (esHoy) INDIGO else TINTA_SUAVE)
        Text(
            "${fecha.dayOfMonth}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (esHoy) INDIGO else TINTA
        )
    }
}

/** Una persona: avatar y nombre completo arriba, sus 7 casillas debajo. */
@Composable
private fun BloqueCuidador(
    cuidadorSemana: CuidadorDisponibilidadSemana,
    categorias: List<CategoriaDisponibilidad>,
    iniciales: String,
    colores: Pair<Color, Color>,
    onClickDia: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FONDO_FILA)
            .padding(PADDING_BLOQUE)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(colores.first),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colores.second)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                cuidadorSemana.caregiver.nombreCompleto,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TINTA,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)) {
            cuidadorSemana.dias.forEach { dia ->
                CeldaDisponibilidad(dia, categorias, modifier = Modifier.weight(1f)) { onClickDia(dia.fecha) }
            }
        }
    }
}

@Composable
private fun CeldaDisponibilidad(
    dia: DiaDisponibilidadCuidador,
    categorias: List<CategoriaDisponibilidad>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primero = dia.bloqueos.minByOrNull { it.horaInicio }
    Box(
        modifier = modifier
            .height(ALTO_CELDA)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primero == null) VERDE else Color(primero.categoriaEn(categorias).color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val emoji = emojiCelda(dia.bloqueos, categorias)
        if (emoji != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 22.sp)
                if (dia.bloqueos.size > 1) {
                    Text("+${dia.bloqueos.size - 1}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TINTA)
                }
            }
        } else textoCelda(dia.bloqueos)?.let { texto ->
            Text(
                texto,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TINTA,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = false
            )
        }
    }
}

/** Qué significa cada color de las casillas. FlowRow para que baje de línea en móviles estrechos. */
@Composable
private fun Leyenda(categorias: List<CategoriaDisponibilidad>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ElementoLeyenda(color = VERDE, texto = "Libre")
        categorias.forEach { categoria ->
            ElementoLeyenda(color = Color(categoria.color), texto = "${categoria.emoji} ${categoria.nombre}")
        }
    }
}

@Composable
private fun ElementoLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = TINTA)
    }
}
