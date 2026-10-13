package com.encaja.app.ui.common

// NOTA: depende de Jetpack Compose y AndroidX Core, no compilado en este entorno.
// Evita que una hoja o un diálogo con campos de texto se cierre (perdiendo lo escrito) al
// minimizar el teclado. Al ocultarlo, Android puede mandar al mismo tiempo un "atrás" o un
// cambio de tamaño de la ventana que la hoja toma por "cierra": si hay teclado, o acaba de
// cerrarse hace un instante, ese cierre no se atiende; solo se oculta el teclado. Cerrar con
// la X, con Guardar o con otro "atrás" posterior sigue funcionando igual.

import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GuardaTeclado {
    private var vista: View? = null
    private var ultimaVezVisibleMs = 0L

    /** Oculta el teclado; lo rellena [ObservarTeclado] desde dentro de la ventana de la hoja. */
    var ocultar: () -> Unit = {}

    internal fun enlazar(v: View?) {
        vista = v
    }

    private fun tecladoVisibleAhora(): Boolean =
        vista?.let { ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) } == true

    internal fun revisar() {
        if (tecladoVisibleAhora()) ultimaVezVisibleMs = SystemClock.uptimeMillis()
    }

    /** true si hay teclado o se acaba de cerrar: un cierre pedido en ese momento no es del usuario. */
    fun bloqueaCierre(): Boolean {
        if (tecladoVisibleAhora()) return true
        return SystemClock.uptimeMillis() - ultimaVezVisibleMs < VENTANA_MS
    }

    private companion object {
        const val VENTANA_MS = 700L
    }
}

/** Va dentro del contenido de la hoja o del diálogo, para vigilar el teclado de SU ventana. */
@Composable
fun ObservarTeclado(guarda: GuardaTeclado) {
    val vista = LocalView.current
    val controlador = LocalSoftwareKeyboardController.current
    DisposableEffect(vista, controlador) {
        guarda.enlazar(vista)
        guarda.ocultar = { controlador?.hide() }
        val oyente = ViewTreeObserver.OnGlobalLayoutListener { guarda.revisar() }
        vista.viewTreeObserver.addOnGlobalLayoutListener(oyente)
        onDispose {
            if (vista.viewTreeObserver.isAlive) vista.viewTreeObserver.removeOnGlobalLayoutListener(oyente)
            guarda.enlazar(null)
        }
    }
}

@Composable
fun rememberGuardaTeclado(): GuardaTeclado = remember { GuardaTeclado() }
