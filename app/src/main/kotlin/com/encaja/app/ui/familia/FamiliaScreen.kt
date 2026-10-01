@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.domain.model.Caregiver
import com.encaja.app.domain.model.CaregiverId
import com.encaja.app.domain.model.CategoriaDisponibilidad
import com.encaja.app.domain.model.FamilyUnit
import com.encaja.app.domain.model.ModoCategoria
import com.encaja.app.domain.model.categoriaEn
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Paleta del diseño de la pantalla Familia. Se leen del tema activo (Cálido/Nocturno)
// para que la pantalla cambie sola con el modo claro/oscuro del teléfono.
private val VERDE: Color
    @Composable get() = LocalEncajaExtraColors.current.verdeContainer
private val FONDO_PANTALLA: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val TINTA: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground   // títulos y textos principales
private val TINTA_SUAVE: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant // subtítulos
private val INDIGO: Color
    @Composable get() = MaterialTheme.colorScheme.primary        // iconos, iniciales, "Esta semana"
private val LAVANDA: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer // pastillas, botones redondos, fondo de iconos
private val FONDO_FILA: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant // filas de la cuadrícula
/** Texto dentro de las casillas: siempre oscuro, porque los colores de las categorías son
 * pastel también en modo Nocturno y con letra clara no se leía. */
private val TINTA_CELDA = Color(0xFF26224A)

/** Fondo y color de letra del avatar de cada persona, por orden de la lista. */
/** Avatar de las unidades familiares: gris azulado, distinto de la paleta de personas. */
private val COLOR_AVATAR_UNIDAD = Color(0xFFDDE3EE) to Color(0xFF3A4A6B)

private val COLORES_AVATAR = listOf(
    Color(0xFFE4E2FA) to Color(0xFF3B3597),
    Color(0xFFD6F1E3) to Color(0xFF1E6B45),
    Color(0xFFE6DEFA) to Color(0xFF4A3B9A),
    Color(0xFFFBDDE4) to Color(0xFF9A2A45),
    Color(0xFFDCEAFA) to Color(0xFF1E5A8C),
    Color(0xFFFCE4D6) to Color(0xFF9A3D1C)
)

// Medidas de la cuadrícula: la semana entera cabe siempre en el ancho de la pantalla.
private val ALTO_CABECERA = 40.dp
private val ALTO_CELDA = 46.dp
private val ESPACIO_CELDAS = 4.dp
private val PADDING_BLOQUE = 8.dp

private val ES = Locale("es")

@Composable
fun FamiliaScreen(viewModel: FamiliaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    // El ViewModel vive ahora lo que vive la app (ver entradaDelGrafo en EncajaApp), para
    // no perder la semana/día elegido al cambiar de pestaña; a cambio, hay que recargar
    // al reentrar para reflejar cambios hechos desde otras pantallas.
    LaunchedEffect(Unit) { viewModel.recargar() }

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
    // Casilla abierta: id (de persona o de unidad familiar, como texto) y fecha.
    var celdaEnEdicion by remember { mutableStateOf<Pair<String, LocalDate>?>(null) }
    val ocultos by viewModel.ocultos.collectAsState()
    val cuidadoresVisibles = remember(estado.cuidadores, ocultos) {
        estado.cuidadores.filter { it.caregiver.id.value !in ocultos }
    }
    val unidadesVisibles = remember(estado.unidades, ocultos) {
        estado.unidades.filter { it.unidad.id.value !in ocultos }
    }
    val totalFilas = estado.cuidadores.size + estado.unidades.size
    val filasVisibles = cuidadoresVisibles.size + unidadesVisibles.size
    var selectorAbierto by remember { mutableStateOf(false) }
    var categoriasAbiertas by remember { mutableStateOf(false) }
    val nombres = remember(estado.cuidadores) { estado.cuidadores.associate { it.caregiver.id to it.caregiver.nombre } }

    if (categoriasAbiertas) {
        DialogoListaCategorias(
            categorias = estado.categorias,
            eliminadas = estado.categoriasEliminadas,
            onGuardar = { viewModel.guardarCategoria(it) },
            onEliminar = { viewModel.eliminarCategoria(it) },
            onRecuperar = { viewModel.recuperarCategoria(it) },
            onCerrar = { categoriasAbiertas = false }
        )
    }

    if (selectorAbierto) {
        DialogoMostrar(
            cuidadores = estado.cuidadores.map { it.caregiver },
            unidades = estado.unidades.map { it.unidad },
            ocultos = ocultos,
            onAlternar = { viewModel.alternarVisibilidad(it) },
            onCerrar = { selectorAbierto = false }
        )
    }

    // Se busca en el estado actual (no en una copia guardada al abrir), así al borrar
    // un bloque el diálogo se actualiza solo tras la recarga. La casilla puede ser de
    // una persona o de una unidad familiar (entonces se aplica a todos sus miembros).
    celdaEnEdicion?.let { (idTexto, fecha) ->
        val cuidadorSemana = estado.cuidadores.firstOrNull { it.caregiver.id.value == idTexto }
        val unidadSemana = estado.unidades.firstOrNull { it.unidad.id.value == idTexto }
        val titulo = cuidadorSemana?.caregiver?.nombre ?: unidadSemana?.unidad?.nombre
        val caregiverIds = cuidadorSemana?.let { listOf(it.caregiver.id) } ?: unidadSemana?.unidad?.miembros
        val bloquesDelDia = (cuidadorSemana?.dias ?: unidadSemana?.dias)?.firstOrNull { it.fecha == fecha }?.bloqueos.orEmpty()
        if (titulo == null || caregiverIds == null) {
            celdaEnEdicion = null
        } else {
            DialogoDisponibilidad(
                titulo = titulo,
                caregiverIds = caregiverIds,
                nombres = nombres,
                fecha = fecha,
                lunes = estado.lunes,
                bloquesDelDia = bloquesDelDia,
                onEliminar = { viewModel.eliminarBloque(it) },
                turnos = estado.turnos,
                categorias = estado.categorias,
                onGuardarHoras = { ids, categoria, fechas, inicio, fin, duplicar, etiqueta ->
                    viewModel.guardarHoras(ids, categoria, fechas, inicio, fin, duplicar, etiqueta)
                },
                onCrearTurno = { nombre, inicio, fin, categoriaId -> viewModel.crearTurno(nombre, inicio, fin, categoriaId) },
                onEliminarTurno = { viewModel.eliminarTurno(it) },
                onGuardarBloques = { viewModel.guardarBloques(it) },
                onGuardarCategoria = { viewModel.guardarCategoria(it) },
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
            onElegirFecha = { viewModel.irASemanaDe(it) },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Tarjeta {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CabeceraTarjeta(
                                icono = Icons.Default.Label,
                                titulo = "Categorías de estado de los cuidadores",
                                subtitulo = "Qué significa cada color de las casillas.",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            // Lápiz: abre la lista de categorías para editarlas o crear nuevas.
                            BotonCircular(Icons.Default.Edit, "Editar categorías", { categoriasAbiertas = true })
                        }
                        Spacer(Modifier.height(12.dp))
                        Leyenda(estado.categorias)
                    }
                }
            }

            item {
                TarjetaCuadricula(
                    lunes = estado.lunes,
                    cuidadores = cuidadoresVisibles,
                    unidades = unidadesVisibles,
                    categorias = estado.categorias,
                    iniciales = iniciales,
                    onClickDia = { idTexto, fecha -> celdaEnEdicion = idTexto to fecha },
                    hayOcultosPorFiltro = totalFilas > 0 && filasVisibles == 0,
                    cabecera = {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            CabeceraTarjeta(
                                icono = Icons.Default.CalendarMonth,
                                titulo = "Disponibilidad",
                                subtitulo = "Toca la casilla de una persona en un día para apuntar algo.",
                                modifier = Modifier.weight(1f)
                            )
                            if (totalFilas > 1) {
                                Spacer(Modifier.width(8.dp))
                                // Filtro de cuidadores: abre el diálogo para elegir a quién ver (se
                                // aplica al momento y solo para esta semana).
                                BotonCircular(
                                    Icons.Default.FilterAlt,
                                    if (filasVisibles == totalFilas) "Cuidadores" else "Cuidadores ($filasVisibles/$totalFilas)",
                                    { selectorAbierto = true }
                                )
                            }
                        }
                    }
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
    onElegirFecha: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var calendarioAbierto by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            "Familia",
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TINTA
        )
        Text("Coordinados, todo encaja", style = MaterialTheme.typography.bodyMedium, color = TINTA_SUAVE)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TINTA,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                // Icono en vez de un texto ("Esta semana") que se cortaba: mismo criterio (y
                // mismo icono, el "punto de disparo") que el botón "Ahora" de la Guía. Solo
                // responde si no se está ya en esta semana.
                BotonCircular(
                    Icons.Default.MyLocation, "Ir a la semana actual", onIrASemanaActual,
                    habilitado = !esSemanaActual
                )
                Spacer(Modifier.width(6.dp))
                BotonCircular(Icons.Default.CalendarMonth, "Ir a una semana", { calendarioAbierto = true })
                Spacer(Modifier.width(6.dp))
                BotonCircular(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Semana siguiente", onSemanaSiguiente)
            }
        }
    }

    if (calendarioAbierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fechaAMillisUtc(lunes))
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
        color = MaterialTheme.colorScheme.surface,
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
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(LAVANDA),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = INDIGO, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TINTA, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = TINTA_SUAVE)
        }
    }
}

@Composable
private fun BotonCircular(icono: ImageVector, descripcion: String, onClick: () -> Unit, habilitado: Boolean = true) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(LAVANDA)
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = if (habilitado) INDIGO else INDIGO.copy(alpha = 0.4f))
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
    unidades: List<UnidadDisponibilidadSemana>,
    categorias: List<CategoriaDisponibilidad>,
    iniciales: Map<CaregiverId, String>,
    onClickDia: (idTexto: String, LocalDate) -> Unit,
    hayOcultosPorFiltro: Boolean = false,
    cabecera: @Composable () -> Unit = {}
) {
    val fechas = (0..6).map { lunes.plusDays(it.toLong()) }
    val hoy = LocalDate.now()

    Tarjeta {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)) { cabecera() }
            // Mismo padding lateral que el interior de cada bloque, para que los días cuadren.
            Row(
                modifier = Modifier.fillMaxWidth().height(ALTO_CABECERA).padding(horizontal = PADDING_BLOQUE),
                horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)
            ) {
                fechas.forEach { fecha ->
                    CabeceraDia(fecha, esHoy = fecha == hoy, modifier = Modifier.weight(1f))
                }
            }

            if (cuidadores.isEmpty() && unidades.isEmpty()) {
                Text(
                    if (hayOcultosPorFiltro) {
                        "Has ocultado a todo el mundo; usa el botón Cuidadores para volver a verlos."
                    } else {
                        "Añade personas en Ajustes para ver aquí su disponibilidad."
                    },
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TINTA_SUAVE
                )
            }

            cuidadores.forEachIndexed { indice, cuidadorSemana ->
                BloqueFila(
                    nombre = cuidadorSemana.caregiver.nombreCompleto,
                    iniciales = iniciales[cuidadorSemana.caregiver.id] ?: "",
                    colores = COLORES_AVATAR[indice % COLORES_AVATAR.size],
                    dias = cuidadorSemana.dias,
                    categorias = categorias,
                    onClickDia = { fecha -> onClickDia(cuidadorSemana.caregiver.id.value, fecha) }
                )
            }

            // Unidades familiares: sus casillas juntan los bloqueos de todos sus miembros;
            // al tocarlas se ve lo de cada uno y lo que se añade se aplica a todos.
            unidades.forEach { unidadSemana ->
                BloqueFila(
                    nombre = unidadSemana.unidad.nombre,
                    iniciales = unidadSemana.unidad.codigo.take(2).uppercase(),
                    colores = COLOR_AVATAR_UNIDAD,
                    dias = unidadSemana.dias,
                    categorias = categorias,
                    esUnidad = true,
                    onClickDia = { fecha -> onClickDia(unidadSemana.unidad.id.value, fecha) }
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
        Text(letraDia(fecha), style = MaterialTheme.typography.labelSmall, color = if (esHoy) INDIGO else TINTA_SUAVE)
        Text(
            "${fecha.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (esHoy) INDIGO else TINTA
        )
    }
}

/** Una fila de la cuadrícula (persona o unidad familiar): avatar y nombre arriba, sus
 * 7 casillas debajo. Con [onClickDia] a null las casillas no se pueden tocar. */
@Composable
private fun BloqueFila(
    nombre: String,
    iniciales: String,
    colores: Pair<Color, Color>,
    dias: List<DiaDisponibilidadCuidador>,
    categorias: List<CategoriaDisponibilidad>,
    esUnidad: Boolean = false,
    onClickDia: ((LocalDate) -> Unit)?
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
                modifier = Modifier
                    .size(28.dp)
                    // Las unidades llevan el avatar cuadrado redondeado, para distinguirlas
                    // a simple vista de las personas (círculo).
                    .clip(if (esUnidad) RoundedCornerShape(8.dp) else CircleShape)
                    .background(colores.first),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colores.second)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TINTA,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (esUnidad) {
                Text("unidad familiar", style = MaterialTheme.typography.labelSmall, color = TINTA_SUAVE)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)) {
            dias.forEach { dia ->
                CeldaDisponibilidad(dia, categorias, modifier = Modifier.weight(1f), onClick = onClickDia?.let { f -> { f(dia.fecha) } })
            }
        }
    }
}

@Composable
private fun CeldaDisponibilidad(
    dia: DiaDisponibilidadCuidador,
    categorias: List<CategoriaDisponibilidad>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val primero = dia.bloqueos.minByOrNull { it.horaInicio }
    Box(
        modifier = modifier
            .height(ALTO_CELDA)
            .clip(RoundedCornerShape(10.dp))
            .background(if (primero == null) VERDE else Color(primero.categoriaEn(categorias).color))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val emoji = emojiCelda(dia.bloqueos, categorias)
        if (emoji != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 19.sp)
                if (dia.bloqueos.size > 1) {
                    Text("+${dia.bloqueos.size - 1}", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = TINTA_CELDA)
                }
            }
        } else textoCelda(dia.bloqueos)?.let { texto ->
            Text(
                texto,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TINTA_CELDA,
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = false
            )
        }
    }
}

/** Una casilla por persona: marcada = aparece en la cuadrícula de esta semana. Los
 * cambios se aplican al momento, sin botón de "aceptar". */
@Composable
private fun DialogoMostrar(
    cuidadores: List<Caregiver>,
    unidades: List<FamilyUnit>,
    ocultos: Set<String>,
    onAlternar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cuidadores a mostrar") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Solo para esta semana; las demás semanas tienen su propia selección.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TINTA_SUAVE
                )
                Spacer(Modifier.height(8.dp))
                if (cuidadores.isNotEmpty()) {
                    Text("Personas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = INDIGO)
                    cuidadores.forEach { caregiver ->
                        FilaCasillaMostrar(caregiver.nombreCompleto, caregiver.id.value, ocultos, onAlternar)
                    }
                }
                if (unidades.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Unidades familiares", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = INDIGO)
                    unidades.forEach { unidad ->
                        FilaCasillaMostrar(unidad.nombre, unidad.id.value, ocultos, onAlternar)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
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
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = TINTA)
    }
}

@Composable
private fun FilaCasillaMostrar(nombre: String, idTexto: String, ocultos: Set<String>, onAlternar: (String) -> Unit) {
    val visible = idTexto !in ocultos
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onAlternar(idTexto) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = visible, onCheckedChange = { onAlternar(idTexto) })
        Text(nombre, style = MaterialTheme.typography.bodyLarge, color = TINTA)
    }
}

/**
 * Lista de todas las categorías (de serie y propias) para editarlas de un toque o crear
 * una nueva; se abre desde el lápiz de la tarjeta "Categorías".
 */
@Composable
private fun DialogoListaCategorias(
    categorias: List<CategoriaDisponibilidad>,
    eliminadas: List<CategoriaDisponibilidad>,
    onGuardar: (CategoriaDisponibilidad) -> Unit,
    onEliminar: (CategoriaDisponibilidad) -> Unit,
    onRecuperar: (CategoriaDisponibilidad) -> Unit,
    onCerrar: () -> Unit
) {
    var enEdicion by remember { mutableStateOf<CategoriaDisponibilidad?>(null) }
    var creando by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Categorías") },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Toca una para cambiar su nombre, icono, color, si va por horas o por días y si ocupa a la persona.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TINTA_SUAVE
                )
                Spacer(Modifier.height(8.dp))
                categorias.forEach { categoria ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { enEdicion = categoria }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(categoria.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(categoria.emoji, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(categoria.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TINTA)
                            Text(
                                listOf(
                                    if (categoria.modo == ModoCategoria.HORAS) "por horas" else "por días",
                                    if (categoria.bloquea) "ocupa" else "no ocupa"
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TINTA_SUAVE
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TINTA_SUAVE, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { creando = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva categoría")
                }
                if (eliminadas.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Borradas (toca para recuperar)", style = MaterialTheme.typography.labelMedium, color = TINTA_SUAVE)
                    eliminadas.forEach { categoria ->
                        TextButton(onClick = { onRecuperar(categoria) }) {
                            Text("${categoria.emoji} ${categoria.nombre}")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )

    if (creando) {
        DialogoCategoria(
            inicial = null,
            categorias = categorias,
            onGuardar = { onGuardar(it); creando = false },
            onEliminar = null,
            onCerrar = { creando = false }
        )
    }

    enEdicion?.let { categoria ->
        DialogoCategoria(
            inicial = categoria,
            categorias = categorias,
            onGuardar = { onGuardar(it); enEdicion = null },
            onEliminar = { onEliminar(categoria); enEdicion = null },
            onCerrar = { enEdicion = null }
        )
    }
}
