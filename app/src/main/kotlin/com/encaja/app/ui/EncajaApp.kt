package com.encaja.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.encaja.app.ui.ajustes.AjustesScreen
import com.encaja.app.ui.compra.CompraScreen
import com.encaja.app.ui.familia.FamiliaScreen
import com.encaja.app.ui.guia.GuiaScreen
import com.encaja.app.ui.menu.MenuScreen
import com.encaja.app.ui.semana.SemanaScreen

private sealed class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    data object Semana : Destino("semana", "Semana", Icons.Default.DateRange)
    data object Guia : Destino("guia", "Guía", Icons.Default.List)
    data object Familia : Destino("familia", "Familia", Icons.Default.Group)
    data object Menu : Destino("menu", "Menú", Icons.Default.Restaurant)
    data object Compra : Destino("compra", "Compra", Icons.Default.ShoppingCart)
}

private val destinosBarraInferior = listOf(Destino.Semana, Destino.Guia, Destino.Familia, Destino.Menu, Destino.Compra)
private const val RUTA_AJUSTES = "ajustes"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncajaApp(onCerrarSesion: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encaja") },
                actions = {
                    IconButton(onClick = { navController.navigate(RUTA_AJUSTES) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val rutaActual = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

                destinosBarraInferior.forEach { destino ->
                    NavigationBarItem(
                        selected = rutaActual == destino.ruta,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                        label = { Text(destino.etiqueta) }
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
            composable(RUTA_AJUSTES) { AjustesScreen(onCerrarSesion = onCerrarSesion) }
        }
    }
}
