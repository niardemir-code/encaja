@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

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

import com.encaja.app.ui.ayuda.ayudaObjetivo
import com.encaja.app.ui.common.nombreVisible
import com.encaja.app.domain.model.MotivoNoDisponibilidad
import com.encaja.app.ui.common.etiquetaCortaDeMotivo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.encaja.app.R
import com.encaja.app.ui.common.rangoSemana
import com.encaja.app.ui.common.letraDia
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Visibility
import com.encaja.app.ui.theme.SelectorFechaHoja
import com.encaja.app.ui.theme.PaletaAvatarPersonas
import com.encaja.app.ui.theme.ColorAvatarUnidad
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Label
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
import com.encaja.app.ui.theme.AccionAvisoHoja
import com.encaja.app.ui.theme.AvisoHoja
import com.encaja.app.ui.theme.CampoTextoConIconoHoja
import com.encaja.app.ui.theme.EstiloAccionAviso
import com.encaja.app.ui.theme.LocalEncajaExtraColors
import java.time.LocalDate

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

// Paletas de los avatares: compartidas con Ajustes (ver ui/theme/ComponentesPantalla.kt).

// Medidas de la cuadrícula: la semana entera cabe siempre en el ancho de la pantalla.
private val ALTO_CABECERA = 40.dp
private val ALTO_CELDA = 46.dp
private val ESPACIO_CELDAS = 4.dp
private val PADDING_BLOQUE = 8.dp

@Composable
fun FamiliaScreen(viewModel: FamiliaViewModel = hiltViewModel()) {
    val pantalla by viewModel.pantalla.collectAsState()
    // El ViewModel vive ahora lo que vive la app (ver entradaDelGrafo en EncajaApp), para
    // no perder la semana/día elegido al cambiar de pestaña; a cambio, hay que recargar
    // al reentrar para reflejar cambios hechos desde otras pantallas.
    LaunchedEffect(Unit) { viewModel.recargar() }
    // La posición del scroll vive aquí, por encima de todo lo que cambia con la selección.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

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
                        stringResource(R.string.comun_sin_familia_ver),
                        textAlign = TextAlign.Center,
                        color = TINTA
                    )
                }
            }

            is FamiliaPantallaEstado.ConDatos -> ContenidoFamilia(estadoActual.estado, viewModel, listState)
        }
    }
}

@Composable
private fun ContenidoFamilia(
    estado: FamiliaUiState,
    viewModel: FamiliaViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val iniciales = remember(estado.cuidadores) {
        calcularInicialesCuidadores(estado.cuidadores.map { it.caregiver })
    }
    // Casilla abierta: id (de persona o de unidad familiar, como texto) y fecha.
    var celdaEnEdicion by remember { mutableStateOf<Pair<String, LocalDate>?>(null) }
    // Casillas marcadas para el borrado masivo (se empieza con una pulsación larga).
    var seleccion by remember { mutableStateOf(emptySet<Pair<String, LocalDate>>()) }
    var confirmandoBorrado by remember { mutableStateOf(false) }
    val ocultos by viewModel.ocultos.collectAsState()
    val filtroCompartido by viewModel.filtroCompartido.collectAsState()
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
    var nuevoCuidadorAbierto by remember { mutableStateOf(false) }
    val nombres = remember(estado.cuidadores) { estado.cuidadores.associate { it.caregiver.id to it.caregiver.nombre } }

    if (nuevoCuidadorAbierto) {
        DialogoNuevoCuidador(
            onCrear = { nombre, apellido1, apellido2 ->
                viewModel.agregarCuidador(nombre, apellido1, apellido2)
                nuevoCuidadorAbierto = false
            },
            onCerrar = { nuevoCuidadorAbierto = false }
        )
    }

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
            compartido = filtroCompartido,
            onCompartido = { viewModel.fijarFiltroCompartido(it) },
            onAlternar = { viewModel.alternarVisibilidad(it) },
            onCerrar = { selectorAbierto = false }
        )
    }

    // Se busca en el estado actual (no en una copia guardada al abrir), así al borrar
    // un bloque el diálogo se actualiza solo tras la recarga. La casilla puede ser de
    // una persona o de una unidad familiar (entonces se aplica a todos sus miembros).
    if (confirmandoBorrado) {
        val bloquesAfectados = seleccion.sumOf { (idTexto, fecha) ->
            val dias = estado.cuidadores.firstOrNull { it.caregiver.id.value == idTexto }?.dias
                ?: estado.unidades.firstOrNull { it.unidad.id.value == idTexto }?.dias
            dias?.firstOrNull { it.fecha == fecha }?.bloqueos?.size ?: 0
        }
        AvisoHoja(
            icono = Icons.Default.Delete,
            titulo = pluralStringResource(R.plurals.familia_vaciar_titulo, seleccion.size, seleccion.size),
            texto = if (bloquesAfectados == 0) stringResource(R.string.familia_casillas_libres)
            else stringResource(R.string.familia_vaciar_texto),
            acciones = listOf(
                AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { confirmandoBorrado = false },
                AccionAvisoHoja(stringResource(R.string.comun_borrar), EstiloAccionAviso.PELIGRO) {
                    val casillas = seleccion.mapNotNull { (idTexto, fecha) ->
                        val ids = estado.cuidadores.firstOrNull { it.caregiver.id.value == idTexto }?.let { listOf(it.caregiver.id) }
                            ?: estado.unidades.firstOrNull { it.unidad.id.value == idTexto }?.unidad?.miembros
                        ids?.let { it to fecha }
                    }
                    viewModel.eliminarCasillas(casillas)
                    seleccion = emptySet()
                    confirmandoBorrado = false
                }
            ),
            onCerrar = { confirmandoBorrado = false }
        )
    }

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
                bloquesDelDia = bloquesDelDia,
                onEliminar = { viewModel.eliminarBloqueDe(caregiverIds, it) },
                turnos = estado.turnos,
                categorias = estado.categorias,
                buscarDeCategoria = { categoria -> viewModel.ocupacionesDeCategoria(caregiverIds, categoria) },
                onGuardar = { categoria, aGuardar, aBorrar, inicio, fin, etiqueta, original ->
                    viewModel.guardarOcupacion(caregiverIds, categoria, aGuardar, aBorrar, inicio, fin, etiqueta, original)
                },
                onCrearTurno = { nombre, inicio, fin, categoriaId -> viewModel.crearTurno(nombre, inicio, fin, categoriaId) },
                onEliminarTurno = { viewModel.eliminarTurno(it) },
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 80.dp)
        ) {
            item {
                Tarjeta {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.ayudaObjetivo("familia_categorias"), verticalAlignment = Alignment.CenterVertically) {
                            CabeceraTarjeta(
                                icono = Icons.AutoMirrored.Filled.Label,
                                titulo = stringResource(R.string.familia_cat_titulo),
                                subtitulo = stringResource(R.string.familia_cat_sub),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            // Lápiz: abre la lista de categorías para editarlas o crear nuevas.
                            BotonCircular(Icons.Default.Edit, stringResource(R.string.familia_editar_categorias), { categoriasAbiertas = true })
                        }
                        Spacer(Modifier.height(12.dp))
                        Leyenda(estado.categorias)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            cuadriculaDisponibilidad(
                    lunes = estado.lunes,
                    cuidadores = cuidadoresVisibles,
                    unidades = unidadesVisibles,
                    categorias = estado.categorias,
                    iniciales = iniciales,
                    seleccion = seleccion,
                    // Con la selección en marcha, tocar una casilla la marca o desmarca; si no, abre el diálogo.
                    onClickDia = { idTexto, fecha ->
                        val casilla = idTexto to fecha
                        if (seleccion.isEmpty()) celdaEnEdicion = casilla
                        else seleccion = if (casilla in seleccion) seleccion - casilla else seleccion + casilla
                    },
                    onLargoDia = { idTexto, fecha ->
                        val casilla = idTexto to fecha
                        seleccion = if (casilla in seleccion) seleccion - casilla else seleccion + casilla
                    },
                    hayOcultosPorFiltro = totalFilas > 0 && filasVisibles == 0,
                    onCrearCuidador = { nuevoCuidadorAbierto = true },
                    cabecera = {
                        Row(modifier = Modifier.fillMaxWidth().ayudaObjetivo("familia_disponibilidad"), verticalAlignment = Alignment.CenterVertically) {
                            CabeceraTarjeta(
                                icono = Icons.Default.CalendarMonth,
                                titulo = stringResource(R.string.familia_disponibilidad),
                                subtitulo = stringResource(R.string.familia_disponibilidad_sub),
                                modifier = Modifier.weight(1f)
                            )
                            if (totalFilas > 1) {
                                Spacer(Modifier.width(8.dp))
                                // Filtro de cuidadores: abre el diálogo para elegir a quién ver (se
                                // aplica al momento y solo para esta semana).
                                BotonCircular(
                                    Icons.Default.FilterAlt,
                                    if (filasVisibles == totalFilas) stringResource(R.string.ajustes_cuidadores_titulo) else stringResource(R.string.familia_cuidadores_n, filasVisibles, totalFilas),
                                    { selectorAbierto = true }
                                )
                            }
                        }
                    }
                )
        }
            // La barra flota sobre la lista (no la desplaza), así el scroll no se mueve.
            if (seleccion.isNotEmpty()) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BarraSeleccion(
                        cantidad = seleccion.size,
                        onBorrar = { confirmandoBorrado = true },
                        onCancelar = { seleccion = emptySet() }
                    )
                }
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
            stringResource(R.string.nav_familia),
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TINTA
        )
        Text(stringResource(R.string.familia_subtitulo), style = MaterialTheme.typography.bodyMedium, color = TINTA_SUAVE)
        Spacer(Modifier.height(12.dp))

        Tarjeta {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonCircular(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.comun_semana_anterior), onSemanaAnterior)
                Text(
                    rangoSemana(lunes),
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
                    Icons.Default.MyLocation, stringResource(R.string.comun_ir_semana_actual), onIrASemanaActual,
                    habilitado = !esSemanaActual
                )
                Spacer(Modifier.width(6.dp))
                BotonCircular(Icons.Default.CalendarMonth, stringResource(R.string.comun_ir_a_semana), { calendarioAbierto = true })
                Spacer(Modifier.width(6.dp))
                BotonCircular(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.comun_semana_siguiente), onSemanaSiguiente)
            }
        }
    }

    if (calendarioAbierto) {
        SelectorFechaHoja(
            fecha = lunes,
            confirmar = stringResource(R.string.comun_ir),
            onElegir = onElegirFecha,
            onCerrar = { calendarioAbierto = false }
        )
    }
}

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
 * La cuadrícula de disponibilidad, repartida en varios elementos de la lista para que
 * la fila de los 7 días quede fija arriba al hacer scroll (stickyHeader) y se vea a la
 * vez que las casillas de las personas de más abajo. Visualmente sigue siendo una
 * tarjeta: la cabecera lleva las esquinas de arriba redondeadas, las filas el mismo
 * fondo y el último elemento las esquinas de abajo.
 */
private fun LazyListScope.cuadriculaDisponibilidad(
    lunes: LocalDate,
    cuidadores: List<CuidadorDisponibilidadSemana>,
    unidades: List<UnidadDisponibilidadSemana>,
    categorias: List<CategoriaDisponibilidad>,
    iniciales: Map<CaregiverId, String>,
    onClickDia: (idTexto: String, LocalDate) -> Unit,
    seleccion: Set<Pair<String, LocalDate>> = emptySet(),
    onLargoDia: (idTexto: String, LocalDate) -> Unit = { _, _ -> },
    hayOcultosPorFiltro: Boolean = false,
    onCrearCuidador: () -> Unit = {},
    cabecera: @Composable () -> Unit = {}
) {
    val fechas = (0..6).map { lunes.plusDays(it.toLong()) }

    item(key = "cuadricula-cabecera") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp)
        ) { cabecera() }
    }

    stickyHeader(key = "cuadricula-dias") {
        val hoy = LocalDate.now()
        // Mismo padding lateral que el interior de cada bloque, para que los días cuadren.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp + PADDING_BLOQUE, vertical = 4.dp)
                .height(ALTO_CABECERA),
            horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)
        ) {
            fechas.forEach { fecha ->
                CabeceraDia(fecha, esHoy = fecha == hoy, modifier = Modifier.weight(1f))
            }
        }
    }

    // Solo se explica algo cuando el filtro ha ocultado a todo el mundo; si no hay nadie
    // todavía, el botón de crear cuidador (más abajo) ocupa el lugar de la cuadrícula.
    if (hayOcultosPorFiltro) {
        item(key = "cuadricula-vacia") {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.familia_ocultado_todos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TINTA_SUAVE
                )
            }
        }
    }

    itemsIndexed(cuidadores, key = { _, c -> "cuidador-" + c.caregiver.id.value }) { indice, cuidadorSemana ->
        FilaDeCuadricula {
            BloqueFila(
                nombre = cuidadorSemana.caregiver.nombreCompleto,
                iniciales = iniciales[cuidadorSemana.caregiver.id] ?: "",
                colores = PaletaAvatarPersonas[indice % PaletaAvatarPersonas.size],
                dias = cuidadorSemana.dias,
                categorias = categorias,
                onClickDia = { fecha -> onClickDia(cuidadorSemana.caregiver.id.value, fecha) },
                onLargoDia = { fecha -> onLargoDia(cuidadorSemana.caregiver.id.value, fecha) },
                seleccionadas = seleccion.filter { it.first == cuidadorSemana.caregiver.id.value }.map { it.second }.toSet()
            )
        }
    }

    // Unidades familiares: sus casillas juntan los bloqueos de todos sus miembros;
    // al tocarlas se ve lo de cada uno y lo que se añade se aplica a todos.
    items(unidades, key = { "unidad-" + it.unidad.id.value }) { unidadSemana ->
        FilaDeCuadricula {
            BloqueFila(
                nombre = unidadSemana.unidad.nombre,
                iniciales = unidadSemana.unidad.codigo.take(2).uppercase(),
                colores = ColorAvatarUnidad,
                dias = unidadSemana.dias,
                categorias = categorias,
                esUnidad = true,
                onClickDia = { fecha -> onClickDia(unidadSemana.unidad.id.value, fecha) },
                onLargoDia = { fecha -> onLargoDia(unidadSemana.unidad.id.value, fecha) },
                seleccionadas = seleccion.filter { it.first == unidadSemana.unidad.id.value }.map { it.second }.toSet()
            )
        }
    }

    // Botón para crear un cuidador: tras el último cuidador o unidad familiar, o justo
    // debajo de los días de la semana si todavía no hay nadie.
    item(key = "cuadricula-nuevo-cuidador") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            OutlinedButton(
                onClick = onCrearCuidador,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.familia_nuevo_cuidador), fontWeight = FontWeight.Bold)
            }
        }
    }

    item(key = "cuadricula-pie") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

/** Una fila de la cuadrícula con el fondo de la tarjeta, para que la tarjeta parezca continua. */
@Composable
private fun FilaDeCuadricula(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { contenido() }
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
        Text(letraDia(fecha.dayOfWeek), style = MaterialTheme.typography.labelSmall, color = if (esHoy) INDIGO else TINTA_SUAVE)
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
    onClickDia: ((LocalDate) -> Unit)?,
    onLargoDia: ((LocalDate) -> Unit)? = null,
    seleccionadas: Set<LocalDate> = emptySet()
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
                Text(stringResource(R.string.familia_unidad), style = MaterialTheme.typography.labelSmall, color = TINTA_SUAVE)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ESPACIO_CELDAS)) {
            dias.forEach { dia ->
                CeldaDisponibilidad(
                    dia, categorias, modifier = Modifier.weight(1f),
                    seleccionada = dia.fecha in seleccionadas,
                    onClick = onClickDia?.let { f -> { f(dia.fecha) } },
                    onLongClick = onLargoDia?.let { f -> { f(dia.fecha) } }
                )
            }
        }
    }
}

@Composable
private fun CeldaDisponibilidad(
    dia: DiaDisponibilidadCuidador,
    categorias: List<CategoriaDisponibilidad>,
    modifier: Modifier = Modifier,
    seleccionada: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    val primero = dia.bloqueos.minByOrNull { it.horaInicio }
    val fondoCelda = if (primero == null) VERDE else Color(primero.categoriaEn(categorias).color)
    // Reborde fino: el mismo color de la casilla, bastante más oscuro, para que destaque
    // sobre el gris violáceo de la fila tanto en Cálido como en Nocturno.
    val bordeCelda = androidx.compose.ui.graphics.lerp(fondoCelda, Color(0xFF26224A), 0.45f)
    Box(
        modifier = modifier
            .height(ALTO_CELDA)
            .clip(RoundedCornerShape(10.dp))
            .background(fondoCelda)
            .border(
                if (seleccionada) 3.dp else 1.dp,
                if (seleccionada) INDIGO else bordeCelda,
                RoundedCornerShape(10.dp)
            )
            .then(
                if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (seleccionada) {
            // Velo del color de la selección, para que se vea marcada aunque la casilla sea oscura.
            Box(Modifier.matchParentSize().background(INDIGO.copy(alpha = 0.25f)))
        }
        val emoji = emojiCelda(dia.bloqueos, categorias)
        if (emoji != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 19.sp)
                if (dia.bloqueos.size > 1) {
                    Text("+${dia.bloqueos.size - 1}", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = TINTA_CELDA)
                }
            }
        } else if (dia.bloqueos.isNotEmpty()) {
            val etiquetasCortas = MotivoNoDisponibilidad.values().associateWith { etiquetaCortaDeMotivo(it) }
            val texto = textoCelda(dia.bloqueos) { etiquetasCortas.getValue(it) }.orEmpty()
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

/** Barra de la selección múltiple: cuántas casillas hay marcadas, borrarlas o cancelar. */
@Composable
private fun BarraSeleccion(cantidad: Int, onBorrar: () -> Unit, onCancelar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LAVANDA)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            pluralStringResource(R.plurals.familia_casillas_marcadas, cantidad, cantidad),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = INDIGO,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onCancelar) { Text(stringResource(R.string.comun_cancelar), color = INDIGO) }
        Button(
            onClick = onBorrar,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.comun_borrar))
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
    compartido: Boolean,
    onCompartido: (Boolean) -> Unit,
    onAlternar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    AvisoHoja(
        icono = Icons.Default.Visibility,
        titulo = stringResource(R.string.familia_cuidadores_mostrar),
        texto = null,
        peligro = false,
        conX = true,
        acciones = listOf(
            AccionAvisoHoja(stringResource(R.string.comun_cerrar), EstiloAccionAviso.BORDE) { onCerrar() }
        ),
        onCerrar = onCerrar,
        contenido = {
            Column {
                Text(
                    if (compartido) stringResource(R.string.familia_seleccion_compartida)
                    else stringResource(R.string.familia_seleccion_propia),
                    style = MaterialTheme.typography.bodySmall,
                    color = TINTA_SUAVE
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.familia_aplicar_todos),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TINTA,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = compartido, onCheckedChange = onCompartido)
                }
                Spacer(Modifier.height(8.dp))
                if (cuidadores.isNotEmpty()) {
                    Text(stringResource(R.string.cuidadores_personas), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = INDIGO)
                    cuidadores.forEach { caregiver ->
                        FilaCasillaMostrar(caregiver.nombreCompleto, caregiver.id.value, ocultos, onAlternar)
                    }
                }
                if (unidades.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.unidades_titulo), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = INDIGO)
                    unidades.forEach { unidad ->
                        FilaCasillaMostrar(unidad.nombre, unidad.id.value, ocultos, onAlternar)
                    }
                }
            }
        
        }
    )
}

/** Qué significa cada color de las casillas. FlowRow para que baje de línea en móviles estrechos. */
@Composable
private fun Leyenda(categorias: List<CategoriaDisponibilidad>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ElementoLeyenda(color = VERDE, texto = stringResource(R.string.familia_libre))
        categorias.forEach { categoria ->
            ElementoLeyenda(color = Color(categoria.color), texto = "${categoria.emoji} ${categoria.nombreVisible()}")
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

    AvisoHoja(
        icono = Icons.Default.Category,
        titulo = stringResource(R.string.familia_categorias),
        texto = null,
        peligro = false,
        conX = true,
        acciones = listOf(
            AccionAvisoHoja(stringResource(R.string.comun_cerrar), EstiloAccionAviso.BORDE) { onCerrar() }
        ),
        onCerrar = onCerrar,
        contenido = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.familia_categorias_ayuda),
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
                            Text(categoria.nombreVisible(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TINTA)
                            Text(
                                listOf(
                                    if (categoria.modo == ModoCategoria.HORAS) stringResource(R.string.familia_modo_horas) else stringResource(R.string.familia_modo_dias),
                                    if (categoria.bloquea) stringResource(R.string.disp_ocupa) else stringResource(R.string.disp_no_ocupa)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TINTA_SUAVE
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.comun_editar), tint = TINTA_SUAVE, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { creando = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.categoria_nueva))
                }
                if (eliminadas.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.familia_borradas), style = MaterialTheme.typography.labelMedium, color = TINTA_SUAVE)
                    eliminadas.forEach { categoria ->
                        TextButton(onClick = { onRecuperar(categoria) }) {
                            Text("${categoria.emoji} ${categoria.nombreVisible()}")
                        }
                    }
                }
            }
        
        }
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


/** Pide el nombre y los apellidos del cuidador nuevo; "Crear" se activa cuando hay nombre. */
@Composable
internal fun DialogoNuevoCuidador(onCrear: (nombre: String, apellido1: String, apellido2: String) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var apellido1 by remember { mutableStateOf("") }
    var apellido2 by remember { mutableStateOf("") }
    AvisoHoja(
        icono = Icons.Default.Group,
        titulo = stringResource(R.string.familia_nuevo_cuidador),
        texto = null,
        peligro = false,
        conX = true,
        accionesEnFila = true,
        acciones = listOf(
            AccionAvisoHoja(stringResource(R.string.comun_cancelar), EstiloAccionAviso.BORDE) { onCerrar() },
            AccionAvisoHoja(stringResource(R.string.comun_crear), EstiloAccionAviso.PRIMARIO, habilitada = nombre.isNotBlank()) {
                onCrear(nombre, apellido1, apellido2)
            }
        ),
        onCerrar = onCerrar,
        contenido = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTextoConIconoHoja(nombre, { nombre = it }, stringResource(R.string.comun_nombre), Icons.Default.Person)
                CampoTextoConIconoHoja(apellido1, { apellido1 = it }, stringResource(R.string.familia_primer_apellido), Icons.Default.Person)
                CampoTextoConIconoHoja(apellido2, { apellido2 = it }, stringResource(R.string.familia_segundo_apellido), Icons.Default.Person)
            }
        }
    )
}
