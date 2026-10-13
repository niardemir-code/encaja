package com.encaja.app.ui.ayuda

// NOTA: depende de Jetpack Compose y Navigation, no compilado en este entorno.
// Tour de introducción: 8 etapas que enseñan el camino real por la app. En los pasos "tocar" el
// usuario tiene que pulsar de verdad el elemento iluminado (engranaje, fila de Ajustes, pestaña
// de la barra inferior) para avanzar; en los demás, pulsa "Siguiente". Se muestra una vez, se
// puede saltar y se repite desde Ajustes ("Ver los consejos otra vez").

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Un globo del tour. [donde]: rutas en las que se muestra (null = en cualquier otra ruta donde
 * no encaje otro paso de la etapa; es el paso de "ve hasta aquí"). [clave]: elemento a iluminar.
 * [tocar]: hay que pulsar el elemento iluminado para continuar.
 */
class PasoTour(
    val donde: Set<String>?,
    val clave: String?,
    @StringRes val titulo: Int,
    @StringRes val texto: Int,
    val tocar: Boolean = false
)

class EtapaTour(val pasos: List<PasoTour>)

/** Qué paso de la etapa toca mostrar según la ruta en la que está el usuario (null = ninguno). */
private fun pasoSegunRuta(etapa: EtapaTour, actual: Int, ruta: String): Int? {
    if (etapa.pasos[actual].donde?.contains(ruta) == true) return actual
    for (i in actual + 1..etapa.pasos.lastIndex) if (etapa.pasos[i].donde?.contains(ruta) == true) return i
    for (i in actual - 1 downTo 0) if (etapa.pasos[i].donde?.contains(ruta) == true) return i
    return etapa.pasos.indexOfFirst { it.donde == null }.takeIf { it >= 0 }
}

@Composable
fun TourInicioHost(
    rutaActual: String?,
    ayuda: AyudaState,
    etapas: List<EtapaTour>,
    activo: Boolean,
    /** Al acabar o saltar el tour: deja la app en la pantalla principal (Semana). */
    alFinalizar: () -> Unit = {}
) {
    val contexto = LocalContext.current
    var etapa by remember { mutableIntStateOf(PreferenciaTour.etapa(contexto)) }
    var paso by remember { mutableIntStateOf(0) }

    // "Ver los consejos otra vez": las preferencias ya se han reiniciado; se empieza de cero.
    LaunchedEffect(ayuda.reinicios) {
        if (ayuda.reinicios > 0) {
            etapa = 0
            paso = 0
        }
    }

    LaunchedEffect(etapa, paso, rutaActual, activo, ayuda.reinicios) {
        if (!activo || rutaActual == null || etapa >= etapas.size) return@LaunchedEffect
        val actual = etapas[etapa]
        val idx = pasoSegunRuta(actual, paso.coerceIn(0, actual.pasos.lastIndex), rutaActual)
        if (idx == null) {
            ayuda.cerrar() // en esta pantalla no hay nada que enseñar de esta etapa
            return@LaunchedEffect
        }
        if (idx != paso) {
            paso = idx
            return@LaunchedEffect // el cambio de paso vuelve a lanzar este efecto
        }
        // Si había un globo de otra pantalla, se quita antes de esperar a que ésta se dibuje.
        if (ayuda.activa) ayuda.cerrar()
        delay(800) // deja que termine de dibujarse la pantalla y se midan los elementos
        val p = actual.pasos[paso]
        // Si el elemento queda fuera de la pantalla (p. ej. al final de una lista), se desplaza hasta él.
        if (p.clave != null) {
            ayuda.traerAVista(p.clave)
            delay(400)
        }
        val esUltimaEtapa = etapa == etapas.size - 1
        ayuda.iniciar(
            listOf(PasoAyuda(p.clave, p.titulo, p.texto, p.tocar)),
            DatosTour(
                etapa = etapa,
                total = etapas.size,
                esUltimo = esUltimaEtapa && paso == actual.pasos.lastIndex,
                alTerminar = {
                    if (paso < actual.pasos.lastIndex) {
                        paso += 1
                    } else {
                        paso = 0
                        etapa += 1
                        PreferenciaTour.guardar(contexto, etapa)
                        if (etapa >= etapas.size) alFinalizar()
                    }
                },
                alSaltar = {
                    etapa = etapas.size
                    PreferenciaTour.guardar(contexto, etapa)
                    alFinalizar()
                }
            )
        )
    }
}
