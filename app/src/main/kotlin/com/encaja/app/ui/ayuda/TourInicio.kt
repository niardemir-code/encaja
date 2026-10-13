package com.encaja.app.ui.ayuda

// NOTA: depende de Jetpack Compose y Navigation, no compilado en este entorno.
// Tour de "Primeros pasos": una visita guiada en 8 etapas que va cambiando de pantalla sola
// (niños, cuidadores, invitar, Guía, Familia, Semana, Menú, Compra). Se muestra una vez; se
// puede saltar y se repite desde Ajustes ("Ver los consejos otra vez").

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

/**
 * Una etapa del tour: en qué [ruta] se enseña (tal como la expone Navigation), a dónde
 * [navegar] para llegar y los [pasos] (globos) que se muestran allí.
 */
class EtapaTour(val ruta: String, val navegar: String, val pasos: List<PasoAyuda>)

@Composable
fun TourInicioHost(
    navController: NavHostController,
    rutaActual: String?,
    ayuda: AyudaState,
    etapas: List<EtapaTour>,
    activo: Boolean
) {
    val contexto = LocalContext.current
    var etapa by remember { mutableIntStateOf(PreferenciaTour.etapa(contexto)) }

    // "Ver los consejos otra vez": las preferencias ya se han reiniciado; se empieza de cero.
    LaunchedEffect(ayuda.reinicios) {
        if (ayuda.reinicios > 0) etapa = 0
    }

    LaunchedEffect(etapa, rutaActual, activo, ayuda.reinicios) {
        if (!activo || rutaActual == null || etapa >= etapas.size || ayuda.activa) return@LaunchedEffect
        val actual = etapas[etapa]
        if (rutaActual != actual.ruta) {
            delay(600)
            navController.navigate(actual.navegar) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
            return@LaunchedEffect // al cambiar la ruta, este efecto se vuelve a lanzar
        }
        delay(900) // deja que termine de dibujarse la pantalla y se midan los elementos
        ayuda.iniciar(
            actual.pasos,
            DatosTour(
                etapa = etapa,
                total = etapas.size,
                hayMas = etapa < etapas.size - 1,
                alTerminar = {
                    etapa += 1
                    PreferenciaTour.guardar(contexto, etapa)
                },
                alSaltar = {
                    etapa = etapas.size
                    PreferenciaTour.guardar(contexto, etapa)
                }
            )
        )
    }
}
