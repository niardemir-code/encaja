package com.encaja.app.ui.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Canal global de errores de las acciones del usuario (guardar, borrar...). La pantalla raíz
 * ([com.encaja.app.ui.EncajaApp]) lo escucha y muestra un aviso.
 */
object ErroresDeUi {
    private val _eventos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val eventos: SharedFlow<Unit> = _eventos.asSharedFlow()

    fun avisar() {
        _eventos.tryEmit(Unit)
    }
}

/**
 * Como `viewModelScope.launch`, pero un fallo (red, permisos de Firestore...) no cierra la app:
 * se registra y, si [avisar] es true, se muestra un aviso al usuario. La cancelación de la
 * corrutina se respeta (se relanza), porque no es un error.
 */
fun ViewModel.lanzarSeguro(avisar: Boolean = true, bloque: suspend CoroutineScope.() -> Unit): Job =
    viewModelScope.launch {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w("Encaja", "Fallo en ${this@lanzarSeguro::class.simpleName}", e)
            if (avisar) ErroresDeUi.avisar()
        }
    }
