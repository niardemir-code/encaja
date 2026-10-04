package com.encaja.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.encaja.app.domain.repository.AuthRepository
import com.encaja.app.domain.repository.DispositivoRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Decide qué pantalla mostrar: login o el resto de la app. Comprueba la
 * sesión al arrancar (síncrono, es solo leer un dato ya guardado por el
 * SDK de Firebase, sin llamada de red) y se actualiza cuando el login
 * o el cierre de sesión tienen éxito.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispositivos: DispositivoRepository
) : ViewModel() {

    private val _autenticado = MutableStateFlow(authRepository.sesionActual() != null)
    val autenticado: StateFlow<Boolean> = _autenticado.asStateFlow()

    fun marcarAutenticado() {
        _autenticado.value = true
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            // Antes de cerrar la sesión (hace falta para borrar el token): este móvil deja de
            // recibir las notificaciones de la cuenta.
            runCatching { dispositivos.olvidarDispositivoActual() }
            authRepository.cerrarSesion()
            _autenticado.value = false
        }
    }
}
