package com.encaja.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.encaja.app.ui.actividades.TodasActividadesScreen
import com.encaja.app.ui.ajustes.AjustesCuidadoresScreen
import com.encaja.app.ui.ajustes.AjustesNinosScreen
import com.encaja.app.ui.ajustes.AjustesScreen
import com.encaja.app.ui.compra.CompraScreen
import com.encaja.app.ui.familia.FamiliaScreen
import com.encaja.app.ui.guia.GuiaScreen
import com.encaja.app.ui.menu.MenuScreen
import com.encaja.app.ui.semana.BotonBarraSuperior
import com.encaja.app.ui.semana.BotonInvitar
import com.encaja.app.ui.semana.SemaforoViewModel
import com.encaja.app.ui.semana.SemanaScreen

private sealed class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    data object Semana : Destino("semana", "Semana", Icons.Default.DateRange)
    data object Guia : Destino("guia", "Guía", Icons.AutoMirrored.Filled.List)
    data object Familia : Destino("familia", "Familia", Icons.Default.Group)
    data object Menu : Destino("menu", "Menú", Icons.Default.Restaurant)
    data object Compra : Destino("compra", "Compra", Icons.Default.ShoppingCart)
}

private val destinosBarraInferior = listOf(Destino.Semana, Destino.Guia, Destino.Familia, Destino.Menu, Destino.Compra)
private const val RUTA_AJUSTES = "ajustes"
private const val RUTA_AJUSTES_NINOS = "ajustes/ninos"
private const val RUTA_AJUSTES_CUIDADORES = "ajustes/cuidadores"
private const val RUTA_TODAS_ACTIVIDADES = "todas_actividades"

/** Rutas fuera de las pestañas de la barra inferior: llevan flecha de "atrás" en vez de
 * quedarse sin icono de navegación a la izquierda. */
private val RUTAS_CON_ATRAS = setOf(RUTA_AJUSTES, RUTA_AJUSTES_NINOS, RUTA_AJUSTES_CUIDADORES, RUTA_TODAS_ACTIVIDADES)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncajaApp(onCerrarSesion: () -> Unit, viewModel: EncajaAppViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val inicialesUsuario by viewModel.inicialesUsuario.collectAsState()

    Scaffold(
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
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
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
                    val semanaEntry = runCatching { navController.getBackStackEntry(Destino.Semana.ruta) }.getOrNull()
                    semanaEntry?.let { entry ->
                        val semanaViewModel: SemaforoViewModel = hiltViewModel(entry)
                        BotonInvitar(semanaViewModel)
                    }
                    if (inicialesUsuario.isNotBlank()) {
                        AvatarUsuario(inicialesUsuario)
                    }
                    BotonBarraSuperior(Icons.Default.Settings, "Ajustes") { navController.navigate(RUTA_AJUSTES) }
                    Spacer(Modifier.width(6.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
                            // saveState/restoreState es lo que hace que cada pestaña conserve su
                            // propio estado (p.ej. el día o la semana en que se estaba) al volver a
                            // ella, en vez de recrearse desde cero cada vez.
                            navController.navigate(destino.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
            composable(Destino.Semana.ruta) { SemanaScreen() }
            composable(Destino.Guia.ruta) { GuiaScreen() }
            composable(Destino.Familia.ruta) { FamiliaScreen() }
            composable(Destino.Menu.ruta) { MenuScreen() }
            composable(Destino.Compra.ruta) { CompraScreen() }
            composable(RUTA_AJUSTES) {
                AjustesScreen(
                    onCerrarSesion = onCerrarSesion,
                    onAbrirNinos = { navController.navigate(RUTA_AJUSTES_NINOS) },
                    onAbrirCuidadores = { navController.navigate(RUTA_AJUSTES_CUIDADORES) },
                    onAbrirTodasActividades = { navController.navigate(RUTA_TODAS_ACTIVIDADES) }
                )
            }
            composable(RUTA_AJUSTES_NINOS) { AjustesNinosScreen() }
            composable(RUTA_AJUSTES_CUIDADORES) { AjustesCuidadoresScreen() }
            composable(RUTA_TODAS_ACTIVIDADES) { TodasActividadesScreen() }
        }
    }
}

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
