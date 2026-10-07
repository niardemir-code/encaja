package com.encaja.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.encaja.app.ui.common.ErroresDeUi
import com.encaja.app.ui.informacion.AvisosBateria
import com.encaja.app.ui.informacion.DialogoAvisosBateria
import com.encaja.app.ui.informacion.DialogoMasInformacionBateria
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.encaja.app.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import com.encaja.app.ui.actividades.TodasActividadesScreen
import com.encaja.app.ui.ayuda.AyudaState
import com.encaja.app.ui.ayuda.CapaAyuda
import com.encaja.app.ui.ayuda.LocalAyuda
import com.encaja.app.ui.ayuda.ayudaObjetivo
import com.encaja.app.ui.ajustes.AjustesCuidadoresScreen
import com.encaja.app.ui.ajustes.AjustesNinosScreen
import com.encaja.app.ui.ajustes.AjustesScreen
import com.encaja.app.ui.compra.CompraScreen
import com.encaja.app.ui.familia.FamiliaScreen
import com.encaja.app.ui.guia.GuiaScreen
import com.encaja.app.ui.menu.MenuScreen
import com.encaja.app.ui.ocupaciones.OcupacionesScreen
import com.encaja.app.ui.semana.BotonBarraSuperior
import com.encaja.app.ui.semana.BotonInvitar
import com.encaja.app.ui.semana.SemaforoViewModel
import com.encaja.app.ui.semana.SemanaScreen

/** Ruta base de Guía (para navegar desde la pestaña, sin argumentos) y su plantilla
 * completa (para declarar el composable y para comparar contra la ruta actual, que
 * Navigation siempre expone como la plantilla, no como la ruta base). Los dos
 * argumentos son opcionales: sin ellos es la pestaña normal; con ellos —usado desde
 * un aviso de Semana— salta directamente a esa fecha y abre esa actividad. */
private const val RUTA_GUIA_BASE = "guia"
private const val RUTA_GUIA_PLANTILLA = "guia?fecha={fecha}&necesidadId={necesidadId}&quedarse={quedarse}"

private sealed class Destino(val ruta: String, val rutaNavegacion: String = ruta, val etiqueta: String, val icono: ImageVector) {
    data object Semana : Destino(ruta = "semana", etiqueta = "Semana", icono = Icons.Default.DateRange)
    data object Guia : Destino(
        ruta = RUTA_GUIA_PLANTILLA,
        rutaNavegacion = RUTA_GUIA_BASE,
        etiqueta = "Guía",
        icono = Icons.AutoMirrored.Filled.List
    )
    data object Familia : Destino(ruta = "familia", etiqueta = "Familia", icono = Icons.Default.Group)
    data object Menu : Destino(ruta = "menu", etiqueta = "Menú", icono = Icons.Default.Restaurant)
    data object Compra : Destino(ruta = "compra", etiqueta = "Compra", icono = Icons.Default.ShoppingCart)
}

private val destinosBarraInferior = listOf(Destino.Semana, Destino.Guia, Destino.Familia, Destino.Menu, Destino.Compra)
private const val RUTA_AJUSTES = "ajustes"
private const val RUTA_AJUSTES_NINOS = "ajustes/ninos"
private const val RUTA_AJUSTES_CUIDADORES = "ajustes/cuidadores"
private const val RUTA_TODAS_ACTIVIDADES = "todas_actividades"
private const val RUTA_OCUPACIONES = "ocupaciones"
private const val RUTA_AYUDA = "ajustes/ayuda"
private const val RUTA_CONTACTO = "ajustes/contacto"
private const val RUTA_LEGAL = "ajustes/legal"

/** Rutas fuera de las pestañas de la barra inferior: llevan flecha de "atrás" en vez de
 * quedarse sin icono de navegación a la izquierda. */
private val RUTAS_CON_ATRAS = setOf(
    RUTA_AJUSTES, RUTA_AJUSTES_NINOS, RUTA_AJUSTES_CUIDADORES, RUTA_TODAS_ACTIVIDADES, RUTA_OCUPACIONES,
    RUTA_AYUDA, RUTA_CONTACTO, RUTA_LEGAL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncajaApp(
    onCerrarSesion: () -> Unit,
    abrirGuia: Pair<String, String>? = null,
    alAbrirGuia: () -> Unit = {},
    abrirTablon: Boolean = false,
    alAbrirTablon: () -> Unit = {},
    viewModel: EncajaAppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Al tocar el aviso de una actividad: va a la Guía, a ese día y con esa actividad abierta.
    LaunchedEffect(abrirGuia) {
        if (abrirGuia != null) {
            navController.navigate("$RUTA_GUIA_BASE?fecha=${abrirGuia.first}&necesidadId=${abrirGuia.second}&quedarse=true") {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
            alAbrirGuia()
        }
    }

    // Al tocar el aviso de un anuncio: va a Semana; ella se desplaza hasta el tablón y avisa.
    LaunchedEffect(abrirTablon) {
        if (abrirTablon) {
            navController.navigate(Destino.Semana.ruta) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    // Aviso de primera vez (tras iniciar sesión): ofrece quitar las restricciones de batería
    // para que los avisos lleguen con la app cerrada. Una sola vez, y solo si hace falta.
    val contexto = LocalContext.current
    var preguntarBateria by remember {
        mutableStateOf(!AvisosBateria.yaPreguntado(contexto) && !AvisosBateria.estaExenta(contexto))
    }
    var masInfoBateria by remember { mutableStateOf(false) }
    if (preguntarBateria) {
        DialogoAvisosBateria(
            onQuitar = {
                AvisosBateria.marcarPreguntado(contexto)
                preguntarBateria = false
                AvisosBateria.abrirQuitarRestricciones(contexto)
            },
            onAhoraNo = {
                AvisosBateria.marcarPreguntado(contexto)
                preguntarBateria = false
            },
            onMasInfo = { masInfoBateria = true }
        )
    }
    if (masInfoBateria) DialogoMasInformacionBateria(onCerrar = { masInfoBateria = false })

    // Fallos de las acciones del usuario (guardar, borrar...): un aviso en vez de cerrar la app.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        ErroresDeUi.eventos.collect {
            snackbarHostState.showSnackbar("No se pudo completar la acción. Revisa la conexión e inténtalo de nuevo.")
        }
    }

    // Un administrador ha dado de baja esta cuenta: se avisa (sin poder cerrar el aviso) y, al
    // aceptar, se borra todo rastro de la familia y de la cuenta en este dispositivo.
    var bajaDetectada by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.membresiaPerdida.collect { bajaDetectada = true }
    }
    if (bajaDetectada) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text("Cuenta dada de baja") },
            text = {
                Text(
                    "Un administrador ha dado de baja tu cuenta en esta familia. Por seguridad, se " +
                        "borrarán todos los datos de Encaja de este dispositivo, incluidos los avisos " +
                        "programados, y la aplicación se cerrará."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.borrarDatosDelDispositivo() }) {
                    Text("Entendido")
                }
            }
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val inicialesUsuario by viewModel.inicialesUsuario.collectAsState()

    // Ayuda interactiva de primera vez: el estado y la capa viven aquí, en la raíz, para poder
    // iluminar tanto el contenido de cada pantalla como la barra superior y la inferior.
    val ayuda = remember { AyudaState() }
    CompositionLocalProvider(LocalAyuda provides ayuda) {
    Box {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // Barra blanca (surface) con el logo y el nombre en índigo, como en la maqueta.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_encaja),
                            contentDescription = null,
                            modifier = Modifier.height(34.dp).aspectRatio(640f / 576f)
                        )
                        Text(
                            "encaja",
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                navigationIcon = {
                    if (rutaActual != null && rutaActual in RUTAS_CON_ATRAS) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    // El botón de invitar vive aquí (y no en la lista de Semana) para que
                    // esté siempre a mano en cualquier pestaña, no solo en Semana; usa el
                    // mismo SemaforoViewModel de esa pestaña, así que solo puede mostrarse
                    // una vez que esa pestaña ya existe en el back stack (Semana es la
                    // pantalla inicial, así que en la práctica siempre está).
                    // remember con la entrada actual como clave: es lo que pide Navigation
                    // para llamar a getBackStackEntry durante la composición.
                    val semanaEntry = remember(backStackEntry) {
                        runCatching { navController.getBackStackEntry(Destino.Semana.ruta) }.getOrNull()
                    }
                    semanaEntry?.let { entry ->
                        val semanaViewModel: SemaforoViewModel = hiltViewModel(entry)
                        Box(Modifier.ayudaObjetivo("barra_invitar")) { BotonInvitar(semanaViewModel) }
                    }
                    if (inicialesUsuario.isNotBlank()) {
                        AvatarUsuario(inicialesUsuario)
                    }
                    BotonBarraSuperior(Icons.Default.Settings, "Ajustes") {
                        if (rutaActual != RUTA_AJUSTES) {
                            navController.navigate(RUTA_AJUSTES) { launchSingleTop = true }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.ayudaObjetivo("barra_inferior")
            ) {
                destinosBarraInferior.forEach { destino ->
                    NavigationBarItem(
                        // Pestaña activa: pastilla lavanda con icono y texto en índigo.
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        selected = rutaActual == destino.ruta,
                        onClick = {
                            // Antes esto usaba saveState/restoreState para conservar el estado de
                            // cada pestaña al volver a ella. Pero eso guarda la pila entera que
                            // hay por encima de "semana" (incluida Ajustes, si se había abierto) y
                            // podía restaurarla más tarde por error: así es como Ajustes se quedaba
                            // "pegada" y reaparecía al volver a una pestaña. Cada pantalla ya vuelve
                            // a cargar sus datos al reentrar (ver sus LaunchedEffect), así que no
                            // hace falta conservar nada: un simple popUpTo sin guardar estado deja
                            // la pila siempre limpia (como mucho "semana" + la pestaña actual).
                            if (rutaActual != destino.ruta) {
                                navController.navigate(destino.rutaNavegacion) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                        label = { Text(destino.etiqueta, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destino.Semana.ruta,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destino.Semana.ruta) {
                SemanaScreen(
                    desplazarATablon = abrirTablon,
                    alDesplazarATablon = alAbrirTablon,
                    // Desde el círculo de un día ámbar/rojo que ya no tiene un aviso o
                    // hueco vigente: solo salta a ese día en la Guía, sin intentar abrir
                    // ninguna actividad en concreto. (Los avisos se editan en la propia
                    // Semana, sin pasar por aquí.)
                    onVerDia = { fecha ->
                        navController.navigate("$RUTA_GUIA_BASE?fecha=$fecha") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Destino.Guia.ruta,
                arguments = listOf(
                    navArgument("fecha") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("necesidadId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("quedarse") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val fechaArg = backStackEntry.arguments?.getString("fecha")
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val necesidadIdArg = backStackEntry.arguments?.getString("necesidadId")
                // Llegada desde la notificación de una actividad: al cerrar el diálogo se
                // queda en la Guía en vez de volver a Semana.
                val quedarseEnGuia = backStackEntry.arguments?.getBoolean("quedarse") ?: false
                GuiaScreen(
                    viewModel = hiltViewModel(entradaDelGrafo(navController, backStackEntry)),
                    fechaInicial = fechaArg,
                    // Desde la notificación de una actividad ("quedarse"): no se abre su edición,
                    // se remarca en la Guía.
                    necesidadIdInicial = if (quedarseEnGuia) null else necesidadIdArg,
                    resaltarNecesidadId = if (quedarseEnGuia) necesidadIdArg else null,
                    // Se llegó aquí resolviendo un aviso concreto de Semana (no
                    // pidieron ver el día entero): al terminar (guardar, borrar o
                    // cancelar), se vuelve a Semana en vez de quedarse en Guía.
                    onVolverDespuesDeAsignar = { if (!quedarseEnGuia) navController.popBackStack() }
                )
            }
            composable(Destino.Familia.ruta) { entry -> FamiliaScreen(viewModel = hiltViewModel(entradaDelGrafo(navController, entry))) }
            composable(Destino.Menu.ruta) { entry -> MenuScreen(viewModel = hiltViewModel(entradaDelGrafo(navController, entry))) }
            composable(Destino.Compra.ruta) { entry -> CompraScreen(viewModel = hiltViewModel(entradaDelGrafo(navController, entry))) }
            composable(RUTA_AJUSTES) {
                AjustesScreen(
                    onCerrarSesion = onCerrarSesion,
                    onAbrirNinos = { navController.navigate(RUTA_AJUSTES_NINOS) },
                    onAbrirCuidadores = { navController.navigate(RUTA_AJUSTES_CUIDADORES) },
                    onAbrirTodasActividades = { navController.navigate(RUTA_TODAS_ACTIVIDADES) },
                    onAbrirOcupaciones = { navController.navigate(RUTA_OCUPACIONES) },
                    onAbrirAyuda = { navController.navigate(RUTA_AYUDA) },
                    onAbrirContacto = { navController.navigate(RUTA_CONTACTO) },
                    onAbrirLegal = { navController.navigate(RUTA_LEGAL) }
                )
            }
            composable(RUTA_AJUSTES_NINOS) { AjustesNinosScreen() }
            composable(RUTA_AJUSTES_CUIDADORES) { AjustesCuidadoresScreen() }
            composable(RUTA_TODAS_ACTIVIDADES) { TodasActividadesScreen() }
            composable(RUTA_OCUPACIONES) { OcupacionesScreen() }
            composable(RUTA_AYUDA) { com.encaja.app.ui.informacion.AyudaScreen() }
            composable(RUTA_CONTACTO) { com.encaja.app.ui.informacion.ContactoScreen() }
            composable(RUTA_LEGAL) { com.encaja.app.ui.informacion.LegalScreen() }
        }
    }
    CapaAyuda(ayuda)
    }
    }
}

/**
 * La entrada del back stack del grafo entero, para darle a cada pestaña un ViewModel
 * que viva mientras viva la app y no solo mientras la pestaña esté en la pila. Al
 * cambiar de pestaña, la anterior sale del back stack (popUpTo sin saveState, ver la
 * barra inferior) y con ella moriría su ViewModel: la Guía volvería siempre a hoy, y
 * Familia y Menú a la semana actual, en vez de quedarse donde estaban.
 */
@Composable
private fun entradaDelGrafo(
    navController: androidx.navigation.NavHostController,
    entradaActual: androidx.navigation.NavBackStackEntry
): androidx.navigation.NavBackStackEntry =
    remember(entradaActual) { navController.getBackStackEntry(navController.graph.id) }

/** Avatar circular con las iniciales del usuario, junto al icono de Ajustes. */
@Composable
private fun AvatarUsuario(iniciales: String) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciales,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
